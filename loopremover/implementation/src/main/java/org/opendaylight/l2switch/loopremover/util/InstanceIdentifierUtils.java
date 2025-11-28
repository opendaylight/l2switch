/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
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
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * InstanceIdentifierUtils provides utility functions related to InstanceIdentifiers.
 */
public final class InstanceIdentifierUtils {
    private InstanceIdentifierUtils() {
        // Hidden on purpose
    }

    // FIXME: move to TopologyDataChangeEventProcessor
    public static KeyedInstanceIdentifier<NodeConnector, NodeConnectorKey> createNodeConnectorIdentifier(
            final String nodeIdValue, final String nodeConnectorIdValue) {
        return InstanceIdentifier.builder(Nodes.class)
            .child(Node.class, new NodeKey(new NodeId(nodeIdValue)))
            .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId(nodeConnectorIdValue)))
            .build();
    }

    // FIXME: move to TopologyDataChangeEventProcessor
    public static KeyedInstanceIdentifier<Topology, TopologyKey> generateTopologyInstanceIdentifier(
            final String topologyId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
            .build();
    }
}
