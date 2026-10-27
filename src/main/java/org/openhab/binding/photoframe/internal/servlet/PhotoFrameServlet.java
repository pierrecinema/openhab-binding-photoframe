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
package org.openhab.binding.photoframe.internal.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PhotoFrameServlet} serves the slideshow pages of all photo frames:
 * <ul>
 * <li>{@code /photoframe/<thing id>/?token=…} – the slideshow page</li>
 * <li>{@code /photoframe/<thing id>/state?token=…} – current state as JSON, polled by the page</li>
 * <li>{@code /photoframe/<thing id>/image/<key>.jpg?token=…} – a display-ready picture</li>
 * </ul>
 * Every request needs the token of the frame, so that not everybody in the network can see the pictures.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class PhotoFrameServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Pattern IMAGE_KEY = Pattern.compile("[0-9a-f]{32}");
    private static final String PAGE_RESOURCE = "/web/slideshow.html";

    private final Logger logger = LoggerFactory.getLogger(PhotoFrameServlet.class);
    private final Map<String, FrameEndpoint> frames = new ConcurrentHashMap<>();
    private final String page;

    public PhotoFrameServlet() {
        this.page = loadPage();
    }

    public void register(String thingId, FrameEndpoint frame) {
        frames.put(thingId, frame);
    }

    public void unregister(String thingId, FrameEndpoint frame) {
        frames.remove(thingId, frame);
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp) throws IOException {
        if (req == null || resp == null) {
            return;
        }
        String path = req.getPathInfo();
        String[] parts = path == null ? new String[0] : path.substring(1).split("/", 3);
        if (parts.length == 0 || parts[0].isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        FrameEndpoint frame = frames.get(parts[0]);
        if (frame == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unknown photo frame");
            return;
        }
        if (!frame.isValidToken(req.getParameter("token"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing token");
            return;
        }

        String action = parts.length > 1 ? parts[1] : null;
        if (action == null) {
            // Relative addresses of the page need a trailing slash
            String query = req.getQueryString();
            resp.sendRedirect(req.getRequestURI() + "/" + (query != null ? "?" + query : ""));
        } else if (action.isEmpty()) {
            send(resp, "text/html; charset=UTF-8", page);
        } else if ("state".equals(action)) {
            send(resp, "application/json; charset=UTF-8", frame.stateJson());
        } else if ("image".equals(action) && parts.length == 3) {
            sendImage(resp, frame, parts[2]);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void sendImage(HttpServletResponse resp, FrameEndpoint frame, String fileName) throws IOException {
        String key = fileName.endsWith(".jpg") ? fileName.substring(0, fileName.length() - 4) : fileName;
        if (!IMAGE_KEY.matcher(key).matches()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path file;
        try {
            file = frame.image(key);
        } catch (Exception e) {
            logger.debug("Cannot prepare picture {}: {}", key, e.getMessage());
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Picture cannot be shown");
            return;
        }
        if (file == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("image/jpeg");
        resp.setContentLengthLong(Files.size(file));
        // The key changes when the picture changes, so the browser may keep it.
        resp.setHeader("Cache-Control", "private, max-age=86400");
        Files.copy(file, resp.getOutputStream());
    }

    private static void send(HttpServletResponse resp, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        resp.setContentType(contentType);
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentLength(bytes.length);
        resp.getOutputStream().write(bytes);
    }

    private static String loadPage() {
        try (InputStream in = PhotoFrameServlet.class.getResourceAsStream(PAGE_RESOURCE)) {
            if (in == null) {
                return "<!doctype html><title>Photoframe</title><p>Slideshow page missing";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<!doctype html><title>Photoframe</title><p>Slideshow page cannot be loaded";
        }
    }
}
