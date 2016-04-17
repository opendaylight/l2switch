/**
 * Copyright (c) 2016 Hewlett Packard Enterprise, Co. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.IcmpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.icmp.rev140528.icmp.packet.received.packet.chain.packet.IcmpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4PacketBuilder;

public class IcmpDecoderTest {
    @Test
    public void testDecode() throws Exception {
        byte[] eth_payload = { 0x00, 0x0c, (byte) 0xce, 0x13, (byte) 0xb9, (byte) 0xa0, 0x00, 0x22, 0x5f, 0x3f, (byte) 0x98, (byte) 0x91, 0x08, 0x00, // ethernet
                0x45, 0x00, 0x00, 0x3c, (byte) 0xc6, 0x3e, 0x00, 0x00, (byte) 0x80, 0x01, (byte) 0xf2, (byte) 0xd7, (byte) 0xc0, (byte) 0xa8, 0x00,
                0x59, (byte) 0xc0, (byte) 0xa8, 0x00, 0x01, // ipv4
                0x08, // Type = 8 (Echo request)
                0x00, // Code = 0
                0x42, // Checksum (+ next byte)
                0x5c, //
                0x02, // Identifier (+ next byte)
                0x00, //
                0x09, // Sequence number (+ next byte)
                0x00, //
                0, 0, 0, 0 // CRC
        };

        NotificationProviderService npServiceMock = Mockito.mock(NotificationProviderService.class);
        ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
        packetChainList.add(new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build());
        packetChainList.add(new PacketChainBuilder().setPacket(new EthernetPacketBuilder().build()).build());
        packetChainList.add(new PacketChainBuilder().setPacket(new Ipv4PacketBuilder().setPayloadOffset(34).build()).build());

        IcmpPacketReceived notification = new IcmpDecoder(npServiceMock)
                .decode(new Ipv4PacketReceivedBuilder().setPacketChain(packetChainList).setPayload(eth_payload).build());

        IcmpPacket icmpPacket = (IcmpPacket) notification.getPacketChain().get(3).getPacket();
        assertEquals(8, icmpPacket.getType().intValue());
        assertEquals(0, icmpPacket.getCode().intValue());
        assertEquals(0x425c, icmpPacket.getCrc().intValue());
        assertEquals(512, icmpPacket.getIdentifier().intValue());
        assertEquals(2304, icmpPacket.getSequenceNumber().intValue());

        assertTrue(Arrays.equals(eth_payload, notification.getPayload()));
    }
}