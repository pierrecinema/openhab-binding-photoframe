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

import java.math.BigInteger;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Compares file names like a person would: numbers are compared by value, so "2.jpg" comes before "10.jpg".
 * Letters are compared without regard to case.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public final class NaturalOrder {

    private NaturalOrder() {
    }

    public static int compare(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int endA = digitsEnd(a, i);
                int endB = digitsEnd(b, j);
                int result = new BigInteger(a.substring(i, endA)).compareTo(new BigInteger(b.substring(j, endB)));
                if (result != 0) {
                    return result;
                }
                i = endA;
                j = endB;
            } else {
                int result = Character.toString(ca).toLowerCase(Locale.ROOT)
                        .compareTo(Character.toString(cb).toLowerCase(Locale.ROOT));
                if (result != 0) {
                    return result;
                }
                i++;
                j++;
            }
        }
        int result = Integer.compare(a.length() - i, b.length() - j);
        return result != 0 ? result : a.compareTo(b);
    }

    private static int digitsEnd(String s, int start) {
        int end = start;
        while (end < s.length() && Character.isDigit(s.charAt(end))) {
            end++;
        }
        return end;
    }
}
