/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.photoframe.internal.handler;

import static org.openhab.binding.photoframe.internal.PhotoFrameBindingConstants.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.photoframe.internal.action.PhotoFrameActions;
import org.openhab.binding.photoframe.internal.config.FrameConfiguration;
import org.openhab.binding.photoframe.internal.image.ImageCache;
import org.openhab.binding.photoframe.internal.servlet.FrameEndpoint;
import org.openhab.binding.photoframe.internal.servlet.FrameState;
import org.openhab.binding.photoframe.internal.servlet.PhotoFrameServlet;
import org.openhab.binding.photoframe.internal.slideshow.Captions;
import org.openhab.binding.photoframe.internal.slideshow.NightSchedule;
import org.openhab.binding.photoframe.internal.slideshow.Slideshow;
import org.openhab.binding.photoframe.internal.source.DropboxLinkSource;
import org.openhab.binding.photoframe.internal.source.ImageSource;
import org.openhab.binding.photoframe.internal.source.LocalFolderSource;
import org.openhab.binding.photoframe.internal.source.SourceException;
import org.openhab.binding.photoframe.internal.source.SourceImage;
import org.openhab.core.OpenHAB;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link PhotoFrameHandler} runs one photo frame: it reads the pictures of its source, keeps display-ready
 * copies in a cache and drives the slideshow shown on the slideshow page.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class PhotoFrameHandler extends BaseThingHandler implements FrameEndpoint {

    private static final int NIGHT_CHECK_SECONDS = 30;
    private static final int MAX_OVERLAY_ITEMS = 4;
    private static final int TOKEN_BYTES = 18;
    private static final String CONFIG_OVERLAY_ITEMS = "overlayItems";

    private final Logger logger = LoggerFactory.getLogger(PhotoFrameHandler.class);
    private final PhotoFrameServlet servlet;
    private final ItemRegistry itemRegistry;
    private final HttpClient httpClient;
    private final Supplier<String> defaultBaseUrl;
    private final Gson gson = new Gson();

    private FrameConfiguration config = new FrameConfiguration();
    private @Nullable ImageSource source;
    private @Nullable ImageCache cache;
    private Slideshow slideshow = new Slideshow(Slideshow.Order.RANDOM);
    private NightSchedule night = new NightSchedule(false, "22:00", "06:30");
    private volatile Map<String, SourceImage> imagesByKey = Map.of();
    private volatile boolean nightActive;
    private String token = "";

    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable ScheduledFuture<?> advanceJob;
    private @Nullable ScheduledFuture<?> nightJob;

    public PhotoFrameHandler(Thing thing, PhotoFrameServlet servlet, ItemRegistry itemRegistry, HttpClient httpClient,
            Supplier<String> defaultBaseUrl) {
        super(thing);
        this.servlet = servlet;
        this.itemRegistry = itemRegistry;
        this.httpClient = httpClient;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    @Override
    public void initialize() {
        config = getConfigAs(FrameConfiguration.class);
        slideshow = new Slideshow("NAME".equals(config.order) ? Slideshow.Order.NAME : Slideshow.Order.RANDOM);
        night = new NightSchedule(!"OFF".equals(config.nightMode), config.nightStart, config.nightEnd);

        Path cacheDir = Path.of(OpenHAB.getUserDataFolder(), "cache", BINDING_ID, getThing().getUID().getId());
        ImageSource source;
        try {
            source = createSource(cacheDir);
        } catch (SourceException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            return;
        }
        this.source = source;

        try {
            cache = new ImageCache(cacheDir, source, config.maxSize, config.cacheSizeMb * 1024L * 1024L);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.config-error.cache [\"" + cacheDir + "\"]");
            return;
        }

        token = getThing().getProperties().getOrDefault(PROPERTY_TOKEN, "");
        if (token.isBlank()) {
            token = newRandomToken();
            updateProperty(PROPERTY_TOKEN, token);
        }
        updateState(CHANNEL_PAGE_URL, new StringType(pageUrl()));

        servlet.register(getThing().getUID().getId(), this);
        updateStatus(ThingStatus.UNKNOWN);

        int refreshMinutes = config.refreshMinutes > 0 ? config.refreshMinutes : source.defaultRefreshMinutes();
        refreshJob = scheduler.scheduleWithFixedDelay(this::refresh, 0, refreshMinutes, TimeUnit.MINUTES);
        nightJob = scheduler.scheduleWithFixedDelay(this::checkNight, 0, NIGHT_CHECK_SECONDS, TimeUnit.SECONDS);
        restartAdvance();
    }

    @Override
    public void dispose() {
        servlet.unregister(getThing().getUID().getId(), this);
        cancel(refreshJob);
        cancel(advanceJob);
        cancel(nightJob);
        refreshJob = null;
        advanceJob = null;
        nightJob = null;
        source = null;
        cache = null;
        super.dispose();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(PhotoFrameActions.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        switch (channelUID.getId()) {
            case CHANNEL_CONTROL -> {
                if (command == PlayPauseType.PLAY) {
                    play();
                } else if (command == PlayPauseType.PAUSE) {
                    pause();
                } else if (command == NextPreviousType.NEXT) {
                    next();
                } else if (command == NextPreviousType.PREVIOUS) {
                    previous();
                } else if (command instanceof RefreshType) {
                    updateState(CHANNEL_CONTROL, slideshow.isPaused() ? PlayPauseType.PAUSE : PlayPauseType.PLAY);
                }
            }
            case CHANNEL_REFRESH -> {
                if (command == OnOffType.ON) {
                    refreshNow();
                    updateState(CHANNEL_REFRESH, OnOffType.OFF);
                }
            }
            case CHANNEL_NIGHT -> {
                if (command instanceof OnOffType onOff) {
                    night.override(onOff == OnOffType.ON, ZonedDateTime.now());
                    checkNight();
                } else if (command instanceof RefreshType) {
                    updateState(CHANNEL_NIGHT, OnOffType.from(nightActive));
                }
            }
            default -> {
                // read-only channels
            }
        }
    }

    // --- actions -------------------------------------------------------------------------------------------

    public void next() {
        slideshow.next();
        restartAdvance();
        publishCurrent();
    }

    public void previous() {
        slideshow.previous();
        restartAdvance();
        publishCurrent();
    }

    public void pause() {
        slideshow.setPaused(true);
        updateState(CHANNEL_CONTROL, PlayPauseType.PAUSE);
    }

    public void play() {
        slideshow.setPaused(false);
        restartAdvance();
        updateState(CHANNEL_CONTROL, PlayPauseType.PLAY);
    }

    public void refreshNow() {
        scheduler.execute(this::refresh);
    }

    /**
     * Creates a new access token; addresses with the old token stop working.
     */
    public String newToken() {
        token = newRandomToken();
        updateProperty(PROPERTY_TOKEN, token);
        updateState(CHANNEL_PAGE_URL, new StringType(pageUrl()));
        logger.info("Photo frame {}: new access token created, update the slideshow address on your screens",
                getThing().getUID());
        return pageUrl();
    }

    // --- slideshow -----------------------------------------------------------------------------------------

    private void refresh() {
        ImageSource source = this.source;
        ImageCache cache = this.cache;
        if (source == null || cache == null) {
            return;
        }
        try {
            List<SourceImage> images = source.list();
            boolean wasEmpty = slideshow.size() == 0;
            slideshow.setImages(images);
            Map<String, SourceImage> byKey = new HashMap<>();
            images.forEach(i -> byKey.put(cache.key(i), i));
            imagesByKey = byKey;
            cache.retain(images);

            updateState(CHANNEL_IMAGE_COUNT, new DecimalType(images.size()));
            updateState(CHANNEL_LAST_REFRESH, new DateTimeType());
            if (images.isEmpty()) {
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE,
                        "@text/online.empty-folder [\"" + describeFolder() + "\"]");
            } else {
                updateStatus(ThingStatus.ONLINE);
            }
            if (wasEmpty && !images.isEmpty()) {
                restartAdvance();
            }
            publishCurrent();
        } catch (SourceException e) {
            logger.debug("Photo frame {}: cannot read the pictures: {}", getThing().getUID(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, e.isConfigurationError() ? ThingStatusDetail.CONFIGURATION_ERROR
                    : ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
    }

    private void advance() {
        if (slideshow.isPaused() || nightActive || slideshow.size() < 2) {
            return;
        }
        slideshow.next();
        publishCurrent();
    }

    private synchronized void restartAdvance() {
        cancel(advanceJob);
        int interval = Math.max(5, config.interval);
        advanceJob = scheduler.scheduleWithFixedDelay(this::advance, interval, interval, TimeUnit.SECONDS);
    }

    private void publishCurrent() {
        SourceImage current = slideshow.current();
        updateState(CHANNEL_CURRENT_IMAGE, current != null ? new StringType(current.name()) : UnDefType.UNDEF);
        prepare(current);
        prepare(slideshow.upcoming());
    }

    /**
     * Converts a picture in the background, so it is ready when the screen asks for it.
     */
    private void prepare(@Nullable SourceImage image) {
        ImageCache cache = this.cache;
        if (image == null || cache == null) {
            return;
        }
        scheduler.execute(() -> {
            try {
                cache.get(image);
            } catch (IOException | SourceException e) {
                logger.debug("Photo frame {}: picture {} cannot be shown: {}", getThing().getUID(), image.name(),
                        e.getMessage());
            }
        });
    }

    private void checkNight() {
        boolean active = night.isNight(ZonedDateTime.now());
        if (active != nightActive) {
            nightActive = active;
            logger.debug("Photo frame {}: night mode {}", getThing().getUID(), active ? "on" : "off");
        }
        updateState(CHANNEL_NIGHT, OnOffType.from(active));
    }

    // --- servlet -------------------------------------------------------------------------------------------

    @Override
    public boolean isValidToken(@Nullable String requestToken) {
        if (requestToken == null || token.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(requestToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String stateJson() {
        ImageCache cache = this.cache;
        FrameState state = new FrameState();
        state.seq = slideshow.getChangeCounter();
        state.count = slideshow.size();
        state.paused = slideshow.isPaused();
        state.night = nightActive;
        if (FrameConfiguration.SOURCE_LOCAL.equals(config.sourceType)) {
            state.folder = describeFolder();
        }
        SourceImage current = slideshow.current();
        SourceImage upcoming = slideshow.upcoming();
        if (cache != null && current != null) {
            state.image = new FrameState.Picture(cache.key(current), current.name(),
                    "FILENAME".equals(config.caption) ? Captions.fromFileName(current.name()) : null);
        }
        if (cache != null && upcoming != null) {
            state.upcoming = new FrameState.Picture(cache.key(upcoming), upcoming.name(), null);
        }
        state.settings.interval = config.interval;
        state.settings.transition = config.transition;
        state.settings.transitionSeconds = config.transitionSeconds;
        state.settings.kenBurns = config.kenBurns;
        state.settings.fitMode = config.fitMode;
        state.settings.clock = config.clock;
        state.settings.nightMode = "OFF".equals(config.nightMode) ? "BLACK" : config.nightMode;
        String tapTarget = config.tapPage.isBlank() ? config.tapUrl : config.tapPage;
        state.settings.tapUrl = tapTarget.isBlank() ? null : tapTarget.trim();
        state.settings.returnSeconds = Math.max(0, config.returnSeconds);
        state.overlay = overlayValues();
        state.weather = weatherValues();
        return gson.toJson(state);
    }

    @Override
    public @Nullable Path image(String key) throws Exception {
        ImageCache cache = this.cache;
        SourceImage image = imagesByKey.get(key);
        return cache != null && image != null ? cache.get(image) : null;
    }

    private List<FrameState.Overlay> overlayValues() {
        return overlayItemNames(getThing().getConfiguration().get(CONFIG_OVERLAY_ITEMS)).stream()
                .map(this::overlayValue).toList();
    }

    /**
     * Returns the configured overlay item names. The setting is a list (item picker); test versions up to 0.0.3
     * stored a comma-separated text, which is still accepted.
     */
    static List<String> overlayItemNames(@Nullable Object value) {
        Stream<String> names;
        if (value instanceof Collection<?> collection) {
            names = collection.stream().map(String::valueOf);
        } else if (value instanceof String text) {
            names = Arrays.stream(text.split(","));
        } else {
            return List.of();
        }
        return names.map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(MAX_OVERLAY_ITEMS).toList();
    }

    private FrameState.Overlay overlayValue(String itemName) {
        try {
            Item item = itemRegistry.getItem(itemName);
            String image = imageData(item.getState());
            if (image != null) {
                return new FrameState.Overlay(image);
            }
            String label = item.getLabel();
            return new FrameState.Overlay(label != null ? label : itemName, format(item));
        } catch (ItemNotFoundException e) {
            return new FrameState.Overlay(itemName, "?");
        }
    }

    private FrameState.@Nullable Weather weatherValues() {
        Item icon = weatherItem(config.weatherIconItem);
        Item temperature = weatherItem(config.weatherTemperatureItem);
        Item text = weatherItem(config.weatherTextItem);
        if (icon == null && temperature == null && text == null) {
            return null;
        }
        FrameState.Weather weather = new FrameState.Weather();
        weather.icon = icon != null ? imageData(icon.getState()) : null;
        weather.temperature = temperature != null ? format(temperature) : null;
        weather.text = text != null ? format(text) : null;
        return weather;
    }

    private @Nullable Item weatherItem(String itemName) {
        return itemName.isBlank() ? null : itemRegistry.get(itemName.trim());
    }

    /**
     * Returns the picture of an image item as data address for an img element, null for any other state.
     */
    static @Nullable String imageData(State state) {
        return state instanceof RawType picture ? picture.toFullString() : null;
    }

    static String formatState(State state, @Nullable String pattern) {
        if (state instanceof UnDefType) {
            return "–";
        }
        if (pattern != null && !pattern.isBlank()) {
            try {
                return state.format(pattern);
            } catch (IllegalArgumentException e) { // includes IllegalFormatException
                // fall back to the plain state
            }
        }
        return state.toString();
    }

    /**
     * Returns the text of the item for its state, e.g. "Waning gibbous" for WANING_GIBBOUS, as openHAB shows it.
     */
    static @Nullable String optionLabel(State state, List<StateOption> options) {
        String value = state.toString();
        for (StateOption option : options) {
            if (option.getValue().equals(value)) {
                return option.getLabel();
            }
        }
        return null;
    }

    private static String format(Item item) {
        StateDescription description = item.getStateDescription();
        if (description == null) {
            return formatState(item.getState(), null);
        }
        String label = optionLabel(item.getState(), description.getOptions());
        return label != null ? label : formatState(item.getState(), description.getPattern());
    }

    // --- helpers -------------------------------------------------------------------------------------------

    private ImageSource createSource(Path cacheDir) throws SourceException {
        switch (config.sourceType) {
            case FrameConfiguration.SOURCE_DROPBOX_LINK:
                if (config.link.isBlank()) {
                    throw new SourceException("@text/offline.config-error.dropbox-link-missing", true);
                }
                // The pictures of the ZIP are kept next to the converted copies
                return new DropboxLinkSource(httpClient, config.link, cacheDir.resolve("source"));
            case FrameConfiguration.SOURCE_LOCAL:
                Path folder = getLocalFolder();
                try {
                    Files.createDirectories(folder);
                } catch (IOException e) {
                    throw new SourceException("@text/offline.config-error.local-folder [\"" + folder + "\"]", true, e);
                }
                updateProperty(PROPERTY_LOCAL_FOLDER, folder.toString());
                return new LocalFolderSource(folder);
            default:
                throw new SourceException("@text/offline.config-error.source-not-available", true);
        }
    }

    /**
     * Returns the configured local folder, or userdata/photoframe when none is configured.
     */
    Path getLocalFolder() {
        if (!config.localFolder.isBlank()) {
            return Path.of(config.localFolder.trim());
        }
        return Path.of(OpenHAB.getUserDataFolder(), BINDING_ID);
    }

    private String describeFolder() {
        return source instanceof LocalFolderSource local ? local.getFolder().toString() : "";
    }

    private String pageUrl() {
        String base = config.baseUrl.isBlank() ? defaultBaseUrl.get() : config.baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + SERVLET_PATH + "/" + getThing().getUID().getId() + "/?token=" + token;
    }

    private static String newRandomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void cancel(@Nullable ScheduledFuture<?> job) {
        if (job != null) {
            job.cancel(false);
        }
    }
}
