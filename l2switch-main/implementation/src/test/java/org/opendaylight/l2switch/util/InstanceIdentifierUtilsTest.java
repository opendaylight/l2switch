/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.util;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InstanceIdentifierUtilsTest {

  private final Short NUM_ID_1 = 1;
  private final String STR_ID_1 = "id1";

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
  public void testGenerateFlowInstanceIdentifier() throws Exception {
    NodeConnectorRef ncRef = new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
      .child(Node.class, new NodeKey(new NodeId(STR_ID_1)))
      .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(STR_ID_1)))
      .toInstance());
    InstanceIdentifier<Flow> flowInsId = InstanceIdentifierUtils.generateFlowInstanceIdentifier(
      ncRef,
      new TableKey(NUM_ID_1),
      new FlowKey(new FlowId(STR_ID_1)));
    assertNotNull(flowInsId);
    assertEquals(NUM_ID_1, flowInsId.firstKeyOf(Table.class, TableKey.class).getId());
    assertEquals(STR_ID_1, flowInsId.firstKeyOf(Flow.class, FlowKey.class).getId().getValue());
  }
}

