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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
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

  /**
   * Construct an InventoryService object with the specified inputs.
   *
   * @param dataService The DataBrokerService associated with the InventoryService.
   */
  public InventoryReader(DataBroker dataService) {
    this.dataService = dataService;
    controllerSwitchConnectors = new HashMap<String, NodeConnectorRef>();
    switchNodeConnectors = new HashMap<String, List<NodeConnectorRef>>();
  }

  public HashMap<String, NodeConnectorRef> getControllerSwitchConnectors() {
    return controllerSwitchConnectors;
  }

  public HashMap<String, List<NodeConnectorRef>> getSwitchNodeConnectors() {
    return switchNodeConnectors;
  }

  /**
   * Get the External NodeConnectors of the network, which are the NodeConnectors connected to hosts.
   *
   * @return The list of external node connectors.
   */
  public void readInventory() {
    // Only run once for now
    if(controllerSwitchConnectors.size() > 0 || switchNodeConnectors.size() > 0) {
      return;
    }

    // Read Inventory
    InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    Nodes nodes = null;
    ReadOnlyTransaction readOnlyTransaction = dataService.newReadOnlyTransaction();

    try {
      Optional<DataObject> dataObjectOptional = null;
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
          }
        }
        switchNodeConnectors.put(node.getId().getValue(), nodeConnectorRefs);
      }
    }
  }
}
