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

/**
 * A picture offered by an {@link ImageSource}.
 *
 * @param id stable identifier within the source (for example the file name or a cloud file id)
 * @param name file name, used for sorting and captions
 * @param version changes whenever the picture content changes (for example modification time and size)
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public record SourceImage(String id, String name, String version) {
}
