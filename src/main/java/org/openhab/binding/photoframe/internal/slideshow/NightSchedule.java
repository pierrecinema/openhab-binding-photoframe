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

import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link NightSchedule} tells whether the night mode is active. The night may span midnight (22:00–06:30).
 * A manual override (from a rule or the night channel) stays active until the next start or end time.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class NightSchedule {

    private final boolean enabled;
    private final LocalTime start;
    private final LocalTime end;

    private @Nullable Boolean override;
    private @Nullable ZonedDateTime overrideUntil;

    /**
     * @param enabled false if the night mode is switched off; manual overrides still work
     * @param start start time, "HH:mm"
     * @param end end time, "HH:mm"
     */
    public NightSchedule(boolean enabled, String start, String end) {
        this.enabled = enabled;
        this.start = parse(start, LocalTime.of(22, 0));
        this.end = parse(end, LocalTime.of(6, 30));
    }

    public synchronized boolean isNight(ZonedDateTime now) {
        Boolean override = this.override;
        ZonedDateTime until = overrideUntil;
        if (override != null && until != null) {
            if (now.isBefore(until)) {
                return override;
            }
            this.override = null;
            overrideUntil = null;
        }
        return enabled && isScheduledNight(now.toLocalTime());
    }

    /**
     * Forces day or night until the next start or end time.
     */
    public synchronized void override(boolean night, ZonedDateTime now) {
        override = night;
        overrideUntil = nextBoundary(now);
    }

    boolean isScheduledNight(LocalTime time) {
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    ZonedDateTime nextBoundary(ZonedDateTime now) {
        ZonedDateTime nextStart = next(now, start);
        ZonedDateTime nextEnd = next(now, end);
        return nextStart.isBefore(nextEnd) ? nextStart : nextEnd;
    }

    private static ZonedDateTime next(ZonedDateTime now, LocalTime time) {
        ZonedDateTime candidate = now.with(time).withSecond(0).withNano(0);
        return candidate.isAfter(now) ? candidate : candidate.plusDays(1);
    }

    static LocalTime parse(String value, LocalTime fallback) {
        try {
            String[] parts = value.trim().split(":");
            return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
