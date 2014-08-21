/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ProactiveFloodFlowWriter is used for the proactive mode of L2Switch.
 * In this mode, flood flows are automatically written to each switch and less traffic is sent to the controller.
 */
public class ProactiveFloodFlowWriter implements DataChangeListener {

  private static final Logger _logger = LoggerFactory.getLogger(ProactiveFloodFlowWriter.class);
  private final DataBroker dataBroker;
  private final SalFlowService salFlowService;
  private final ScheduledExecutorService stpStatusDataChangeEventProcessor = Executors.newScheduledThreadPool(1);
  private boolean flowRefreshScheduled = false;
  private final long DEFAULT_DELAY = 2;
  private final short FLOW_TABLE_ID = 0;//TODO:hard coded to 0 may need change if multiple tables are used.
  private AtomicLong flowIdInc = new AtomicLong();
  private AtomicLong flowCookieInc = new AtomicLong(0x2b00000000000000L);

  public ProactiveFloodFlowWriter(DataBroker dataBroker, SalFlowService salFlowService) {
    this.dataBroker = dataBroker;
    this.salFlowService = salFlowService;
  }

  /**
   * Registers as a data listener to receive changes done to
   * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
   * under {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
   * operation data root.
   */
  public ListenerRegistration<DataChangeListener> registerAsDataChangeListener() {
    InstanceIdentifier<StpStatusAwareNodeConnector> path = InstanceIdentifier.<Nodes>builder(Nodes.class)
        .<Node>child(Node.class)
        .<NodeConnector>child(NodeConnector.class)
        .<StpStatusAwareNodeConnector>augmentation(StpStatusAwareNodeConnector.class)
        .toInstance();
    return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, this, AsyncDataBroker.DataChangeScope.BASE);
  }

  @Override
  public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
    if(dataChangeEvent == null) {
      return;
    }
    Map<InstanceIdentifier<?>, DataObject> createdData = dataChangeEvent.getCreatedData();
    Set<InstanceIdentifier<?>> removedPaths = dataChangeEvent.getRemovedPaths();
    Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
    boolean refreshFlows = (createdData != null && !createdData.isEmpty()) ||
        (removedPaths != null && !removedPaths.isEmpty() && originalData != null && !originalData.isEmpty());

    if(!refreshFlows) {
      return;
    }
    if(!flowRefreshScheduled) {
      synchronized(this) {
        if(!flowRefreshScheduled) {
          stpStatusDataChangeEventProcessor.schedule(new StpStatusDataChangeEventProcessor(), DEFAULT_DELAY, TimeUnit.SECONDS);
          flowRefreshScheduled = true;
          _logger.debug("Scheduled Flows for refresh.");
        }
      }
    } else {
      _logger.debug("Already scheduled for flow refresh.");
    }
  }

  private class StpStatusDataChangeEventProcessor implements Runnable {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> instanceIdentifierDataObjectAsyncDataChangeEvent;

    @Override
    public void run() {
      _logger.debug("In flow refresh thread.");
      flowRefreshScheduled = false;
      installFloodFlows();
    }

    /**
     * Installs a FloodFlow on each node
     */
    private void installFloodFlows() {
      Nodes nodes = null;
      try {
        InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);
        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
        Optional<Nodes> dataObjectOptional = null;
        dataObjectOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodesInsIdBuilder.toInstance()).get();
        if(dataObjectOptional.isPresent()) {
          nodes = (Nodes) dataObjectOptional.get();
        }
        readOnlyTransaction.close();
      } catch(InterruptedException e) {
        _logger.error("Failed to read nodes from Operation data store.");
        throw new RuntimeException("Failed to read nodes from Operation data store.", e);
      } catch(ExecutionException e) {
        _logger.error("Failed to read nodes from Operation data store.");
        throw new RuntimeException("Failed to read nodes from Operation data store.", e);
      }

      if(nodes != null) {
        for(Node node : nodes.getNode()) {
          // Install a FloodFlow on each node
          List<NodeConnector> nodeConnectors = node.getNodeConnector();
          if(nodeConnectors != null) {
            ArrayList<Action> outputActions = new ArrayList<Action>();
            for(NodeConnector nodeConnector : nodeConnectors) {
              if(!nodeConnector.getId().toString().contains("LOCAL")) {
                // NodeConnectors without STP status (external ports) and NodeConnectors that are "forwarding" will be flooded on
                StpStatusAwareNodeConnector saNodeConnector = nodeConnector.getAugmentation(StpStatusAwareNodeConnector.class);
                if(saNodeConnector == null || StpStatus.Forwarding.equals(saNodeConnector.getStatus())) {
                  outputActions.add(new ActionBuilder() //
                      .setOrder(0)
                      .setAction(new OutputActionCaseBuilder() //
                          .setOutputAction(new OutputActionBuilder() //
                              .setMaxLength(new Integer(0xffff)) //
                              .setOutputNodeConnector(nodeConnector.getId()) //
                              .build()) //
                          .build()) //
                      .build());
                }
              }
            }

            // Add controller port to outputActions
            outputActions.add(new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                    .setOutputAction(new OutputActionBuilder()
                        .setMaxLength(new Integer(0xffff))
                        .setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
                        .build())
                    .build())
                .build());

            // Create an Apply Action
            ApplyActions applyActions = new ApplyActionsBuilder().setAction(outputActions).build();

            // Wrap our Apply Action in an Instruction
            Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()//
                    .setApplyActions(applyActions) //
                    .build()) //
                .build();

            FlowBuilder floodFlowBuilder = createBaseFlow();
            floodFlowBuilder.setInstructions(new InstructionsBuilder() //
                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                .build()); //

            writeFlowToSwitch(node.getId(), floodFlowBuilder.build());
          }
        }
      }
    }

    private FlowBuilder createBaseFlow() {
      FlowBuilder floodFlow = new FlowBuilder()
          .setTableId(FLOW_TABLE_ID)
          .setFlowName("flood");
      floodFlow.setId(new FlowId(Long.toString(floodFlow.hashCode())));

      Match match = new MatchBuilder()
          .build();

      floodFlow
          .setMatch(match) //
          .setPriority(2) //
          .setBufferId(0L) //
          .setHardTimeout(0) //
          .setIdleTimeout(0) //
          .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
          .setFlags(new FlowModFlags(false, false, false, false, false));
      return floodFlow;
    }

    /**
     * Starts and commits data change transaction which
     * modifies provided flow path with supplied body.
     */
    private Future<RpcResult<AddFlowOutput>> writeFlowToSwitch(NodeId nodeId, Flow flow) {
      InstanceIdentifier<Node> nodeInstanceId = InstanceIdentifier.<Nodes>builder(Nodes.class)
          .<Node, NodeKey>child(Node.class, new NodeKey(nodeId)).toInstance();
      InstanceIdentifier<Table> tableInstanceId = nodeInstanceId.<FlowCapableNode>augmentation(FlowCapableNode.class)
          .<Table, TableKey>child(Table.class, new TableKey(FLOW_TABLE_ID));
      InstanceIdentifier<Flow> flowPath = tableInstanceId
          .<Flow, FlowKey>child(Flow.class, new FlowKey(new FlowId(String.valueOf(flowIdInc.getAndIncrement()))));

      final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow)
          .setNode(new NodeRef(nodeInstanceId))
          .setFlowTable(new FlowTableRef(tableInstanceId))
          .setFlowRef(new FlowRef(flowPath))
          .setTransactionUri(new Uri(flow.getId().getValue()));
      return salFlowService.addFlow(builder.build());
    }
  }
}
