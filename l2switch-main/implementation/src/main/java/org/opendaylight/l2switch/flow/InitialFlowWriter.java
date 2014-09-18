/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import com.google.common.collect.ImmutableList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.OpendaylightInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adds a flow, which drops all packets, on all switches.
 * Registers as ODL Inventory listener so that it can add flows once a new node i.e. switch is added
 */
public class InitialFlowWriter implements OpendaylightInventoryListener {
  private final Logger _logger = LoggerFactory.getLogger(InitialFlowWriter.class);

  private final ExecutorService initialFlowExecutor = Executors.newCachedThreadPool();
  private final SalFlowService salFlowService;
  private final short DEFAULT_FLOW_TABLE_ID = 0;
  private final int DEFAULT_FLOW_PRIORITY = 0;
  private final int DEFAULT_FLOW_IDLE_TIMEOUT = 0;
  private final int DEFAULT_FLOW_HARD_TIMEOUT = 0;

  private AtomicLong flowIdInc = new AtomicLong();
  private AtomicLong flowCookieInc = new AtomicLong(0x2b00000000000000L);
  private short flowTableId;
  private int flowPriority;
  private int flowIdleTimeout;
  private int flowHardTimeout;

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

  @Override
  public void onNodeConnectorRemoved(NodeConnectorRemoved nodeConnectorRemoved) {
    //do nothing
  }

  @Override
  public void onNodeConnectorUpdated(NodeConnectorUpdated nodeConnectorUpdated) {
    //do nothing
  }

  @Override
  public void onNodeRemoved(NodeRemoved nodeRemoved) {
    //do nothing
  }

  @Override
  public void onNodeUpdated(NodeUpdated nodeUpdated) {
    initialFlowExecutor.submit(new InitialFlowWriterProcessor(nodeUpdated));
  }

  /**
   * A private class to process the node updated event in separate thread. Allows to release the
   * thread that invoked the data node updated event. Avoids any thread lock it may cause.
   */
  private class InitialFlowWriterProcessor implements Runnable {
    private NodeUpdated nodeUpdated;

    public InitialFlowWriterProcessor(NodeUpdated nodeUpdated) {
      this.nodeUpdated = nodeUpdated;
    }

    @Override
    public void run() {

      if(nodeUpdated == null) {
        return;
      }

      addInitialFlows((InstanceIdentifier<Node>) nodeUpdated.getNodeRef().getValue());

    }

    /**
     * Adds a flow, which drops all packets, on the specifide node.
     * @param nodeId The node to install the flow on.
     */
    public void addInitialFlows(InstanceIdentifier<Node> nodeId) {
      _logger.debug("adding initial flows for node {} ", nodeId);

      InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
      InstanceIdentifier<Flow> flowId = getFlowInstanceId(tableId);

      //add drop all flow
      writeFlowToController(nodeId, tableId, flowId, createDropAllFlow(flowTableId, flowPriority));

      _logger.debug("Added initial flows for node {} ", nodeId);
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
      FlowId flowId = new FlowId(String.valueOf(flowIdInc.getAndIncrement()));
      FlowKey flowKey = new FlowKey(flowId);
      return tableId.child(Flow.class, flowKey);
    }

    private Flow createDropAllFlow(Short tableId, int priority) {

      // start building flow
      FlowBuilder dropAll = new FlowBuilder() //
          .setTableId(tableId) //
          .setFlowName("dropall");

      // use its own hash code for id.
      dropAll.setId(new FlowId(Long.toString(dropAll.hashCode())));

      Match match = new MatchBuilder().build();


      Action dropAllAction = new ActionBuilder() //
          .setOrder(0)
          .setAction(new DropActionCaseBuilder().build())
          .build();

      // Create an Apply Action
      ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(dropAllAction))
          .build();

      // Wrap our Apply Action in an Instruction
      Instruction applyActionsInstruction = new InstructionBuilder() //
          .setOrder(0)
          .setInstruction(new ApplyActionsCaseBuilder()//
              .setApplyActions(applyActions) //
              .build()) //
          .build();

      // Put our Instruction in a list of Instructions
      dropAll
          .setMatch(match) //
          .setInstructions(new InstructionsBuilder() //
              .setInstruction(ImmutableList.of(applyActionsInstruction)) //
              .build()) //
          .setPriority(priority) //
          .setBufferId(0L) //
          .setHardTimeout(flowHardTimeout) //
          .setIdleTimeout(flowIdleTimeout) //
          .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
          .setFlags(new FlowModFlags(false, false, false, false, false));

      return dropAll.build();
    }

    private Future<RpcResult<AddFlowOutput>> writeFlowToController(InstanceIdentifier<Node> nodeInstanceId,
                                                                   InstanceIdentifier<Table> tableInstanceId,
                                                                   InstanceIdentifier<Flow> flowPath,
                                                                   Flow flow) {
      final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
      builder.setNode(new NodeRef(nodeInstanceId));
      builder.setFlowRef(new FlowRef(flowPath));
      builder.setFlowTable(new FlowTableRef(tableInstanceId));
      builder.setTransactionUri(new Uri(flow.getId().getValue()));
      return salFlowService.addFlow(builder.build());
    }
  }
}
