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

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HostAddress}.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
class HostAddressTest {

    @Test
    void dockerAddressesAreRecognized() {
        assertTrue(HostAddress.isDockerAddress("172.26.0.1"));
        assertTrue(HostAddress.isDockerAddress("172.17.0.2"));
        assertTrue(HostAddress.isDockerAddress("172.31.255.254"));
        assertFalse(HostAddress.isDockerAddress("172.15.0.1"));
        assertFalse(HostAddress.isDockerAddress("172.32.0.1"));
        assertFalse(HostAddress.isDockerAddress("192.168.1.201"));
        assertFalse(HostAddress.isDockerAddress("10.0.0.5"));
        assertFalse(HostAddress.isDockerAddress("not an address"));
    }

    @Test
    void homeNetworkAddressIsKept() {
        assertEquals("192.168.1.201", HostAddress.find("192.168.1.201"));
        assertEquals("10.0.0.5", HostAddress.find("10.0.0.5"));
    }

    @Test
    void virtualInterfacesAreSkipped() {
        assertTrue(HostAddress.isVirtualInterface("docker0"));
        assertTrue(HostAddress.isVirtualInterface("br-3f2a9c"));
        assertTrue(HostAddress.isVirtualInterface("veth12ab"));
        assertTrue(HostAddress.isVirtualInterface("tailscale0"));
        assertFalse(HostAddress.isVirtualInterface("eth0"));
        assertFalse(HostAddress.isVirtualInterface("enp3s0"));
        assertFalse(HostAddress.isVirtualInterface("wlan0"));
    }
}
