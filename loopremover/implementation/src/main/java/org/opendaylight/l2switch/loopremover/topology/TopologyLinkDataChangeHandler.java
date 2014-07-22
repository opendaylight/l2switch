/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.loopremover.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens to data change events on topology links
 * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
 * and maintains a topology graph using provided NetworkGraphService
 * {@link org.opendaylight.l2switch.loopremover.topology.NetworkGraphService}.
 * It refreshes the graph after a delay(default 10 sec) to accommodate burst of change events if they come in bulk.
 * This is to avoid continuous refresh of graph on a series of change events in short time.
 */
public class TopologyLinkDataChangeHandler implements DataChangeListener {
  private static final Logger _logger = LoggerFactory.getLogger(TopologyLinkDataChangeHandler.class);
  private static final String DEFAULT_TOPOLOGY_ID = "flow:1";

  private final ExecutorService topologyDataChangeEventProcessor = Executors.newCachedThreadPool();

  private final NetworkGraphService networkGraphService;
  private final DataBroker dataBroker;
  boolean doneOnce = false;

  /**
   * Uses default delay to refresh topology graph if this constructor is used.
   *
   * @param dataBroker
   * @param networkGraphService
   */
  public TopologyLinkDataChangeHandler(DataBroker dataBroker, NetworkGraphService networkGraphService) {
    Preconditions.checkNotNull(dataBroker, "dataBroker should not be null.");
    Preconditions.checkNotNull(networkGraphService, "networkGraphService should not be null.");
    this.dataBroker = dataBroker;
    this.networkGraphService = networkGraphService;
  }

  /**
   * Registers as a data listener to receive changes done to
   * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
   * under {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology}
   * operation data root.
   */

