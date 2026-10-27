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
package org.openhab.binding.photoframe.internal;

import static org.openhab.binding.photoframe.internal.PhotoFrameBindingConstants.*;

import javax.servlet.ServletException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.photoframe.internal.handler.DropboxBridgeHandler;
import org.openhab.binding.photoframe.internal.handler.PhotoFrameHandler;
import org.openhab.binding.photoframe.internal.servlet.PhotoFrameServlet;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.net.HttpServiceUtil;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PhotoFrameHandlerFactory} is responsible for creating things and thing
 * handlers. It also registers the servlet that serves the slideshow pages.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.photoframe", service = ThingHandlerFactory.class)
public class PhotoFrameHandlerFactory extends BaseThingHandlerFactory {

    private static final int DEFAULT_PORT = 8080;

    private final Logger logger = LoggerFactory.getLogger(PhotoFrameHandlerFactory.class);
    private final OAuthFactory oAuthFactory;
    private final HttpClientFactory httpClientFactory;
    private final HttpService httpService;
    private final NetworkAddressService networkAddressService;
    private final ItemRegistry itemRegistry;
    private final PhotoFrameServlet servlet = new PhotoFrameServlet();

    @Activate
    public PhotoFrameHandlerFactory(@Reference OAuthFactory oAuthFactory,
            @Reference HttpClientFactory httpClientFactory, @Reference HttpService httpService,
            @Reference NetworkAddressService networkAddressService, @Reference ItemRegistry itemRegistry) {
        this.oAuthFactory = oAuthFactory;
        this.httpClientFactory = httpClientFactory;
        this.httpService = httpService;
        this.networkAddressService = networkAddressService;
        this.itemRegistry = itemRegistry;
        try {
            httpService.registerServlet(SERVLET_PATH, servlet, null, null);
        } catch (ServletException | NamespaceException e) {
            logger.warn("Cannot register the photo frame servlet at {}: {}", SERVLET_PATH, e.getMessage());
        }
    }

    @Deactivate
    public void deactivate() {
        try {
            httpService.unregister(SERVLET_PATH);
        } catch (IllegalArgumentException e) {
            // not registered
        }
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_DROPBOX.equals(thingTypeUID)) {
            return new DropboxBridgeHandler((Bridge) thing, oAuthFactory, httpClientFactory.getCommonHttpClient());
        } else if (THING_TYPE_FRAME.equals(thingTypeUID)) {
            return new PhotoFrameHandler(thing, servlet, itemRegistry, httpClientFactory.getCommonHttpClient(),
                    this::defaultBaseUrl);
        }

        return null;
    }

    /**
     * Returns http://&lt;home network address&gt;:&lt;HTTP port&gt; of this openHAB instance; Docker network
     * addresses are skipped.
     */
    private String defaultBaseUrl() {
        String host = HostAddress.find(networkAddressService.getPrimaryIpv4HostAddress());
        int port = bundleContext != null ? HttpServiceUtil.getHttpServicePort(bundleContext) : -1;
        return "http://" + host + ":" + (port > 0 ? port : DEFAULT_PORT);
    }
}
