/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ArpPacketHandlerTest {

  @MockitoAnnotations.Mock private PacketDispatcher packetDispatcher;
  private ArpPacketHandler arpPacketHandler;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    arpPacketHandler = new ArpPacketHandler(packetDispatcher);
  }

  @Test
  public void onArpPacketReceivedTest() throws Exception {
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new ArpPacketBuilder().build())
      .build());
    ArpPacketReceived arpReceived = new ArpPacketReceivedBuilder().setPacketChain(packetChainList).build();
    arpPacketHandler.onArpPacketReceived(arpReceived);

    verify(packetDispatcher, times(1)).dispatchPacket(any(byte[].class), any(NodeConnectorRef.class), any(MacAddress.class), any(MacAddress.class));
  }

  @Test
  public void onArpPacketReceivedTest_NullInput() throws Exception {
    arpPacketHandler.onArpPacketReceived(null);
    verify(packetDispatcher, times(0)).dispatchPacket(any(byte[].class), any(NodeConnectorRef.class), any(MacAddress.class), any(MacAddress.class));
  }

  @Test
  public void onArpPacketReceivedTest_NullPacketChain() throws Exception {
    ArpPacketReceived arpReceived = new ArpPacketReceivedBuilder().build();
    arpPacketHandler.onArpPacketReceived(arpReceived);
    verify(packetDispatcher, times(0)).dispatchPacket(any(byte[].class), any(NodeConnectorRef.class), any(MacAddress.class), any(MacAddress.class));
  }

  @Test
  public void onArpPacketReceivedTest_EmptyPacketChain() throws Exception {
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    ArpPacketReceived arpReceived = new ArpPacketReceivedBuilder().setPacketChain(packetChainList).build();
    arpPacketHandler.onArpPacketReceived(arpReceived);
    verify(packetDispatcher, times(0)).dispatchPacket(any(byte[].class), any(NodeConnectorRef.class), any(MacAddress.class), any(MacAddress.class));
  }

  @Test
  public void onArpPacketReceivedTest_NoRawPacket() throws Exception {
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new ArpPacketBuilder().build())
      .build());
    ArpPacketReceived arpReceived = new ArpPacketReceivedBuilder().setPacketChain(packetChainList).build();
    arpPacketHandler.onArpPacketReceived(arpReceived);

    verify(packetDispatcher, times(0)).dispatchPacket(any(byte[].class), any(NodeConnectorRef.class), any(MacAddress.class), any(MacAddress.class));
  }

  @Test
  public void onArpPacketReceivedTest_NoEthernetPacket() throws Exception {
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new ArpPacketBuilder().build())
      .build());
    ArpPacketReceived arpReceived = new ArpPacketReceivedBuilder().setPacketChain(packetChainList).build();
    arpPacketHandler.onArpPacketReceived(arpReceived);

    verify(packetDispatcher, times(0)).dispatchPacket(any(byte[].class), any(NodeConnectorRef.class), any(MacAddress.class), any(MacAddress.class));
  }

  @Test
  public void onArpPacketReceivedTest_NoArpPacket() throws Exception {
    ArrayList<PacketChain> packetChainList = new ArrayList<PacketChain>();
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new RawPacketBuilder().build())
      .build());
    packetChainList.add(new PacketChainBuilder()
      .setPacket(new EthernetPacketBuilder().build())
      .build());
    ArpPacketReceived arpReceived = new ArpPacketReceivedBuilder().setPacketChain(packetChainList).build();
    arpPacketHandler.onArpPacketReceived(arpReceived);

    verify(packetDispatcher, times(0)).dispatchPacket(any(byte[].class), any(NodeConnectorRef.class), any(MacAddress.class), any(MacAddress.class));
  }
}
