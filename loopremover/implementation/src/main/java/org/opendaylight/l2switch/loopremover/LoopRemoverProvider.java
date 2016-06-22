/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.l2switch.loopremover.flow.InitialFlowWriter;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphImpl;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphService;
import org.opendaylight.l2switch.loopremover.topology.TopologyLinkDataChangeHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.loop.remover.config.rev140528.LoopRemoverConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopRemoverProvider {

    private final static Logger LOG = LoggerFactory.getLogger(LoopRemoverProvider.class);

    private final DataBroker dataService;
    private final SalFlowService salFlowService;
    private final LoopRemoverConfig loopRemoverConfig;

    private Registration listenerRegistration = null, topoNodeListnerReg = null;
    private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;

    public LoopRemoverProvider(final DataBroker dataBroker,
            final SalFlowService salFlowService,
            final LoopRemoverConfig config) {
        this.dataService = dataBroker;
        this.salFlowService = salFlowService;
        this.loopRemoverConfig = config;
    }

    public void init() {
        //Write initial flows
        if (loopRemoverConfig.isIsInstallLldpFlow()) {
            LOG.info("LoopRemover will install an lldp flow");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(loopRemoverConfig.getLldpFlowTableId());
            initialFlowWriter.setFlowPriority(loopRemoverConfig.getLldpFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(loopRemoverConfig.getLldpFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(loopRemoverConfig.getLldpFlowHardTimeout());
            topoNodeListnerReg = initialFlowWriter.registerAsDataChangeListener(dataService);
        }

        // Register Topology DataChangeListener
        NetworkGraphService networkGraphService = new NetworkGraphImpl();
        this.topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataService, networkGraphService);
        topologyLinkDataChangeHandler.setGraphRefreshDelay(loopRemoverConfig.getGraphRefreshDelay());
        topologyLinkDataChangeHandler.setTopologyId(loopRemoverConfig.getTopologyId());
        listenerRegistration = topologyLinkDataChangeHandler.registerAsDataChangeListener();

        LOG.info("LoopRemover initialized.");
    }

    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
        if (topoNodeListnerReg != null) {
            topoNodeListnerReg.close();
        }
        LOG.info("LoopRemover (instance {}) torn down.", this);
    }
}
