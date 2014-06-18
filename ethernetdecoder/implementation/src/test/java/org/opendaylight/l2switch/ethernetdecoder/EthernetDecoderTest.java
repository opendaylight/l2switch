/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.ethernetdecoder;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.BasePacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.BasePacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.EthernetPacketGrp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.Header8021qType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.KnownEtherType;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertNotNull;

public class EthernetDecoderTest {

  @Test
  public void testDecode_IPv4EtherType() throws Exception {
    byte[] packet = {
        0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
        (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,  //No VLAN
        0x08, 0x00, //IPv4 Ethertype
        0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11
    };
    EthernetPacketGrp e = (EthernetPacketGrp) new EthernetDecoder().decode(getBasePacket(new RawPacketBuilder().setPayload(packet).build()));
    assertEquals(e.getEthertype(), KnownEtherType.Ipv4);
    assertNull(e.getEthernetLength());
    assertNull(e.getOuterTag());
    assertEquals(e.getDestinationMac().getValue(), "01:23:45:67:89:ab");
    assertEquals(e.getSourceMac().getValue(), "cd:ef:01:23:45:67");
    assertTrue(Arrays.equals(e.getEthernetPayload(), Arrays.copyOfRange(packet, 14, packet.length)));
  }

    @Test
    public void testDecode_Length() throws Exception {
        byte[] packet = {
                0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
                (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
                0x00, (byte)0x40,
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x22, 0x33,
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x22, 0x33,
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x22, 0x33,
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x22, 0x33
        };
        EthernetPacketGrp e = (EthernetPacketGrp) new EthernetDecoder().decode(getBasePacket(new RawPacketBuilder().setPayload(packet).build()));
        assertNull(e.getEthertype());
        assertEquals(e.getEthernetLength().intValue(), 64);
        assertNull(e.getOuterTag());
        assertEquals(e.getDestinationMac().getValue(), "01:23:45:67:89:ab");
        assertEquals(e.getSourceMac().getValue(), "cd:ef:01:23:45:67");
        assertTrue(Arrays.equals(e.getEthernetPayload(), Arrays.copyOfRange(packet, 14, packet.length)));
    }

    @Test
    public void testDecode_IPv6EtherTypeWith8021qHeader() throws Exception {
        byte[] packet = {
                0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
                (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
                (byte) 0x81, 0x00,
                (byte) 0xff, (byte) 0xff,
                (byte) 0x86, (byte) 0xdd,
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11
        };
        EthernetPacketGrp e = (EthernetPacketGrp) new EthernetDecoder().decode(getBasePacket(new RawPacketBuilder().setPayload(packet).build()));
        assertEquals(e.getEthertype(), KnownEtherType.Ipv6);
        assertNull(e.getEthernetLength());
        assertNotNull(e.getOuterTag(). getVlan());
        assertEquals(e.getOuterTag().getTPID().getIntValue(), Header8021qType.VlanTagged.getIntValue());
        assertEquals(e.getOuterTag().getPriorityCode().intValue(), 7);
        assertTrue(e.getOuterTag().isDropEligible());
        assertEquals(e.getOuterTag().getVlan().getValue().intValue(), 4095);
        assertEquals(e.getDestinationMac().getValue(), "01:23:45:67:89:ab");
        assertEquals(e.getSourceMac().getValue(), "cd:ef:01:23:45:67");
        assertTrue(Arrays.equals(e.getEthernetPayload(), Arrays.copyOfRange(packet, 18, packet.length)));
    }

    @Test
    public void testDecode_IPv6EtherTypeWithQinQ() throws Exception {
        byte[] packet = {
                0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
                (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
                (byte) 0x91, 0x00,
                (byte) 0xff, (byte) 0xff,
                (byte) 0x81, 0x00,
                (byte) 0xa0, (byte) 0x0a,
                (byte) 0x86, (byte) 0xdd,
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11
        };
        EthernetPacketGrp e = (EthernetPacketGrp) new EthernetDecoder().decode(getBasePacket(new RawPacketBuilder().setPayload(packet).build()));
        assertEquals(e.getEthertype(), KnownEtherType.Ipv6);
        assertNull(e.getEthernetLength());
        assertNotNull(e.getOuterTag().getTPID());
        assertEquals(e.getOuterTag().getTPID().getIntValue(), Header8021qType.QInQ.getIntValue());
        assertEquals(e.getOuterTag().getPriorityCode().intValue(), 7);
        assertTrue(e.getOuterTag().isDropEligible().booleanValue());
        assertEquals(e.getOuterTag().getVlan().getValue().intValue(), 4095);
        assertEquals(e.getOuterTag().getInnerTag().getTPID().getIntValue(), Header8021qType.VlanTagged.getIntValue());
        assertEquals(e.getOuterTag().getInnerTag().getPriorityCode().intValue(), 5);
        assertFalse(e.getOuterTag().getInnerTag().isDropEligible().booleanValue());
        assertEquals(e.getOuterTag().getInnerTag().getVlan().getValue().intValue(), 10);
        assertEquals(e.getDestinationMac().getValue(), "01:23:45:67:89:ab");
        assertEquals(e.getSourceMac().getValue(), "cd:ef:01:23:45:67");
        assertTrue(Arrays.equals(e.getEthernetPayload(), Arrays.copyOfRange(packet, 22, packet.length)));
    }

    private BasePacket getBasePacket(RawPacket rawPacket) {

        return new BasePacketBuilder()
                .setPacketPayloadType(getRawEthernetPacketPayloadType())
                .setRawPacket(rawPacket).build();
    }

    private PacketPayloadType getRawEthernetPacketPayloadType() {

        //currently doesn't make use of packet received as currently only ethernet packets are received so following is hard coded.
        return new PacketPayloadTypeBuilder().setPacketType(PacketType.Raw).setPayloadType(PacketType.Ethernet.getIntValue()).build();
    }

}
