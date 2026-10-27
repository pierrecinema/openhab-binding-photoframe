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
package org.openhab.binding.photoframe.internal.image;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Reads the EXIF orientation tag (0x0112) of a JPEG file. Phones store pictures unrotated and only set this tag, so
 * without it portrait photos would be shown on their side.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public final class ExifOrientation {

    /** Orientation value for "no transformation needed". */
    public static final int NORMAL = 1;

    private static final int TAG_ORIENTATION = 0x0112;
    private static final int MARKER_SOS = 0xDA;
    private static final int MARKER_APP1 = 0xE1;

    private ExifOrientation() {
    }

    /**
     * Returns the EXIF orientation (1–8) of the given JPEG data, or {@link #NORMAL} if there is none or the data is
     * not a JPEG file.
     */
    public static int read(byte[] data) {
        if (data.length < 4 || (data[0] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xD8) {
            return NORMAL;
        }
        int pos = 2;
        while (pos + 4 <= data.length) {
            if ((data[pos] & 0xFF) != 0xFF) {
                return NORMAL;
            }
            int marker = data[pos + 1] & 0xFF;
            if (marker == MARKER_SOS) {
                return NORMAL;
            }
            int length = readShort(data, pos + 2, true);
            if (length < 2 || pos + 2 + length > data.length) {
                return NORMAL;
            }
            if (marker == MARKER_APP1 && isExif(data, pos + 4)) {
                return readTiff(data, pos + 10, pos + 2 + length);
            }
            pos += 2 + length;
        }
        return NORMAL;
    }

    private static boolean isExif(byte[] data, int pos) {
        return pos + 6 <= data.length && data[pos] == 'E' && data[pos + 1] == 'x' && data[pos + 2] == 'i'
                && data[pos + 3] == 'f' && data[pos + 4] == 0 && data[pos + 5] == 0;
    }

    private static int readTiff(byte[] data, int tiff, int end) {
        if (tiff + 8 > end) {
            return NORMAL;
        }
        boolean bigEndian;
        if (data[tiff] == 'M' && data[tiff + 1] == 'M') {
            bigEndian = true;
        } else if (data[tiff] == 'I' && data[tiff + 1] == 'I') {
            bigEndian = false;
        } else {
            return NORMAL;
        }
        long ifdOffset = readInt(data, tiff + 4, bigEndian);
        int ifd = (int) (tiff + ifdOffset);
        if (ifdOffset < 8 || ifd + 2 > end) {
            return NORMAL;
        }
        int count = readShort(data, ifd, bigEndian);
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > end) {
                return NORMAL;
            }
            if (readShort(data, entry, bigEndian) == TAG_ORIENTATION) {
                int value = readShort(data, entry + 8, bigEndian);
                return value >= 1 && value <= 8 ? value : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int readShort(byte[] data, int pos, boolean bigEndian) {
        int b0 = data[pos] & 0xFF;
        int b1 = data[pos + 1] & 0xFF;
        return bigEndian ? (b0 << 8) | b1 : (b1 << 8) | b0;
    }

    private static long readInt(byte[] data, int pos, boolean bigEndian) {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            int b = data[pos + (bigEndian ? i : 3 - i)] & 0xFF;
            value = (value << 8) | b;
        }
        return value;
    }
}
