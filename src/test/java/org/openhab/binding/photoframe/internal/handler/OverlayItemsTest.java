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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;

/**
 * Tests for the overlay settings of {@link PhotoFrameHandler}.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
class OverlayItemsTest {

    @Test
    void itemPickerListIsUsed() {
        assertEquals(List.of("Aussen_Temp", "Wohnzimmer_Temp"),
                PhotoFrameHandler.overlayItemNames(List.of("Aussen_Temp", " Wohnzimmer_Temp ")));
    }

    @Test
    void oldCommaSeparatedTextStillWorks() {
        assertEquals(List.of("A", "B"), PhotoFrameHandler.overlayItemNames("A, B,,"));
    }

    @Test
    void atMostFourDistinctItems() {
        assertEquals(List.of("A", "B", "C", "D"),
                PhotoFrameHandler.overlayItemNames(List.of("A", "B", "A", "C", "D", "E")));
        assertTrue(PhotoFrameHandler.overlayItemNames(null).isEmpty());
    }

    @Test
    void statesAreFormattedWithThePattern() {
        // decimal separator depends on the system language (21.7 / 21,7)
        assertTrue(PhotoFrameHandler.formatState(new QuantityType<>("21.68 °C"), "%.1f %unit%").matches("21[.,]7 °C"));
        assertEquals("42", PhotoFrameHandler.formatState(new DecimalType(42), null));
        assertEquals("–", PhotoFrameHandler.formatState(UnDefType.NULL, "%.1f"));
        assertEquals("7", PhotoFrameHandler.formatState(new DecimalType(7), "%s %d broken"));
    }

    @Test
    void optionsGiveTheTextOfAState() {
        List<StateOption> options = List.of(new StateOption("FULL", "Vollmond"),
                new StateOption("WANING_GIBBOUS", "Abnehmender Mond"));

        assertEquals("Abnehmender Mond", PhotoFrameHandler.optionLabel(new StringType("WANING_GIBBOUS"), options));
        assertNull(PhotoFrameHandler.optionLabel(new StringType("NEW"), options));
    }

    @Test
    void imageItemsBecomeDataAddresses() {
        assertEquals("data:image/png;base64,AQID",
                PhotoFrameHandler.imageData(new RawType(new byte[] { 1, 2, 3 }, "image/png")));
        assertNull(PhotoFrameHandler.imageData(new StringType("Bewölkt")));
    }
}
