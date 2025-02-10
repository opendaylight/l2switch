/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover;

import org.opendaylight.l2switch.loopremover.flow.InitialFlowWriter;
import org.opendaylight.l2switch.loopremover.topology.NetworkGraphImpl;
import org.opendaylight.l2switch.loopremover.topology.TopologyLinkDataChangeHandler;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.loop.remover.config.rev140528.LoopRemoverConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: @Component(service = { }) once we have the required constructor
public final class LoopRemoverProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(LoopRemoverProvider.class);

    private Registration listenerRegistration;
    private Registration topoNodeListnerReg;

    // FIXME: an @Activate constructor which deals with dynamic config
    public LoopRemoverProvider(final DataBroker dataBroker, final RpcConsumerRegistry rpcService,
            final LoopRemoverConfig config) {
        // Write initial flows
        if (config.getIsInstallLldpFlow()) {
            LOG.info("LoopRemover will install an lldp flow");
            var initialFlowWriter = new InitialFlowWriter(rpcService.getRpc(AddFlow.class));
            initialFlowWriter.setFlowTableId(config.getLldpFlowTableId());
            initialFlowWriter.setFlowPriority(config.getLldpFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(config.getLldpFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(config.getLldpFlowHardTimeout());
            topoNodeListnerReg = initialFlowWriter.registerAsDataChangeListener(dataBroker);
        }

        // Register Topology DataChangeListener
        var topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataBroker, new NetworkGraphImpl());
        topologyLinkDataChangeHandler.setGraphRefreshDelay(config.getGraphRefreshDelay().toJava());
        topologyLinkDataChangeHandler.setTopologyId(config.getTopologyId());
        listenerRegistration = topologyLinkDataChangeHandler.registerAsDataChangeListener();
        LOG.info("LoopRemover initialized.");
    }

    // FIXME: @Deactivate
    @Override
    public void close() {
        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }
        if (topoNodeListnerReg != null) {
            topoNodeListnerReg.close();
            topoNodeListnerReg = null;
        }
        LOG.info("LoopRemover (instance {}) torn down.", this);
    }
}
