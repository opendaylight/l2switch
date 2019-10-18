/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.inventory;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InventoryReader reads the opendaylight-inventory tree in MD-SAL data store.
 */
public class InventoryReader implements DataTreeChangeListener<DataObject> {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReader.class);
    private final DataBroker dataService;
    // Key: SwitchId, Value: NodeConnectorRef that corresponds to NC between
    // controller & switch
    private final HashMap<String, NodeConnectorRef> controllerSwitchConnectors;
    // Key: SwitchId, Value: List of node connectors on this switch
    private final HashMap<String, List<NodeConnectorRef>> switchNodeConnectors;
    private final List<Registration> listenerRegistrationList = new CopyOnWriteArrayList<>();

    private volatile boolean refreshData = false;
    private final long refreshDataDelay = 20L;
    private volatile boolean refreshDataScheduled = false;
    private final ScheduledExecutorService nodeConnectorDataChangeEventProcessor = Executors.newScheduledThreadPool(1);

    /**
     * Construct an InventoryService object with the specified inputs.
     *
     * @param dataService
     *            The DataBrokerService associated with the InventoryService.
     */
    public InventoryReader(DataBroker dataService) {
        this.dataService = dataService;
        controllerSwitchConnectors = new HashMap<>();
        switchNodeConnectors = new HashMap<>();
    }

    public void setRefreshData(boolean refreshData) {
        this.refreshData = refreshData;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void registerAsDataChangeListener() {
        InstanceIdentifier<NodeConnector> nodeConnector = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class)
                .child(NodeConnector.class)
                .build();
        this.listenerRegistrationList.add(dataService.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, nodeConnector),
                    (DataTreeChangeListener)this));

        InstanceIdentifier<StpStatusAwareNodeConnector> stpStatusAwareNodeConnecto =
            InstanceIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class)
                .augmentation(StpStatusAwareNodeConnector.class)
                .build();
        this.listenerRegistrationList.add(dataService.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, stpStatusAwareNodeConnecto),
                    (DataTreeChangeListener)this));
    }


    public HashMap<String, NodeConnectorRef> getControllerSwitchConnectors() {
        return controllerSwitchConnectors;
    }

    public HashMap<String, List<NodeConnectorRef>> getSwitchNodeConnectors() {
        return switchNodeConnectors;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<DataObject>> changes) {
        if (!refreshDataScheduled) {
            synchronized (this) {
                if (!refreshDataScheduled) {
                    nodeConnectorDataChangeEventProcessor.schedule(new NodeConnectorDataChangeEventProcessor(),
                            refreshDataDelay, TimeUnit.MILLISECONDS);
                    refreshDataScheduled = true;
                }
            }
        }
    }


    public void close() {
        listenerRegistrationList.forEach(reg -> reg.close());
    }

    /**
     * Read the Inventory data tree to find information about the Nodes and
     * NodeConnectors. Create the list of NodeConnectors for a given switch.
     * Also determine the STP status of each NodeConnector.
     */
    public void readInventory() {
        // Only run once for now
        if (!refreshData) {
            return;
        }
        synchronized (this) {
            if (!refreshData) {
                return;
            }
            // Read Inventory
            InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier
                    .<Nodes>builder(Nodes.class);
            Nodes nodes = null;
            try (ReadOnlyTransaction readOnlyTransaction = dataService.newReadOnlyTransaction()) {
                Optional<Nodes> dataObjectOptional = readOnlyTransaction
                        .read(LogicalDatastoreType.OPERATIONAL, nodesInsIdBuilder.build()).get();
                if (dataObjectOptional.isPresent()) {
                    nodes = dataObjectOptional.get();
                }
            } catch (InterruptedException e) {
                LOG.error("Failed to read nodes from Operation data store.");
                throw new RuntimeException("Failed to read nodes from Operation data store.", e);
            } catch (ExecutionException e) {
                LOG.error("Failed to read nodes from Operation data store.");
                throw new RuntimeException("Failed to read nodes from Operation data store.", e);
            }

            if (nodes != null) {
                // Get NodeConnectors for each node
                for (Node node : nodes.getNode()) {
                    ArrayList<NodeConnectorRef> nodeConnectorRefs = new ArrayList<>();
                    List<NodeConnector> nodeConnectors = node.getNodeConnector();
                    if (nodeConnectors != null) {
                        for (NodeConnector nodeConnector : nodeConnectors) {
                            // Read STP status for this NodeConnector
                            StpStatusAwareNodeConnector saNodeConnector = nodeConnector
                                    .augmentation(StpStatusAwareNodeConnector.class);
                            if (saNodeConnector != null && StpStatus.Discarding.equals(saNodeConnector.getStatus())) {
                                continue;
                            }
                            if (nodeConnector.key().toString().contains("LOCAL")) {
                                continue;
                            }
                            NodeConnectorRef ncRef = new NodeConnectorRef(InstanceIdentifier.<Nodes>builder(Nodes.class)
                                    .<Node, NodeKey>child(Node.class, node.key())
                                    .<NodeConnector, NodeConnectorKey>child(NodeConnector.class, nodeConnector.key())
                                    .build());
                            nodeConnectorRefs.add(ncRef);
                        }
                    }

                    switchNodeConnectors.put(node.getId().getValue(), nodeConnectorRefs);
                    NodeConnectorRef ncRef = new NodeConnectorRef(InstanceIdentifier.<Nodes>builder(Nodes.class)
                            .<Node, NodeKey>child(Node.class, node.key())
                            .<NodeConnector, NodeConnectorKey>child(NodeConnector.class,
                                    new NodeConnectorKey(new NodeConnectorId(node.getId().getValue() + ":LOCAL")))
                            .build());
                    LOG.debug("Local port for node {} is {}", node.key(), ncRef);
                    controllerSwitchConnectors.put(node.getId().getValue(), ncRef);
                }
            }

            refreshData = false;

            if (listenerRegistrationList.isEmpty()) {
                registerAsDataChangeListener();
            }
        }
    }

    /**
     * Get the NodeConnector on the specified node with the specified MacAddress
     * observation.
     *
     * @param nodeInsId
     *            InstanceIdentifier for the node on which to search for.
     * @param macAddress
     *            MacAddress to be searched for.
     * @return NodeConnectorRef that pertains to the NodeConnector containing
     *         the MacAddress observation.
     */
    public NodeConnectorRef getNodeConnector(InstanceIdentifier<Node> nodeInsId, MacAddress macAddress) {
        if (nodeInsId == null || macAddress == null) {
            return null;
        }

        NodeConnectorRef destNodeConnector = null;
        long latest = -1;
        ReadOnlyTransaction readOnlyTransaction = dataService.newReadOnlyTransaction();
        try {
            Optional<Node> dataObjectOptional = null;
            dataObjectOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeInsId).get();
            if (dataObjectOptional.isPresent()) {
                Node node = dataObjectOptional.get();
                LOG.debug("Looking address{} in node : {}", macAddress, nodeInsId);
                if (node.getNodeConnector() != null) {
                    for (NodeConnector nc : node.getNodeConnector()) {
                        // Don't look for mac in discarding node connectors
                        StpStatusAwareNodeConnector saNodeConnector = nc
                                .augmentation(StpStatusAwareNodeConnector.class);
                        if (saNodeConnector != null && StpStatus.Discarding.equals(saNodeConnector.getStatus())) {
                            continue;
                        }
                        LOG.debug("Looking address{} in nodeconnector : {}", macAddress, nc.key());
                        AddressCapableNodeConnector acnc = nc.augmentation(AddressCapableNodeConnector.class);
                        if (acnc != null) {
                            List<Addresses> addressesList = acnc.getAddresses();
                            for (Addresses add : addressesList) {
                                if (macAddress.equals(add.getMac())) {
                                    if (add.getLastSeen() > latest) {
                                        destNodeConnector = new NodeConnectorRef(
                                                nodeInsId.child(NodeConnector.class, nc.key()));
                                        latest = add.getLastSeen();
                                        LOG.debug("Found address{} in nodeconnector : {}", macAddress, nc.key());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOG.debug("Node connectors data is not present for node {}", node.getId());
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            readOnlyTransaction.close();
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        } catch (ExecutionException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            readOnlyTransaction.close();
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        }
        readOnlyTransaction.close();
        return destNodeConnector;
    }

    private class NodeConnectorDataChangeEventProcessor implements Runnable {

        @Override
        public void run() {
            controllerSwitchConnectors.clear();
            switchNodeConnectors.clear();
            refreshDataScheduled = false;
            setRefreshData(true);
            readInventory();
        }
    }
}
