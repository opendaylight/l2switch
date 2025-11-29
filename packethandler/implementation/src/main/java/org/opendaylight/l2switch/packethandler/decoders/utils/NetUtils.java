/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class containing the common utility functions needed for operating on
 * networking data structures.
 */
public final class NetUtils {
    private static final Logger LOG = LoggerFactory.getLogger(NetUtils.class);

    private NetUtils() {
        // Hidden on purpose
    }

    /**
     * Return the InetAddress Network Mask given the length of the prefix bit
     * mask. The prefix bit mask indicates the contiguous leading bits that are
     * NOT masked out. Example: A prefix bit mask length of 8 will give an
     * InetAddress Network Mask of 255.0.0.0
     *
     * @param prefixMaskLength
     *            integer representing the length of the prefix network mask
     * @param isV6
     *            boolean representing the IP version of the returned address
     */
    public static InetAddress getInetNetworkMask(int prefixMaskLength, boolean isV6) {
        if (prefixMaskLength < 0 || !isV6 && prefixMaskLength > 32 || isV6 && prefixMaskLength > 128) {
            return null;
        }
        byte[] v4Address = { 0, 0, 0, 0 };
        byte[] v6Address = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] address = isV6 ? v6Address : v4Address;
        int numBytes = prefixMaskLength / 8;
        int numBits = prefixMaskLength % 8;
        int index = 0;
        for (; index < numBytes; index++) {
            address[index] = (byte) 0xff;
        }
        if (numBits > 0) {
            int rem = 0;
            for (int j = 0; j < numBits; j++) {
                rem |= 1 << 7 - j;
            }
            address[index] = (byte) rem;
        }

        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            LOG.error("Failed to decode {}", address, e);
        }
        return null;
    }
}
