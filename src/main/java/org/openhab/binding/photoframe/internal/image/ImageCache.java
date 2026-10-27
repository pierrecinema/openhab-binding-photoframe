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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.photoframe.internal.source.ImageSource;
import org.openhab.binding.photoframe.internal.source.SourceException;
import org.openhab.binding.photoframe.internal.source.SourceImage;

/**
 * The {@link ImageCache} keeps display-ready copies of the pictures of one photo frame. Each picture is converted
 * once; the cache key contains the picture version and the target size, so changed pictures are converted again.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class ImageCache {

    /** Pictures larger than this are refused, to protect the memory of small devices. */
    static final int MAX_ORIGINAL_BYTES = 60 * 1024 * 1024;

    private static final String SUFFIX = ".jpg";

    private final Path directory;
    private final ImageSource source;
    private final int maxSize;
    private final long maxBytes;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public ImageCache(Path directory, ImageSource source, int maxSize, long maxBytes) throws IOException {
        this.directory = directory;
        this.source = source;
        this.maxSize = maxSize;
        this.maxBytes = maxBytes;
        Files.createDirectories(directory);
    }

    /**
     * Returns the cache key of a picture. It is also used in the picture address of the slideshow page.
     */
    public String key(SourceImage image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest
                    .digest((image.id() + "\n" + image.version() + "\n" + maxSize).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Returns the converted picture, converting it first if needed.
     */
    public Path get(SourceImage image) throws IOException, SourceException {
        String key = key(image);
        Path file = directory.resolve(key + SUFFIX);
        if (Files.isRegularFile(file)) {
            touch(file);
            return file;
        }
        Object lock = Objects.requireNonNull(locks.computeIfAbsent(key, k -> new Object()));
        synchronized (lock) {
            try {
                if (Files.isRegularFile(file)) {
                    return file;
                }
                byte[] data;
                try (InputStream in = source.open(image)) {
                    data = in.readNBytes(MAX_ORIGINAL_BYTES + 1);
                }
                if (data.length > MAX_ORIGINAL_BYTES) {
                    throw new IOException("Picture " + image.name() + " is larger than 60 MB");
                }
                Path temp = Files.createTempFile(directory, key, ".tmp");
                try {
                    try (OutputStream out = Files.newOutputStream(temp)) {
                        ImageProcessor.process(data, maxSize, out);
                    }
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temp);
                }
            } finally {
                locks.remove(key);
            }
        }
        enforceLimit();
        return file;
    }

    /**
     * Deletes cached pictures that no longer belong to the given pictures (removed or changed pictures).
     */
    public void retain(Collection<SourceImage> images) {
        Set<String> keep = images.stream().map(i -> key(i) + SUFFIX).collect(Collectors.toSet());
        for (Path file : files()) {
            Path name = file.getFileName();
            if (name != null && !keep.contains(name.toString())) {
                deleteQuietly(file);
            }
        }
    }

    /**
     * Deletes the least recently used pictures while the cache is larger than its limit.
     */
    void enforceLimit() {
        List<Path> files = files();
        long total = files.stream().mapToLong(ImageCache::size).sum();
        if (total <= maxBytes) {
            return;
        }
        files.sort(Comparator.comparingLong(ImageCache::lastModified));
        for (Path file : files) {
            if (total <= maxBytes) {
                break;
            }
            total -= size(file);
            deleteQuietly(file);
        }
    }

    private List<Path> files() {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(p -> p.toString().endsWith(SUFFIX)).forEach(result::add);
        } catch (IOException e) {
            // empty or missing cache directory
        }
        return result;
    }

    private static void touch(Path file) {
        try {
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException e) {
            // only used for the least-recently-used order
        }
    }

    private static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private static long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // deleted on the next run
        }
    }
}
