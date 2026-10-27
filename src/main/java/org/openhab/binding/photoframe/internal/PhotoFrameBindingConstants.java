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
package org.openhab.binding.photoframe.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link PhotoFrameBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class PhotoFrameBindingConstants {

    public static final String BINDING_ID = "photoframe";

    // Thing types
    public static final ThingTypeUID THING_TYPE_DROPBOX = new ThingTypeUID(BINDING_ID, "dropbox");
    public static final ThingTypeUID THING_TYPE_FRAME = new ThingTypeUID(BINDING_ID, "frame");

    /** The Dropbox bridge is offered once the Dropbox login is available. */
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_FRAME);

    // Frame channels
    public static final String CHANNEL_CONTROL = "control";
    public static final String CHANNEL_CURRENT_IMAGE = "currentImage";
    public static final String CHANNEL_IMAGE_COUNT = "imageCount";
    public static final String CHANNEL_PAGE_URL = "pageUrl";
    public static final String CHANNEL_LAST_REFRESH = "lastRefresh";
    public static final String CHANNEL_REFRESH = "refresh";
    public static final String CHANNEL_NIGHT = "night";

    // Frame properties
    public static final String PROPERTY_TOKEN = "token";
    public static final String PROPERTY_LOCAL_FOLDER = "localFolder";

    /** Path of the servlet serving the slideshow pages. */
    public static final String SERVLET_PATH = "/photoframe";
}
