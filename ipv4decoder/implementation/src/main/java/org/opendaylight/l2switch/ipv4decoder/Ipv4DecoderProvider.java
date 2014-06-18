/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.ipv4decoder;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140616.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ipv4DecoderProvider serves as the Activator for our Ipv4Decoder OSGI bundle.
 */
public class Ipv4DecoderProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(Ipv4DecoderProvider.class);

  /**
   * Setup the IPv4 packet handler.
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    //ToDo: Replace with config subsystem call
    PacketDecoderService packetDecoderService = consumerContext.getSALService(PacketDecoderService.class);
    packetDecoderService.registerDecoder(getEthernetIpv4PacketPayloadType(), new Ipv4Decoder(), Ipv4PacketReceived.class);
  }

  /**
   * Cleanup the IPv4 packet handler
   */
  @Override
  public void close() {
  }

  private PacketPayloadType getEthernetIpv4PacketPayloadType() {
    return new PacketPayloadTypeBuilder()
      .setPacketType(PacketType.Ethernet)
      .setPayloadType(KnownEtherType.Ipv4.getIntValue())
      .build();
  }
}
