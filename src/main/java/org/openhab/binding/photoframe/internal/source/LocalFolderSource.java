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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link LocalFolderSource} reads the pictures of a folder on the openHAB server. Sub folders are ignored.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class LocalFolderSource implements ImageSource {

    private final Path folder;

    public LocalFolderSource(Path folder) {
        this.folder = folder;
    }

    public Path getFolder() {
        return folder;
    }

    @Override
    public List<SourceImage> list() throws SourceException {
        if (!Files.isDirectory(folder)) {
            throw new SourceException("Folder " + folder + " does not exist", true);
        }
        List<SourceImage> images = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Path fileName = file.getFileName();
                if (fileName == null || !ImageSource.isSupported(fileName.toString())) {
                    continue;
                }
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                if (attributes.isRegularFile()) {
                    String name = fileName.toString();
                    images.add(new SourceImage(name, name,
                            attributes.lastModifiedTime().toMillis() + "-" + attributes.size()));
                }
            }
        } catch (IOException e) {
            throw new SourceException("Cannot read folder " + folder + ": " + e.getMessage(), false, e);
        }
        return images;
    }

    @Override
    public InputStream open(SourceImage image) throws SourceException, IOException {
        Path file = folder.resolve(image.id()).normalize();
        if (!file.startsWith(folder.normalize())) {
            throw new SourceException("Invalid picture " + image.id(), true);
        }
        return Files.newInputStream(file);
    }

    @Override
    public int defaultRefreshMinutes() {
        return 5;
    }
}
