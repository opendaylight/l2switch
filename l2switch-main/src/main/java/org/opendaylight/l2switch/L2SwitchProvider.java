/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.l2switch.addressobserver.AddressObservationWriter;
import org.opendaylight.l2switch.addressobserver.AddressObserver;
import org.opendaylight.l2switch.flow.FlowWriterService;
import org.opendaylight.l2switch.flow.FlowWriterServiceImpl;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.l2switch.packet.PacketDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * L2SwitchProvider serves as the Activator for our L2Switch-Main OSGI bundle.
 */
public class L2SwitchProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(L2SwitchProvider.class);
  private Registration<NotificationListener> listenerRegistration;

  /**
   * Setup the L2Switch.
   *
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    // Setup FlowWriterService
    DataBroker dataService = consumerContext.<DataBroker>getSALService(DataBroker.class);
    FlowWriterService flowWriterService = new FlowWriterServiceImpl(dataService);

    // Setup InventoryReader
    InventoryReader inventoryReader = new InventoryReader(dataService);

    // Setup PacketDispatcher
    PacketProcessingService packetProcessingService =
        consumerContext.<PacketProcessingService>getRpcService(PacketProcessingService.class);
    PacketDispatcher packetDispatcher = new PacketDispatcher();
    packetDispatcher.setInventoryReader(inventoryReader);
    packetDispatcher.setPacketProcessingService(packetProcessingService);
    packetDispatcher.setFlowWriterService(flowWriterService);

    // Setup AddressObserver & AddressObservationWriter
    AddressObservationWriter addressObservationWriter = new AddressObservationWriter(dataService);
    AddressObserver addressObserver = new AddressObserver(inventoryReader, addressObservationWriter, packetDispatcher);

    // Register AddressObserver for notifications
    NotificationService notificationService = consumerContext.<NotificationService>getSALService(NotificationService.class);
    this.listenerRegistration = notificationService.registerNotificationListener(addressObserver);
  }

  /**
   * Cleanup the L2Switch
   */
  @Override
  public void close() throws Exception {
    if(listenerRegistration != null) {
      listenerRegistration.close();
    }
  }
}
