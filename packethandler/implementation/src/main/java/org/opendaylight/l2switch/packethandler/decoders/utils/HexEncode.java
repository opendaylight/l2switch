/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders.utils;

/**
 * The class provides methods to convert hex encode strings.
 */
public final class HexEncode {
    private HexEncode() {
        // Hidden on purpose
    }

    public static byte[] bytesFromHexString(String values) {
        String target = "";
        if (values != null) {
            target = values;
        }
        String[] octets = target.split(":");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }

    /**
     * This method converts byte array into HexString format with ":" inserted.
     */
    public static String bytesToHexStringFormat(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        String ret = "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            short u8byte = (short) (bytes[i] & 0xff);
            String tmp = Integer.toHexString(u8byte);
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
        }
        ret = sb.toString();
        return ret;
    }
}
