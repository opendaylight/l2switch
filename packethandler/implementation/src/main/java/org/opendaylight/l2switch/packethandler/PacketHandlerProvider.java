/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import com.google.common.collect.ImmutableSet;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.packethandler.decoders.AbstractPacketDecoder;
import org.opendaylight.l2switch.packethandler.decoders.ArpDecoder;
import org.opendaylight.l2switch.packethandler.decoders.EthernetDecoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv4Decoder;
import org.opendaylight.l2switch.packethandler.decoders.Ipv6Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PacketHandlerProvider serves as the Activator for our L2Switch OSGI bundle.
 */
public class PacketHandlerProvider extends AbstractBindingAwareProvider
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(PacketHandlerProvider.class);

  ImmutableSet<AbstractPacketDecoder> decoders;


  /**
   * Setup the packet handler.
   *
   * @param providerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitiated(BindingAwareBroker.ProviderContext providerContext) {

    NotificationProviderService notificationProviderService =
        providerContext.<NotificationProviderService>getSALService(NotificationProviderService.class);

    initiateDecoders(notificationProviderService);

  }

  /**
   * Cleanup the packet handler..
   *
   * @throws Exception occurs when the NotificationListener is closed
   */
  @Override
  public void close() throws Exception {
    closeDecoders();
  }

  private void initiateDecoders(NotificationProviderService notificationProviderService) {
    decoders = new ImmutableSet.Builder<AbstractPacketDecoder>()
        .add(new EthernetDecoder(notificationProviderService))
        .add(new ArpDecoder(notificationProviderService))
        .add(new Ipv4Decoder(notificationProviderService))
        .add(new Ipv6Decoder(notificationProviderService))
        .build();
  }

  private void closeDecoders() throws Exception {
    if(decoders != null && !decoders.isEmpty()) {
      for(AbstractPacketDecoder decoder : decoders) {
        decoder.close();
      }
    }
  }
}