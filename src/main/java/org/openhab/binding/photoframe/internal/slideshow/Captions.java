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
package org.openhab.binding.photoframe.internal.slideshow;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Builds a caption from a file name: "012_Strand Mallorca.jpg" becomes "Strand Mallorca".
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public final class Captions {

    /** A sort number such as "012_" in front of a word; dates like "2024-07-01" are kept. */
    private static final Pattern LEADING_NUMBER = Pattern.compile("^\\d+[\\s_.\\-]+(?=\\p{L})");

    private Captions() {
    }

    public static String fromFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String caption = LEADING_NUMBER.matcher(base).replaceFirst("").replace('_', ' ').replaceAll("\\s+", " ").trim();
        return caption.isEmpty() ? base : caption;
    }
}
