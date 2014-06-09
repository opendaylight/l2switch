/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.l2switch.packethandler.decoders.DecoderRegistry;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderService;
import org.opendaylight.l2switch.packethandler.decoders.PacketDecoderServiceImpl;
import org.opendaylight.l2switch.packethandler.decoders.PacketNotificationRegistry;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PacketHandlerProvider serves as the Activator for our L2Switch OSGI bundle.
 */
public class PacketHandlerProvider extends AbstractBindingAwareProvider
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(PacketHandlerProvider.class);

  private Registration<NotificationListener> rawPacketListenerRegistration;


  /**
   * Setup the packet handler.
   *
   * @param providerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitiated(BindingAwareBroker.ProviderContext providerContext) {
    DataBrokerService dataService = providerContext.<DataBrokerService>getSALService(DataBrokerService.class);

    NotificationProviderService notificationProviderService =
        providerContext.<NotificationProviderService>getSALService(NotificationProviderService.class);

    DecoderRegistry decoderRegistry = new DecoderRegistry();

    PacketNotificationRegistry packetNotificationRegistry = new PacketNotificationRegistry();
    notificationProviderService.registerInterestListener(packetNotificationRegistry);

    PacketDecoderService packetDecoderService = new PacketDecoderServiceImpl(decoderRegistry, packetNotificationRegistry);

    RawPacketHandler rawPacketHandler = new RawPacketHandler();
    rawPacketHandler.setNotificationProviderService(notificationProviderService);
    rawPacketHandler.setDecoderRegistry(decoderRegistry);
    rawPacketHandler.setPacketNotificationRegistry(packetNotificationRegistry);
    this.rawPacketListenerRegistration = notificationProviderService.registerNotificationListener(rawPacketHandler);
  }

  /**
   * Cleanup the packet handler..
   *
   * @throws Exception occurs when the NotificationListener is closed
   */
  @Override
  public void close() throws Exception {
    if(rawPacketListenerRegistration != null)
      rawPacketListenerRegistration.close();
  }
}