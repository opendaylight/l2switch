/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler.decoders;


import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketGrp;

/**
 * PacketDecoder should be implemented by all the decoders that are further going to decode EthernetPacket.
 * E.g. LLDPDecoder, ARPDecoder etc.
 */
public interface PacketDecoder {
  public <E extends EthernetPacketGrp> E decode(EthernetPacket ethernetPacket);
}
