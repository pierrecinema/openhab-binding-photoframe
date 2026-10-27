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
package org.openhab.binding.photoframe.internal.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.photoframe.internal.handler.PhotoFrameHandler;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * The {@link PhotoFrameActions} provides rule actions for a photo frame.
 *
 * @author Pierrecinema - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = PhotoFrameActions.class)
@ThingActionsScope(name = "photoframe")
@NonNullByDefault
public class PhotoFrameActions implements ThingActions {

    private @Nullable PhotoFrameHandler handler;

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof PhotoFrameHandler frameHandler) {
            this.handler = frameHandler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }

    @RuleAction(label = "@text/action.next.label", description = "@text/action.next.description")
    public void next() {
        PhotoFrameHandler handler = this.handler;
        if (handler != null) {
            handler.next();
        }
    }

    @RuleAction(label = "@text/action.previous.label", description = "@text/action.previous.description")
    public void previous() {
        PhotoFrameHandler handler = this.handler;
        if (handler != null) {
            handler.previous();
        }
    }

    @RuleAction(label = "@text/action.pause.label", description = "@text/action.pause.description")
    public void pause() {
        PhotoFrameHandler handler = this.handler;
        if (handler != null) {
            handler.pause();
        }
    }

    @RuleAction(label = "@text/action.play.label", description = "@text/action.play.description")
    public void play() {
        PhotoFrameHandler handler = this.handler;
        if (handler != null) {
            handler.play();
        }
    }

    @RuleAction(label = "@text/action.refresh.label", description = "@text/action.refresh.description")
    public void refresh() {
        PhotoFrameHandler handler = this.handler;
        if (handler != null) {
            handler.refreshNow();
        }
    }

    @RuleAction(label = "@text/action.new-token.label", description = "@text/action.new-token.description")
    public @ActionOutput(type = "java.lang.String", label = "@text/action.output.page-url.label") String newToken() {
        PhotoFrameHandler handler = this.handler;
        return handler != null ? handler.newToken() : "";
    }

    public static void next(ThingActions actions) {
        ((PhotoFrameActions) actions).next();
    }

    public static void previous(ThingActions actions) {
        ((PhotoFrameActions) actions).previous();
    }

    public static void pause(ThingActions actions) {
        ((PhotoFrameActions) actions).pause();
    }

    public static void play(ThingActions actions) {
        ((PhotoFrameActions) actions).play();
    }

    public static void refresh(ThingActions actions) {
        ((PhotoFrameActions) actions).refresh();
    }

    public static String newToken(ThingActions actions) {
        return ((PhotoFrameActions) actions).newToken();
    }
}
