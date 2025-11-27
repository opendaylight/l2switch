/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * InstanceIdentifierUtils provides utility functions related to InstanceIdentifiers.
 */
public final class InstanceIdentifierUtils {
    private InstanceIdentifierUtils() {
        // Hidden on purpose
    }

    /**
     * Creates an Instance Identifier (path) for node with specified id.
     *
     * @param nodeId the node id
     * @return the node InstanceIdentifier
     */
    public static WithKey<Node, NodeKey> createNodePath(final NodeId nodeId) {
        return DataObjectIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(nodeId)).build();
    }

    /**
     * Shorten's node child path to node path.
     *
     * @param nodeChild
     *            child of node, from which we want node path.
     * @return the Node InstanceIdentifier
     */
    public static DataObjectIdentifier<Node> getNodePath(final DataObjectIdentifier<?> nodeChild) {
        return nodeChild.trimTo(Node.class);
    }

    /**
     * Creates a table path by appending table specific location to node path.
     *
     * @param nodePath the node path
     * @param tableKey the table key
     * @return the table InstanceIdentifier
     */
    public static DataObjectIdentifier<Table> createTablePath(final DataObjectIdentifier<Node> nodePath,
            final TableKey tableKey) {
        return nodePath.toBuilder().augmentation(FlowCapableNode.class).child(Table.class, tableKey).build();
    }

    /**
     * Creates a path for particular flow, by appending flow-specific
     * information to table path.
     *
     * @param table the table path
     * @param flowKey the floe key
     * @return the flow InstanceIdentifier
     */
    public static DataObjectIdentifier<Flow> createFlowPath(final DataObjectIdentifier<Table> table,
            final FlowKey flowKey) {
        return table.toBuilder().child(Flow.class, flowKey).build();
    }

    /**
     * Extract table id from table path.
     *
     * @param tablePath the table path
     * @return the table id
     */
    public static Uint8 getTableId(final DataObjectIdentifier<Table> tablePath) {
        return tablePath.getFirstKeyOf(Table.class).getId();
    }

    /**
     * Extracts NodeConnectorKey from node connector path.
     */
    public static NodeConnectorKey getNodeConnectorKey(final DataObjectIdentifier<?> nodeConnectorPath) {
        return nodeConnectorPath.firstKeyOf(NodeConnector.class);
    }

    /**
     * Extracts NodeKey from node path.
     */
    public static NodeKey getNodeKey(final DataObjectIdentifier<?> nodePath) {
        return nodePath.firstKeyOf(Node.class);
    }

    public static WithKey<NodeConnector, NodeConnectorKey> createNodeConnectorIdentifier(final String nodeIdValue,
            final String nodeConnectorIdValue) {
        return createNodePath(new NodeId(nodeIdValue)).toBuilder()
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnectorIdValue)))
            .build();
    }

    public static DataObjectIdentifier<Node> generateNodeInstanceIdentifier(final NodeConnectorRef nodeConnectorRef) {
        return ((DataObjectIdentifier<?>)nodeConnectorRef.getValue()).trimTo(Node.class);
    }

    public static WithKey<Table, TableKey> generateFlowTableInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey) {
        return generateNodeInstanceIdentifier(nodeConnectorRef).toBuilder()
            .augmentation(FlowCapableNode.class)
            .child(Table.class, flowTableKey)
            .build();
    }

    public static WithKey<Flow, FlowKey> generateFlowInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey, final FlowKey flowKey) {
        return generateFlowTableInstanceIdentifier(nodeConnectorRef, flowTableKey).toBuilder()
            .child(Flow.class, flowKey)
            .build();
    }

    public static WithKey<Topology, TopologyKey> generateTopologyInstanceIdentifier(final String topologyId) {
        return DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
            .build();
    }
}
