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
    private final ConcurrentHashMap<InstanceIdentifier<Node>, K> instanceIDs;
    private static final Logger log = LoggerFactory.getLogger(ConcurrentClusterAwareHostHashMap.class);
    private final ConcurrentHashMap<K, V> hostHashMap;

    public ConcurrentClusterAwareHostHashMap(DataBroker dataService) {
        this.dataService = dataService;
        hostHashMap = new ConcurrentHashMap<>();
        instanceIDs = new ConcurrentHashMap<>();
    }

    public synchronized V removeLocally(InstanceIdentifier<Node> iiN) {
        K hostId = instanceIDs.get(iiN);
        if (hostId != null) {
            instanceIDs.remove(iiN);
            return hostHashMap.remove(hostId);
        }
        return null;
    }

    public synchronized V removeLocally(K key) {
        Iterator<Entry<InstanceIdentifier<Node>, K>> iterator = instanceIDs.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().equals(key)) {
                iterator.remove();
                break;
            }
        }
        return hostHashMap.remove(key);
    }

    public synchronized V putLocally(InstanceIdentifier<Node> ii, V value) {
        Host h = ((Host) value);
        instanceIDs.put(ii, (K) h.getId());
        return hostHashMap.put((K) h.getId(), value);
    }

    public synchronized void removeAll(List<Host> hosts) {
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        for (Map.Entry<InstanceIdentifier<Node>, K> e : instanceIDs.entrySet()) {
            for (Host h : hosts) {
                if (e.getValue().equals(h.getId())) {
                    writeTx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey());
                    hostHashMap.remove(e.getValue());
                    break;
                }
            }
        }
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                log.debug("Hosttracker write successful for tx :{}", writeTx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Hosttracker write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
            }
        });
        hostHashMap.clear();
    }

    public synchronized void submit(HostId hostid) {
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        Host h = (Host) hostHashMap.get((K) hostid);
        Node hostNode = h.getHostNode();
        InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
        putLocally(buildNodeIID, (V) h);
        instanceIDs.put(buildNodeIID, (K) h.getId());
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                log.debug("Hosttracker write successful for tx :{}", writeTx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Hosttracker write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
            }
        });
    }

    public synchronized void putAll(List<Host> hosts) {
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        for (Host h : hosts) {
            Node hostNode = h.getHostNode();
            InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey());
            writeTx.put(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
            putLocally(buildNodeIID, (V) h);
            instanceIDs.put(buildNodeIID, (K) h.getId());
        }
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                log.debug("Hosttracker write successful for tx :{}", writeTx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Hosttracker write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
            }
        });
    }

    @Override
    public synchronized V put(K hostId, V host) {
        Node hostNode = ((Host) host).getHostNode();
        InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey());
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                log.debug("Hosttracker write successful for tx :{}", writeTx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Hosttracker write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
            }
        });
        return putLocally(buildNodeIID, host);
    }

    @Override
    public synchronized V remove(Object hostId) {
        V removedValue = hostHashMap.remove(hostId);
        if (removedValue != null) {
            Node hostNode = ((Host) removedValue).getHostNode();
            InstanceIdentifier<Node> hnIID = Utilities.buildNodeIID(hostNode.getKey());
            final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, hnIID);
            instanceIDs.remove(hnIID);
            final CheckedFuture writeTxResultFuture = writeTx.submit();
            Futures.addCallback(writeTxResultFuture, new FutureCallback() {
                @Override
                public void onSuccess(Object o) {
                    log.debug("Hosttracker write successful for tx :{}", writeTx.getIdentifier());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("Hosttracker write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
                }
            });
        }
        return removedValue;
    }

    @Override
    public synchronized V putIfAbsent(K key, V value) {
        if (!hostHashMap.contains(value)) {
            return hostHashMap.put(key, value);
        } else {
            return hostHashMap.get(key);
        }
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        if (hostHashMap.containsKey((K) key) && hostHashMap.get((K) key).equals(value)) {
            remove((K) key);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        if (hostHashMap.containsKey((K) key) && hostHashMap.get((K) key).equals(oldValue)) {
            put(key, newValue);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized V replace(K key, V value) {
        if (hostHashMap.containsKey(key)) {
            return put(key, value);
        } else {
            return null;
        }
    }

    @Override
    public synchronized int size() {
        return hostHashMap.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return hostHashMap.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return hostHashMap.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return hostHashMap.contains(value);
    }

    @Override
    public synchronized V get(Object key) {
        return hostHashMap.get(key);
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            Node hostNode = ((Host) e.getValue()).getHostNode();
            InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey());
            writeTx.put(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
            putLocally(buildNodeIID, e.getValue());
        }
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                log.debug("Hosttracker write successful for tx :{}", writeTx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Hosttracker write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
            }
        });
    }

    @Override
    public synchronized void clear() {
        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        for (Map.Entry<? extends InstanceIdentifier<Node>, ? extends K> e : instanceIDs.entrySet()) {
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey());
        }
        final CheckedFuture writeTxResultFuture = writeTx.submit();
        Futures.addCallback(writeTxResultFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object o) {
                log.debug("Hosttracker write successful for tx :{}", writeTx.getIdentifier());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Hosttracker write transaction {} failed", writeTx.getIdentifier(), throwable.getCause());
            }
        });
        hostHashMap.clear();
    }

    @Override
    public synchronized Set<K> keySet() {
        return hostHashMap.keySet();
    }

    @Override
    public synchronized Collection<V> values() {
        return hostHashMap.values();
    }

    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        return hostHashMap.entrySet();
    }

}
