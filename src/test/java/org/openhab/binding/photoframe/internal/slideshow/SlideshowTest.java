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

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.photoframe.internal.source.SourceImage;

/**
 * Tests for {@link Slideshow}, {@link NaturalOrder}, {@link NightSchedule} and {@link Captions}.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
class SlideshowTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    @Test
    void randomShowsEveryPictureOncePerRound() {
        Slideshow show = new Slideshow(Slideshow.Order.RANDOM, new Random(42));
        show.setImages(images(10));
        for (int round = 0; round < 20; round++) {
            Set<String> seen = new HashSet<>();
            String last = null;
            for (int i = 0; i < 10; i++) {
                SourceImage current = show.current();
                assertNotNull(current);
                assertTrue(seen.add(current.id()), "no repetition within a round");
                last = current.id();
                show.next();
            }
            assertEquals(10, seen.size());
            SourceImage first = show.current();
            assertNotNull(first);
            assertNotEquals(last, first.id(), "a new round does not start with the last picture");
        }
    }

    @Test
    void nameOrderIsNatural() {
        Slideshow show = new Slideshow(Slideshow.Order.NAME);
        show.setImages(List.of(img("10.jpg"), img("2.jpg"), img("b.jpg"), img("A.jpg"), img("1.jpg")));
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SourceImage current = show.current();
            assertNotNull(current);
            names.add(current.name());
            show.next();
        }
        assertEquals(List.of("1.jpg", "2.jpg", "10.jpg", "A.jpg", "b.jpg"), names);
        SourceImage wrapped = show.current();
        assertNotNull(wrapped);
        assertEquals("1.jpg", wrapped.name(), "starts again at the beginning");
    }

    @Test
    void refreshKeepsCurrentPicture() {
        Slideshow show = new Slideshow(Slideshow.Order.RANDOM, new Random(1));
        show.setImages(images(5));
        show.next();
        SourceImage current = show.current();
        assertNotNull(current);
        long counter = show.getChangeCounter();

        List<SourceImage> more = new ArrayList<>(images(5));
        more.add(img("new.jpg"));
        show.setImages(more);
        assertEquals(current, show.current(), "current picture stays when pictures are added");
        assertEquals(counter, show.getChangeCounter(), "screens do not change the picture");
        assertEquals(6, show.size());

        show.setImages(List.of(img("other.jpg")));
        SourceImage replaced = show.current();
        assertNotNull(replaced);
        assertEquals("other.jpg", replaced.name(), "removed current picture is replaced");
        assertTrue(show.getChangeCounter() > counter);
    }

    @Test
    void previousAndEmpty() {
        Slideshow show = new Slideshow(Slideshow.Order.NAME);
        assertNull(show.current());
        show.next();
        show.previous();
        assertNull(show.current());

        show.setImages(List.of(img("1.jpg"), img("2.jpg"), img("3.jpg")));
        show.previous();
        SourceImage last = show.current();
        assertNotNull(last);
        assertEquals("3.jpg", last.name(), "previous of the first picture is the last one");
        SourceImage upcoming = show.upcoming();
        assertNull(upcoming, "the end of the round has no fixed upcoming picture");
    }

    @Test
    void previousReturnsToTheRoundBefore() {
        Slideshow show = new Slideshow(Slideshow.Order.RANDOM, new Random(7));
        show.setImages(images(6));
        for (int i = 0; i < 5; i++) {
            show.next();
        }
        SourceImage last = show.current();
        assertNotNull(last);
        show.next();
        show.previous();
        assertEquals(last, show.current(), "previous after a new round shows the last picture of the old round");
    }

    @Test
    void naturalOrder() {
        assertTrue(NaturalOrder.compare("2.jpg", "10.jpg") < 0);
        assertTrue(NaturalOrder.compare("img2", "img10") < 0);
        assertTrue(NaturalOrder.compare("a", "B") < 0);
        assertTrue(NaturalOrder.compare("photo", "photo1") < 0);
        assertEquals(0, NaturalOrder.compare("x.jpg", "x.jpg"));
        assertTrue(NaturalOrder.compare("99999999999999999999.jpg", "100000000000000000000.jpg") < 0);
    }

    @Test
    void nightScheduleOverMidnight() {
        NightSchedule night = new NightSchedule(true, "22:00", "06:30");
        assertTrue(night.isScheduledNight(LocalTime.of(23, 0)));
        assertTrue(night.isScheduledNight(LocalTime.of(2, 0)));
        assertTrue(night.isScheduledNight(LocalTime.of(22, 0)));
        assertFalse(night.isScheduledNight(LocalTime.of(6, 30)));
        assertFalse(night.isScheduledNight(LocalTime.of(12, 0)));

        NightSchedule day = new NightSchedule(true, "13:00", "15:00");
        assertTrue(day.isScheduledNight(LocalTime.of(14, 0)));
        assertFalse(day.isScheduledNight(LocalTime.of(16, 0)));

        assertFalse(new NightSchedule(true, "10:00", "10:00").isScheduledNight(LocalTime.of(10, 0)));
        assertFalse(new NightSchedule(false, "22:00", "06:30").isNight(at(23, 0)), "switched off");
    }

    @Test
    void nightOverrideLastsUntilNextBoundary() {
        NightSchedule night = new NightSchedule(true, "22:00", "06:30");
        ZonedDateTime evening = at(20, 0);
        night.override(true, evening);
        assertTrue(night.isNight(evening.plusMinutes(30)), "forced night");
        assertTrue(night.isNight(at(23, 0)), "scheduled night after the boundary");

        night.override(false, at(23, 0));
        assertFalse(night.isNight(at(23, 30)), "forced day until 06:30");
        assertFalse(night.isNight(at(23, 0).plusHours(8)), "day after 06:30");

        assertEquals(at(22, 0), night.nextBoundary(at(20, 0)));
        assertEquals(at(6, 30).plusDays(1), night.nextBoundary(at(23, 0)));
    }

    @Test
    void invalidNightTimesUseDefaults() {
        assertEquals(LocalTime.of(22, 0), NightSchedule.parse("bad", LocalTime.of(22, 0)));
        assertEquals(LocalTime.of(7, 5), NightSchedule.parse(" 7:05 ", LocalTime.MIDNIGHT));
    }

    @Test
    void captionsFromFileNames() {
        assertEquals("Strand Mallorca", Captions.fromFileName("012_Strand Mallorca.jpg"));
        assertEquals("Familie im Park", Captions.fromFileName("Familie_im_Park.JPG"));
        assertEquals("2024-07-01", Captions.fromFileName("2024-07-01.jpg"));
        assertEquals("IMG-20221121-WA0034", Captions.fromFileName("IMG-20221121-WA0034.jpg"));
        assertEquals("noext", Captions.fromFileName("noext"));
    }

    private static ZonedDateTime at(int hour, int minute) {
        return ZonedDateTime.of(2026, 10, 27, hour, minute, 0, 0, ZONE);
    }

    private static List<SourceImage> images(int count) {
        List<SourceImage> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(img(i + ".jpg"));
        }
        return list;
    }

    private static SourceImage img(String name) {
        return new SourceImage(name, name, "1");
    }
}
