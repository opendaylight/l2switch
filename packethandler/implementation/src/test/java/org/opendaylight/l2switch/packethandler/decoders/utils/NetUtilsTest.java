/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Test;

public class NetUtilsTest {
    @Test
    public void testMasksV4() throws UnknownHostException {

        InetAddress mask = InetAddress.getByName("128.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(1, false)));

        mask = InetAddress.getByName("192.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(2, false)));

        mask = InetAddress.getByName("224.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(3, false)));

        mask = InetAddress.getByName("240.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(4, false)));

        mask = InetAddress.getByName("248.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(5, false)));

        mask = InetAddress.getByName("252.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(6, false)));

        mask = InetAddress.getByName("254.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(7, false)));

        mask = InetAddress.getByName("255.0.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(8, false)));

        mask = InetAddress.getByName("255.128.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(9, false)));

        mask = InetAddress.getByName("255.192.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(10, false)));

        mask = InetAddress.getByName("255.224.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(11, false)));

        mask = InetAddress.getByName("255.240.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(12, false)));

        mask = InetAddress.getByName("255.248.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(13, false)));

        mask = InetAddress.getByName("255.252.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(14, false)));

        mask = InetAddress.getByName("255.254.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(15, false)));

        mask = InetAddress.getByName("255.255.0.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(16, false)));

        mask = InetAddress.getByName("255.255.128.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(17, false)));

        mask = InetAddress.getByName("255.255.192.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(18, false)));

        mask = InetAddress.getByName("255.255.224.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(19, false)));

        mask = InetAddress.getByName("255.255.240.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(20, false)));

        mask = InetAddress.getByName("255.255.248.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(21, false)));

        mask = InetAddress.getByName("255.255.252.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(22, false)));

        mask = InetAddress.getByName("255.255.254.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(23, false)));

        mask = InetAddress.getByName("255.255.255.0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(24, false)));

        mask = InetAddress.getByName("255.255.255.128");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(25, false)));

        mask = InetAddress.getByName("255.255.255.192");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(26, false)));

        mask = InetAddress.getByName("255.255.255.224");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(27, false)));

        mask = InetAddress.getByName("255.255.255.240");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(28, false)));

        mask = InetAddress.getByName("255.255.255.248");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(29, false)));

        mask = InetAddress.getByName("255.255.255.252");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(30, false)));

        mask = InetAddress.getByName("255.255.255.254");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(31, false)));

        mask = InetAddress.getByName("255.255.255.255");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(32, false)));
    }

    @Test
    public void testMasksV6() throws UnknownHostException {

        InetAddress mask = InetAddress.getByName("ff00::0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(8, true)));

        mask = InetAddress.getByName("8000::0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(1, true)));

        mask = InetAddress.getByName("f800::0");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(5, true)));

        mask = InetAddress.getByName("ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffe");
        assertTrue(mask.equals(NetUtils.getInetNetworkMask(127, true)));
    }

    @Test
    public void testGetSubnetLen() {

        byte[] address = { (byte) 128, (byte) 0, (byte) 0, 0 };
        assertTrue(NetUtils.getSubnetMaskLength(address) == 1);

        byte[] address1 = { (byte) 255, 0, 0, 0 };
        assertTrue(NetUtils.getSubnetMaskLength(address1) == 8);

        byte[] address2 = { (byte) 255, (byte) 255, (byte) 248, 0 };
        assertTrue(NetUtils.getSubnetMaskLength(address2) == 21);

        byte[] address4 = { (byte) 255, (byte) 255, (byte) 255, (byte) 254 };
        assertTrue(NetUtils.getSubnetMaskLength(address4) == 31);

        byte[] address5 = {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
            (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
            (byte) 255};
        assertTrue(NetUtils.getSubnetMaskLength(address5) == 128);

        byte[] address6 = {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
            (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
            (byte) 254};
        assertTrue(NetUtils.getSubnetMaskLength(address6) == 127);

        byte[] address7 = {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
            (byte) 255, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        assertTrue(NetUtils.getSubnetMaskLength(address7) == 64);

        byte[] address8 = {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255,
            (byte) 254, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        assertTrue(NetUtils.getSubnetMaskLength(address8) == 63);

        byte[] address9 = {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 128,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        assertTrue(NetUtils.getSubnetMaskLength(address9) == 49);

        byte[] address10 = {(byte) 128, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        assertTrue(NetUtils.getSubnetMaskLength(address10) == 1);

        byte[] address11 = {(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
            (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        assertTrue(NetUtils.getSubnetMaskLength(address11) == 0);
    }

    @Test
    public void testGetSubnetPrefix() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName("192.128.64.252");
        int maskLen = 25;
        assertTrue(NetUtils.getSubnetPrefix(ip, maskLen).equals(InetAddress.getByName("192.128.64.128")));
    }

    @Test
    public void testIsIPv6Valid() throws UnknownHostException {
        assertTrue(NetUtils.isIPv6AddressValid("fe80:0000:0000:0000:0204:61ff:fe9d:f156")); // normal ipv6
        assertTrue(NetUtils.isIPv6AddressValid("fe80:0:0:0:204:61ff:fe9d:f156")); // no leading zeroes
        assertTrue(NetUtils.isIPv6AddressValid("fe80::204:61ff:fe9d:f156")); // zeroes to ::
        assertTrue(NetUtils.isIPv6AddressValid("fe80:0000:0000:0000:0204:61ff:254.157.241.86")); // ipv4 ending
        assertTrue(NetUtils.isIPv6AddressValid("fe80:0:0:0:0204:61ff:254.157.241.86")); // no leading zeroes, ipv4 end
        assertTrue(NetUtils.isIPv6AddressValid("fe80::204:61ff:254.157.241.86")); // zeroes ::, no leading zeroes

        assertTrue(NetUtils.isIPv6AddressValid("2001::")); // link-local prefix
        assertTrue(NetUtils.isIPv6AddressValid("::1")); // localhost
        assertTrue(NetUtils.isIPv6AddressValid("fe80::")); // global-unicast
        assertFalse(NetUtils.isIPv6AddressValid("abcd")); // not valid
        assertFalse(NetUtils.isIPv6AddressValid("1")); // not valid
        assertFalse(NetUtils.isIPv6AddressValid("fe80:0:0:0:204:61ff:fe9d")); // not valid, too short
        assertFalse(NetUtils.isIPv6AddressValid("fe80:::0:0:0:204:61ff:fe9d")); // not valid
        assertFalse(NetUtils.isIPv6AddressValid("192.168.1.1")); // not valid,ipv4
        assertFalse(NetUtils.isIPv6AddressValid("2001:0000:1234:0000:10001:C1C0:ABCD:0876")); // invalid, extra number
        assertFalse(NetUtils.isIPv6AddressValid("20010:0000:1234:0000:10001:C1C0:ABCD:0876")); // invalid, extra number

        assertTrue(NetUtils.isIPv6AddressValid("2001:0DB8:0000:CD30:0000:0000:0000:0000/60")); // full with mask
        assertTrue(NetUtils.isIPv6AddressValid("2001:0DB8:0:CD30::/64")); // shortened with mask
        assertTrue(NetUtils.isIPv6AddressValid("2001:0DB8:0:CD30::/0")); // 0 subnet with mask
        assertTrue(NetUtils.isIPv6AddressValid("::1/128")); // localhost 128 mask

        assertFalse(NetUtils.isIPv6AddressValid("124.15.6.89/60")); // invalid, ip with mask
        assertFalse(NetUtils.isIPv6AddressValid("2001:0DB8:0000:CD30:0000:0000:0000:0000/130")); // invalid, mask >128
        assertFalse(NetUtils.isIPv6AddressValid("2001:0DB8:0:CD30::/-5")); // invalid, mask < 0
        assertFalse(NetUtils.isIPv6AddressValid("fe80:::0:0:0:204:61ff:fe9d/64")); // not valid ip, valid netmask
        assertFalse(NetUtils.isIPv6AddressValid("fe80:::0:0:0:204:61ff:fe9d/-1")); // not valid both

    }

    @Test
    public void testIPAddressValidity() {
        assertFalse(NetUtils.isIPAddressValid(null));
        assertFalse(NetUtils.isIPAddressValid("abc"));
        assertFalse(NetUtils.isIPAddressValid("1.1.1"));
        assertFalse(NetUtils.isIPAddressValid("1.1.1.1/49"));

        assertTrue(NetUtils.isIPAddressValid("1.1.1.1"));
        assertTrue(NetUtils.isIPAddressValid("1.1.1.1/32"));
        assertTrue(NetUtils.isIPAddressValid("2001:420:281:1004:407a:57f4:4d15:c355"));
    }

    @Test
    public void testGetUnsignedByte() {
        assertEquals(0, NetUtils.getUnsignedByte((byte) 0x00));
        assertEquals(1, NetUtils.getUnsignedByte((byte) 0x01));
        assertEquals(127, NetUtils.getUnsignedByte((byte) 0x7f));

        assertEquals(128, NetUtils.getUnsignedByte((byte) 0x80));
        assertEquals(255, NetUtils.getUnsignedByte((byte) 0xff));
    }

    @Test
    public void testGetUnsignedShort() {
        assertEquals(0, NetUtils.getUnsignedShort((short) 0x0000));
        assertEquals(1, NetUtils.getUnsignedShort((short) 0x0001));
        assertEquals(32767, NetUtils.getUnsignedShort((short) 0x7fff));

        assertEquals(32768, NetUtils.getUnsignedShort((short) 0x8000));
        assertEquals(65535, NetUtils.getUnsignedShort((short) 0xffff));
    }

    @Test
    public void testMulticastMACAddr() {
        byte[] empty = new byte[0];
        assertFalse(NetUtils.isUnicastMACAddr(empty));
        assertFalse(NetUtils.isMulticastMACAddr(empty));

        byte[] bcast = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, };
        assertFalse(NetUtils.isUnicastMACAddr(bcast));
        assertFalse(NetUtils.isMulticastMACAddr(bcast));

        byte[] firstOctet = { (byte) 0x00, (byte) 0x20, (byte) 0x80, (byte) 0xfe, };
        for (int len = 1; len <= 10; len++) {
            byte[] ba = new byte[len];
            boolean valid = len == 6;
            for (byte first : firstOctet) {
                ba[0] = first;
                assertFalse(NetUtils.isMulticastMACAddr(ba));
                assertEquals(valid, NetUtils.isUnicastMACAddr(ba));

                ba[0] |= (byte) 0x01;
                assertEquals(valid, NetUtils.isMulticastMACAddr(ba));
                assertFalse(NetUtils.isUnicastMACAddr(ba));
            }
        }
    }
}
