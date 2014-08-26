/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.l2switch.loopremover.flow.InitialFlowWriter;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphImpl;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphService;
import org.opendaylight.l2switch.loopremover.topology.TopologyLinkDataChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class LoopRemoverProvider extends AbstractBindingAwareConsumer
    implements AutoCloseable {

  private final static Logger _logger = LoggerFactory.getLogger(LoopRemoverProvider.class);
  private Registration listenerRegistration = null, invListenerReg = null;
  private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;

  /**
   * Setup the loop remover.
   *
   * @param consumerContext The context of the L2Switch.
   */
  @Override
  public void onSessionInitialized(BindingAwareBroker.ConsumerContext consumerContext) {
    // Setup FlowWriterService
    DataBroker dataService = consumerContext.<DataBroker>getSALService(DataBroker.class);
    NetworkGraphService networkGraphService = new NetworkGraphImpl();
    SalFlowService salFlowService = consumerContext.getRpcService(SalFlowService.class);
    NotificationService notificationService = consumerContext.<NotificationService>getSALService(NotificationService.class);

    //Write initial flows
    InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
    invListenerReg = notificationService.registerNotificationListener(initialFlowWriter);

    // Register Topology DataChangeListener
    this.topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataService, networkGraphService);
    listenerRegistration = topologyLinkDataChangeHandler.registerAsDataChangeListener();

  }

  /**
   * Cleanup the loop remover
   */
  @Override
  public void close() throws Exception {
    if(listenerRegistration != null) {
      listenerRegistration.close();
    }
    if(invListenerReg != null) {
      invListenerReg.close();
    }
  }
}
