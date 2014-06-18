/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arpdecoder;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.KnownEtherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.opendaylight.yangtools.concepts.Registration;
//import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * ArpDecoderProvider serves as the Activator for our ArpDecoder OSGI bundle.
 */
public class ArpDecoderProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(ArpDecoderProvider.class);

  /**
   * Setup the ARP packet handler.
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    //ToDo: Replace with config subsystem call
    PacketDecoderService packetDecoderService = consumerContext.getSALService(PacketDecoderService.class);
    packetDecoderService.registerDecoder(getEthernetArpPacketPayloadType(), new ArpDecoder(), ArpPacketReceived.class);
  }

  /**
   * Cleanup the ARP packet handler
   */
  @Override
  public void close() {
  }

  private PacketPayloadType getEthernetArpPacketPayloadType() {
    return new PacketPayloadTypeBuilder()
        .setPacketType(PacketType.Ethernet)
        .setPayloadType(KnownEtherType.Arp.getIntValue())
        .build();
  }
}
