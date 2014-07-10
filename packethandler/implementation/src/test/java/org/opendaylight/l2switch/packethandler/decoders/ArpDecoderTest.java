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
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownHardwareType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.KnownOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;

import java.util.ArrayList;

import static junit.framework.Assert.*;

public class ArpDecoderTest {

  @Test
  public void testDecode_RequestIPv4() throws Exception {
    byte[] packet = {
      0x00, 0x00, 0x00, 0x00, 0x00, // Offset is 5
      0x00, 0x01, // Hardware Type -- Ethernet
      0x08, 0x00, // Protocol Type -- Ipv4
      0x06, // Hardware Length -- 6
      0x04, // Protcool Length -- 4
      0x00, 0x01, // Operator -- Request
      0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, // Src Hardware Address
      (byte)0xc0, (byte)0xa8, 0x00, 0x01, // Src Protocol Address
      (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67, // Dest Hardware Address
      0x01, 0x02, 0x03, 0x04 // Dest Protocol Address
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setOrder((byte)0)
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setOrder((byte)1)
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(5).setPayloadLength(33).build())
      .build());
    ArpPacketReceived notification = new ArpDecoder(mock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(packet)
      .build());

    ArpPacket arpPacket = (ArpPacket)notification.getPacketChain().get(2).getPacket();
    assertEquals(KnownHardwareType.Ethernet, arpPacket.getHardwareType());
    assertEquals(KnownEtherType.Ipv4, arpPacket.getProtocolType());
    assertEquals(6, arpPacket.getHardwareLength().intValue());
    assertEquals(4, arpPacket.getProtocolLength().intValue());
    assertEquals(KnownOperation.Request, arpPacket.getOperation());
    assertEquals("01:23:45:67:89:ab", arpPacket.getSourceHardwareAddress());
    assertEquals("192.168.0.1", arpPacket.getSourceProtocolAddress());
    assertEquals("cd:ef:01:23:45:67", arpPacket.getDestinationHardwareAddress());
    assertEquals("1.2.3.4", arpPacket.getDestinationProtocolAddress());
  }

  @Test
  public void testDecode_ReplyIPv4() throws Exception {
    byte[] packet = {
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Offset is 8
      0x00, 0x01, // Hardware Type -- Ethernet
      0x08, 0x00, // Protocol Type -- Ipv4
      0x06, // Hardware Length -- 6
      0x04, // Protcool Length -- 4
      0x00, 0x02, // Operator -- Reply
      0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, // Src Hardware Address
      (byte)0xc0, (byte)0xa8, 0x00, 0x01, // Src Protocol Address
      (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67, // Dest Hardware Address
      0x01, 0x02, 0x03, 0x04 // Dest Protocol Address
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setOrder((byte)0)
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setOrder((byte)1)
      .setPacket(new EthernetPacketBuilder().setPayloadOffset(8).setPayloadLength(36).build())
      .build());
    ArpPacketReceived notification = new ArpDecoder(mock).decode(new EthernetPacketReceivedBuilder()
      .setPacketChain(packetChainList)
      .setPayload(packet)
      .build());

    ArpPacket arpPacket = (ArpPacket)notification.getPacketChain().get(2).getPacket();
    assertEquals(KnownHardwareType.Ethernet, arpPacket.getHardwareType());
    assertEquals(KnownEtherType.Ipv4, arpPacket.getProtocolType());
    assertEquals(6, arpPacket.getHardwareLength().intValue());
    assertEquals(4, arpPacket.getProtocolLength().intValue());
    assertEquals(KnownOperation.Reply, arpPacket.getOperation());
    assertEquals("01:23:45:67:89:ab", arpPacket.getSourceHardwareAddress());
    assertEquals("192.168.0.1", arpPacket.getSourceProtocolAddress());
    assertEquals("cd:ef:01:23:45:67", arpPacket.getDestinationHardwareAddress());
    assertEquals("1.2.3.4", arpPacket.getDestinationProtocolAddress());
  }
}
