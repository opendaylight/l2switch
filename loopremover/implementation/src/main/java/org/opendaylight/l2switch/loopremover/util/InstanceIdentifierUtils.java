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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * InstanceIdentifierUtils provides utility functions related to InstanceIdentifiers.
 */
public final class InstanceIdentifierUtils {

    private InstanceIdentifierUtils() {
        throw new UnsupportedOperationException("Utility class should never be instantiated");
    }

    /**
     * Creates an Instance Identifier (path) for node with specified id.
     *
     * @param nodeId the node id
     * @return the node InstanceIdentifier
     */
    public static InstanceIdentifier<Node> createNodePath(final NodeId nodeId) {
        return InstanceIdentifier.builder(Nodes.class) //
                .child(Node.class, new NodeKey(nodeId)) //
                .build();
    }

    /**
     * Shorten's node child path to node path.
     *
     * @param nodeChild
     *            child of node, from which we want node path.
     * @return the Node InstanceIdentifier
     */
    public static InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
        return nodeChild.firstIdentifierOf(Node.class);
    }

    /**
     * Creates a table path by appending table specific location to node path.
     *
     * @param nodePath the node path
     * @param tableKey the table key
     * @return the table InstanceIdentifier
     */
    public static InstanceIdentifier<Table> createTablePath(final InstanceIdentifier<Node> nodePath,
            final TableKey tableKey) {
        return nodePath.builder().augmentation(FlowCapableNode.class).child(Table.class, tableKey).build();
    }

    /**
     * Creates a path for particular flow, by appending flow-specific
     * information to table path.
     *
     * @param table the table path
     * @param flowKey the floe key
     * @return the flow InstanceIdentifier
     */
    public static InstanceIdentifier<Flow> createFlowPath(final InstanceIdentifier<Table> table,
            final FlowKey flowKey) {
        return table.child(Flow.class, flowKey);
    }

    /**
     * Extract table id from table path.
     *
     * @param tablePath the table path
     * @return the table id
     */
    public static Short getTableId(final InstanceIdentifier<Table> tablePath) {
        return Uint16.valueOf(tablePath.firstKeyOf(Table.class).getId()).shortValue();
    }

    /**
     * Extracts NodeConnectorKey from node connector path.
     */
    public static NodeConnectorKey getNodeConnectorKey(final InstanceIdentifier<?> nodeConnectorPath) {
        return nodeConnectorPath.firstKeyOf(NodeConnector.class);
    }

    /**
     * Extracts NodeKey from node path.
     */
    public static NodeKey getNodeKey(final InstanceIdentifier<?> nodePath) {
        return nodePath.firstKeyOf(Node.class);
    }

    public static InstanceIdentifier<NodeConnector> createNodeConnectorIdentifier(final String nodeIdValue,
            final String nodeConnectorIdValue) {
        return createNodePath(new NodeId(nodeIdValue)).child(NodeConnector.class,
                new NodeConnectorKey(new NodeConnectorId(nodeConnectorIdValue)));
    }

    public static InstanceIdentifier<Node> generateNodeInstanceIdentifier(final NodeConnectorRef nodeConnectorRef) {
        return nodeConnectorRef.getValue().firstIdentifierOf(Node.class);
    }

    public static InstanceIdentifier<Table> generateFlowTableInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey) {
        return generateNodeInstanceIdentifier(nodeConnectorRef).builder().augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey).build();
    }

    public static InstanceIdentifier<Flow> generateFlowInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey, final FlowKey flowKey) {
        return generateFlowTableInstanceIdentifier(nodeConnectorRef, flowTableKey).child(Flow.class, flowKey);
    }

    public static InstanceIdentifier<Topology> generateTopologyInstanceIdentifier(final String topologyId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId))).build();
    }
}
