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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.ui.components.RootUIComponent;

/**
 * Tests for {@link PageOptionProvider}.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
class PageOptionProviderTest {

    @Test
    void pagesAreSortedByLabelWithTheirAddress() {
        List<ParameterOption> options = PageOptionProvider.pageOptions(
                List.of(page("page_b", "Wetter"), page("overview", "Overview"), page("page_a", "lichter")));

        assertEquals(List.of("lichter", "Overview", "Wetter"),
                options.stream().map(ParameterOption::getLabel).toList());
        assertEquals(List.of("/page/page_a", "/overview/", "/page/page_b"),
                options.stream().map(ParameterOption::getValue).toList());
    }

    @Test
    void pageWithoutLabelShowsItsId() {
        List<ParameterOption> options = PageOptionProvider.pageOptions(List.of(page("page_c", null)));

        assertEquals("page_c", options.get(0).getLabel());
    }

    @Test
    void sitemapsOpenInBasicUi() {
        List<ParameterOption> options = PageOptionProvider
                .sitemapOptions(List.of("_default", "uicomponents_haus", "garten", "garten"));

        assertEquals(List.of("Sitemap: garten", "Sitemap: haus"),
                options.stream().map(ParameterOption::getLabel).toList());
        assertEquals(List.of("/basicui/app?sitemap=garten", "/basicui/app?sitemap=uicomponents_haus"),
                options.stream().map(ParameterOption::getValue).toList());
    }

    @Test
    void sitemapFilesAreFoundByTheirEnding(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("haus.sitemap"), "sitemap haus {}");
        Files.writeString(folder.resolve("readme.txt"), "");

        assertEquals(List.of("haus"), PageOptionProvider.sitemapFiles(folder));
        assertEquals(List.of(), PageOptionProvider.sitemapFiles(folder.resolve("missing")));
    }

    private static RootUIComponent page(String uid, @Nullable String label) {
        RootUIComponent page = new RootUIComponent(uid, "oh-layout-page");
        if (label != null) {
            page.addConfig("label", label);
        }
        return page;
    }
}
