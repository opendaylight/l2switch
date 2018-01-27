/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.flow;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a flow, which sends all LLDP packets to the controller, on all switches.
 * Registers as ODL Inventory listener so that it can add flows once a new node i.e. switch is added.
 */
public class InitialFlowWriter implements DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(InitialFlowWriter.class);

    private static final String FLOW_ID_PREFIX = "L2switch-";
    private static  final int LLDP_ETHER_TYPE = 35020;

    private final ExecutorService initialFlowExecutor = Executors.newCachedThreadPool();
    private final SalFlowService salFlowService;
    private short flowTableId;
    private int flowPriority;
    private int flowIdleTimeout;
    private int flowHardTimeout;

    private final AtomicLong flowIdInc = new AtomicLong();
    private final AtomicLong flowCookieInc = new AtomicLong(0x2b00000000000000L);


    public InitialFlowWriter(SalFlowService salFlowService) {
        this.salFlowService = salFlowService;
    }

    public void setFlowTableId(short flowTableId) {
        this.flowTableId = flowTableId;
    }

    public void setFlowPriority(int flowPriority) {
        this.flowPriority = flowPriority;
    }

    public void setFlowIdleTimeout(int flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    public void setFlowHardTimeout(int flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }

    public ListenerRegistration<InitialFlowWriter> registerAsDataChangeListener(DataBroker dataBroker) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();

        return dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                nodeInstanceIdentifier), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        Set<InstanceIdentifier<?>> nodeIds = new HashSet<>();
        for (DataTreeModification<Node> change: changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            final InstanceIdentifier<Node> identifier = change.getRootPath().getRootIdentifier();
            switch (rootNode.getModificationType()) {
                case WRITE:
                    if (rootNode.getDataBefore() == null) {
                        nodeIds.add(identifier);
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

        InitialFlowWriterProcessor(Set<InstanceIdentifier<?>> nodeIds) {
            this.nodeIds = nodeIds;
        }

        @Override
        public void run() {

            if (nodeIds == null) {
                return;
            }

            for (InstanceIdentifier<?> nodeId : nodeIds) {
                if (Node.class.isAssignableFrom(nodeId.getTargetType())) {
                    InstanceIdentifier<Node> topoNodeId = (InstanceIdentifier<Node>)nodeId;
                    if (topoNodeId.firstKeyOf(Node.class,NodeKey.class).getId().getValue().contains("openflow:")) {
                        addInitialFlows(topoNodeId);
                    }
                }
            }

        }

        /**
         * Adds a flow, which sends all LLDP packets to the controller, to the specified node.
         * @param nodeId The node to write the flow on.
         */
        public void addInitialFlows(InstanceIdentifier<Node> nodeId) {
            LOG.debug("adding initial flows for node {} ", nodeId);

            InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
            InstanceIdentifier<Flow> flowId = getFlowInstanceId(tableId);

            //add lldpToController flow
            writeFlowToController(nodeId, tableId, flowId, createLldpToControllerFlow(flowTableId, flowPriority));

            LOG.debug("Added initial flows for node {} ", nodeId);
        }

        private InstanceIdentifier<Table> getTableInstanceId(InstanceIdentifier<Node> nodeId) {
            // get flow table key
            TableKey flowTableKey = new TableKey(flowTableId);
            return nodeId.builder()
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class, flowTableKey)
                    .build();
        }

        private InstanceIdentifier<Flow> getFlowInstanceId(InstanceIdentifier<Table> tableId) {
            // generate unique flow key
            FlowId flowId = new FlowId(FLOW_ID_PREFIX + String.valueOf(flowIdInc.getAndIncrement()));
            FlowKey flowKey = new FlowKey(flowId);
            return tableId.child(Flow.class, flowKey);
        }

        private Flow createLldpToControllerFlow(Short tableId, int priority) {

            // start building flow
            FlowBuilder lldpFlow = new FlowBuilder() //
                    .setTableId(tableId) //
                    .setFlowName("lldptocntrl");

            // use its own hash code for id.
            lldpFlow.setId(new FlowId(Long.toString(lldpFlow.hashCode())));
            EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder()
                    .setEthernetType(new EthernetTypeBuilder()
                            .setType(new EtherType(Long.valueOf(LLDP_ETHER_TYPE))).build());

            Match match = new MatchBuilder()
                    .setEthernetMatch(ethernetMatchBuilder.build())
                    .build();

            // Create an Apply Action
            ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList
                    .of(getSendToControllerAction())).build();

            // Wrap our Apply Action in an Instruction
            Instruction applyActionsInstruction = new InstructionBuilder() //
                    .setOrder(0)
                    .setInstruction(new ApplyActionsCaseBuilder()//
                            .setApplyActions(applyActions) //
                            .build()) //
                    .build();

            // Put our Instruction in a list of Instructions
            lldpFlow
                    .setMatch(match) //
                    .setInstructions(new InstructionsBuilder() //
                            .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                            .build()) //
                    .setPriority(priority) //
                    .setBufferId(OFConstants.OFP_NO_BUFFER) //
                    .setHardTimeout(flowHardTimeout) //
                    .setIdleTimeout(flowIdleTimeout) //
                    .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                    .setFlags(new FlowModFlags(false, false, false, false, false));

            return lldpFlow.build();
        }

        private Action getSendToControllerAction() {
            Action sendToController = new ActionBuilder()
                    .setOrder(0)
                    .setKey(new ActionKey(0))
                    .setAction(new OutputActionCaseBuilder()
                            .setOutputAction(new OutputActionBuilder()
                                    .setMaxLength(0xffff)
                                    .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                                    .build())
                            .build())
                    .build();

            return sendToController;
        }

        private Future<RpcResult<AddFlowOutput>> writeFlowToController(InstanceIdentifier<Node> nodeInstanceId,
                                                                       InstanceIdentifier<Table> tableInstanceId,
                                                                       InstanceIdentifier<Flow> flowPath,
                                                                       Flow flow) {
            LOG.trace("Adding flow to node {}",nodeInstanceId.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(flowPath));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            builder.setTransactionUri(new Uri(flow.getId().getValue()));
            return salFlowService.addFlow(builder.build());
        }
    }
}
