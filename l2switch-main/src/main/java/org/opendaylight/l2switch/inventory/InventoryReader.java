/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.inventory;

import java.util.ArrayList;
import java.util.List;
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

        NodeConnectorRef destNodeConnector = null;
        long latest = -1;
        ReadTransaction readOnlyTransaction = dataService.newReadOnlyTransaction();
        try {
            Optional<Node> dataObjectOptional = null;
            dataObjectOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, nodeInsId).get();
            if (dataObjectOptional.isPresent()) {
                Node node = (Node) dataObjectOptional.get();
                LOG.debug("Looking address{} in node : {}", macAddress, nodeInsId);
                for (NodeConnector nc : node.getNodeConnector().values()) {
                    // Don't look for mac in discarding node connectors
                    StpStatusAwareNodeConnector saNodeConnector = nc.augmentation(StpStatusAwareNodeConnector.class);
                    if (saNodeConnector != null && StpStatus.Discarding.equals(saNodeConnector.getStatus())) {
                        continue;
                    }
                    LOG.debug("Looking address{} in nodeconnector : {}", macAddress, nc.key());
                    AddressCapableNodeConnector acnc = nc.augmentation(AddressCapableNodeConnector.class);
                    if (acnc != null) {
                        List<Addresses> addressesList = new ArrayList<Addresses>(acnc.getAddresses().values());
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

}
