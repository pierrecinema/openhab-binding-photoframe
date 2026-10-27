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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Finds the address of openHAB as seen from the screens in the home network.
 * <p>
 * openHAB in Docker often reports a Docker network address (e.g. 172.26.0.1) as primary address, which tablets
 * cannot reach. Such addresses are skipped when a normal home network address (192.168.x.x, 10.x.x.x) exists.
 *
 * @author Pierrecinema - Initial contribution
 */
@NonNullByDefault
public final class HostAddress {

    private static final List<String> VIRTUAL_INTERFACES = List.of("docker", "br-", "veth", "virbr", "cni", "flannel",
            "cali", "vmnet", "vboxnet", "tun", "tap", "wg", "zt", "lo");

    private HostAddress() {
    }

    /**
     * Returns the best address for the screens.
     *
     * @param primary the primary address configured in / detected by openHAB, may be null
     */
    public static String find(@Nullable String primary) {
        if (primary != null && !isDockerAddress(primary)) {
            return primary;
        }
        String lan = findLanAddress();
        if (lan != null) {
            return lan;
        }
        return primary != null ? primary : "localhost";
    }

    /**
     * Returns true for addresses in 172.16.0.0/12, the range Docker uses for its networks.
     */
    static boolean isDockerAddress(String address) {
        String[] parts = address.split("\\.");
        if (parts.length != 4 || !"172".equals(parts[0])) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static boolean isVirtualInterface(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return VIRTUAL_INTERFACES.stream().anyMatch(lower::startsWith);
    }

    private static @Nullable String findLanAddress() {
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual() || isVirtualInterface(nic.getName())) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    String host = address.getHostAddress();
                    if (address instanceof Inet4Address && address.isSiteLocalAddress() && host != null
                            && !isDockerAddress(host)) {
                        return host;
                    }
                }
            }
        } catch (SocketException e) {
            // no further network interfaces readable
        }
        return null;
    }
}
