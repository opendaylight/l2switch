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
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * PacketDecoder should be implemented by all the decoders that are further going to decode EthernetPacket.
 * E.g. LLDPDecoder, ARPDecoder etc.
 */
public interface PacketDecoder {

  /**
   * Decodes the given EthernetPacket payload further and returns a extension of Ethernet packet.
   * e.g. ARP, IPV4, LLDP etc.
   * @param ethernetPacket
   * @return
   */
  public EthernetPacketGrp decode(EthernetPacket ethernetPacket);

  /**
   * This is utility method for converting the decoded packet to its corresponding notification.
   * @param decodedEthernetPacket
   * @return
   */
  public Notification buildPacketNotification(EthernetPacketGrp decodedEthernetPacket);
}
