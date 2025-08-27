/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.inventory;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
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
    private static final long REFRESH_DATA_DELAY = 20L;
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
        DataObjectIdentifier<NodeConnector> nodeConnector = DataObjectIdentifier.builder(Nodes.class)
                .child(Node.class)
                .child(NodeConnector.class)
                .build();
        this.listenerRegistrationList.add(dataService.registerDataTreeChangeListener(
                         DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL,nodeConnector),
                                                   (DataTreeChangeListener)this));

        DataObjectIdentifier<StpStatusAwareNodeConnector> stpStatusAwareNodeConnecto =
            DataObjectIdentifier.builder(Nodes.class).child(Node.class).child(NodeConnector.class)
                .augmentation(StpStatusAwareNodeConnector.class)
                .build();
        this.listenerRegistrationList.add(dataService.registerDataTreeChangeListener(
                 DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL, stpStatusAwareNodeConnecto),
                                           (DataTreeChangeListener)this));
    }


    public HashMap<String, NodeConnectorRef> getControllerSwitchConnectors() {
        return controllerSwitchConnectors;
    }

    public HashMap<String, List<NodeConnectorRef>> getSwitchNodeConnectors() {
        return switchNodeConnectors;
    }

    @Override
    public void onDataTreeChanged(List<DataTreeModification<DataObject>> changes) {
        if (!refreshDataScheduled) {
            synchronized (this) {
                if (!refreshDataScheduled) {
                    nodeConnectorDataChangeEventProcessor.schedule(new NodeConnectorDataChangeEventProcessor(),
                            REFRESH_DATA_DELAY, TimeUnit.MILLISECONDS);
                    refreshDataScheduled = true;
                }
            }
        }
    }


    public void close() {
        listenerRegistrationList.forEach(Registration::close);
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
            Nodes nodes = null;
            try (ReadTransaction readOnlyTransaction = dataService.newReadOnlyTransaction()) {
                Optional<Nodes> dataObjectOptional = readOnlyTransaction
                        .read(LogicalDatastoreType.OPERATIONAL, DataObjectIdentifier.builder(Nodes.class)
                        .build()).get();
                if (dataObjectOptional.isPresent()) {
                    nodes = dataObjectOptional.orElseThrow();
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
                for (Node node : nodes.nonnullNode().values()) {
                    ArrayList<NodeConnectorRef> nodeConnectorRefs = new ArrayList<>();
                    for (NodeConnector nodeConnector : node.nonnullNodeConnector().values()) {
                        // Read STP status for this NodeConnector
                        StpStatusAwareNodeConnector saNodeConnector = nodeConnector
                            .augmentation(StpStatusAwareNodeConnector.class);
                        if (saNodeConnector != null && StpStatus.Discarding.equals(saNodeConnector.getStatus())) {
                            continue;
                        }
                        if (nodeConnector.key().toString().contains("LOCAL")) {
                            continue;
                        }
                        NodeConnectorRef ncRef = new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
                            .child(Node.class, node.key())
                            .child(NodeConnector.class, nodeConnector.key())
                            .build());
                        nodeConnectorRefs.add(ncRef);
                    }

                    switchNodeConnectors.put(node.getId().getValue(), nodeConnectorRefs);
                    NodeConnectorRef ncRef = new NodeConnectorRef(DataObjectIdentifier.builder(Nodes.class)
                            .child(Node.class, node.key())
                            .child(NodeConnector.class,
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
    public NodeConnectorRef getNodeConnector(DataObjectIdentifier<Node> nodeInsId, MacAddress macAddress) {
        if (nodeInsId == null || macAddress == null) {
            return null;
        }

        final FluentFuture<Optional<Node>> readFuture;
        try (ReadTransaction readOnlyTransaction = dataService.newReadOnlyTransaction()) {
            readFuture = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeInsId);
        }

        final Optional<Node> dataObjectOptional;
        try {
            dataObjectOptional = readFuture.get();
        } catch (InterruptedException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        } catch (ExecutionException e) {
            LOG.error("Failed to read nodes from Operation data store.");
            throw new RuntimeException("Failed to read nodes from Operation data store.", e);
        }

        if (dataObjectOptional.isEmpty()) {
            return null;
        }

        final Node node = dataObjectOptional.orElseThrow();
        final Map<NodeConnectorKey, NodeConnector> connectors = node.getNodeConnector();
        if (connectors == null) {
            LOG.debug("Node connectors data is not present for node {}", node.getId());
            return null;
        }

        long latest = -1;
        NodeConnectorRef destNodeConnector = null;
        for (NodeConnector nc : connectors.values()) {
            // Don't look for mac in discarding node connectors
            StpStatusAwareNodeConnector saNodeConnector = nc.augmentation(StpStatusAwareNodeConnector.class);
            if (saNodeConnector != null && StpStatus.Discarding.equals(saNodeConnector.getStatus())) {
                continue;
            }
            LOG.debug("Looking address{} in nodeconnector : {}", macAddress, nc.key());
            AddressCapableNodeConnector acnc = nc.augmentation(AddressCapableNodeConnector.class);
            if (acnc != null) {
                for (Addresses add : acnc.nonnullAddresses().values()) {
                    if (macAddress.equals(add.getMac())) {
                        final long lastSeen = add.getLastSeen();
                        if (lastSeen > latest) {
                            latest = lastSeen;
                            LOG.debug("Found address{} in nodeconnector : {}", macAddress, nc.key());
                            destNodeConnector = new NodeConnectorRef(nodeInsId.toBuilder()
                                .child(NodeConnector.class, nc.key())
                                .build().toIdentifier());
                            break;
                        }
                    }
                }
            }
        }
        return destNodeConnector;
    }

    private final class NodeConnectorDataChangeEventProcessor implements Runnable {

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
