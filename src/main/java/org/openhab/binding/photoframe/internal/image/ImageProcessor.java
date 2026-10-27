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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Decodes a picture, applies the EXIF orientation, scales it down to a maximum size and encodes it as JPEG.
 * Only one picture is processed at a time to keep the memory use low on small devices such as a Raspberry Pi.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public final class ImageProcessor {

    private static final float JPEG_QUALITY = 0.85f;
    private static final ReentrantLock LOCK = new ReentrantLock();

    private ImageProcessor() {
    }

    /**
     * Converts the original picture data into a display-ready JPEG.
     *
     * @param data original picture (JPEG, PNG, GIF, BMP)
     * @param maxSize longest side of the result in pixels; smaller pictures are not enlarged
     * @param out receives the JPEG data
     * @throws IOException if the format is not supported or the data is broken
     */
    public static void process(byte[] data, int maxSize, OutputStream out) throws IOException {
        LOCK.lock();
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new IOException("Unsupported picture format");
            }
            BufferedImage result = scale(orient(image, ExifOrientation.read(data)), maxSize);
            writeJpeg(result, out);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Rotates and/or mirrors the image according to the EXIF orientation (1–8).
     */
    static BufferedImage orient(BufferedImage image, int orientation) {
        if (orientation <= ExifOrientation.NORMAL || orientation > 8) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = orientation >= 5;
        // AffineTransform(m00, m10, m01, m11, m02, m12): x' = m00*x + m01*y + m02, y' = m10*x + m11*y + m12
        AffineTransform t = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0); // mirror horizontally
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h); // rotate 180°
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h); // mirror vertically
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0); // transpose
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0); // rotate 90° clockwise
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w); // transverse
            default -> new AffineTransform(0, -1, 1, 0, 0, w); // 8: rotate 90° counter-clockwise
        };
        BufferedImage result = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.drawImage(image, t, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    /**
     * Scales the image so that its longest side is at most {@code maxSize}. Large reductions are done in steps of
     * half the size, which gives a much smoother result than a single step.
     */
    static BufferedImage scale(BufferedImage image, int maxSize) {
        int w = image.getWidth();
        int h = image.getHeight();
        double factor = Math.min(1.0, (double) maxSize / Math.max(w, h));
        int targetW = Math.max(1, (int) Math.round(w * factor));
        int targetH = Math.max(1, (int) Math.round(h * factor));

        BufferedImage current = image;
        while (current.getWidth() / 2 >= targetW && current.getHeight() / 2 >= targetH) {
            current = draw(current, current.getWidth() / 2, current.getHeight() / 2);
        }
        if (current.getWidth() != targetW || current.getHeight() != targetH
                || current.getType() != BufferedImage.TYPE_INT_RGB) {
            current = draw(current, targetW, targetH);
        }
        return current;
    }

    private static BufferedImage draw(BufferedImage image, int w, int h) {
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // Transparent areas of PNG/GIF pictures become black, like the slideshow background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);
            g.drawImage(image, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static void writeJpeg(BufferedImage image, OutputStream out) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
