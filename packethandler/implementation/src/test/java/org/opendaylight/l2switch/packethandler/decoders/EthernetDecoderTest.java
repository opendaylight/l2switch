/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.Header8021qType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.MatchBuilder;

public class EthernetDecoderTest {

  @Test
  public void testDecode_IPv4EtherType() throws Exception {
    byte[] packet = {
      0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
      (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
      0x08, 0x00,
      0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
      (byte)0x98, (byte)0xfe, (byte)0xdc, (byte)0xba
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    EthernetPacketReceived notification = new EthernetDecoder(mock).decode(new PacketReceivedBuilder()
      .setPayload(packet)
      .setMatch(new MatchBuilder().build())
      .build());
    EthernetPacket ethernetPacket = (EthernetPacket)notification.getPacketChain().get(1).getPacket();
    assertEquals(ethernetPacket.getEthertype(), KnownEtherType.Ipv4);
    assertNull(ethernetPacket.getEthernetLength());
    assertNull(ethernetPacket.getHeader8021q());
    assertEquals(ethernetPacket.getDestinationMac().getValue(), "01:23:45:67:89:ab");
    assertEquals(ethernetPacket.getSourceMac().getValue(), "cd:ef:01:23:45:67");
    assertEquals(14, ethernetPacket.getPayloadOffset().intValue());
    assertEquals(14, ethernetPacket.getPayloadLength().intValue());
    assertEquals(2566839482L, ethernetPacket.getCrc().longValue());
    assertTrue(Arrays.equals(packet, notification.getPayload()));
  }

  @Test
  public void testDecode_Length() throws Exception {
    byte[] packet = {
      0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
      (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
      0x00, 0x0e,
      0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22,
      0x00, (byte)0xfe, (byte)0xdc, (byte)0xba
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    EthernetPacketReceived notification = new EthernetDecoder(mock).decode(new PacketReceivedBuilder()
      .setPayload(packet)
      .setMatch(new MatchBuilder().build())
      .build());
    EthernetPacket ethernetPacket = (EthernetPacket)notification.getPacketChain().get(1).getPacket();
    assertNull(ethernetPacket.getEthertype());
    assertEquals(14, ethernetPacket.getEthernetLength().intValue());
    assertNull(ethernetPacket.getHeader8021q());
    assertEquals("01:23:45:67:89:ab", ethernetPacket.getDestinationMac().getValue());
    assertEquals("cd:ef:01:23:45:67", ethernetPacket.getSourceMac().getValue());
    assertEquals(14, ethernetPacket.getPayloadOffset().intValue());
    assertEquals(13, ethernetPacket.getPayloadLength().intValue());
    assertEquals(16702650L, ethernetPacket.getCrc().longValue());
    assertTrue(Arrays.equals(packet, notification.getPayload()));
  }

  @Test
  public void testDecode_IPv6EtherTypeWith8021qHeader() throws Exception {
    byte[] packet = {
      0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab,
      (byte) 0xcd, (byte) 0xef, 0x01, 0x23, 0x45, 0x67,
      (byte) 0x81, 0x00,
      (byte) 0xff, (byte) 0xff,
      (byte) 0x86, (byte) 0xdd,
      0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
      0x00, (byte)0x00, (byte)0xdc, (byte)0xba
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    EthernetPacketReceived notification = new EthernetDecoder(mock).decode(new PacketReceivedBuilder()
      .setPayload(packet)
      .setMatch(new MatchBuilder().build())
      .build());
    EthernetPacket ethernetPacket = (EthernetPacket)notification.getPacketChain().get(1).getPacket();
    assertEquals(ethernetPacket.getEthertype(), KnownEtherType.Ipv6);
    assertNull(ethernetPacket.getEthernetLength());
    assertEquals(1, ethernetPacket.getHeader8021q().size());
    assertEquals(Header8021qType.VlanTagged, ethernetPacket.getHeader8021q().get(0).getTPID());
    assertEquals(7, ethernetPacket.getHeader8021q().get(0).getPriorityCode().intValue());
    assertTrue(ethernetPacket.getHeader8021q().get(0).isDropEligible());
    assertEquals(4095, ethernetPacket.getHeader8021q().get(0).getVlan().getValue().intValue());
    assertEquals("01:23:45:67:89:ab", ethernetPacket.getDestinationMac().getValue());
    assertEquals("cd:ef:01:23:45:67", ethernetPacket.getSourceMac().getValue());
    assertEquals(18, ethernetPacket.getPayloadOffset().intValue());
    assertEquals(8, ethernetPacket.getPayloadLength().intValue());
    assertEquals(56506L, ethernetPacket.getCrc().longValue());
    assertTrue(Arrays.equals(packet, notification.getPayload()));
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
      0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
      (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    EthernetPacketReceived notification = new EthernetDecoder(mock).decode(new PacketReceivedBuilder()
      .setPayload(packet)
      .setMatch(new MatchBuilder().build())
      .build());
    EthernetPacket ethernetPacket = (EthernetPacket)notification.getPacketChain().get(1).getPacket();
    assertEquals(ethernetPacket.getEthertype(), KnownEtherType.Ipv6);
    assertNull(ethernetPacket.getEthernetLength());
    assertEquals(2, ethernetPacket.getHeader8021q().size());
    assertEquals(Header8021qType.QInQ, ethernetPacket.getHeader8021q().get(0).getTPID());
    assertEquals(7, ethernetPacket.getHeader8021q().get(0).getPriorityCode().intValue());
    assertTrue(ethernetPacket.getHeader8021q().get(0).isDropEligible());
    assertEquals(4095, ethernetPacket.getHeader8021q().get(0).getVlan().getValue().intValue());
    assertEquals(Header8021qType.VlanTagged, ethernetPacket.getHeader8021q().get(1).getTPID());
    assertEquals(5, ethernetPacket.getHeader8021q().get(1).getPriorityCode().intValue());
    assertFalse(ethernetPacket.getHeader8021q().get(1).isDropEligible());
    assertEquals(10, ethernetPacket.getHeader8021q().get(1).getVlan().getValue().intValue());
    assertEquals("01:23:45:67:89:ab", ethernetPacket.getDestinationMac().getValue());
    assertEquals("cd:ef:01:23:45:67", ethernetPacket.getSourceMac().getValue());
    assertEquals(22, ethernetPacket.getPayloadOffset().intValue());
    assertEquals(14, ethernetPacket.getPayloadLength().intValue());
    assertEquals(168496141L, ethernetPacket.getCrc().longValue());
    assertTrue(Arrays.equals(packet, notification.getPayload()));
  }

  // This test is from a Mininet VM, taken from a wireshark dump
  @Test
  public void testDecode_Ipv6Udp() throws Exception {
    byte[] packet = {
      // Ethernet start
      0x33, 0x33, 0x00, 0x00, 0x00, (byte)0xfb, (byte)0xa2, (byte)0xe6, (byte)0xda, 0x67, (byte)0xef, (byte)0x95,
      (byte)0x86, (byte)0xdd,
      // IPv6 packet start
      0x60, 0x00, 0x00, 0x00, 0x00, 0x35, 0x11, (byte)0xff, (byte)0xfe, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xa0, (byte)0xe6,
      (byte)0xda, (byte)0xff, (byte)0xfe, 0x67, (byte)0xef, (byte)0x95, (byte)0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xfb,
      // UDP start
      0x14, (byte)0xe9, 0x14, (byte)0xe9, 0x00, 0x35, 0x6b, (byte)0xd4, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x5f, 0x69, 0x70, 0x70, 0x73,
      0x04, 0x5f, 0x74, 0x63, 0x70, 0x05, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x00, 0x00, 0x0c, 0x00, 0x01,
      0x04, 0x5f, 0x69, 0x70, 0x70, (byte)0xc0, 0x12, 0x00, 0x0c, 0x00, 0x01
    };
    NotificationProviderService mock = Mockito.mock(NotificationProviderService.class);
    EthernetPacketReceived notification = new EthernetDecoder(mock).decode(new PacketReceivedBuilder()
      .setPayload(packet)
      .setMatch(new MatchBuilder().build())
      .build());
    EthernetPacket ethernetPacket = (EthernetPacket)notification.getPacketChain().get(1).getPacket();
    assertEquals(ethernetPacket.getEthertype(), KnownEtherType.Ipv6);
    assertNull(ethernetPacket.getEthernetLength());
    assertNull(ethernetPacket.getHeader8021q());
    assertEquals("33:33:00:00:00:fb", ethernetPacket.getDestinationMac().getValue());
    assertEquals("a2:e6:da:67:ef:95", ethernetPacket.getSourceMac().getValue());
    assertEquals(14, ethernetPacket.getPayloadOffset().intValue());
    // Wirehshark didn't include a CRC, so not testing for length & crc fields
    assertTrue(Arrays.equals(packet, notification.getPayload()));
  }
}
