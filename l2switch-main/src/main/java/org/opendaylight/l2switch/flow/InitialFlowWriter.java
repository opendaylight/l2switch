/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a flow, which drops all packets, on all switches.
 * Registers as ODL Inventory listener so that it can add flows once a new node i.e. switch is added.
 */
public class InitialFlowWriter implements DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(InitialFlowWriter.class);
    private static final String FLOW_ID_PREFIX = "L2switch-";

    private final ExecutorService initialFlowExecutor = Executors.newCachedThreadPool();
    private final AddFlow addFlow;

    private final AtomicLong flowIdInc = new AtomicLong();
    private final AtomicLong flowCookieInc = new AtomicLong(0x2b00000000000000L);
    private Uint8 flowTableId = Uint8.ZERO;
    private Uint16 flowPriority = Uint16.ZERO;
    private Uint16 flowIdleTimeout = Uint16.ZERO;
    private Uint16 flowHardTimeout = Uint16.ZERO;

    public InitialFlowWriter(final AddFlow flowService) {
        this.addFlow = flowService;
    }

    public void setFlowTableId(final Uint8 flowTableId) {
        this.flowTableId = flowTableId;
    }

    public void setFlowPriority(final Uint16 flowPriority) {
        this.flowPriority = flowPriority;
    }

    public void setFlowIdleTimeout(final Uint16 flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    public void setFlowHardTimeout(final Uint16 flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }

    public ListenerRegistration<InitialFlowWriter> registerAsDataChangeListener(final DataBroker dataBroker) {
        return dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(Nodes.class).child(Node.class).build()), this);
    }

    @Override
    public void onDataTreeChanged(final List<DataTreeModification<Node>> changes) {
        final var nodeIds = new HashSet<InstanceIdentifier<?>>();
        for (var change : changes) {
            final var rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    if (rootNode.getDataBefore() == null) {
                        nodeIds.add(change.getRootPath().getRootIdentifier());
                    }
                    break;
                default:
                    break;
            }
        }

        if (!nodeIds.isEmpty()) {
            initialFlowExecutor.execute(new InitialFlowWriterProcessor(nodeIds));
        }
    }

    /**
     * A private class to process the node updated event in separate thread. Allows to release the
     * thread that invoked the data node updated event. Avoids any thread lock it may cause.
     */
    private class InitialFlowWriterProcessor implements Runnable {
        private final Set<InstanceIdentifier<?>> nodeIds;

        InitialFlowWriterProcessor(final Set<InstanceIdentifier<?>> nodeIds) {
            this.nodeIds = requireNonNull(nodeIds);
        }

        @Override
        public void run() {
            for (var nodeId : nodeIds) {
                if (Node.class.isAssignableFrom(nodeId.getTargetType())) {
                    final var invNodeId = (InstanceIdentifier<Node>) nodeId;
                    if (invNodeId.firstKeyOf(Node.class).getId().getValue().contains("openflow:")) {
                        addInitialFlows(invNodeId);
                    }
                }
            }
        }

        /**
         * Adds a flow, which drops all packets, on the specifide node.
         * @param nodeId The node to install the flow on.
         */
        public void addInitialFlows(final InstanceIdentifier<Node> nodeId) {
            LOG.debug("adding initial flows for node {} ", nodeId);

            final var tableId = getTableInstanceId(nodeId);
            final var flowId = getFlowInstanceId(tableId);

            // add drop all flow
            writeFlowToController(nodeId, tableId, flowId, createDropAllFlow(flowTableId, flowPriority));

            LOG.debug("Added initial flows for node {} ", nodeId);
        }

        private InstanceIdentifier<Table> getTableInstanceId(final InstanceIdentifier<Node> nodeId) {
            return nodeId.builder()
                .augmentation(FlowCapableNode.class)
                // get flow table key
                .child(Table.class, new TableKey(flowTableId))
                .build();
        }

        private InstanceIdentifier<Flow> getFlowInstanceId(final InstanceIdentifier<Table> tableId) {
            return tableId.child(Flow.class,
                // generate unique flow key
                new FlowKey(new FlowId(FLOW_ID_PREFIX + String.valueOf(flowIdInc.getAndIncrement()))));
        }

        private Flow createDropAllFlow(final Uint8 tableId, final Uint16 priority) {

            // start building flow
            final var dropAll = new FlowBuilder()
                .setTableId(tableId)
                .setFlowName("dropall");

            return dropAll
                // use its own hash code for id.
                .setId(new FlowId(Long.toString(dropAll.hashCode())))
                .setMatch(new MatchBuilder().build())
                // Put our Instruction in a list of Instructions
                .setInstructions(new InstructionsBuilder()
                    // Wrap our Apply Action in an Instruction
                    .setInstruction(BindingMap.of(new InstructionBuilder()
                        .setOrder(0)
                        .setInstruction(new ApplyActionsCaseBuilder()
                            // Create an Apply Action
                            .setApplyActions(new ApplyActionsBuilder().setAction(BindingMap.of(new ActionBuilder()
                                .setOrder(0)
                                .setAction(new DropActionCaseBuilder().build())
                                .build()))
                                .build())
                            .build())
                        .build()))
                    .build())
                .setPriority(priority)
                .setBufferId(OFConstants.OFP_NO_BUFFER)
                .setHardTimeout(flowHardTimeout)
                .setIdleTimeout(flowIdleTimeout)
                .setCookie(new FlowCookie(Uint64.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false))
                .build();
        }

        private Future<RpcResult<AddFlowOutput>> writeFlowToController(final InstanceIdentifier<Node> nodeInstanceId,
                                                                       final InstanceIdentifier<Table> tableInstanceId,
                                                                       final InstanceIdentifier<Flow> flowPath,
                                                                       final Flow flow) {
            LOG.trace("Adding flow to node {}", nodeInstanceId.firstKeyOf(Node.class).getId().getValue());
            return addFlow.invoke(new AddFlowInputBuilder(flow)
                .setNode(new NodeRef(nodeInstanceId))
                .setFlowRef(new FlowRef(flowPath))
                .setFlowTable(new FlowTableRef(tableInstanceId))
                .setTransactionUri(new Uri(flow.getId().getValue()))
                .build());
        }
    }
}
