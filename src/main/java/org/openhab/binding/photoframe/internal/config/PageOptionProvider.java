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
package org.openhab.binding.photoframe.internal.config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link PageOptionProvider} offers the Main UI pages and sitemaps as choices for the page opened by a tap, so no
 * address has to be typed in.
 *
 * @author Pierrecinema - Initial contribution
 */
@Component(service = ConfigOptionProvider.class)
@NonNullByDefault
public class PageOptionProvider implements ConfigOptionProvider {

    private static final String THING_CONFIG_URI = "thing-type:photoframe:frame";
    private static final String TAP_PAGE_PARAMETER = "tapPage";
    private static final String PAGE_NAMESPACE = "ui:page";
    private static final String OVERVIEW_UID = "overview";

    private static final String UI_SITEMAP_PREFIX = "uicomponents_";
    private static final String DEFAULT_SITEMAP = "_default";

    private static final String SITEMAP_NAMESPACE = "system:sitemap";
    private static final String SITEMAP_FILE_SUFFIX = ".sitemap";

    private final UIComponentRegistryFactory registryFactory;

    @Activate
    public PageOptionProvider(@Reference UIComponentRegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (!THING_CONFIG_URI.equals(uri.toString()) || !TAP_PAGE_PARAMETER.equals(param)) {
            return null;
        }
        List<ParameterOption> options = pageOptions(registryFactory.getRegistry(PAGE_NAMESPACE).getAll());
        // The sitemap services of openHAB changed between 5.x versions, so the sitemaps are looked up by their storage
        List<String> sitemaps = sitemapFiles(Path.of(OpenHAB.getConfigFolder(), "sitemaps"));
        registryFactory.getRegistry(SITEMAP_NAMESPACE).getAll()
                .forEach(sitemap -> sitemaps.add(UI_SITEMAP_PREFIX + sitemap.getUID()));
        options.addAll(sitemapOptions(sitemaps));
        return options;
    }

    static List<String> sitemapFiles(Path folder) {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return names;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.map(file -> file.getFileName().toString()).filter(name -> name.endsWith(SITEMAP_FILE_SUFFIX))
                    .forEach(name -> names.add(name.substring(0, name.length() - SITEMAP_FILE_SUFFIX.length())));
        } catch (IOException e) {
            // Only the choices in the settings are affected
        }
        return names;
    }

    static List<ParameterOption> pageOptions(Collection<RootUIComponent> pages) {
        List<ParameterOption> options = new ArrayList<>();
        for (RootUIComponent page : pages) {
            String uid = page.getUID();
            // The overview has its own address in Main UI
            String address = OVERVIEW_UID.equals(uid) ? "/overview/" : "/page/" + uid;
            options.add(new ParameterOption(address, label(page)));
        }
        options.sort(Comparator.comparing(ParameterOption::getLabel, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    static List<ParameterOption> sitemapOptions(Collection<String> names) {
        List<ParameterOption> options = new ArrayList<>();
        names.stream().filter(name -> !DEFAULT_SITEMAP.equals(name)).distinct().sorted().forEach(name -> {
            String shown = name.startsWith(UI_SITEMAP_PREFIX) ? name.substring(UI_SITEMAP_PREFIX.length()) : name;
            options.add(new ParameterOption("/basicui/app?sitemap=" + name, "Sitemap: " + shown));
        });
        return options;
    }

    private static String label(RootUIComponent page) {
        Object label = page.getConfig().get("label");
        return label instanceof String text && !text.isBlank() ? text : page.getUID();
    }
}
