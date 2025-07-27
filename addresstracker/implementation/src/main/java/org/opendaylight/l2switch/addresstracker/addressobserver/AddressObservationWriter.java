/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.addresstracker.addressobserver;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AddressObservationWriter manages the MD-SAL data tree for address
 * observations (mac, ip) on each node-connector.
 */
public class AddressObservationWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AddressObservationWriter.class);

    private static final class NodeConnectorLock {
    }

    private final AtomicLong addressKey = new AtomicLong(0);
    private long timestampUpdateInterval;
    private final DataBroker dataService;
    private final Map<NodeConnectorRef, NodeConnectorLock> lockMap = new ConcurrentHashMap<>();
    private final Map<NodeConnectorLock, FluentFuture<? extends CommitInfo>> futureMap = new ConcurrentHashMap<>();

    /**
     * Construct an AddressTracker with the specified inputs.
     *
     * @param dataService
     *            The DataBrokerService for the AddressTracker
     */
    public AddressObservationWriter(DataBroker dataService) {
        this.dataService = dataService;
    }

    public void setTimestampUpdateInterval(long timestampUpdateInterval) {
        this.timestampUpdateInterval = timestampUpdateInterval;
    }

    /**
     * Add addresses into the MD-SAL data tree.
     *
     * @param macAddress
     *            The MacAddress of the new L2Address object
     * @param ipAddress
     *            The Source Protocol Address
     * @param nodeConnectorRef
     *            The NodeConnectorRef of the new L2Address object
     */
    public void addAddress(MacAddress macAddress, IpAddress ipAddress, NodeConnectorRef nodeConnectorRef) {
        if (macAddress == null || ipAddress == null || nodeConnectorRef == null) {
            return;
        }

        // get the lock for given node connector so at a time only one
        // observation can be made on a node connector
        NodeConnectorLock nodeConnectorLock = lockMap.computeIfAbsent(nodeConnectorRef, key -> new NodeConnectorLock());

        synchronized (nodeConnectorLock) {
            // Ensure previous transaction finished writing to the db
            final var future = futureMap.get(nodeConnectorLock);
            if (future != null) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Exception while waiting for previous transaction to finish", e);
                }
            }

            // Initialize builders
            long now = new Date().getTime();
            final AddressCapableNodeConnectorBuilder acncBuilder = new AddressCapableNodeConnectorBuilder();
            final AddressesBuilder addressBuilder = new AddressesBuilder().setIp(ipAddress).setMac(macAddress)
                    .setFirstSeen(now).setLastSeen(now);

            // Read existing address observations from data tree
            final FluentFuture<Optional<NodeConnector>> readFuture;
            try (ReadTransaction readTransaction = dataService.newReadOnlyTransaction()) {
                readFuture = readTransaction.read(LogicalDatastoreType.OPERATIONAL,
                    (DataObjectIdentifier<NodeConnector>) nodeConnectorRef.getValue());
            }

            final NodeConnector nc;
            try {
                final Optional<NodeConnector> dataObjectOptional = readFuture.get();
                if (dataObjectOptional.isEmpty()) {
                    return;
                }
                nc = dataObjectOptional.orElseThrow();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error reading node connector {}", nodeConnectorRef.getValue());
                throw new RuntimeException("Error reading from operational store, node connector : " + nodeConnectorRef,
                        e);
            }
            AddressCapableNodeConnector acnc = nc.augmentation(AddressCapableNodeConnector.class);

            // FIXME: This block is quite inefficient: we have a unique key and yet we end up re-merging the entire
            //        'addresses' list over and over again -- which is costly due to the HashMap copy below as well as
            //        the resulting merge() operation. After issuing the read, we should determine the key of the
            //        matching entry (or allocate a new key) and issue a simple put() on only the key.
            final Map<AddressesKey, Addresses> addresses;
            if (acnc != null) {
                // Address observations exist
                addresses = new HashMap<>(acnc.nonnullAddresses());
                // Search for this mac-ip pair in the existing address observations & update last-seen timestamp
                for (Addresses existing : addresses.values()) {
                    if (ipAddress.equals(existing.getIp()) && macAddress.equals(existing.getMac())) {
                        if ((now - existing.getLastSeen() <= timestampUpdateInterval)) {
                            // Update interval has not elapsed, do not run update
                            return;
                        }
                        addressBuilder.setFirstSeen(existing.getFirstSeen()).withKey(existing.key());
                        break;
                    }
                }
            } else {
                // Address observations don't exist, so create the list
                addresses = new HashMap<>();
            }

            if (addressBuilder.key() == null) {
                addressBuilder.withKey(new AddressesKey(Uint64.fromLongBits(addressKey.getAndIncrement())));
            }

            // Add as an augmentation
            final Addresses address = addressBuilder.build();
            addresses.put(address.key(), address);
            acncBuilder.setAddresses(addresses);

            // build Instance Id for AddressCapableNodeConnector
            final var addressCapableNcInstanceId = ((DataObjectIdentifier<NodeConnector>) nodeConnectorRef.getValue())
                .toBuilder()
                .augmentation(AddressCapableNodeConnector.class)
                .build();
            final WriteTransaction writeTransaction = dataService.newWriteOnlyTransaction();
            // Update this AddressCapableNodeConnector in the MD-SAL data tree
            writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, addressCapableNcInstanceId, acncBuilder.build());

            final var writeTxResultFuture = writeTransaction.commit();
            Futures.addCallback(writeTxResultFuture, new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(CommitInfo notUsed) {
                    LOG.debug("AddressObservationWriter write successful for tx :{}", writeTransaction.getIdentifier());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    LOG.error("AddressObservationWriter write transaction {} failed", writeTransaction.getIdentifier(),
                        throwable.getCause());
                }
            }, MoreExecutors.directExecutor());
            futureMap.put(nodeConnectorLock, writeTxResultFuture);
        }
    }
}
