/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FlowWriterServiceImplTest {

    @MockitoAnnotations.Mock private SalFlowService salFlowService;
    private FlowWriterServiceImpl flowWriterService;
    private InstanceIdentifier<NodeConnector> nodeConnectorInstanceIdentifier;
    private NodeConnectorRef nodeConnectorRef;


    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        flowWriterService = new FlowWriterServiceImpl(salFlowService);
    }

    @Test
    public void addMacToMacFlowTest() {

        nodeConnectorInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(
            Node.class, new NodeKey(new NodeId("node-id"))).child(
            NodeConnector.class, new NodeConnectorKey(new NodeConnectorId("nodeconnector-id"))).toInstance();
        nodeConnectorRef = new NodeConnectorRef(nodeConnectorInstanceIdentifier);

        MacAddress sourceMac = new MacAddress("00:00:ac:f0:01:01");
        MacAddress destMac = new MacAddress("00:00:ac:f0:02:02");

        flowWriterService.addMacToMacFlow(sourceMac, destMac, nodeConnectorRef);
        verify(salFlowService, times(1)).addFlow(any(AddFlowInput.class));
    }

}
