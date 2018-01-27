/**
 * Copyright (c) 2014 André Martins and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.hosttracker.plugin.inventory.Host;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will (try to) submit all writes and deletes in to the MD-SAL database.
 * The removeLocally and putLocally methods should be used when dataChanges are dealt locally and not update to MD-SAL.
 */
public class ConcurrentClusterAwareHostHashMap {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentClusterAwareHostHashMap.class);

    private final OperationProcessor opProcessor;
    private final String topologyId;

    /**
     * The instance identifiers for each host submitted to MD-SAL.
     */
    private final ConcurrentHashMap<InstanceIdentifier<Node>, HostId> instanceIDs = new ConcurrentHashMap<>();

    /**
     * The local Hosts' HashMap.
     */
    private final ConcurrentHashMap<HostId, Host> hostHashMap = new ConcurrentHashMap<>();

    public ConcurrentClusterAwareHostHashMap(OperationProcessor opProcessor, String topologyId) {
        this.opProcessor = opProcessor;
        this.topologyId = topologyId;
    }

    /**
     * Removes, if exists, the Host with the given
     * InstanceIdentifier&lt;Node&gt; from this local HashMap. Ideally used for
     * host data listener events.
     *
     * @param iiN
     *            the InstanceIdentifier&lt;Node&gt; of the Host to remove.
     * @return the removed Host if exits, null if it doesn't exist.
     */
    public synchronized Host removeLocally(InstanceIdentifier<Node> iiN) {
        HostId hostId = this.instanceIDs.remove(iiN);
        if (hostId != null) {
            return this.hostHashMap.remove(hostId);
        }
        return null;
    }

    /**
     * Removes, if exists, the Host with the given Key (HostId) from this local
     * HashMap. Ideally used for host data listener events.
     *
     * @param key
     *            the key (HostId) of the Host to remove.
     * @return the removed Host if exits, null if it doesn't exist.
     */
    public synchronized Host removeLocally(HostId key) {
        Iterator<Entry<InstanceIdentifier<Node>, HostId>> iterator = this.instanceIDs.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().equals(key)) {
                iterator.remove();
                break;
            }
        }
        return hostHashMap.remove(key);
    }

    /**
     * Puts the given value (Host) only in this local HashMap. Ideally used for
     * host data listener events.
     *
     * @param ii
     *            the value's (Host's) InstanceIdentifier&lt;Node&gt;
     * @param value
     *            the Host to store locally.
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     *         if there was no mapping for <tt>key</tt>
     */
    public synchronized Host putLocally(InstanceIdentifier<Node> ii, Host value) {
        Host host = value;
        LOG.trace("Putting locally {}", host.getId());
        this.instanceIDs.put(ii, host.getId());
        return this.hostHashMap.put(host.getId(), value);
    }

    /**
     * Removes the given hosts both locally and on MD-SAL database.
     *
     * @param hosts
     *            the hosts to remove.
     */
    public synchronized void removeAll(List<Host> hosts) {
        for (final Map.Entry<InstanceIdentifier<Node>, HostId> e : this.instanceIDs.entrySet()) {
            for (Host h : hosts) {
                if (e.getValue().equals(h.getId())) {
                    this.opProcessor.enqueueOperation(tx -> tx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey()));
                    this.hostHashMap.remove(e.getValue());
                    break;
                }
            }
        }
    }

    /**
     * Forces the local Host with the given HostId to be merged into MD-SAL
     * database.
     *
     * @param hostid
     *            the Host's hostId that will be merged into MD-SAL database.
     */
    public synchronized void submit(HostId hostid) {
        Host host = this.hostHashMap.get(hostid);
        final Node hostNode = host.getHostNode();
        final InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
        this.opProcessor.enqueueOperation(tx -> tx.merge(LogicalDatastoreType.OPERATIONAL, buildNodeIID,
                hostNode, true));
        putLocally(buildNodeIID, host);
        this.instanceIDs.put(buildNodeIID, host.getId());
        LOG.trace("Enqueued for MD-SAL transaction {}", hostNode.getNodeId());
    }

    /**
     * Puts all the given hosts into this local HashMap and into MD-SAL
     * database.
     *
     * @param hosts
     *            the hosts to be sent into MD-SAL database.
     */
    public synchronized void putAll(List<Host> hosts) {
        for (Host h : hosts) {
            final Node hostNode = h.getHostNode();
            final InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
            this.opProcessor.enqueueOperation(tx -> tx.merge(LogicalDatastoreType.OPERATIONAL, buildNodeIID,
                    hostNode, true));
            putLocally(buildNodeIID, h);
            this.instanceIDs.put(buildNodeIID, h.getId());
            LOG.trace("Putting MD-SAL {}", hostNode.getNodeId());
        }
    }

    /**
     * Puts the given host in the this local HashMap and into MD-SAL database.
     *
     * @param hostId
     *            the key for the map
     * @param host
     *            the value for the map
     * @return the old value from the local cache if present, null otherwise.
     */
    public synchronized Host put(HostId hostId, Host host) {
        final Node hostNode = host.getHostNode();
        final InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
        this.opProcessor.enqueueOperation(tx -> tx.merge(LogicalDatastoreType.OPERATIONAL, buildNodeIID,
                hostNode, true));
        LOG.trace("Putting MD-SAL {}", hostNode.getNodeId());
        return putLocally(buildNodeIID, host);
    }

    /**
     * Removes the value (Host) with the given hostId from this local HashMap
     * and MD-SAL database.
     *
     * @param hostId
     *            the Host's hostId to remove
     * @return the old value from the local cache if present, null otherwise.
     */
    public synchronized Host remove(HostId hostId) {
        Host removedValue = this.hostHashMap.remove(hostId);
        if (removedValue != null) {
            Node hostNode = removedValue.getHostNode();
            final InstanceIdentifier<Node> hnIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
            this.opProcessor.enqueueOperation(tx -> tx.delete(LogicalDatastoreType.OPERATIONAL, hnIID));
            this.instanceIDs.remove(hnIID);
        }
        return removedValue;
    }

    public boolean containsKey(Object key) {
        return this.hostHashMap.containsKey(key);
    }

    public Host get(HostId key) {
        return this.hostHashMap.get(key);
    }

    /**
     * Removes all of the mappings from this local HashMap and from MD-SAL. The
     * local HashMap will be empty after this call returns.
     */
    public synchronized void clear() {
        for (final Map.Entry<? extends InstanceIdentifier<Node>, HostId> e : this.instanceIDs.entrySet()) {
            this.opProcessor.enqueueOperation(tx -> tx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey()));
        }
        this.hostHashMap.clear();
    }

    /**
     * Returns the Values from this local HashMap.
     *
     * @return the Values from this local HashMap.
     */
    public Collection<Host> values() {
        return this.hostHashMap.values();
    }
}
