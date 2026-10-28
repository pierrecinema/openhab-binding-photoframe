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
package org.openhab.binding.photoframe.internal.servlet;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * State of a photo frame as sent to the slideshow page (serialized with Gson).
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class FrameState {

    public long seq;
    public int count;
    public boolean paused;
    public boolean night;
    public @Nullable String folder;
    public @Nullable Picture image;
    public @Nullable Picture upcoming;
    public Settings settings = new Settings();
    public List<Overlay> overlay = new ArrayList<>();

    public static class Picture {
        public String key;
        public String name;
        public @Nullable String caption;

        public Picture(String key, String name, @Nullable String caption) {
            this.key = key;
            this.name = name;
            this.caption = caption;
        }
    }

    public static class Settings {
        public int interval;
        public String transition = "FADE";
        public double transitionSeconds;
        public boolean kenBurns;
        public String fitMode = "BLUR";
        public String clock = "NONE";
        public String nightMode = "OFF";
        public @Nullable String tapUrl;
        public int returnSeconds;
    }

    public static class Overlay {
        public String label;
        public String value;

        public Overlay(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
