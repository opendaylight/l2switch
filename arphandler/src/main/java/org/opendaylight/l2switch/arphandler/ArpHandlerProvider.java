/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.l2switch.arphandler.core.ArpPacketHandler;
import org.opendaylight.l2switch.arphandler.core.PacketDispatcher;
import org.opendaylight.l2switch.arphandler.core.ProactiveFloodFlowWriter;
import org.opendaylight.l2switch.arphandler.flow.InitialFlowWriter;
import org.opendaylight.l2switch.arphandler.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ArpHandlerProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(ArpHandlerProvider.class);
  private Registration listenerRegistration = null, floodListenerReg = null, invListenerReg = null;

  @Override
  public void close() throws Exception {
    if(listenerRegistration != null) {
      listenerRegistration.close();
    }
    if(floodListenerReg != null) {
      floodListenerReg.close();
    }
    if(invListenerReg != null) {
      invListenerReg.close();
    }
  }

  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    DataBroker dataService = consumerContext.<DataBroker>getSALService(DataBroker.class);
    NotificationService notificationService = consumerContext.<NotificationService>getSALService(NotificationService.class);
    SalFlowService salFlowService = consumerContext.getRpcService(SalFlowService.class);

    if(isProactiveFloodMode()) {
      //Setup proactive flow writer, which writes flood flows
      ProactiveFloodFlowWriter floodFlowWriter = new ProactiveFloodFlowWriter(dataService, salFlowService);
      floodListenerReg = floodFlowWriter.registerAsDataChangeListener();
    } else {

      //Write initial flows to send arp to controller
      InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
      invListenerReg = notificationService.registerNotificationListener(initialFlowWriter);

      // Setup InventoryReader
      InventoryReader inventoryReader = new InventoryReader(dataService);

      // Setup PacketDispatcher
      PacketProcessingService packetProcessingService =
          consumerContext.<PacketProcessingService>getRpcService(PacketProcessingService.class);
      PacketDispatcher packetDispatcher = new PacketDispatcher();
      packetDispatcher.setInventoryReader(inventoryReader);
      packetDispatcher.setPacketProcessingService(packetProcessingService);

      // Setup ArpPacketHandler
      ArpPacketHandler arpPacketHandler = new ArpPacketHandler(packetDispatcher);

      // Register ArpPacketHandler
      this.listenerRegistration = notificationService.registerNotificationListener(arpPacketHandler);
    }
  }

  /**
   * Reads config subsystem to determine the flood mode (proactive or reactive)
   *
   * @return True if the flood mode is proactive
   */
  private boolean isProactiveFloodMode() {
    // Config Subsystem integration to be done
    // For now, proactive mode is always enabled for performance
    return true;
  }


}
