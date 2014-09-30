/**
 * Copyright (c) 2014 André Martins and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
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
 * The {@link #removeLocally(org.opendaylight.yangtools.yang.binding.InstanceIdentifier)},
 * {@link #removeLocally(java.lang.Object) }
 * {@link #putLocally(org.opendaylight.yangtools.yang.binding.InstanceIdentifier, java.lang.Object)}
 * methods should be used when dataChanges are dealt locally and not update to
 * MD-SAL.
 *
 * @param <K> Must be a HostId
 * @param <V> Must be
 * org.opendaylight.l2switch.hosttracker.plugin.inventory.Host;
 */
public class ConcurrentClusterAwareHostHashMap<K, V> implements ConcurrentMap<K, V> {

    private final DataBroker dataService;
    private final String topologyId;

    private static final Logger log = LoggerFactory.getLogger(ConcurrentClusterAwareHostHashMap.class);

    /**
     * The instance identifiers for each host submitted to MD-SAL.
     */
    private final ConcurrentHashMap<InstanceIdentifier<Node>, K> instanceIDs;

    /**
     * The local Hosts' HashMap.
     */
    private final ConcurrentHashMap<K, V> hostHashMap;

    public ConcurrentClusterAwareHostHashMap(DataBroker dataService, String topologyId) {
        this.dataService = dataService;
        this.topologyId = topologyId;
        this.hostHashMap = new ConcurrentHashMap<>();
        this.instanceIDs = new ConcurrentHashMap<>();
    }

    /**
     * Removes, if exists, the Host with the given InstanceIdentifier<Node> from
     * this local HashMap. Ideally used for host data listener events.
     *
     * @param iiN the InstanceIdentifier<Node> of the Host to remove.
     * @return the removed Host if exits, null if it doesn't exist.
     */
    public synchronized V removeLocally(InstanceIdentifier<Node> iiN) {
        K hostId = this.instanceIDs.get(iiN);
        if (hostId != null) {
            this.instanceIDs.remove(iiN);
            return this.hostHashMap.remove(hostId);
        }
        return null;
    }

    /**
     * Removes, if exists, the Host with the given Key (HostId) from this local
     * HashMap. Ideally used for host data listener events.
     *
     * @param key the key (HostId) of the Host to remove.
     * @return the removed Host if exits, null if it doesn't exist.
     */
    public synchronized V removeLocally(K key) {
        Iterator<Entry<InstanceIdentifier<Node>, K>> iterator = this.instanceIDs.entrySet().iterator();
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
     * @param ii the value's (Host's) InstanceIdentifier<Node> f
     * @param value the Host to store locally.
     * @return the previous value associated with <tt>key</tt>, or
     * <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    public synchronized V putLocally(InstanceIdentifier<Node> ii, V value) {
        Host h = ((Host) value);
        log.trace("Putting locally {}", h.getId());
        this.instanceIDs.put(ii, (K) h.getId());
        return this.hostHashMap.put((K) h.getId(), value);
    }

