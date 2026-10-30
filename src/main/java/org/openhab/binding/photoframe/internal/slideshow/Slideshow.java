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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.photoframe.internal.source.SourceImage;

/**
 * The {@link Slideshow} decides which picture is shown. It runs on the server, so every screen shows the same
 * picture and commands from openHAB act on all screens.
 * <p>
 * In random order every picture is shown once before any picture is repeated; a new round never starts with the
 * picture that ended the previous round.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class Slideshow {

    public enum Order {
        RANDOM,
        NAME
    }

    private final Order order;
    private final Random random;

    private List<SourceImage> sequence = new ArrayList<>();
    private List<SourceImage> previousRound = new ArrayList<>();
    private int position;
    private boolean paused;
    private long changeCounter;

    public Slideshow(Order order) {
        this(order, new Random());
    }

    Slideshow(Order order, Random random) {
        this.order = order;
        this.random = random;
    }

    /**
     * Replaces the pictures after the folder was read. The current picture stays on screen if it still exists.
     */
    public synchronized void setImages(List<SourceImage> images) {
        SourceImage current = current();
        Set<String> newIds = new HashSet<>();
        images.forEach(i -> newIds.add(i.id()));
        Set<String> oldIds = new HashSet<>();
        sequence.forEach(i -> oldIds.add(i.id()));

        if (order == Order.NAME) {
            List<SourceImage> sorted = new ArrayList<>(images);
            sorted.sort((a, b) -> NaturalOrder.compare(a.name(), b.name()));
            sequence = sorted;
        } else if (!newIds.equals(oldIds) || !sameVersions(images)) {
            // Keep the pictures already shown in this round where they are, mix the new ones into the rest.
            List<SourceImage> shown = new ArrayList<>();
            for (int i = 0; i <= position && i < sequence.size(); i++) {
                SourceImage image = find(images, sequence.get(i).id());
                if (image != null) {
                    shown.add(image);
                }
            }
            List<SourceImage> rest = new ArrayList<>();
            for (SourceImage image : images) {
                if (shown.stream().noneMatch(s -> s.id().equals(image.id()))) {
                    rest.add(image);
                }
            }
            Collections.shuffle(rest, random);
            shown.addAll(rest);
            sequence = shown;
            previousRound = new ArrayList<>();
        } else {
            return;
        }

        int index = current != null ? indexOf(current.id()) : -1;
        position = index >= 0 ? index : 0;
        if (index < 0) {
            changeCounter++;
        }
    }

    public synchronized @Nullable SourceImage current() {
        return position < sequence.size() ? sequence.get(position) : null;
    }

    /**
     * Returns the picture that will be shown next, so the screen can load it in advance.
     */
    public synchronized @Nullable SourceImage upcoming() {
        if (sequence.size() < 2) {
            return null;
        }
        return position + 1 < sequence.size() ? sequence.get(position + 1) : null;
    }

    public synchronized void next() {
        if (sequence.isEmpty()) {
            return;
        }
        if (position + 1 < sequence.size()) {
            position++;
        } else {
            startNewRound();
        }
        changeCounter++;
    }

    public synchronized void previous() {
        if (sequence.isEmpty()) {
            return;
        }
        if (position == 0 && !previousRound.isEmpty()) {
            sequence = previousRound;
            previousRound = new ArrayList<>();
        }
        position = position > 0 ? position - 1 : sequence.size() - 1;
        changeCounter++;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
    }

    public synchronized int size() {
        return sequence.size();
    }

    /**
     * Increases whenever another picture is shown; the screens use it to notice a change.
     */
    public synchronized long getChangeCounter() {
        return changeCounter;
    }

    synchronized List<SourceImage> sequence() {
        return List.copyOf(sequence);
    }

    private void startNewRound() {
        if (order == Order.RANDOM && sequence.size() > 1) {
            SourceImage last = sequence.get(sequence.size() - 1);
            List<SourceImage> next = new ArrayList<>(sequence);
            Collections.shuffle(next, random);
            if (next.get(0).id().equals(last.id())) {
                Collections.swap(next, 0, 1 + random.nextInt(next.size() - 1));
            }
            previousRound = sequence;
            sequence = next;
        }
        position = 0;
    }

    private boolean sameVersions(List<SourceImage> images) {
        for (SourceImage image : images) {
            SourceImage old = find(sequence, image.id());
            if (old == null || !old.version().equals(image.version())) {
                return false;
            }
        }
        return true;
    }

    private int indexOf(String id) {
        for (int i = 0; i < sequence.size(); i++) {
            if (sequence.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static @Nullable SourceImage find(List<SourceImage> images, String id) {
        for (SourceImage image : images) {
            if (image.id().equals(id)) {
                return image;
            }
        }
        return null;
    }
}
