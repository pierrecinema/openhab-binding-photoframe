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
package org.openhab.binding.photoframe.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.photoframe.internal.config.DropboxConfiguration;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;

/**
 * The {@link DropboxBridgeHandler} is the bridge to a Dropbox account. It runs the OAuth2 PKCE login without
 * redirect and provides the Dropbox API to its photo frames.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public class DropboxBridgeHandler extends BaseBridgeHandler {

    private final OAuthFactory oAuthFactory;
    private final HttpClient httpClient;

    private DropboxConfiguration config = new DropboxConfiguration();

    public DropboxBridgeHandler(Bridge bridge, OAuthFactory oAuthFactory, HttpClient httpClient) {
        super(bridge);
        this.oAuthFactory = oAuthFactory;
        this.httpClient = httpClient;
    }

    @Override
    public void initialize() {
        config = getConfigAs(DropboxConfiguration.class);
        if (config.appKey.isBlank()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/offline.config-error.missing-app-key");
            return;
        }
        // The Dropbox login is implemented in milestone M5.
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                "@text/offline.config-pending.dropbox-not-implemented");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // The bridge has no channels.
    }

    OAuthFactory getOAuthFactory() {
        return oAuthFactory;
    }

    HttpClient getHttpClient() {
        return httpClient;
    }
}
