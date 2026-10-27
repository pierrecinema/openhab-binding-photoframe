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

import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * What the {@link PhotoFrameServlet} needs from a photo frame.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public interface FrameEndpoint {

    /**
     * Returns true if the token of a request matches the token of the frame.
     */
    boolean isValidToken(@Nullable String token);

    /**
     * Returns the current state of the slideshow as JSON.
     */
    String stateJson();

    /**
     * Returns the display-ready picture for a cache key, or null if the key is unknown.
     */
    @Nullable
    Path image(String key) throws Exception;
}
