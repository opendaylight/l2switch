/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.packethandler;

import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PacketHandlerProvider serves as the Activator for our L2Switch OSGI bundle.
 */
public class PacketHandlerProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(PacketHandlerProvider.class);

  private Registration<NotificationListener> rawPacketListenerRegistration;


  /**
   * Setup the packet handler.
   *
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    DataBrokerService dataService = consumerContext.<DataBrokerService>getSALService(DataBrokerService.class);
    NotificationService notificationService =
        consumerContext.<NotificationService>getSALService(NotificationService.class);
    PacketProcessingService packetProcessingService =
        consumerContext.<PacketProcessingService>getRpcService(PacketProcessingService.class);
    RawPacketHandler rawPacketHandler = new RawPacketHandler();
    this.rawPacketListenerRegistration = notificationService.registerNotificationListener(rawPacketHandler);
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