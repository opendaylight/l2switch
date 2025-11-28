/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.raw.packet.RawPacketFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacketBuilder;

@ExtendWith(MockitoExtension.class)
class ArpPacketHandlerTest {
    @Mock
    private PacketDispatcher packetDispatcher;
    private ArpPacketHandler arpPacketHandler;

    @BeforeEach
    void beforeEach() {
        arpPacketHandler = new ArpPacketHandler(packetDispatcher);
    }

    @Test
    void onArpPacketReceivedTest() throws Exception {
        arpPacketHandler.onNotification(new ArpPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder()
                    .setPacket(new RawPacketBuilder().setRawPacketFields(new RawPacketFieldsBuilder().build()).build())
                .build(),
                new PacketChainBuilder().setPacket(new EthernetPacketBuilder().build()).build(),
                new PacketChainBuilder().setPacket(new ArpPacketBuilder().build()).build()))
            .build());
        verify(packetDispatcher, times(1)).dispatchPacket(null, null, null, null);
    }

    @Test
    void onArpPacketReceivedTest_NullInput() throws Exception {
        arpPacketHandler.onNotification(null);
        verifyNoInteractions(packetDispatcher);
    }

    @Test
    void onArpPacketReceivedTest_NullPacketChain() throws Exception {
        arpPacketHandler.onNotification(new ArpPacketReceivedBuilder().build());
        verifyNoInteractions(packetDispatcher);
    }

    @Test
    void onArpPacketReceivedTest_EmptyPacketChain() throws Exception {
        arpPacketHandler.onNotification(new ArpPacketReceivedBuilder().setPacketChain(List.of()).build());
        verifyNoInteractions(packetDispatcher);
    }

    @Test
    void onArpPacketReceivedTest_NoRawPacket() throws Exception {
        arpPacketHandler.onNotification(new ArpPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new EthernetPacketBuilder().build()).build(),
                new PacketChainBuilder().setPacket(new ArpPacketBuilder().build()).build()))
            .build());
        verifyNoInteractions(packetDispatcher);
    }

    @Test
    void onArpPacketReceivedTest_NoEthernetPacket() throws Exception {
        arpPacketHandler.onNotification(new ArpPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder().setPacket(new ArpPacketBuilder().build()).build()))
            .build());
        verifyNoInteractions(packetDispatcher);
    }

    @Test
    void onArpPacketReceivedTest_NoArpPacket() throws Exception {
        arpPacketHandler.onNotification(new ArpPacketReceivedBuilder()
            .setPacketChain(List.of(
                new PacketChainBuilder().setPacket(new RawPacketBuilder().build()).build(),
                new PacketChainBuilder().setPacket(new EthernetPacketBuilder().build()).build()))
            .build());
        verifyNoInteractions(packetDispatcher);
    }
}
