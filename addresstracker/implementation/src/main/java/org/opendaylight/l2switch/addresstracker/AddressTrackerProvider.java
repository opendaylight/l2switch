/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObservationWriter;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObserver;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class AddressTrackerProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(AddressTrackerProvider.class);
  private Registration listenerRegistration;

  /**
   * Setup the L2Switch.
   *
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    // Setup FlowWriterService
    DataBroker dataService = consumerContext.<DataBroker>getSALService(DataBroker.class);

    // Setup AddressObserver & AddressObservationWriter
    AddressObservationWriter addressObservationWriter = new AddressObservationWriter(dataService);
    AddressObserver addressObserver = new AddressObserver(addressObservationWriter);

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
