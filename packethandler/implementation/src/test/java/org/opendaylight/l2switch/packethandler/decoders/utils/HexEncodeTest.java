/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HexEncodeTest {
    @Test
    void testbytesToHexString() {
        assertEquals("01:02:03", HexEncode.bytesToHexStringFormat(new byte[] { 0x01, 0x02, 0x03 }));
        assertEquals("11:22:33", HexEncode.bytesToHexStringFormat(new byte[] { 0x11, 0x22, 0x33 }));
    }

    @Test
    void testLongToHexString() {
        assertEquals("00:00:00:00:00:bc:61:4e", HexEncode.longToHexString(12345678L));
        assertEquals("00:00:00:00:05:e3:0a:78", HexEncode.longToHexString(98765432L));
    }

    @Test
    void testBytesFromHexString() {
        assertArrayEquals(new byte[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55 },
            HexEncode.bytesFromHexString("00:11:22:33:44:55"));
        assertArrayEquals(new byte[] { 0x55, 0x44, 0x33, 0x22, 0x11, 0x00 },
            HexEncode.bytesFromHexString("55:44:33:22:11:00"));
    }
}
