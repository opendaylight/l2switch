/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.inventory;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * InventoryReader reads the opendaylight-inventory tree in MD-SAL data store.
 */
public class InventoryReader {

  private Logger _logger = LoggerFactory.getLogger(InventoryReader.class);
  private DataBroker dataService;
  // Key: SwitchId, Value: NodeConnectorRef that corresponds to NC between controller & switch
  private HashMap<String, NodeConnectorRef> controllerSwitchConnectors;
  // Key: SwitchId, Value: List of node connectors on this switch
  private HashMap<String, List<NodeConnectorRef>> switchNodeConnectors;
  private HashSet<String> discardingNodeConnectors;

  public void setRefreshData(boolean refreshData) {
    this.refreshData = refreshData;
  }

  private boolean refreshData = false;

  /**
   * Construct an InventoryService object with the specified inputs.
   *
   * @param dataService The DataBrokerService associated with the InventoryService.
   */
  public InventoryReader(DataBroker dataService) {
    this.dataService = dataService;
    controllerSwitchConnectors = new HashMap<String, NodeConnectorRef>();
    switchNodeConnectors = new HashMap<String, List<NodeConnectorRef>>();
    discardingNodeConnectors = new HashSet<String>();
  }

  public HashMap<String, NodeConnectorRef> getControllerSwitchConnectors() {
    return controllerSwitchConnectors;
  }

  public HashMap<String, List<NodeConnectorRef>> getSwitchNodeConnectors() {
    return switchNodeConnectors;
  }

  public HashSet<String> getDiscardingNodeConnectors() {
    return discardingNodeConnectors;
  }

  /**
   * Read the Inventory data tree to find information about the Nodes & NodeConnectors.
   * Create the list of NodeConnectors for a given switch.  Also determine the STP status of each NodeConnector.
   */
  public void readInventory() {
    // Only run once for now
    if(!refreshData) {
      return;
    }
    synchronized(this) {
      if(!refreshData)
        return;
      // Read Inventory
      InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);
      Nodes nodes = null;
      ReadOnlyTransaction readOnlyTransaction = dataService.newReadOnlyTransaction();

      try {
        Optional<Nodes> dataObjectOptional = null;
        dataObjectOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodesInsIdBuilder.toInstance()).get();
        if(dataObjectOptional.isPresent())
          nodes = (Nodes) dataObjectOptional.get();
      } catch(InterruptedException e) {
        _logger.error("Failed to read nodes from Operation data store.");
        throw new RuntimeException("Failed to read nodes from Operation data store.", e);
      } catch(ExecutionException e) {
        _logger.error("Failed to read nodes from Operation data store.");
        throw new RuntimeException("Failed to read nodes from Operation data store.", e);
      }

      if(nodes != null) {
        // Get NodeConnectors for each node
        for(Node node : nodes.getNode()) {
          ArrayList<NodeConnectorRef> nodeConnectorRefs = new ArrayList<NodeConnectorRef>();
          List<NodeConnector> nodeConnectors = node.getNodeConnector();
          if(nodeConnectors != null) {
            for(NodeConnector nodeConnector : nodeConnectors) {
              NodeConnectorRef ncRef = new NodeConnectorRef(
                  InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey())
                      .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nodeConnector.getKey()).toInstance());
              if(nodeConnector.getKey().toString().contains("LOCAL")) {
                controllerSwitchConnectors.put(node.getId().getValue(), ncRef);
              } else {
                nodeConnectorRefs.add(ncRef);
              }

              // Read STP status for this NodeConnector
              try {
                readOnlyTransaction = dataService.newReadOnlyTransaction();
                Optional<NodeConnector> dataObjectOptional =
                    readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<NodeConnector>) ncRef.getValue()).get();
                if(dataObjectOptional.isPresent()) {
                  NodeConnector configNodeConnector = (NodeConnector) dataObjectOptional.get();
                  StpStatusAwareNodeConnector saNodeConnector = configNodeConnector.getAugmentation(StpStatusAwareNodeConnector.class);
                  if(saNodeConnector != null && StpStatus.Discarding.equals(saNodeConnector.getStatus())) {
                    discardingNodeConnectors.add(nodeConnector.getId().getValue());
                  }
                }
              } catch(InterruptedException | ExecutionException e) {
                _logger.error("Failed to read nodes from Operational data store.");
                throw new RuntimeException("Failed to read nodes from Operational data store.", e);
              }
            }
          }
          switchNodeConnectors.put(node.getId().getValue(), nodeConnectorRefs);
        }
      }
      refreshData = false;
    }
  }

  /**
   * Get the NodeConnector on the specified node with the specified MacAddress observation.
   * @param nodeInsId InstanceIdentifier for the node on which to search for.
   * @param macAddress MacAddress to be searched for.
   * @return NodeConnectorRef that pertains to the NodeConnector containing the MacAddress observation.
   */
  public NodeConnectorRef getNodeConnector(InstanceIdentifier<Node> nodeInsId, MacAddress macAddress) {
    if (nodeInsId == null || macAddress == null) {
      return null;
    }

    NodeConnectorRef destNodeConnector = null;
    long latest = -1;
    ReadOnlyTransaction readOnlyTransaction = dataService.newReadOnlyTransaction();
    try {
      Optional<Node> dataObjectOptional = null;
      dataObjectOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeInsId).get();
      if(dataObjectOptional.isPresent()) {
        Node node = (Node)dataObjectOptional.get();
        for (NodeConnector nc : node.getNodeConnector()) {
          // Don't look for mac in discarding node connectors
          if (!discardingNodeConnectors.contains(nc.getId().getValue())) {
            AddressCapableNodeConnector acnc = nc.getAugmentation(AddressCapableNodeConnector.class);
            if (acnc != null) {
              List<Addresses> addressesList = acnc.getAddresses();
              for (Addresses add : addressesList) {
                if (macAddress.equals(add.getMac())) {
                  if (add.getLastSeen() > latest) {
                    destNodeConnector = new NodeConnectorRef(nodeInsId.child(NodeConnector.class, nc.getKey()));
                    latest = add.getLastSeen();
                    break;
                  }
                }
              }
            }
          }
        }
      }
    } catch(InterruptedException e) {
      _logger.error("Failed to read nodes from Operation data store.");
      throw new RuntimeException("Failed to read nodes from Operation data store.", e);
    } catch(ExecutionException e) {
      _logger.error("Failed to read nodes from Operation data store.");
      throw new RuntimeException("Failed to read nodes from Operation data store.", e);
    }
    return destNodeConnector;
  }

}
