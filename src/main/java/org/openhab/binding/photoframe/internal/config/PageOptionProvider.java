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
package org.openhab.binding.photoframe.internal.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.model.sitemap.SitemapProvider;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentRegistryFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link PageOptionProvider} offers the Main UI pages and sitemaps as choices for the page opened by a tap, so no
 * address has to be typed in.
 *
 * @author Pierrecinema - Initial contribution
 */
@Component(service = ConfigOptionProvider.class)
@NonNullByDefault
public class PageOptionProvider implements ConfigOptionProvider {

    private static final String THING_CONFIG_URI = "thing-type:photoframe:frame";
    private static final String TAP_PAGE_PARAMETER = "tapPage";
    private static final String PAGE_NAMESPACE = "ui:page";
    private static final String OVERVIEW_UID = "overview";

    private static final String UI_SITEMAP_PREFIX = "uicomponents_";
    private static final String DEFAULT_SITEMAP = "_default";

    private final UIComponentRegistryFactory registryFactory;
    private final List<SitemapProvider> sitemapProviders = new CopyOnWriteArrayList<>();

    @Activate
    public PageOptionProvider(@Reference UIComponentRegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addSitemapProvider(SitemapProvider provider) {
        sitemapProviders.add(provider);
    }

    public void removeSitemapProvider(SitemapProvider provider) {
        sitemapProviders.remove(provider);
    }

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (!THING_CONFIG_URI.equals(uri.toString()) || !TAP_PAGE_PARAMETER.equals(param)) {
            return null;
        }
        List<ParameterOption> options = pageOptions(registryFactory.getRegistry(PAGE_NAMESPACE).getAll());
        List<String> sitemaps = new ArrayList<>();
        sitemapProviders.forEach(provider -> sitemaps.addAll(provider.getSitemapNames()));
        options.addAll(sitemapOptions(sitemaps));
        return options;
    }

    static List<ParameterOption> pageOptions(Collection<RootUIComponent> pages) {
        List<ParameterOption> options = new ArrayList<>();
        for (RootUIComponent page : pages) {
            String uid = page.getUID();
            // The overview has its own address in Main UI
            String address = OVERVIEW_UID.equals(uid) ? "/overview/" : "/page/" + uid;
            options.add(new ParameterOption(address, label(page)));
        }
        options.sort(Comparator.comparing(ParameterOption::getLabel, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    static List<ParameterOption> sitemapOptions(Collection<String> names) {
        List<ParameterOption> options = new ArrayList<>();
        names.stream().filter(name -> !DEFAULT_SITEMAP.equals(name)).distinct().sorted().forEach(name -> {
            String shown = name.startsWith(UI_SITEMAP_PREFIX) ? name.substring(UI_SITEMAP_PREFIX.length()) : name;
            options.add(new ParameterOption("/basicui/app?sitemap=" + name, "Sitemap: " + shown));
        });
        return options;
    }

    private static String label(RootUIComponent page) {
        Object label = page.getConfig().get("label");
        return label instanceof String text && !text.isBlank() ? text : page.getUID();
    }
}