  public ListenerRegistration<DataChangeListener> registerAsDataChangeListener() {
    InstanceIdentifier<Link> linkInstance = InstanceIdentifier.builder(NetworkTopology.class)
        .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).child(Link.class).toInstance();
    return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, linkInstance, this, AsyncDataBroker.DataChangeScope.BASE);
  }

  @Override
  public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> instanceIdentifierDataObjectAsyncDataChangeEvent) {
    if(instanceIdentifierDataObjectAsyncDataChangeEvent == null) {
      return;
    }

    topologyDataChangeEventProcessor.submit(new TopologyDataChangeEventProcessor(instanceIdentifierDataObjectAsyncDataChangeEvent));
    _logger.info("************After topologyDataChangeEventProcessor called ");
  }

  /**
   *
   */
  private class TopologyDataChangeEventProcessor implements Runnable {
    AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> instanceIdentifierDataObjectAsyncDataChangeEvent;


    public TopologyDataChangeEventProcessor(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
      this.instanceIdentifierDataObjectAsyncDataChangeEvent = dataChangeEvent;
    }

    @Override
    public void run() {

      if(instanceIdentifierDataObjectAsyncDataChangeEvent == null) {
        return;
      }
      Map<InstanceIdentifier<?>, DataObject> createdData = instanceIdentifierDataObjectAsyncDataChangeEvent.getCreatedData();
      Set<InstanceIdentifier<?>> removedPaths = instanceIdentifierDataObjectAsyncDataChangeEvent.getRemovedPaths();
      Map<InstanceIdentifier<?>, DataObject> originalData = instanceIdentifierDataObjectAsyncDataChangeEvent.getOriginalData();
      boolean isGraphUpdated = false;
      ReadWriteTransaction readWriteTransaction = null;

      _logger.info("Topology Event Started********************** ");
      if(createdData != null && !createdData.isEmpty()) {
        List<Link> links = new ArrayList<>();
        for(InstanceIdentifier<?> instanceId : createdData.keySet()) {
          if(Link.class.isAssignableFrom(instanceId.getTargetType())) {
            links.add((Link) createdData.get(instanceId));
          }
        }
        if(!links.isEmpty()) {
          networkGraphService.addLinks(links);
          isGraphUpdated = true;
        }
      }

      List<Link> removedLinks = null;
      if(removedPaths != null && !removedPaths.isEmpty() && originalData != null && !originalData.isEmpty()) {
        removedLinks = new ArrayList<>();
        for(InstanceIdentifier<?> instanceId : removedPaths) {
          if(Link.class.isAssignableFrom(instanceId.getTargetType())) {
            Link link = (Link) originalData.get(instanceId);
            removedLinks.add(link);
          }
        }
        if(!removedLinks.isEmpty()) {
          networkGraphService.removeLinks(removedLinks);
          isGraphUpdated = true;
        }
      }

      if(isGraphUpdated) {
        readWriteTransaction = dataBroker.newReadWriteTransaction();
        if(removedLinks != null && !removedLinks.isEmpty()) {
          for(Link link : removedLinks) {
            updateNodeConnector(readWriteTransaction, getSourceNodeConnectorRef(link), StpStatus.Discarding);
            updateNodeConnector(readWriteTransaction, getDestNodeConnectorRef(link), StpStatus.Discarding);
          }
        }
        updateNodeConnectorStatus(readWriteTransaction);
        readWriteTransaction.commit();
      }
    }

    /**
     * @param readWriteTransaction
     */
    private void updateNodeConnectorStatus(ReadWriteTransaction readWriteTransaction) {
      List<Link> allLinks = networkGraphService.getAllLinks();
      if(allLinks == null || allLinks.isEmpty()) {
        return;
      }

      List<Link> mstLinks = networkGraphService.getLinksInMst();
      for(Link link : allLinks) {
        if(mstLinks != null && !mstLinks.isEmpty() && mstLinks.contains(link)) {
          updateNodeConnector(readWriteTransaction, getSourceNodeConnectorRef(link), StpStatus.Forwarding);
          updateNodeConnector(readWriteTransaction, getDestNodeConnectorRef(link), StpStatus.Forwarding);
        } else {
          updateNodeConnector(readWriteTransaction, getSourceNodeConnectorRef(link), StpStatus.Discarding);
          updateNodeConnector(readWriteTransaction, getDestNodeConnectorRef(link), StpStatus.Discarding);
        }
      }
    }

    /**
     * @param link
     * @return
     */
    private NodeConnectorRef getSourceNodeConnectorRef(Link link) {
      InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
          = InstanceIdentifierUtils.createNodeConnectorIdentifier(
          link.getSource().getSourceNode().getValue(),
          link.getSource().getSourceTp().getValue());
      return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
    }

    /**
     * @param link
     * @return
     */
    private NodeConnectorRef getDestNodeConnectorRef(Link link) {
      InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier
          = InstanceIdentifierUtils.createNodeConnectorIdentifier(
          link.getDestination().getDestNode().getValue(),
          link.getDestination().getDestTp().getValue());

      return new NodeConnectorRef(nodeConnectorInstanceIdentifier);
    }

    /**
     * @param readWriteTransaction
     * @param nodeConnectorRef
     * @param stpStatus
     */
    private void updateNodeConnector(ReadWriteTransaction readWriteTransaction, NodeConnectorRef nodeConnectorRef, StpStatus stpStatus) {
      StpStatusAwareNodeConnectorBuilder stpStatusAwareNodeConnectorBuilder = new StpStatusAwareNodeConnectorBuilder()
          .setStatus(stpStatus);
      checkIfExistAndUpdateNodeConnector(readWriteTransaction, nodeConnectorRef, stpStatusAwareNodeConnectorBuilder.build());
    }

    /**
     * @param readWriteTransaction
     * @param nodeConnectorRef
     * @param stpStatusAwareNodeConnector
     */
    private void checkIfExistAndUpdateNodeConnector(ReadWriteTransaction readWriteTransaction, NodeConnectorRef nodeConnectorRef, StpStatusAwareNodeConnector stpStatusAwareNodeConnector) {
      NodeConnector nc = null;
      try {
        Optional<NodeConnector> dataObjectOptional = readWriteTransaction.read(LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<NodeConnector>)nodeConnectorRef.getValue()).get();
        if(dataObjectOptional.isPresent())
          nc = (NodeConnector) dataObjectOptional.get();
      } catch(Exception e) {
        _logger.error("Error reading node connector {}", nodeConnectorRef.getValue());
        readWriteTransaction.commit();
        throw new RuntimeException("Error reading from operational store, node connector : " + nodeConnectorRef, e);
      }
      NodeConnectorBuilder nodeConnectorBuilder;
      if(nc != null) {
        if(sameStatusPresent(nc.getAugmentation(StpStatusAwareNodeConnector.class), stpStatusAwareNodeConnector.getStatus())) {
          return;
        }
        nodeConnectorBuilder = new NodeConnectorBuilder(nc)
            .setKey(nc.getKey())
            .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector);
        readWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<NodeConnector>)nodeConnectorRef.getValue(), nodeConnectorBuilder.build());
        _logger.info("Updated node connector in operational {}", nodeConnectorRef);
      } else {

        NodeConnectorKey nodeConnectorKey = InstanceIdentifierUtils.getNodeConnectorKey(nodeConnectorRef.getValue());
        nodeConnectorBuilder = new NodeConnectorBuilder()
            .setKey(nodeConnectorKey)
            .setId(nodeConnectorKey.getId())
            .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector);
        nc = nodeConnectorBuilder.build();
        checkIfExistAndUpdateNode(readWriteTransaction, nodeConnectorRef, nc);
      }
    }

    /**
     * @param stpStatusAwareNodeConnector
     * @return
     */
    private boolean sameStatusPresent(StpStatusAwareNodeConnector stpStatusAwareNodeConnector, StpStatus stpStatus) {

      if(stpStatusAwareNodeConnector == null)
        return false;

      if(stpStatusAwareNodeConnector.getStatus() == null)
        return false;

      if(stpStatus.getIntValue() != stpStatusAwareNodeConnector.getStatus().getIntValue())
        return false;

      return true;
    }

    /**
     * @param readWriteTransaction
     * @param nodeConnectorRef
     * @param nc
     */
    private void checkIfExistAndUpdateNode(ReadWriteTransaction readWriteTransaction, NodeConnectorRef nodeConnectorRef, NodeConnector nc) {
      Node node = null;
      InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifierUtils.generateNodeInstanceIdentifier(nodeConnectorRef);
      try {
        Optional<Node> dataObjectOptional = readWriteTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier).get();
        if(dataObjectOptional.isPresent())
          node = (Node) dataObjectOptional.get();
      } catch(Exception e) {
        _logger.error("Error reading node {}", nodeInstanceIdentifier);
        readWriteTransaction.commit();
        throw new RuntimeException("Error reading from operational store, node  : " + nodeInstanceIdentifier, e);
      }
      if(node != null) {
        List<NodeConnector> nodeConnectors = node.getNodeConnector();
        if(nodeConnectors == null) {
          nodeConnectors = new ArrayList<>();
        }
        nodeConnectors.add(nc);
        NodeBuilder nodeBuilder = new NodeBuilder(node)
            .setNodeConnector(nodeConnectors);
        node = nodeBuilder.build();
        readWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier, node);
        _logger.info("Updated node {}  in operational store with node id {}", node, nodeInstanceIdentifier);
      } else {
        NodeKey nodeKey = nodeConnectorRef.getValue().firstKeyOf(Node.class, NodeKey.class);
        List<NodeConnector> nodeConnectors = new ArrayList<>();
        nodeConnectors.add(nc);
        NodeBuilder nodeBuilder = new NodeBuilder()
            .setKey(nodeKey)
            .setId(nodeKey.getId())
            .setNodeConnector(nodeConnectors);
        node = nodeBuilder.build();

        checkIfExistsAndUpdateNodes(readWriteTransaction, nodeConnectorRef, node);
      }
    }

    /**
     * @param readWriteTransaction
     * @param nodeConnectorRef
     * @param node
     */
    private void checkIfExistsAndUpdateNodes(ReadWriteTransaction readWriteTransaction, NodeConnectorRef nodeConnectorRef, Node node) {

      List<Node> nodesList = null;

      Nodes nodes = null;
      InstanceIdentifier<Nodes> nodesInstanceIdentifier = nodeConnectorRef.getValue().firstIdentifierOf(Nodes.class);
      try {
        Optional<Nodes> dataObjectOptional = readWriteTransaction.read(LogicalDatastoreType.OPERATIONAL, nodesInstanceIdentifier).get();
        if(dataObjectOptional.isPresent())
          nodes = (Nodes) dataObjectOptional.get();
      } catch(Exception e) {
        _logger.error("Error reading nodes  {}", nodesInstanceIdentifier);
        readWriteTransaction.commit();
        throw new RuntimeException("Error reading from operational store, nodes  : " + nodesInstanceIdentifier, e);
      }
      if(nodes != null) {
        nodesList = nodes.getNode();
      }
      if(nodesList == null) {
        nodesList = new ArrayList<>();
      }
      nodesList.add(node);
      NodesBuilder nodesBuilder = new NodesBuilder()
          .setNode(nodesList);
      nodes = nodesBuilder.build();
      readWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, nodesInstanceIdentifier, nodes);
      _logger.info("Updated nodes {}  in operational store with nodes id {}", nodes, nodesInstanceIdentifier);
    }
  }
}
