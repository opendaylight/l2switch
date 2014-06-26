/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.ethernetdecoder;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.opendaylight.yangtools.concepts.Registration;
//import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * EthernetDecoderProvider serves as the Activator for our EthernetDecoder OSGI bundle.
 */
public class EthernetDecoderProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(EthernetDecoderProvider.class);
  Registration<NotificationListener> registration;

  /**
   * Setup the Ethernet Decoder.
   * @param consumerContext The context of the ethernet decoder.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    //ToDo: Replace with config subsystem call
    NotificationProviderService notificationProviderService = consumerContext.getSALService(NotificationProviderService.class);
    EthernetDecoder ethernetDecoder = new EthernetDecoder(notificationProviderService);
    registration = notificationProviderService.registerNotificationListener(ethernetDecoder);
  }
  /**
   * Cleanup the Ethernet Decoder
   */
  @Override
  public void close() throws Exception{
    registration.close();
  }
}
