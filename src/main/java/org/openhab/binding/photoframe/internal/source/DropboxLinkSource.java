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
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpStatus;

/**
 * The {@link DropboxLinkSource} reads a shared Dropbox folder without a Dropbox account.
 * <p>
 * With {@code dl=1} Dropbox delivers the whole shared folder as ZIP file. The binding downloads it on every refresh,
 * keeps the pictures of the top level in a local folder and only rewrites pictures that changed.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class DropboxLinkSource implements ImageSource {

    /** Larger folders should use the Dropbox login, which loads only changed pictures. */
    static final long MAX_ZIP_BYTES = 2L * 1024 * 1024 * 1024;

    private static final int CONNECT_TIMEOUT_MINUTES = 15;

    private final HttpClient httpClient;
    private final String downloadUrl;
    private final Path folder;

    /**
     * @param httpClient shared HTTP client of openHAB
     * @param link shared folder link as copied from Dropbox
     * @param folder local folder that keeps the extracted pictures
     * @throws SourceException if the link is not a shared Dropbox folder link
     */
    public DropboxLinkSource(HttpClient httpClient, String link, Path folder) throws SourceException {
        this.httpClient = httpClient;
        this.downloadUrl = toDownloadUrl(link);
        this.folder = folder;
    }

    @Override
    public List<SourceImage> list() throws SourceException {
        Path zip = null;
        try {
            Files.createDirectories(folder);
            zip = Files.createTempFile(folder, "download", ".zip.tmp");
            download(zip);
            return extract(zip, folder);
        } catch (ZipException e) {
            throw new SourceException("@text/offline.config-error.dropbox-not-folder", true, e);
        } catch (IOException e) {
            throw new SourceException("Cannot store the Dropbox pictures: " + e.getMessage(), false, e);
        } finally {
            if (zip != null) {
                try {
                    Files.deleteIfExists(zip);
                } catch (IOException e) {
                    // deleted with the next download
                }
            }
        }
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
        return 60;
    }

    private void download(Path target) throws SourceException, IOException {
        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(downloadUrl).followRedirects(true).timeout(CONNECT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .send(listener);
        Response response;
        try {
            response = listener.get(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SourceException("Download interrupted", false, e);
        } catch (TimeoutException | ExecutionException e) {
            throw new SourceException("Cannot reach Dropbox: " + e.getMessage(), false, e);
        }

        int status = response.getStatus();
        try (InputStream in = listener.getInputStream()) {
            if (status == HttpStatus.NOT_FOUND_404 || status == HttpStatus.GONE_410
                    || status == HttpStatus.FORBIDDEN_403) {
                throw new SourceException("@text/offline.config-error.dropbox-link-unavailable", true);
            }
            if (status == HttpStatus.TOO_MANY_REQUESTS_429 || status >= 500) {
                throw new SourceException("Dropbox is not available right now (HTTP " + status + ")", false);
            }
            if (status != HttpStatus.OK_200) {
                throw new SourceException("Unexpected answer from Dropbox (HTTP " + status + ")", false);
            }
            String type = response.getHeaders().get("Content-Type");
            if (type != null && type.toLowerCase(Locale.ROOT).startsWith("text/html")) {
                // Dropbox shows a web page instead of the ZIP, e.g. for a file link or a private folder
                throw new SourceException("@text/offline.config-error.dropbox-not-folder", true);
            }
            try (OutputStream out = Files.newOutputStream(target)) {
                copyLimited(in, out);
            }
        }
    }

    private static void copyLimited(InputStream in, OutputStream out) throws IOException, SourceException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_ZIP_BYTES) {
                throw new SourceException("@text/offline.config-error.dropbox-too-large", true);
            }
            out.write(buffer, 0, read);
        }
    }

    /**
     * Extracts the pictures of the top level of the ZIP into the folder. Unchanged pictures are not rewritten,
     * pictures that are no longer in the ZIP are deleted. Sub folders and entries with unsafe names are ignored.
     */
    static List<SourceImage> extract(Path zip, Path folder) throws IOException {
        List<SourceImage> images = new ArrayList<>();
        Set<String> names = new HashSet<>();
        // ZipFile reads the central directory; the streaming ZipInputStream cannot read the ZIP files of Dropbox
        // ("only DEFLATED entries can have EXT descriptor").
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            for (ZipEntry entry : Collections.list(zipFile.entries())) {
                String name = topLevelName(entry.getName());
                if (entry.isDirectory() || name == null || !ImageSource.isSupported(name) || !names.add(name)) {
                    continue;
                }
                Path file = folder.resolve(name);
                Path temp = Files.createTempFile(folder, "picture", ".tmp");
                try (InputStream in = zipFile.getInputStream(entry)) {
                    Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                    String version = Files.size(temp) + "-" + crc(temp);
                    if (!Files.isRegularFile(file) || !version.equals(Files.size(file) + "-" + crc(file))) {
                        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                    images.add(new SourceImage(name, name, version));
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
        }
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                Path fileName = file.getFileName();
                if (fileName != null && ImageSource.isSupported(fileName.toString())
                        && !names.contains(fileName.toString())) {
                    Files.deleteIfExists(file);
                }
            }
        }
        return images;
    }

    /**
     * Returns the file name of a ZIP entry in the top level folder, or null for entries in sub folders and for
     * unsafe names. Dropbox puts the files either directly or in a single root folder ("/").
     */
    static @Nullable String topLevelName(String entryName) {
        String name = entryName.replace('\\', '/');
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.isEmpty() || name.contains("/") || name.equals("..") || name.startsWith(".")) {
            return null;
        }
        return name;
    }

    /**
     * Converts a shared folder link into the address of the ZIP download ({@code dl=1}).
     */
    static String toDownloadUrl(String link) throws SourceException {
        URI uri;
        try {
            uri = new URI(link.trim());
        } catch (URISyntaxException e) {
            throw new SourceException("@text/offline.config-error.dropbox-link-invalid", true, e);
        }
        String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
        String path = uri.getPath() != null ? uri.getPath() : "";
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !(host.equals("dropbox.com") || host.endsWith(".dropbox.com"))
                || !(path.startsWith("/scl/fo/") || path.startsWith("/sh/"))) {
            throw new SourceException("@text/offline.config-error.dropbox-link-invalid", true);
        }
        List<String> params = new ArrayList<>();
        String query = uri.getRawQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (!param.isEmpty() && !param.startsWith("dl=") && !param.startsWith("raw=")) {
                    params.add(param);
                }
            }
        }
        params.add("dl=1");
        return "https://www.dropbox.com" + uri.getRawPath() + "?" + String.join("&", params);
    }

    private static long crc(Path file) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[64 * 1024];
        try (InputStream in = Files.newInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) > 0) {
                crc.update(buffer, 0, read);
            }
        }
        return crc.getValue();
    }
}
