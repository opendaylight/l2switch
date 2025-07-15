/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.util;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.ExactDataObjectStep;

/**
 * InstanceIdentifierUtils provides utility functions related to InstanceIdentifiers.
 */
public final class InstanceIdentifierUtils {

    private InstanceIdentifierUtils() {
        throw new UnsupportedOperationException("Utility class should never be instantiated");
    }

    public static DataObjectIdentifier<Node> generateNodeInstanceIdentifier(final NodeConnectorRef nodeConnectorRef) {
    	return firstIdentifierOf((DataObjectIdentifier<?>) nodeConnectorRef.getValue(), Node.class);
    }

    public static DataObjectIdentifier<Table> generateFlowTableInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey) {
        return generateNodeInstanceIdentifier(nodeConnectorRef).toBuilder().augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey).build();
    }

    public static DataObjectIdentifier<Flow> generateFlowInstanceIdentifier(final NodeConnectorRef nodeConnectorRef,
            final TableKey flowTableKey, final FlowKey flowKey) {
        return generateFlowTableInstanceIdentifier(nodeConnectorRef, flowTableKey).toBuilder().child(Flow.class, flowKey).build();
    }
	public static <I extends DataObject> DataObjectIdentifier<I> firstIdentifierOf(DataObjectIdentifier<?> identifier,
			Class<I> type) {
		List<ExactDataObjectStep<?>> collected = new ArrayList<>();
		for (ExactDataObjectStep<?> step : identifier.steps()) {
			collected.add(step);
			if (type.equals(step.type())) {
				@SuppressWarnings("unchecked")
				DataObjectIdentifier<I> result = (DataObjectIdentifier<I>) DataObjectIdentifier
						.ofUnsafeSteps(List.copyOf(collected));
				return result;
			}
		}
		return null;
	}
}
