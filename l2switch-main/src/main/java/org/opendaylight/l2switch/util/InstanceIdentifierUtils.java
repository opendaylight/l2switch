/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.PropertyIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * InstanceIdentifierUtils provides utility functions related to InstanceIdentifiers.
 */
public final class InstanceIdentifierUtils {
    private InstanceIdentifierUtils() {
        // Hidden on purpose
    }

    public static InstanceIdentifier<Node> generateNodeInstanceIdentifier(final NodeConnectorRef nodeConnectorRef) {
        final var container = switch (nodeConnectorRef.getValue()) {
            case DataObjectIdentifier<?> doi -> doi;
            case PropertyIdentifier<?, ?> pi -> pi.container();
        };
        return container.toLegacy().firstIdentifierOf(Node.class);
    }

    public static InstanceIdentifier<Table> generateFlowTableInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey) {
        return generateNodeInstanceIdentifier(nodeConnectorRef).toBuilder()
            .augmentation(FlowCapableNode.class)
            .child(Table.class, flowTableKey)
            .build();
    }

    public static InstanceIdentifier<Flow> generateFlowInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey, final FlowKey flowKey) {
        return generateFlowTableInstanceIdentifier(nodeConnectorRef, flowTableKey).child(Flow.class, flowKey);
    }
}
