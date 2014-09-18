/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.util;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InstanceIdentifierUtilsTest {

  private final Short NUM_ID_1 = 1;
  private final String STR_ID_1 = "id1";
  private final String STR_ID_2 = "id2";

  @Test
  public void testCreateNodePath() throws Exception {
    InstanceIdentifier<Node> insId = InstanceIdentifierUtils.createNodePath(new NodeId(STR_ID_1));
    assertNotNull(insId);
    assertNotNull(insId.firstIdentifierOf(Nodes.class));
    assertEquals(STR_ID_1, insId.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
  }

  @Test
  public void testGetNodePath() throws Exception {
    InstanceIdentifier<NodeConnector> ncInsId = InstanceIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class).toInstance();
    assertNotNull(InstanceIdentifierUtils.getNodePath(ncInsId));
  }

  @Test
  public void testCreateTablePath() throws Exception {
    InstanceIdentifier<Table> tableInsId =
      InstanceIdentifierUtils.createTablePath(
        InstanceIdentifier.builder(Nodes.class).child(Node.class).build(),
        new TableKey(NUM_ID_1));
    assertNotNull(tableInsId);
    assertEquals(NUM_ID_1.shortValue(), tableInsId.firstKeyOf(Table.class, TableKey.class).getId().shortValue());
    assertNotNull(tableInsId.firstIdentifierOf(FlowCapableNode.class));
  }

  @Test
  public void testCreateNodeConnectorIdentifier() throws Exception {
    InstanceIdentifier<NodeConnector> ncInsId = InstanceIdentifierUtils.createNodeConnectorIdentifier(STR_ID_1, STR_ID_2);
    assertNotNull(ncInsId);
    assertEquals(STR_ID_1, ncInsId.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
    assertEquals(STR_ID_2, ncInsId.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue());
  }

  @Test
  public void testGenerateNodeInstanceIdentifier() throws Exception {
    NodeConnectorRef ncRef = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class).toInstance());
    assertNotNull(InstanceIdentifierUtils.generateNodeInstanceIdentifier(ncRef));
  }

  @Test
  public void testGenerateFlowTableInstanceIdentifier() throws Exception {
    NodeConnectorRef ncRef = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class).toInstance());
    InstanceIdentifier<Table> tableInsId = InstanceIdentifierUtils.generateFlowTableInstanceIdentifier(ncRef, new TableKey(NUM_ID_1));
    assertNotNull(tableInsId);
    assertEquals(NUM_ID_1, tableInsId.firstKeyOf(Table.class, TableKey.class).getId());
  }

  @Test
  public void testGenerateTopologyInstanceIdentifier() throws Exception {
    InstanceIdentifier<Topology> topologyInsId = InstanceIdentifierUtils.generateTopologyInstanceIdentifier(STR_ID_1);
    assertNotNull(topologyInsId);
    assertEquals(STR_ID_1, topologyInsId.firstKeyOf(Topology.class, TopologyKey.class).getTopologyId().getValue());
  }
}
