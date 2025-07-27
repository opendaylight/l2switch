/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketReceived;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.util.BindingMap;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProactiveFloodFlowWriter is used for the proactive mode of L2Switch. In this
 * mode, flood flows are automatically written to each switch and less traffic
 * is sent to the controller.
 */
public class ProactiveFloodFlowWriter implements DataTreeChangeListener<StpStatusAwareNodeConnector>,
        NotificationService.Listener<EthernetPacketReceived> {

    private static final Logger LOG = LoggerFactory.getLogger(ProactiveFloodFlowWriter.class);

    private static final String FLOW_ID_PREFIX = "L2switch-";

    private final DataBroker dataBroker;
    private final AddFlow addFlow;
    private final ScheduledExecutorService stpStatusDataChangeEventProcessor = Executors.newScheduledThreadPool(1);
    private volatile boolean flowRefreshScheduled = false;
    private volatile boolean threadReschedule = false;
    private long flowInstallationDelay;
    private Uint8 flowTableId = Uint8.ZERO;
    private Uint16 flowPriority = Uint16.ZERO;
    private Uint16 flowIdleTimeout = Uint16.ZERO;
    private Uint16 flowHardTimeout = Uint16.ZERO;
    private final AtomicLong flowIdInc = new AtomicLong();
    private final AtomicLong flowCookieInc = new AtomicLong(0x2b00000000000000L);

    public ProactiveFloodFlowWriter(DataBroker dataBroker, AddFlow addFlow) {
        this.dataBroker = requireNonNull(dataBroker);
        this.addFlow = requireNonNull(addFlow);
    }

    public void setFlowInstallationDelay(long flowInstallationDelay) {
        this.flowInstallationDelay = flowInstallationDelay;
    }

    public void setFlowTableId(Uint8 flowTableId) {
        this.flowTableId = requireNonNull(flowTableId);
    }

    public void setFlowPriority(Uint16 flowPriority) {
        this.flowPriority = requireNonNull(flowPriority);
    }

    public void setFlowIdleTimeout(Uint16 flowIdleTimeout) {
        this.flowIdleTimeout = requireNonNull(flowIdleTimeout);
    }

    public void setFlowHardTimeout(Uint16 flowHardTimeout) {
        this.flowHardTimeout = requireNonNull(flowHardTimeout);
    }

    @Override
    public void onNotification(EthernetPacketReceived packetRecevied) {
        if (!flowRefreshScheduled) {
            synchronized (this) {
                if (!flowRefreshScheduled) {
                    stpStatusDataChangeEventProcessor.schedule(new StpStatusDataChangeEventProcessor(),
                            flowInstallationDelay, TimeUnit.MILLISECONDS);
                    flowRefreshScheduled = true;
                    LOG.debug("Scheduled Flows for refresh.");
                }
            }
        } else {
            LOG.debug("Already scheduled for flow refresh.");
            threadReschedule = true;
        }
    }

    /**
     * Registers as a data listener for Nodes.
     */
    public Registration registerAsDataChangeListener() {
        return dataBroker.registerDataTreeChangeListener(
            DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Nodes.class)
                .child(Node.class)
                .child(NodeConnector.class)
                .augmentation(StpStatusAwareNodeConnector.class)
                .build()), this);
    }

    /**
     * Install flows when a link comes up/down.
     */
    @Override
    public void onDataTreeChanged(List<DataTreeModification<StpStatusAwareNodeConnector>> changes) {
        if (!flowRefreshScheduled) {
            synchronized (this) {
                if (!flowRefreshScheduled) {
                    stpStatusDataChangeEventProcessor.schedule(new StpStatusDataChangeEventProcessor(),
                            flowInstallationDelay, TimeUnit.MILLISECONDS);
                    flowRefreshScheduled = true;
                    LOG.debug("Scheduled Flows for refresh.");
                }
            }
        } else {
            LOG.debug("Already scheduled for flow refresh.");
            threadReschedule = true;
        }
    }

    private final class StpStatusDataChangeEventProcessor implements Runnable {
        @Override
        public void run() {
            LOG.debug("In flow refresh thread.");
            if (threadReschedule) {
                LOG.debug("Rescheduling thread");
                stpStatusDataChangeEventProcessor.schedule(this, flowInstallationDelay, TimeUnit.MILLISECONDS);
                threadReschedule = false;
                return;
            }

            flowRefreshScheduled = false;
            installFloodFlows();
        }

        /**
         * Installs a FloodFlow on each node.
         */
        private void installFloodFlows() {
            final FluentFuture<Optional<Nodes>> readFuture;
            try (ReadTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction()) {
                readFuture = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL,
                    InstanceIdentifier.create(Nodes.class));
            }

            final Nodes nodes;
            try {
                nodes = readFuture.get().orElse(null);
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to read nodes from Operation data store.");
                throw new RuntimeException("Failed to read nodes from Operation data store.", e);
            }

            if (nodes == null) {
                // Reschedule thread when the data store read had errors
                LOG.debug("Rescheduling flow refresh thread because datastore read failed.");
                if (!flowRefreshScheduled) {
                    flowRefreshScheduled = true;
                    stpStatusDataChangeEventProcessor.schedule(this, flowInstallationDelay, TimeUnit.MILLISECONDS);
                }
            } else {
                for (Node node : nodes.nonnullNode().values()) {
                    // Install a FloodFlow on each node
                    final Map<NodeConnectorKey, NodeConnector> nodeConnectors = node.getNodeConnector();
                    if (nodeConnectors != null) {
                        for (NodeConnector outerNodeConnector : nodeConnectors.values()) {
                            StpStatusAwareNodeConnector outerSaNodeConnector = outerNodeConnector
                                    .augmentation(StpStatusAwareNodeConnector.class);
                            if (outerSaNodeConnector != null
                                    && StpStatus.Discarding.equals(outerSaNodeConnector.getStatus())) {
                                continue;
                            }
                            if (!outerNodeConnector.getId().toString().contains("LOCAL")) {
                                int order = 0;
                                ArrayList<Action> outputActions = new ArrayList<>();
                                for (NodeConnector nodeConnector : nodeConnectors.values()) {
                                    if (!nodeConnector.getId().toString().contains("LOCAL")
                                            && !outerNodeConnector.equals(nodeConnector)) {
                                        // NodeConnectors without STP status
                                        // (external ports) and NodeConnectors
                                        // that are "forwarding" will be flooded
                                        // on
                                        StpStatusAwareNodeConnector saNodeConnector = nodeConnector
                                                .augmentation(StpStatusAwareNodeConnector.class);
                                        if (saNodeConnector == null
                                                || StpStatus.Forwarding.equals(saNodeConnector.getStatus())) {
                                            outputActions.add(new ActionBuilder()
                                                .setOrder(order++)
                                                .setAction(
                                                    new OutputActionCaseBuilder()
                                                    .setOutputAction(new OutputActionBuilder()
                                                        .setMaxLength(Uint16.MAX_VALUE)
                                                        .setOutputNodeConnector(nodeConnector.getId())
                                                        .build())
                                                    .build())
                                                .build());
                                        }
                                    }
                                }

                                // Add controller port to outputActions for
                                // external ports only
                                if (outerSaNodeConnector == null) {
                                    outputActions.add(new ActionBuilder().withKey(new ActionKey(order++))
                                        .setAction(
                                            new OutputActionCaseBuilder()
                                            .setOutputAction(new OutputActionBuilder()
                                                .setMaxLength(Uint16.MAX_VALUE)
                                                .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                                .build())
                                            .build())
                                        .build());
                                }

                                // Create an Apply Action
                                ApplyActions applyActions = new ApplyActionsBuilder()
                                        .setAction(BindingMap.ordered(outputActions)).build();

                                // Wrap our Apply Action in an Instruction
                                Instruction applyActionsInstruction = new InstructionBuilder()
                                        .setOrder(0)
                                        .setInstruction(new ApplyActionsCaseBuilder()
                                                .setApplyActions(applyActions)
                                                .build())
                                        .build();

                                FlowBuilder floodFlowBuilder = createBaseFlowForPortMatch(outerNodeConnector);
                                floodFlowBuilder.setInstructions(new InstructionsBuilder()
                                        .setInstruction(BindingMap.of(applyActionsInstruction))
                                        .build());

                                writeFlowToSwitch(node.getId(), floodFlowBuilder.build());
                            }
                        }
                    }
                }
            }
        }

        private FlowBuilder createBaseFlowForPortMatch(NodeConnector nc) {
            FlowBuilder floodFlow = new FlowBuilder().setTableId(flowTableId).setFlowName("flood");
            floodFlow.setId(new FlowId(Long.toString(floodFlow.hashCode())));

            Match match = new MatchBuilder().setInPort(nc.getId()).build();

            floodFlow.setMatch(match)
                    .setPriority(flowPriority)
                    .setBufferId(OFConstants.OFP_NO_BUFFER)
                    .setHardTimeout(flowHardTimeout)
                    .setIdleTimeout(flowIdleTimeout)
                    .setCookie(new FlowCookie(Uint64.fromLongBits(flowCookieInc.getAndIncrement())))
                    .setFlags(new FlowModFlags(false, false, false, false, false));
            return floodFlow;
        }

        /**
         * Starts and commits data change transaction which modifies provided
         * flow path with supplied body.
         */
        private Future<RpcResult<AddFlowOutput>> writeFlowToSwitch(NodeId nodeId, Flow flow) {
            final var nodeInstanceId = DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .build();
            final var tableInstanceId = nodeInstanceId.toBuilder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowTableId))
                .build();
            final var flowPath = tableInstanceId.toBuilder()
                .child(Flow.class, new FlowKey(new FlowId(FLOW_ID_PREFIX + flowIdInc.getAndIncrement())))
                .build();

            return addFlow.invoke(new AddFlowInputBuilder(flow)
                .setNode(new NodeRef(nodeInstanceId))
                .setFlowTable(new FlowTableRef(tableInstanceId))
                .setFlowRef(new FlowRef(flowPath))
                .setTransactionUri(new Uri(flow.getId().getValue()))
                .build());
        }
    }
}
