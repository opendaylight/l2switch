/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch;

import org.opendaylight.l2switch.flow.FlowWriterServiceImpl;
import org.opendaylight.l2switch.flow.InitialFlowWriter;
import org.opendaylight.l2switch.flow.ReactiveFlowWriter;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.l2switch.config.rev140528.L2switchConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2SwitchMainProvider {
    private static final Logger LOG = LoggerFactory.getLogger(L2SwitchMainProvider.class);
    private Registration topoNodeListherReg;
    private Registration reactFlowWriterReg;

    private final DataBroker dataService;
    private final NotificationService notificationService;
    private final RpcService rpcFlowService;
    private final L2switchConfig mainConfig;

    public L2SwitchMainProvider(final DataBroker dataBroker,
            final NotificationService notificationPublishService,
            final RpcService rpcService, final L2switchConfig config) {
        this.dataService = dataBroker;
        this.notificationService = notificationPublishService;
        this.rpcFlowService = rpcService;
        this.mainConfig = config;
    }

    public void init() {
        // Setup FlowWrtierService
        FlowWriterServiceImpl flowWriterService = new FlowWriterServiceImpl(rpcFlowService.getRpc(AddFlow.class));
        flowWriterService.setFlowTableId(mainConfig.getReactiveFlowTableId());
        flowWriterService.setFlowPriority(mainConfig.getReactiveFlowPriority());
        flowWriterService.setFlowIdleTimeout(mainConfig.getReactiveFlowIdleTimeout());
        flowWriterService.setFlowHardTimeout(mainConfig.getReactiveFlowHardTimeout());

        // Setup InventoryReader
        InventoryReader inventoryReader = new InventoryReader(dataService);

        // Write initial flows
        if (mainConfig.getIsInstallDropallFlow()) {
            LOG.info("L2Switch will install a dropall flow on each switch");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(rpcFlowService.getRpc(AddFlow.class));
            initialFlowWriter.setFlowTableId(mainConfig.getDropallFlowTableId());
            initialFlowWriter.setFlowPriority(mainConfig.getDropallFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(mainConfig.getDropallFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(mainConfig.getDropallFlowHardTimeout());
            topoNodeListherReg = initialFlowWriter.registerAsDataChangeListener(dataService);
        }
        else {
            LOG.info("Dropall flows will not be installed");
        }

        if (mainConfig.getIsLearningOnlyMode()) {
            LOG.info("L2Switch is in Learning Only Mode");
        }
        else {
            // Setup reactive flow writer
            LOG.info("L2Switch will react to network traffic and install flows");
            ReactiveFlowWriter reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader, flowWriterService);
            reactFlowWriterReg = notificationService.registerListener(ArpPacketReceived.class, reactiveFlowWriter);
        }
        LOG.info("L2SwitchMain initialized.");
    }

    public void close() {
        if (reactFlowWriterReg != null) {
            reactFlowWriterReg.close();
        }

        if (topoNodeListherReg != null) {
            topoNodeListherReg.close();
        }
        LOG.info("L2SwitchMain (instance {}) torn down.", this);
    }
}
