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

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openhab.binding.photoframe.internal.source.LocalFolderSource;
import org.openhab.binding.photoframe.internal.source.SourceException;
import org.openhab.binding.photoframe.internal.source.SourceImage;

/**
 * Tests for {@link ExifOrientation}, {@link ImageProcessor} and {@link ImageCache}.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
class ImageTest {

    @TempDir
    @NonNullByDefault({})
    Path tempDir;

    @Test
    void exifOrientationIsRead() throws IOException {
        byte[] jpeg = jpeg(40, 20);
        assertEquals(1, ExifOrientation.read(jpeg));
        for (int orientation = 1; orientation <= 8; orientation++) {
            assertEquals(orientation, ExifOrientation.read(withExif(jpeg, orientation, true)), "big endian");
            assertEquals(orientation, ExifOrientation.read(withExif(jpeg, orientation, false)), "little endian");
        }
        assertEquals(1, ExifOrientation.read(new byte[] { 1, 2, 3 }));
        assertEquals(1, ExifOrientation.read(png(10, 10)));
    }

    @Test
    void orientationRotatesAndMirrors() {
        // 3x2 picture with a red pixel in the top left corner
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());

        assertRed(ImageProcessor.orient(image, 1), 3, 2, 0, 0);
        assertRed(ImageProcessor.orient(image, 2), 3, 2, 2, 0);
        assertRed(ImageProcessor.orient(image, 3), 3, 2, 2, 1);
        assertRed(ImageProcessor.orient(image, 4), 3, 2, 0, 1);
        assertRed(ImageProcessor.orient(image, 5), 2, 3, 0, 0);
        assertRed(ImageProcessor.orient(image, 6), 2, 3, 1, 0);
        assertRed(ImageProcessor.orient(image, 7), 2, 3, 1, 2);
        assertRed(ImageProcessor.orient(image, 8), 2, 3, 0, 2);
    }

    @Test
    void processScalesDownAndKeepsSmallPictures() throws IOException {
        BufferedImage big = read(process(jpeg(4000, 3000), 1920));
        assertEquals(1920, big.getWidth());
        assertEquals(1440, big.getHeight());

        BufferedImage small = read(process(png(800, 600), 1920));
        assertEquals(800, small.getWidth());
        assertEquals(600, small.getHeight());

        BufferedImage portrait = read(process(withExif(jpeg(400, 200), 6, true), 1920));
        assertEquals(200, portrait.getWidth(), "rotated by EXIF");
        assertEquals(400, portrait.getHeight());
    }

    @Test
    void processRejectsUnsupportedData() {
        assertThrows(IOException.class, () -> process("not a picture".getBytes(), 1920));
    }

    @Test
    void cacheConvertsOnceAndRetains() throws IOException, SourceException {
        Path pictures = Files.createDirectory(tempDir.resolve("pictures"));
        Files.write(pictures.resolve("a.jpg"), jpeg(3000, 2000));
        Files.write(pictures.resolve("b.png"), png(100, 100));
        Files.writeString(pictures.resolve("notes.txt"), "ignored");
        LocalFolderSource source = new LocalFolderSource(pictures);
        ImageCache cache = new ImageCache(tempDir.resolve("cache"), source, 1024, 100L * 1024 * 1024);

        List<SourceImage> images = source.list();
        assertEquals(2, images.size(), "only pictures are listed");

        SourceImage a = images.stream().filter(i -> i.name().equals("a.jpg")).findFirst().orElseThrow();
        Path converted = cache.get(a);
        assertEquals(1024, read(Files.readAllBytes(converted)).getWidth());
        assertEquals(converted, cache.get(a), "second call uses the cache");
        assertEquals(32, cache.key(a).length());

        cache.retain(List.of());
        assertFalse(Files.exists(converted), "removed pictures are deleted from the cache");
    }

    @Test
    void localSourceRejectsPathsOutsideTheFolder() throws IOException {
        Path pictures = Files.createDirectory(tempDir.resolve("pics"));
        LocalFolderSource source = new LocalFolderSource(pictures);
        assertThrows(SourceException.class, () -> source.open(new SourceImage("../secret.jpg", "x", "1")));
        assertThrows(SourceException.class, () -> new LocalFolderSource(tempDir.resolve("missing")).list());
    }

    private static void assertRed(BufferedImage image, int w, int h, int x, int y) {
        assertEquals(w, image.getWidth());
        assertEquals(h, image.getHeight());
        assertEquals(Color.RED.getRGB(), image.getRGB(x, y), "red pixel at " + x + "," + y);
    }

    private static byte[] process(byte[] data, int maxSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageProcessor.process(data, maxSize, out);
        return out.toByteArray();
    }

    private static BufferedImage read(byte[] data) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        assertNotNull(image);
        return image;
    }

    private static byte[] jpeg(int w, int h) throws IOException {
        return encode(w, h, "jpeg");
    }

    private static byte[] png(int w, int h) throws IOException {
        return encode(w, h, "png");
    }

    private static byte[] encode(int w, int h, String format) throws IOException {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    /**
     * Inserts an APP1 EXIF segment with the given orientation right after the JPEG start marker.
     */
    private static byte[] withExif(byte[] jpeg, int orientation, boolean bigEndian) {
        ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        if (bigEndian) {
            tiff.writeBytes(new byte[] { 'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0,
                    (byte) orientation, 0, 0, 0, 0, 0, 0 });
        } else {
            tiff.writeBytes(new byte[] { 'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0,
                    (byte) orientation, 0, 0, 0, 0, 0, 0, 0 });
        }
        byte[] body = tiff.toByteArray();
        int length = 2 + 6 + body.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(0xD8);
        out.write(0xFF);
        out.write(0xE1);
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.writeBytes(new byte[] { 'E', 'x', 'i', 'f', 0, 0 });
        out.writeBytes(body);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}
