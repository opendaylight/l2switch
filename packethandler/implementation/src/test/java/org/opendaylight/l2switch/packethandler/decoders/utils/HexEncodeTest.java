/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HexEncodeTest {
    @Test
    void testbytesToHexString() {
        assertEquals("01:02:03", HexEncode.bytesToHexStringFormat(new byte[] { 0x01, 0x02, 0x03 }));
        assertEquals("11:22:33", HexEncode.bytesToHexStringFormat(new byte[] { 0x11, 0x22, 0x33 }));
    }
}
