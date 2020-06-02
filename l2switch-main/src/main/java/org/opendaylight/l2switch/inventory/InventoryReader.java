/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.inventory;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.rev140714.StpStatusAwareNodeConnector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InventoryReader reads the opendaylight-inventory tree in MD-SAL data store.
 */
public class InventoryReader {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryReader.class);
    private DataBroker dataService;

    /**
     * Construct an InventoryService object with the specified inputs.
     *
     * @param dataService
     *            The DataBrokerService associated with the InventoryService.
     */
    public InventoryReader(DataBroker dataService) {
        this.dataService = dataService;
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
        LOG.debug("Looking address{} in node : {}", macAddress, nodeInsId);

        NodeConnectorRef destNodeConnector = null;
        long latest = -1;
        for (NodeConnector nc : node.nonnullNodeConnector().values()) {
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
                            destNodeConnector = new NodeConnectorRef(nodeInsId.child(NodeConnector.class, nc.key()));
                            latest = lastSeen;
                            LOG.debug("Found address{} in nodeconnector : {}", macAddress, nc.key());
                            break;
                        }
                    }
                }
            }
        }
        return destNodeConnector;
    }
}
