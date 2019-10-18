/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.l2switch.util.InstanceIdentifierUtils;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of
 * FlowWriterService{@link org.opendaylight.l2switch.flow.FlowWriterService},
 * that builds required flow and writes to configuration data store using
 * provided {@link org.opendaylight.controller.md.sal.binding.api.DataBroker}.
 */
public class FlowWriterServiceImpl implements FlowWriterService {
    private static final Logger LOG = LoggerFactory.getLogger(FlowWriterServiceImpl.class);
    private static final String FLOW_ID_PREFIX = "L2switch-";

    private final SalFlowService salFlowService;
    private short flowTableId;
    private int flowPriority;
    private int flowIdleTimeout;
    private int flowHardTimeout;

    private final AtomicLong flowIdInc = new AtomicLong();
    private final AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);

    public FlowWriterServiceImpl(SalFlowService salFlowService) {
        Preconditions.checkNotNull(salFlowService, "salFlowService should not be null.");
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

    @Override
    public void addMacToMacFlow(MacAddress sourceMac, MacAddress destMac, NodeConnectorRef destNodeConnectorRef) {

        Preconditions.checkNotNull(destMac, "Destination mac address should not be null.");
        Preconditions.checkNotNull(destNodeConnectorRef, "Destination port should not be null.");

        // do not add flow if both macs are same.
        if (sourceMac != null && destMac.equals(sourceMac)) {
            LOG.info("In addMacToMacFlow: No flows added. Source and Destination mac are same.");
            return;
        }

        // get flow table key
        TableKey flowTableKey = new TableKey(flowTableId);

        // build a flow path based on node connector to program flow
        InstanceIdentifier<Flow> flowPath = buildFlowPath(destNodeConnectorRef, flowTableKey);

        // build a flow that target given mac id
        Flow flowBody = createMacToMacFlow(flowTableKey.getId(), flowPriority, sourceMac, destMac,
                destNodeConnectorRef);

        // commit the flow in config data
        writeFlowToConfigData(flowPath, flowBody);
    }

    @Override
    public void addBidirectionalMacToMacFlows(MacAddress sourceMac, NodeConnectorRef sourceNodeConnectorRef,
            MacAddress destMac, NodeConnectorRef destNodeConnectorRef) {
        Preconditions.checkNotNull(sourceMac, "Source mac address should not be null.");
        Preconditions.checkNotNull(sourceNodeConnectorRef, "Source port should not be null.");
        Preconditions.checkNotNull(destMac, "Destination mac address should not be null.");
        Preconditions.checkNotNull(destNodeConnectorRef, "Destination port should not be null.");

        if (sourceNodeConnectorRef.equals(destNodeConnectorRef)) {
            LOG.info(
                    "In addMacToMacFlowsUsingShortestPath: No flows added. Source and Destination ports are same.");
            return;

        }

        // add destMac-To-sourceMac flow on source port
        addMacToMacFlow(destMac, sourceMac, sourceNodeConnectorRef);

        // add sourceMac-To-destMac flow on destination port
        addMacToMacFlow(sourceMac, destMac, destNodeConnectorRef);
    }

    private InstanceIdentifier<Flow> buildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {

        // generate unique flow key
        FlowId flowId = new FlowId(FLOW_ID_PREFIX + String.valueOf(flowIdInc.getAndIncrement()));
        FlowKey flowKey = new FlowKey(flowId);

        return InstanceIdentifierUtils.generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
    }

    /**
     * Builds a flow that forwards all packets with destMac to given port.
     *
     * @param tableId the table id
     * @param priority the flow priority
     * @param sourceMac the source MAC of the flow
     * @param destMac the destination MAC of the flow
     * @param destPort the destination port
     * @return the Flow
     */
    private Flow createMacToMacFlow(Short tableId, int priority, MacAddress sourceMac, MacAddress destMac,
            NodeConnectorRef destPort) {

        // start building flow
        FlowBuilder macToMacFlow = new FlowBuilder() //
                .setTableId(tableId) //
                .setFlowName("mac2mac");

        // use its own hash code for id.
        macToMacFlow.setId(new FlowId(Long.toString(macToMacFlow.hashCode())));

        // create a match that has mac to mac ethernet match
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder() //
                .setEthernetDestination(new EthernetDestinationBuilder() //
                        .setAddress(destMac) //
                        .build());
        // set source in the match only if present
        if (sourceMac != null) {
            ethernetMatchBuilder.setEthernetSource(new EthernetSourceBuilder().setAddress(sourceMac).build());
        }
        EthernetMatch ethernetMatch = ethernetMatchBuilder.build();
        Match match = new MatchBuilder().setEthernetMatch(ethernetMatch).build();

        Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class).getId();

        Action outputToControllerAction = new ActionBuilder() //
                .setOrder(0)
                .setAction(new OutputActionCaseBuilder() //
                        .setOutputAction(new OutputActionBuilder() //
                                .setMaxLength(0xffff) //
                                .setOutputNodeConnector(destPortUri) //
                                .build()) //
                        .build()) //
                .build();

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
                .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()//
                        .setApplyActions(applyActions) //
                        .build()) //
                .build();

        // Put our Instruction in a list of Instructions
        macToMacFlow.setMatch(match) //
                .setInstructions(new InstructionsBuilder() //
                        .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                        .build()) //
                .setPriority(priority) //
                .setBufferId(OFConstants.OFP_NO_BUFFER) //
                .setHardTimeout(flowHardTimeout) //
                .setIdleTimeout(flowIdleTimeout) //
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        return macToMacFlow.build();
    }

    /**
     * Starts and commits data change transaction which modifies provided flow
     * path with supplied body.
     *
     * @param flowPath the Flow path
     * @param flow the Flow
     * @return transaction commit
     */
    private Future<RpcResult<AddFlowOutput>> writeFlowToConfigData(InstanceIdentifier<Flow> flowPath, Flow flow) {
        final InstanceIdentifier<Table> tableInstanceId = flowPath.<Table>firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeInstanceId = flowPath.<Node>firstIdentifierOf(Node.class);
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowRef(new FlowRef(flowPath));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setTransactionUri(new Uri(flow.getId().getValue()));
        return salFlowService.addFlow(builder.build());
    }
}
