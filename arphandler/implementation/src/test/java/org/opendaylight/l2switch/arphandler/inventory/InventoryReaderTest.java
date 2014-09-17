/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.inventory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnectorBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InventoryReaderTest {

  @MockitoAnnotations.Mock private DataBroker dataBroker;
  private InventoryReader inventoryReader;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    inventoryReader = new InventoryReader(dataBroker);
  }

  @Test
  public void testGetControllerSwitchConnectors() throws Exception {
    assertEquals(0, inventoryReader.getControllerSwitchConnectors().size());
  }

  @Test
  public void testGetSwitchNodeConnectors() throws Exception {
    assertEquals(0, inventoryReader.getSwitchNodeConnectors().size());
  }

  @Test
  public void testGetNodeConnector() throws Exception {
    List<Addresses> addressesList = new ArrayList<Addresses>();
    addressesList.add(new AddressesBuilder().setLastSeen(0L).setMac(new MacAddress("")).build());
    AddressCapableNodeConnector addressCapableNodeConnector = new AddressCapableNodeConnectorBuilder().setAddresses(addressesList).build();
    StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder().setStatus(StpStatus.Forwarding).build();
    NodeConnector nodeConnector = new NodeConnectorBuilder()
      .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
      .addAugmentation(AddressCapableNodeConnector.class, addressCapableNodeConnector)
      .build();

    List<NodeConnector> nodeConnectors = new ArrayList<NodeConnector>();
    nodeConnectors.add(nodeConnector);
    Node node = new NodeBuilder().setNodeConnector(nodeConnectors).build();
    Optional<Node> optionalNode = Optional.of(node);

    ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
    CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
    when(checkedFuture.get()).thenReturn(optionalNode);
    when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
    when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

    assertNotNull(inventoryReader.getNodeConnector(
      InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(""))).toInstance(),
      new MacAddress("")));
    verify(readOnlyTransaction, times(1)).close();
  }

  @Test
  public void testGetNodeConnector_NullNodeInsId() throws Exception {
    assertNull(inventoryReader.getNodeConnector(null, Mockito.mock(MacAddress.class)));
    verify(dataBroker, times(0)).newReadOnlyTransaction();
  }

  @Test
  public void testGetNodeConnector_NullMacAddress() throws Exception {
    assertNull(inventoryReader.getNodeConnector(Mockito.mock(InstanceIdentifier.class), null));
    verify(dataBroker, times(0)).newReadOnlyTransaction();
  }

  @Test
  public void testReadInventory_NoRefresh() throws Exception {
    inventoryReader.setRefreshData(false);
    inventoryReader.readInventory();
    verify(dataBroker, times(0)).newReadOnlyTransaction();
  }

  @Test
  public void testReadInventory_Refresh() throws Exception {
    StpStatusAwareNodeConnector stpStatusAwareNodeConnector = new StpStatusAwareNodeConnectorBuilder().setStatus(StpStatus.Discarding).build();
    NodeConnector nc1 = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
      .build();
    NodeConnector nc2 = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("2")))
      .build();
    NodeConnector nc3 = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("3")))
      .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
      .build();
    NodeConnector ncLocal = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("LOCAL")))
      .addAugmentation(StpStatusAwareNodeConnector.class, stpStatusAwareNodeConnector)
      .build();

    List<NodeConnector> nodeConnectors = new ArrayList<NodeConnector>();
    nodeConnectors.add(nc1);
    nodeConnectors.add(nc2);
    nodeConnectors.add(nc3);
    nodeConnectors.add(ncLocal);
    Node node = new NodeBuilder()
      .setId(new NodeId("1"))
      .setNodeConnector(nodeConnectors)
      .build();

    List<Node> nodeList = new ArrayList<Node>();
    nodeList.add(node);
    Nodes nodes = new NodesBuilder().setNode(nodeList).build();
    Optional<Nodes> optionalNodes = Optional.of(nodes);

    ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
    CheckedFuture checkedFuture = Mockito.mock(CheckedFuture.class);
    when(checkedFuture.get()).thenReturn(optionalNodes);
    when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture);
    when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

    inventoryReader.setRefreshData(true);
    inventoryReader.readInventory();
    verify(dataBroker, times(1)).newReadOnlyTransaction();
    assertEquals(1, inventoryReader.getControllerSwitchConnectors().size());
    assertEquals(1, inventoryReader.getSwitchNodeConnectors().size());
    assertEquals(2, inventoryReader.getSwitchNodeConnectors().get("1").size());
    // Ensure that refreshData is set to false
    inventoryReader.readInventory();
    verify(dataBroker, times(1)).newReadOnlyTransaction();
  }
}
