/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

@ExtendWith(MockitoExtension.class)
class FlowWriterServiceImplTest {
    @Mock
    private AddFlow addFlow;

    private FlowWriterServiceImpl flowWriterService;
    private DataObjectIdentifier<NodeConnector> nodeConnectorInstanceIdentifier;
    private NodeConnectorRef nodeConnectorRef;

    @BeforeEach
    void beforeEach() {
        flowWriterService = new FlowWriterServiceImpl(addFlow);
    }

    @Test
    void addMacToMacFlowTest() {
        nodeConnectorInstanceIdentifier = DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("node-id")))
                .child(NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("nodeconnector-id")))
                .build();
        nodeConnectorRef = new NodeConnectorRef(nodeConnectorInstanceIdentifier);

        MacAddress sourceMac = new MacAddress("00:00:ac:f0:01:01");
        MacAddress destMac = new MacAddress("00:00:ac:f0:02:02");

        flowWriterService.addMacToMacFlow(sourceMac, destMac, nodeConnectorRef);
        verify(addFlow).invoke(any(AddFlowInput.class));
    }
}
