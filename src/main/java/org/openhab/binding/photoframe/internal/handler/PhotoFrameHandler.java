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
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.photoframe.internal.config.FrameConfiguration;
import org.openhab.core.OpenHAB;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;

/**
 * The {@link PhotoFrameHandler} runs one photo frame: it reads the pictures of its source and drives the
 * slideshow shown on the slideshow page.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class PhotoFrameHandler extends BaseThingHandler {

    private FrameConfiguration config = new FrameConfiguration();

    public PhotoFrameHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(FrameConfiguration.class);

        if (FrameConfiguration.SOURCE_LOCAL.equals(config.sourceType)) {
            Path folder = getLocalFolder();
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "@text/offline.config-error.local-folder [\"" + folder + "\"]");
                return;
            }
            updateProperty(PROPERTY_LOCAL_FOLDER, folder.toString());
        }

        // The slideshow is implemented in milestone M2.
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Slideshow control is implemented in milestone M2.
    }

    /**
     * Returns the configured local folder, or userdata/photoframe/&lt;thing id&gt; when none is configured.
     */
    Path getLocalFolder() {
        if (!config.localFolder.isBlank()) {
            return Path.of(config.localFolder.trim());
        }
        return Path.of(OpenHAB.getUserDataFolder(), BINDING_ID, getThing().getUID().getId());
    }
}