    /**
     * Removes the given hosts both locally and on MD-SAL database.
     *
     * @param hosts the hosts to remove.
     */
    public synchronized void removeAll(List<Host> hosts) {
        final WriteTransaction writeTx = this.dataService.newWriteOnlyTransaction();
        for (Map.Entry<InstanceIdentifier<Node>, K> e : this.instanceIDs.entrySet()) {
            for (Host h : hosts) {
                if (e.getValue().equals(h.getId())) {
                    writeTx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey());
                    this.hostHashMap.remove(e.getValue());
                    break;
                }
            }
        }
        submit(writeTx);
    }

    /**
     * Forces the local Host with the given HostId to be merged into MD-SAL
     * database.
     *
     * @param hostid the Host's hostId that will be merged into MD-SAL database.
     */
    public synchronized void submit(HostId hostid) {
        final WriteTransaction writeTx = this.dataService.newWriteOnlyTransaction();
        Host h = (Host) this.hostHashMap.get((K) hostid);
        Node hostNode = h.getHostNode();
        InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
        putLocally(buildNodeIID, (V) h);
        this.instanceIDs.put(buildNodeIID, (K) h.getId());
        log.trace("Putting MD-SAL {}", hostNode.getNodeId());
        submit(writeTx);
    }

    /**
     * Puts all the given hosts into this local HashMap and into MD-SAL
     * database.
     *
     * @param hosts the hosts to be sent into MD-SAL database.
     */
    public synchronized void putAll(List<Host> hosts) {
        final WriteTransaction writeTx = this.dataService.newWriteOnlyTransaction();
        for (Host h : hosts) {
            Node hostNode = h.getHostNode();
            InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
            writeTx.merge(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
            putLocally(buildNodeIID, (V) h);
            this.instanceIDs.put(buildNodeIID, (K) h.getId());
            log.trace("Putting MD-SAL {}", hostNode.getNodeId());
        }
        submit(writeTx);
    }

    /**
     * Puts the given host in the this local HashMap and into MD-SAL database.
     *
     * @param hostId the key for the map
     * @param host the value for the map
     * @return the old value from the local cache if present, null otherwise.
     */
    @Override
    public synchronized V put(K hostId, V host) {
        Node hostNode = ((Host) host).getHostNode();
        InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
        final WriteTransaction writeTx = this.dataService.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
        log.trace("Putting MD-SAL {}", hostNode.getNodeId());
        submit(writeTx);
        return putLocally(buildNodeIID, host);
    }

    /**
     * Removes the value (Host) with the given hostId from this local HashMap
     * and MD-SAL database.
     *
     * @param hostId the Host's hostId to remove
     * @return the old value from the local cache if present, null otherwise.
     */
    @Override
    public synchronized V remove(Object hostId) {
        V removedValue = this.hostHashMap.remove(hostId);
        if (removedValue != null) {
            Node hostNode = ((Host) removedValue).getHostNode();
            InstanceIdentifier<Node> hnIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
            final WriteTransaction writeTx = this.dataService.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, hnIID);
            this.instanceIDs.remove(hnIID);
            submit(writeTx);
        }
        return removedValue;
    }

    /**
     * If it's absent from the this local HashMap, puts the given host in the
     * this local HashMap and into MD-SAL database.
     *
     * @param key the key for the map
     * @param value the value for the map
     * @return the old value from the local cache if present, null otherwise.
     */
    @Override
    public synchronized V putIfAbsent(K key, V value) {
        if (!this.hostHashMap.contains(value)) {
            return this.hostHashMap.put(key, value);
        } else {
            return this.hostHashMap.get(key);
        }
    }

    /**
     * Removes the entry for a key only if currently mapped to a given value.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return <tt>true</tt> if the value was removed
     */
    @Override
    public synchronized boolean remove(Object key, Object value) {
        if (this.hostHashMap.containsKey((K) key) && this.hostHashMap.get((K) key).equals(value)) {
            remove((K) key);
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * Replaces the entry for a key only if currently mapped to a given value.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     */
    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        if (this.hostHashMap.containsKey((K) key) && this.hostHashMap.get((K) key).equals(oldValue)) {
            put(key, newValue);
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * Replaces the entry for a key only if currently mapped to some value.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     * <tt>null</tt> if there was no mapping for the key. (A <tt>null</tt>
     * return can also indicate that the map previously associated <tt>null</tt>
     * with the key, if the implementation supports null values.)
     */
    @Override
    public synchronized V replace(K key, V value) {
        if (this.hostHashMap.containsKey(key)) {
            return put(key, value);
        } else {
            return null;
        }
    }

    @Override
    public synchronized int size() {
        return this.hostHashMap.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.hostHashMap.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return this.hostHashMap.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return this.hostHashMap.contains(value);
    }

    @Override
    public synchronized V get(Object key) {
        return this.hostHashMap.get(key);
    }

    /**
     * Copies all of the mappings from the specified map to this local HashMap
     * and into MD-SAL.
     *
     * @param m mappings to be stored in this local HashMap and into MD-SAL
     */
    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        final WriteTransaction writeTx = this.dataService.newWriteOnlyTransaction();
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            Node hostNode = ((Host) e.getValue()).getHostNode();
            InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey(), topologyId);
            writeTx.merge(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
            putLocally(buildNodeIID, e.getValue());
        }
        submit(writeTx);
    }

    /**
     *
     * Removes all of the mappings from this local HashMap and from MD-SAL. The
     * local HashMap will be empty after this call returns.
     *
     */
    @Override
    public synchronized void clear() {
        final WriteTransaction writeTx = this.dataService.newWriteOnlyTransaction();
        for (Map.Entry<? extends InstanceIdentifier<Node>, ? extends K> e : this.instanceIDs.entrySet()) {
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey());
        }
        submit(writeTx);
        this.hostHashMap.clear();
    }

    /**
     * Returns the KeySet from this local HashMap.
     *
     * @return the KeySet from this local HashMap.
     */
    @Override
    public synchronized Set<K> keySet() {
        return this.hostHashMap.keySet();
    }

    /**
     * Returns the Values from this local HashMap.
     *
     * @return the Values from this local HashMap.
     */
    @Override
    public synchronized Collection<V> values() {
        return this.hostHashMap.values();
    }

    /**
     * Returns the EntrySet from this local HashMap.
     *
     * @return the EntrySet from this local HashMap.
     */
    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        return this.hostHashMap.entrySet();
    }

    /**
     * Submits and adds a callback to the result from the submition of the given
     * WriteTransaction
     *
     * @param writeTx the WriteTransaction to submit
     */
    private void submit(final WriteTransaction writeTx) {
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                log.debug("ConcurrentHashMap write successful for tx :{}", writeTx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("ConcurrentHashMap write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
            }
        });
    }

}
