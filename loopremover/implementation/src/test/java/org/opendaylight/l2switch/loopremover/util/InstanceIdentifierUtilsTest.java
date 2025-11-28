/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint8;

class InstanceIdentifierUtilsTest {
    private static final Uint8 NUM_ID_1 = Uint8.ONE;
    private static final String STR_ID_1 = "id1";
    private static final String STR_ID_2 = "id2";

    @Test
    void testCreateNodePath() {
        assertEquals(InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(STR_ID_1)).build(),
            InstanceIdentifierUtils.createNodePath(new NodeId(STR_ID_1)));
    }

    @Test
    void testGetNodePath() {
        var ncInsId = InstanceIdentifier.builder(Nodes.class).child(Node.class)
                .child(NodeConnector.class).build();
        assertNotNull(InstanceIdentifierUtils.getNodePath(ncInsId));
    }

    @Test
    void testCreateTablePath() {
        var tableInsId = InstanceIdentifierUtils.createTablePath(
                InstanceIdentifier.builder(Nodes.class).child(Node.class).build(), new TableKey(NUM_ID_1));
        assertNotNull(tableInsId);
        assertEquals(NUM_ID_1.shortValue(), tableInsId.getFirstKeyOf(Table.class).getId().shortValue());
        assertNotNull(tableInsId.firstIdentifierOf(FlowCapableNode.class));
    }

    @Test
    void testCreateNodeConnectorIdentifier() {
        var ncInsId = InstanceIdentifierUtils.createNodeConnectorIdentifier(STR_ID_1, STR_ID_2);
        assertNotNull(ncInsId);
        assertEquals(STR_ID_1, ncInsId.getFirstKeyOf(Node.class).getId().getValue());
        assertEquals(STR_ID_2, ncInsId.getFirstKeyOf(NodeConnector.class).getId().getValue());
    }

    @Test
    void testGenerateNodeInstanceIdentifier() {
        var ncRef = new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("def")))
            .build());
        assertNotNull(InstanceIdentifierUtils.generateNodeInstanceIdentifier(ncRef));
    }

    @Test
    void testGenerateFlowTableInstanceIdentifier() {
        var ncRef = new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId("abc")))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("def")))
            .build());
        var tableInsId = InstanceIdentifierUtils.generateFlowTableInstanceIdentifier(ncRef, new TableKey(NUM_ID_1));
        assertNotNull(tableInsId);
        assertEquals(NUM_ID_1, tableInsId.getFirstKeyOf(Table.class).getId());
    }

    @Test
    void testGenerateTopologyInstanceIdentifier() {
        var topologyInsId = InstanceIdentifierUtils.generateTopologyInstanceIdentifier(STR_ID_1);
        assertNotNull(topologyInsId);
        assertEquals(STR_ID_1, topologyInsId.getFirstKeyOf(Topology.class).getTopologyId().getValue());
    }
}
