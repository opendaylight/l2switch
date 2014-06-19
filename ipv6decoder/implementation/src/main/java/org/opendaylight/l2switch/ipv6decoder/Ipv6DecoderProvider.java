/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.ipv6decoder;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ipv6DecoderProvider serves as the Activator for our Ipv6Decoder OSGI bundle.
 */
public class Ipv6DecoderProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(Ipv6DecoderProvider.class);

  /**
   * Setup the IPv6 packet handler.
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    //ToDo: Replace with config subsystem call
    PacketDecoderService packetDecoderService = consumerContext.getSALService(PacketDecoderService.class);
    packetDecoderService.registerDecoder(getEthernetIpv6PacketPayloadType(), new Ipv6Decoder(), Ipv6PacketReceived.class);
  }

  /**
   * Cleanup the IPv6 packet handler
   */
  @Override
  public void close() {
  }

  private PacketPayloadType getEthernetIpv6PacketPayloadType() {
    return new PacketPayloadTypeBuilder()
      .setPacketType(PacketType.Ethernet)
      .setPayloadType(KnownEtherType.Ipv6.getIntValue())
      .build();
  }
}
