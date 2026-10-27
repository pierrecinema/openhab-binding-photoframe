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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link FrameConfiguration} class contains fields mapping the photo frame thing configuration parameters.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class FrameConfiguration {

    public static final String SOURCE_LOCAL = "LOCAL";
    public static final String SOURCE_GOOGLE_DRIVE = "GOOGLE_DRIVE";
    public static final String SOURCE_DROPBOX_LINK = "DROPBOX_LINK";
    public static final String SOURCE_DROPBOX = "DROPBOX";

    // Source
    public String sourceType = SOURCE_LOCAL;
    public String localFolder = "";
    public String link = "";
    public String apiKey = "";
    public String folder = "";
    public int refreshMinutes = 0;

    // Slideshow
    public int interval = 30;
    public String order = "RANDOM";
    public String transition = "FADE";
    public double transitionSeconds = 1.5;
    public boolean kenBurns = true;
    public String fitMode = "BLUR";
    public int maxSize = 1920;
    public String tapUrl = "";
    /** Address of openHAB as seen from the screens, e.g. http://192.168.1.10:8080; empty detects it. */
    public String baseUrl = "";
    public int cacheSizeMb = 500;

    // Overlays
    public String clock = "NONE";
    public String overlayItems = "";
    public String caption = "NONE";

    // Night mode
    public String nightMode = "OFF";
    public String nightStart = "22:00";
    public String nightEnd = "06:30";
}
