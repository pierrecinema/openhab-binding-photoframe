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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link DropboxLinkSource}.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
class DropboxLinkSourceTest {

    @TempDir
    @NonNullByDefault({})
    Path tempDir;

    @Test
    void sharedFolderLinkBecomesZipDownload() throws SourceException {
        assertEquals("https://www.dropbox.com/scl/fo/abc/XYZ?rlkey=key1&st=s1&dl=1",
                DropboxLinkSource.toDownloadUrl("https://www.dropbox.com/scl/fo/abc/XYZ?rlkey=key1&st=s1&dl=0"));
        assertEquals("https://www.dropbox.com/scl/fo/abc/XYZ?rlkey=key1&dl=1",
                DropboxLinkSource.toDownloadUrl(" https://dropbox.com/scl/fo/abc/XYZ?rlkey=key1 "));
        assertEquals("https://www.dropbox.com/sh/old/folder?dl=1",
                DropboxLinkSource.toDownloadUrl("https://www.dropbox.com/sh/old/folder?raw=1"));
    }

    @Test
    void otherLinksAreRejected() {
        assertThrows(SourceException.class,
                () -> DropboxLinkSource.toDownloadUrl("https://www.dropbox.com/scl/fi/abc/picture.jpg?dl=0"));
        assertThrows(SourceException.class,
                () -> DropboxLinkSource.toDownloadUrl("https://drive.google.com/drive/folders/1EEe"));
        assertThrows(SourceException.class,
                () -> DropboxLinkSource.toDownloadUrl("http://www.dropbox.com/scl/fo/abc/XYZ"));
        assertThrows(SourceException.class,
                () -> DropboxLinkSource.toDownloadUrl("https://evil-dropbox.com/scl/fo/abc"));
        assertThrows(SourceException.class, () -> DropboxLinkSource.toDownloadUrl("not a link"));
    }

    @Test
    void topLevelNamesOnly() {
        assertEquals("a.jpg", DropboxLinkSource.topLevelName("/a.jpg"));
        assertEquals("a.jpg", DropboxLinkSource.topLevelName("a.jpg"));
        assertNull(DropboxLinkSource.topLevelName("/"));
        assertNull(DropboxLinkSource.topLevelName("sub/b.jpg"));
        assertNull(DropboxLinkSource.topLevelName("../../etc/passwd"));
        assertNull(DropboxLinkSource.topLevelName("..\\evil.jpg"));
        assertNull(DropboxLinkSource.topLevelName(".hidden.jpg"));
    }

    @Test
    void extractKeepsPicturesOfTheTopLevel() throws IOException {
        Path folder = Files.createDirectory(tempDir.resolve("source"));
        Path zip = zip(Map.of("/", "", "/a.jpg", "AAA", "b.png", "BB", "notes.txt", "x", "sub/c.jpg", "C",
                "../evil.jpg", "E"));

        List<SourceImage> images = DropboxLinkSource.extract(zip, folder);
        assertEquals(List.of("a.jpg", "b.png"), images.stream().map(SourceImage::name).sorted().toList());
        assertEquals("AAA", Files.readString(folder.resolve("a.jpg")));
        assertFalse(Files.exists(folder.resolve("notes.txt")));
        assertFalse(Files.exists(folder.resolve("c.jpg")));
        assertFalse(Files.exists(tempDir.resolve("evil.jpg")), "no files outside the folder");
    }

    @Test
    void extractUpdatesChangedAndRemovesDeletedPictures() throws IOException {
        Path folder = Files.createDirectory(tempDir.resolve("source"));
        List<SourceImage> first = DropboxLinkSource.extract(zip(Map.of("a.jpg", "one", "b.jpg", "two")), folder);
        String versionA = first.stream().filter(i -> i.name().equals("a.jpg")).findFirst().orElseThrow().version();

        List<SourceImage> second = DropboxLinkSource.extract(zip(Map.of("a.jpg", "one", "c.jpg", "three")), folder);
        assertEquals(versionA,
                second.stream().filter(i -> i.name().equals("a.jpg")).findFirst().orElseThrow().version(),
                "unchanged picture keeps its version, so it is not converted again");
        assertFalse(Files.exists(folder.resolve("b.jpg")), "removed from Dropbox");
        assertTrue(Files.exists(folder.resolve("c.jpg")), "new in Dropbox");

        List<SourceImage> third = DropboxLinkSource.extract(zip(Map.of("a.jpg", "changed")), folder);
        assertNotEquals(versionA, third.get(0).version());
        assertEquals("changed", Files.readString(folder.resolve("a.jpg")));
    }

    private Path zip(Map<String, String> entries) throws IOException {
        Path zip = Files.createTempFile(tempDir, "test", ".zip");
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream z = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return zip;
    }
}
