/**
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will (try to) submit all writes and deletes in to the MD-SAL database.
 * The
 * {@link #removeLocally(org.opendaylight.yangtools.yang.binding.InstanceIdentifier)}
 * {@link #removeLocally(java.lang.Object) }
 * {@link #putLocally(org.opendaylight.yangtools.yang.binding.InstanceIdentifier, java.lang.Object)}
 * methods should be used when dataChanges are dealt locally and not update to MD-SAL.
 *
 * @param <K>
 *            Must be a Link
 * @param <V>
 *            Must be
 *            org.opendaylight.l2switch.hosttracker.plugin.inventory.Link;
 */

public class ConcurrentClusterAwareLinkHashMap<K, V> implements
        ConcurrentMap<K, V> {
    private final OperationProcessor opProcessor;
    private final String topologyId;

    private static final Logger LOG = LoggerFactory
            .getLogger(ConcurrentClusterAwareLinkHashMap.class);

    /**
     * The instance identifiers for each Link submitted to MD-SAL.
     */
    private final ConcurrentHashMap<InstanceIdentifier<Link>, K> instanceIDs;

    /**
     * The local Links' HashMap.
     */
    private final ConcurrentHashMap<K, V> linkHashMap;

    public ConcurrentClusterAwareLinkHashMap(OperationProcessor opProcessor,
                                             String topologyId) {
        this.opProcessor = opProcessor;
        this.topologyId = topologyId;
        this.linkHashMap = new ConcurrentHashMap<>();
        this.instanceIDs = new ConcurrentHashMap<>();
    }


    /**
     * Puts the given value (Link) only in this local HashMap. Ideally used for
     * Link data listener events.
     *
     * @param ii the value's (Link's) InstanceIdentifier&lt;Link&gt;
     * @param value the Link to store locally.
     * @return the previous value associated with <tt>key</tt>, or
     * <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    public synchronized V putLocally(InstanceIdentifier<Link> ii, V value) {
        Link l = ((Link) value);
        LOG.trace("Putting locally {}", l.getLinkId());
        this.instanceIDs.put(ii, (K) l.getLinkId());
        return this.linkHashMap.put((K) l.getLinkId(), value);
    }

    /**
     * Copies all of the mappings from the specified map to this local HashMap
     * and into MD-SAL.
     *
     * @param m
     *            mappings to be stored in this local HashMap and into MD-SAL
     */
    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            final Link linkNode = ((Link) e.getValue());
            final InstanceIdentifier<Link> buildLinkIID = Utilities
                    .buildLinkIID(linkNode.getKey(), topologyId);
            this.opProcessor.enqueueOperation(new HostTrackerOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction tx) {
                    tx.merge(LogicalDatastoreType.OPERATIONAL, buildLinkIID,
                            linkNode, true);
                }
            });
            putLocally(buildLinkIID, e.getValue());
        }
    }

    /**
     * Removes the given links both locally and on MD-SAL database.
     *
     * @param links
     *            the links to remove.
     */
    public synchronized void removeAll(List<Link> links) {
        for (final Map.Entry<InstanceIdentifier<Link>, K> e : this.instanceIDs
                .entrySet()) {
            LOG.debug("Links to remove from local & MD-SAL database", links.toString());
            for (Link l : links) {
                if (e.getValue().equals(l.getLinkId())) {
                    this.opProcessor
                            .enqueueOperation(new HostTrackerOperation() {
                                @Override
                                public void applyOperation(
                                        ReadWriteTransaction tx) {
                                    tx.delete(LogicalDatastoreType.OPERATIONAL,
                                            e.getKey());
                                }
                            });
                    this.linkHashMap.remove(e.getValue());
                    break;
                }
            }
        }
    }

    /**
     *
     * Replaces the entry for a key only if currently mapped to a given value.
     *
     * @param key
     *            key with which the specified value is associated
     * @param oldValue
     *            value expected to be associated with the specified key
     * @param newValue
     *            value to be associated with the specified key
     */
    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        if (this.linkHashMap.containsKey((K) key)
                && this.linkHashMap.get((K) key).equals(oldValue)) {
            put(key, newValue);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the KeySet from this local HashMap.
     *
     * @return the KeySet from this local HashMap.
     */
    @Override
    public synchronized Set<K> keySet() {
        return this.linkHashMap.keySet();
    }

    /**
     * Removes the entry for a key only if currently mapped to a given value.
     *
     * @param key
     *            key with which the specified value is associated
     * @param value
     *            value expected to be associated with the specified key
     * @return <tt>true</tt> if the value was removed
     */
    @Override
    public synchronized boolean remove(Object key, Object value) {
        if (this.linkHashMap.containsKey((K) key)
                && this.linkHashMap.get((K) key).equals(value)) {
            remove((K) key);
            return true;
        } else {
            return false;
        }
    }


    /**
     *
     * Replaces the entry for a key only if currently mapped to some value.
     *
     * @param key
     *            key with which the specified value is associated
     * @param value
     *            value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         <tt>null</tt> if there was no mapping for the key. (A
     *         <tt>null</tt> return can also indicate that the map previously
     *         associated <tt>null</tt> with the key, if the implementation
     *         supports null values.)
     */
    @Override
    public synchronized V replace(K key, V value) {
        if (this.linkHashMap.containsKey(key)) {
            return put(key, value);
        } else {
            return null;
        }
    }


    /**
     * If it's absent from the this local HashMap, puts the given host in the
     * this local HashMap and into MD-SAL database.
     *
     * @param key
     *            the key for the map
     * @param value
     *            the value for the map
     * @return the old value from the local cache if present, null otherwise.
     */
    @Override
    public synchronized V putIfAbsent(K key, V value) {
        if (!this.linkHashMap.contains(value)) {
            return this.linkHashMap.put(key, value);
        } else {
            return this.linkHashMap.get(key);
        }
    }


    /**
     * Puts the given link in the this local HashMap and into MD-SAL database.
     *
     * @param linkId
     *            the key for the map
     * @param link
     *            the value for the map
     * @return the old value from the local cache if present, null otherwise.
     */
    @Override
    public synchronized V put(K linkId, V link) {
        final Link linkNode = ((Link) link);
        final InstanceIdentifier<Link> buildLinkIID = Utilities.buildLinkIID(
                linkNode.getKey(), topologyId);
        this.opProcessor.enqueueOperation(new HostTrackerOperation() {
            @Override
            public void applyOperation(ReadWriteTransaction tx) {
                tx.merge(LogicalDatastoreType.OPERATIONAL, buildLinkIID,
                        linkNode, true);
            }
        });
        LOG.trace("Putting MD-SAL {}", linkNode.getLinkId());
        return putLocally(buildLinkIID, link);
    }

    @Override
    public synchronized int size() {
        return this.linkHashMap.size();
    }

    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        return this.linkHashMap.entrySet();
    }


    @Override
    public synchronized boolean isEmpty() {
        return this.linkHashMap.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return this.linkHashMap.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return this.linkHashMap.contains(value);
    }

    @Override
    public synchronized V get(Object key) {
        return this.linkHashMap.get(key);
    }

    /**
     * Removes the value (Host) with the given linkId from this local HashMap
     * and MD-SAL database.
     *
     * @param linkId
     *            the link's linkId to remove
     * @return the old value from the local cache if present, null otherwise.
     */
    @Override
    public synchronized V remove(Object linkId) {
        V removedValue = this.linkHashMap.remove(linkId);
        if (removedValue != null) {
            Link linkNode = (Link) removedValue;
            final InstanceIdentifier<Link> lnIID = Utilities.buildLinkIID(
                    linkNode.getKey(), topologyId);
            this.opProcessor.enqueueOperation(new HostTrackerOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction tx) {
                    tx.delete(LogicalDatastoreType.OPERATIONAL, lnIID);
                }
            });
            this.instanceIDs.remove(lnIID);
        }
        return removedValue;
    }
    /**
     * Returns the Values from this local HashMap.
     *
     * @return the Values from this local HashMap.
     */
    @Override
    public synchronized Collection<V> values() {
        return this.linkHashMap.values();
    }

    /**
     * Removes, if exists, the Link with the given InstanceIdentifier&lt;Link&gt; from
     * this local HashMap. Ideally used for link data listener events.
     *
     * @param iiL
     *            the InstanceIdentifier&lt;Link&gt; of the Link to remove.
     * @return the removed Link if exits, null if it doesn't exist.
     */
    public synchronized V removeLocally(InstanceIdentifier<Link> iiL) {
        K linkId = this.instanceIDs.get(iiL);
        if (linkId != null) {
            this.instanceIDs.remove(iiL);
            return this.linkHashMap.remove(linkId);
        }
        return null;
    }

    /**
     * Removes, if exists, the Link with the given Key (LinkId) from this local
     * HashMap. Ideally used for link data listener events.
     *
     * @param key
     *            the key (LinkId) of the Link to remove.
     * @return the removed Link if exits, null if it doesn't exist.
     */
    public synchronized V removeLocally(K key) {
        Iterator<Entry<InstanceIdentifier<Link>, K>> iterator = this.instanceIDs
                .entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().equals(key)) {
                iterator.remove();
                break;
            }
        }
        return linkHashMap.remove(key);
    }

    /**
     *
     * Removes all of the mappings from this local HashMap and from MD-SAL. The
     * local HashMap will be empty after this call returns.
     *
     */
    @Override
    public synchronized void clear() {
        for (final Map.Entry<? extends InstanceIdentifier<Link>, ? extends K> e : this.instanceIDs
                .entrySet()) {
            this.opProcessor.enqueueOperation(new HostTrackerOperation() {
                @Override
                public void applyOperation(ReadWriteTransaction tx) {
                    tx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey());
                }
            });
        }
        this.linkHashMap.clear();
    }
}
