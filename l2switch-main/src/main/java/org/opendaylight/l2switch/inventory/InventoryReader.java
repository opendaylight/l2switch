/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.inventory;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.l2switch.util.InstanceIdentifierUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.*;

/**
 * InventoryReader reads the opendaylight-inventory tree in MD-SAL data store.
 */
public class InventoryReader {

  private DataBrokerService dataService;
  // Key: SwitchId, Value: NodeConnectorRef that corresponds to NC between controller & switch
  private HashMap<String, NodeConnectorRef> controllerSwitchConnectors;
  // Key: SwitchId, Value: List of node connectors on this switch
  private HashMap<String, List<NodeConnectorRef>> switchNodeConnectors;

  /**
   * Construct an InventoryService object with the specified inputs.
   * @param dataService  The DataBrokerService associated with the InventoryService.
   */
  public InventoryReader(DataBrokerService dataService) {
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
   * @return  The list of external node connectors.
   */
  public void readInventory() {
    // Only run once for now
    if (controllerSwitchConnectors.size() > 0 || switchNodeConnectors.size() > 0) {
      return;
    }

    // Read Inventory
    InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier.<Nodes>builder(Nodes.class);
    Nodes nodes = (Nodes)dataService.readOperationalData(nodesInsIdBuilder.toInstance());
    if (nodes != null) {
      for (Node node : nodes.getNode()) {
        Node completeNode = (Node)dataService.readOperationalData(InstanceIdentifierUtils.createNodePath(node.getId()));
        ArrayList<NodeConnectorRef> nodeConnectors = new ArrayList<NodeConnectorRef>();
        for (NodeConnector nodeConnector : completeNode.getNodeConnector()) {
          NodeConnectorRef ncRef = new NodeConnectorRef(
            InstanceIdentifier.<Nodes>builder(Nodes.class).<Node, NodeKey>child(Node.class, node.getKey())
              .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nodeConnector.getKey()).toInstance());

          // Regular node connectors have "-" in their name for mininet, i.e. "s1-eth1"
          if (nodeConnector.getAugmentation(FlowCapableNodeConnector.class).getName().contains("-")) {
            nodeConnectors.add(ncRef);
          }
          // Controller-to-switch internal node connectors are just "s1" or "s2" in mininet
          else {
            controllerSwitchConnectors.put(node.getId().getValue(), ncRef);
          }
        }
        switchNodeConnectors.put(node.getId().getValue(), nodeConnectors);
      }
    }
  }

}
