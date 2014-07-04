/*
* Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.l2switch.packethandler.decoders;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketOverRawReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.over.raw.fields.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.over.raw.fields.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketOverEthernetReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class Ipv4DecoderTest {

    @Test
    public void testDecode() throws Exception {

        byte[] eth_payload = {
            0x01, 0x23, 0x66, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            0x08, 0x00,
            0x45, // Version = 4,  IHL = 5
            0x00, // DSCP =0, ECN = 0
            0x00, 0x1E, // Total Length -- 30
            0x01, 0x1E, // Identification -- 286
            0x00, 0x00, // Flags = all off & Fragment offset = 0
            0x12, 0x11, // TTL = 18, Protocol = UDP
            0x00, 0x00, // Checksum = 0
            (byte) 0xc0, (byte) 0xa8, 0x00, 0x01, // Src IP Address
            0x01, 0x02, 0x03, 0x04, // Dest IP Address
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, // Data
            (byte) 0x98, (byte) 0xfe, (byte) 0xdc, (byte) 0xba // CRC
        };

        NotificationProviderService npServiceMock =
            Mockito.mock(NotificationProviderService.class);
        RawPacket rawPacket = Mockito.mock(RawPacket.class);
        EthernetPacketBuilder ethernetPacketBuilder =
            new EthernetPacketBuilder();
        ethernetPacketBuilder.setPayloadLength(30);
        ethernetPacketBuilder.setPayloadOffset(14);

        Ipv4PacketOverEthernetReceived notification =
            new Ipv4Decoder(npServiceMock).decode
                (new EthernetPacketOverRawReceivedBuilder()
                    .setEthernetPacket(ethernetPacketBuilder.build())
                    .setRawPacket(rawPacket)
                    .setPayload(eth_payload)
                    .build());

        assertEquals(4, notification.getIpv4Packet().getVersion().intValue());
        assertEquals(5, notification.getIpv4Packet().getIhl().intValue());
        assertEquals(30,
            notification.getIpv4Packet().getIpv4Length().intValue());
        assertEquals(0, notification.getIpv4Packet().getDscp().intValue());
        assertEquals(0, notification.getIpv4Packet().getEcn().intValue());
        assertEquals(30,
            notification.getIpv4Packet().getIpv4Length().intValue());
        assertEquals(286, notification.getIpv4Packet().getId().intValue());
        assertFalse(notification.getIpv4Packet().isReservedFlag());
        assertFalse(notification.getIpv4Packet().isDfFlag());
        assertFalse(notification.getIpv4Packet().isMfFlag());
        assertEquals(0,
            notification.getIpv4Packet().getFragmentOffset().intValue());
        assertEquals(18, notification.getIpv4Packet().getTtl().intValue());
        assertEquals(KnownIpProtocols.Udp,
            notification.getIpv4Packet().getProtocol());
        assertEquals(0, notification.getIpv4Packet().getChecksum().intValue());

        Ipv4Address src_address = new Ipv4Address("192.168.0.1");
        Ipv4Address dst_address = new Ipv4Address("1.2.3.4");
        assertEquals(src_address, notification.getIpv4Packet().getSourceIpv4());
        assertEquals(dst_address,
            notification.getIpv4Packet().getDestinationIpv4());

        byte[] ipv4_payload = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10
        };
        assertEquals(10,
            notification.getIpv4Packet().getPayloadLength().intValue());
        assertEquals(34,
            notification.getIpv4Packet().getPayloadOffset().intValue());

        int ipv4PayloadEnd = notification.getIpv4Packet().getPayloadOffset() +
            notification.getIpv4Packet().getPayloadLength();
        assertTrue(Arrays
            .equals(ipv4_payload, Arrays.copyOfRange(notification.getPayload(),
                notification.getIpv4Packet().getPayloadOffset(),
                ipv4PayloadEnd)));

    }


    @Test
    public void testDecode_WithDiffServAndFlagsAndOffset() throws Exception {

        byte[] eth_payload = {
            0x01, 0x23, 0x66, 0x67, (byte) 0x89, (byte) 0xab, //src mac
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67, //dst mac
            (byte) 0x81, 0x00,
            0x08, 0x00, // EtherType
            0x45, // Version = 4,  IHL = 5
            (byte) 0xff, // DSCP =63, ECN = 3
            0x00, 0x1E, // Total Length -- 30
            0x01, 0x1E, // Identification -- 286
            (byte) 0xf0, 0x00, // Flags = all on & Fragment offset = 0
            0x12, 0x06, // TTL = 18, Protocol = TCP
            (byte) 0x00, 0x00, // Checksum = 0
            (byte) 0xc0, (byte) 0xa8, 0x00, 0x01, // Src IP Address
            0x01, 0x02, 0x03, 0x04, // Dest IP Address
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11,
            0x12, 0x13, // Data
            (byte) 0x98, (byte) 0xfe, (byte) 0xdc, (byte) 0xba // CRC
        };

        NotificationProviderService npServiceMock =
            Mockito.mock(NotificationProviderService.class);
        RawPacket rawPacket = Mockito.mock(RawPacket.class);
        EthernetPacketBuilder ethernetPacketBuilder =
            new EthernetPacketBuilder();
        ethernetPacketBuilder.setPayloadLength(33);
        ethernetPacketBuilder.setPayloadOffset(16);

        Ipv4PacketOverEthernetReceived notification =
            new Ipv4Decoder(npServiceMock).decode
                (new EthernetPacketOverRawReceivedBuilder()
                    .setEthernetPacket(ethernetPacketBuilder.build())
                    .setRawPacket(rawPacket)
                    .setPayload(eth_payload)
                    .build());

        assertEquals(4, notification.getIpv4Packet().getVersion().intValue());
        assertEquals(5, notification.getIpv4Packet().getIhl().intValue());
        assertEquals(30,
            notification.getIpv4Packet().getIpv4Length().intValue());
        assertEquals(63, notification.getIpv4Packet().getDscp().intValue());
        assertEquals(3, notification.getIpv4Packet().getEcn().intValue());
        assertEquals(30,
            notification.getIpv4Packet().getIpv4Length().intValue());
        assertEquals(286, notification.getIpv4Packet().getId().intValue());
        assertTrue(notification.getIpv4Packet().isReservedFlag());
        assertTrue(notification.getIpv4Packet().isDfFlag());
        assertTrue(notification.getIpv4Packet().isMfFlag());
        assertEquals(4096,
            notification.getIpv4Packet().getFragmentOffset().intValue());
        assertEquals(18, notification.getIpv4Packet().getTtl().intValue());
        assertEquals(KnownIpProtocols.Tcp,
            notification.getIpv4Packet().getProtocol());
        assertEquals(0, notification.getIpv4Packet().getChecksum().intValue());

        Ipv4Address src_address = new Ipv4Address("192.168.0.1");
        Ipv4Address dst_address = new Ipv4Address("1.2.3.4");
        assertEquals(src_address, notification.getIpv4Packet().getSourceIpv4());
        assertEquals(dst_address,
            notification.getIpv4Packet().getDestinationIpv4());

        byte[] ipv4_payload = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11,
            0x12, 0x13 // Data
        };

        int ipv4PayloadEnd = notification.getIpv4Packet().getPayloadOffset() +
            notification.getIpv4Packet().getPayloadLength();
        assertTrue(Arrays
            .equals(ipv4_payload, Arrays.copyOfRange(notification.getPayload(),
                notification.getIpv4Packet().getPayloadOffset(),
                ipv4PayloadEnd)));
    }

    @Test
    public void testDecode_AlternatingBits() throws Exception {
        byte[] eth_payload = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
            (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
            (byte) 0x81, 0x00,
            (byte) 0xff, (byte) 0xff,
            (byte) 0x86, (byte) 0xdd,
            (byte) 0xf5, // Version = 15,  IHL = 5
            (byte) 0x0f, // DSCP =3, ECN = 3
            0x00, 0x00, // Total Length -- 30
            (byte) 0xff, (byte) 0xff, // Identification -- 65535
            (byte) 0x1f, (byte) 0xff,
            // Flags = all off & Fragment offset = 8191
            0x00, 0x06, // TTL = 00, Protocol = TCP
            (byte) 0xff, (byte) 0xff, // Checksum = 65535
            (byte) 0x00, (byte) 0x00, 0x00, 0x00, // Src IP Address
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            // Dest IP Address
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, // Data
            (byte) 0x98, (byte) 0xfe, (byte) 0xdc, (byte) 0xba // CRC
        };

        NotificationProviderService npServiceMock =
            Mockito.mock(NotificationProviderService.class);
        RawPacket rawPacket = Mockito.mock(RawPacket.class);
        EthernetPacketBuilder ethernetPacketBuilder =
            new EthernetPacketBuilder();
        ethernetPacketBuilder.setPayloadLength(30);
        ethernetPacketBuilder.setPayloadOffset(18);

        Ipv4PacketOverEthernetReceived notification =
            new Ipv4Decoder(npServiceMock).decode
                (new EthernetPacketOverRawReceivedBuilder()
                    .setEthernetPacket(ethernetPacketBuilder.build())
                    .setRawPacket(rawPacket)
                    .setPayload(eth_payload)
                    .build());

        assertEquals(15, notification.getIpv4Packet().getVersion().intValue());
        assertEquals(5, notification.getIpv4Packet().getIhl().intValue());
        assertEquals(0,
            notification.getIpv4Packet().getIpv4Length().intValue());
        assertEquals(3, notification.getIpv4Packet().getDscp().intValue());
        assertEquals(3, notification.getIpv4Packet().getEcn().intValue());
        assertEquals(0,
            notification.getIpv4Packet().getIpv4Length().intValue());
        assertEquals(65535, notification.getIpv4Packet().getId().intValue());
        assertFalse(notification.getIpv4Packet().isReservedFlag());
        assertFalse(notification.getIpv4Packet().isDfFlag());
        assertFalse(notification.getIpv4Packet().isMfFlag());
        assertEquals(8191,
            notification.getIpv4Packet().getFragmentOffset().intValue());
        assertEquals(0, notification.getIpv4Packet().getTtl().intValue());
        assertEquals(KnownIpProtocols.Tcp,
            notification.getIpv4Packet().getProtocol());
        assertEquals(65535,
            notification.getIpv4Packet().getChecksum().intValue());

        Ipv4Address src_address = new Ipv4Address("0.0.0.0");
        Ipv4Address dst_address = new Ipv4Address("255.255.255.255");
        assertEquals(src_address, notification.getIpv4Packet().getSourceIpv4());
        assertEquals(dst_address,
            notification.getIpv4Packet().getDestinationIpv4());

        byte[] ipv4_payload = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10
        };
        assertEquals(10,
            notification.getIpv4Packet().getPayloadLength().intValue());
        assertEquals(38,
            notification.getIpv4Packet().getPayloadOffset().intValue());

        int ipv4PayloadEnd = notification.getIpv4Packet().getPayloadOffset() +
            notification.getIpv4Packet().getPayloadLength();
        assertTrue(Arrays
            .equals(ipv4_payload, Arrays.copyOfRange(notification.getPayload(),
                notification.getIpv4Packet().getPayloadOffset(),
                ipv4PayloadEnd)));

    }

}
