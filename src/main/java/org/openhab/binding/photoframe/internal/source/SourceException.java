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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link SourceException} is thrown when a picture source cannot be read.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class SourceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final boolean configurationError;

    /**
     * @param message readable message for the thing status
     * @param configurationError true if the user has to fix the configuration (wrong path, link or key), false for
     *            temporary problems such as a missing network connection
     */
    public SourceException(String message, boolean configurationError) {
        this(message, configurationError, null);
    }

    public SourceException(String message, boolean configurationError, @Nullable Throwable cause) {
        super(message, cause);
        this.configurationError = configurationError;
    }

    public boolean isConfigurationError() {
        return configurationError;
    }
}
