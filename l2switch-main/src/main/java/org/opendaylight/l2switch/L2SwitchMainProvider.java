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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.l2switch.config.rev140528.L2switchConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class L2SwitchMainProvider {
    private static final Logger LOG = LoggerFactory.getLogger(L2SwitchMainProvider.class);
    private Registration topoNodeListherReg;
    private Registration reactFlowWriterReg;

    private final DataBroker dataService;
    private final NotificationService notificationService;
    private final SalFlowService salFlowService;
    private final L2switchConfig mainConfig;


    public L2SwitchMainProvider(final DataBroker dataBroker,
            final NotificationService notificationPublishService,
            final SalFlowService salFlowService, final L2switchConfig config) {
        this.dataService = dataBroker;
        this.notificationService = notificationPublishService;
        this.salFlowService = salFlowService;
        this.mainConfig = config;
    }

    public void init() {
        // Setup FlowWrtierService
        FlowWriterServiceImpl flowWriterService = new FlowWriterServiceImpl(salFlowService);
        flowWriterService.setFlowTableId(Uint16.valueOf(mainConfig.getReactiveFlowTableId()).shortValue());
        flowWriterService.setFlowPriority(mainConfig.getReactiveFlowPriority().intValue());
        flowWriterService.setFlowIdleTimeout(mainConfig.getReactiveFlowIdleTimeout().intValue());
        flowWriterService.setFlowHardTimeout(mainConfig.getReactiveFlowHardTimeout().intValue());

        // Setup InventoryReader
        InventoryReader inventoryReader = new InventoryReader(dataService);

        // Write initial flows
        if (mainConfig.isIsInstallDropallFlow()) {
            LOG.info("L2Switch will install a dropall flow on each switch");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(Uint16.valueOf(mainConfig.getDropallFlowTableId()).shortValue());
            initialFlowWriter.setFlowPriority(mainConfig.getDropallFlowPriority().intValue());
            initialFlowWriter.setFlowIdleTimeout(mainConfig.getDropallFlowIdleTimeout().intValue());
            initialFlowWriter.setFlowHardTimeout(mainConfig.getDropallFlowHardTimeout().intValue());
            topoNodeListherReg = initialFlowWriter.registerAsDataChangeListener(dataService);
        }
        else {
            LOG.info("Dropall flows will not be installed");
        }

        if (mainConfig.isIsLearningOnlyMode()) {
            LOG.info("L2Switch is in Learning Only Mode");
        }
        else {
            // Setup reactive flow writer
            LOG.info("L2Switch will react to network traffic and install flows");
            ReactiveFlowWriter reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader, flowWriterService);
            reactFlowWriterReg = notificationService.registerNotificationListener(reactiveFlowWriter);
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
