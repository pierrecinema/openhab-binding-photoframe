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
package org.openhab.binding.photoframe.internal.source;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link ImageSource} provides the pictures of one folder (local, Google Drive, Dropbox, …).
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public interface ImageSource {

    /** File extensions of picture formats the JDK can decode. */
    Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp");

    /**
     * Lists the pictures currently in the folder.
     */
    List<SourceImage> list() throws SourceException;

    /**
     * Opens the original picture data. The caller closes the stream.
     */
    InputStream open(SourceImage image) throws SourceException, IOException;

    /**
     * Minutes between two reads of the folder when the user did not configure an interval.
     */
    int defaultRefreshMinutes();

    /**
     * Returns true if the file name has a supported picture extension.
     */
    static boolean isSupported(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 && !fileName.startsWith(".")
                && SUPPORTED_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
