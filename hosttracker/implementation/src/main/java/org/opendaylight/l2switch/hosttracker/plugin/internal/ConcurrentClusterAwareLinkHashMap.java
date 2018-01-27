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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This will (try to) submit all writes and deletes in to the MD-SAL database.
 * The removeLocally and putLocally methods should be used when dataChanges are dealt locally and not update to MD-SAL.
 */

public class ConcurrentClusterAwareLinkHashMap {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentClusterAwareLinkHashMap.class);

    private final OperationProcessor opProcessor;

    /**
     * The instance identifiers for each Link submitted to MD-SAL.
     */
    private final ConcurrentHashMap<InstanceIdentifier<Link>, LinkId> instanceIDs = new ConcurrentHashMap<>();

    /**
     * The local Links' HashMap.
     */
    private final ConcurrentHashMap<LinkId, Link> linkHashMap = new ConcurrentHashMap<>();

    public ConcurrentClusterAwareLinkHashMap(OperationProcessor opProcessor) {
        this.opProcessor = opProcessor;
    }


    /**
     * Puts the given value (Link) only in this local HashMap. Ideally used for
     * Link data listener events.
     *
     * @param ii the value's (Link's) InstanceIdentifier&lt;Link&gt;
     * @param link the Link to store locally.
     * @return the previous value associated with <tt>key</tt>, or
     * <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    public synchronized Link putLocally(InstanceIdentifier<Link> ii, Link link) {
        LOG.trace("Putting locally {}", link.getLinkId());
        this.instanceIDs.put(ii, link.getLinkId());
        return this.linkHashMap.put(link.getLinkId(), link);
    }

    /**
     * Removes the given links both locally and on MD-SAL database.
     *
     * @param links
     *            the links to remove.
     */
    public synchronized void removeAll(List<Link> links) {
        for (final Map.Entry<InstanceIdentifier<Link>, LinkId> e : this.instanceIDs.entrySet()) {
            LOG.debug("Links to remove from local & MD-SAL database", links.toString());
            for (Link l : links) {
                if (e.getValue().equals(l.getLinkId())) {
                    this.opProcessor.enqueueOperation(tx -> tx.delete(LogicalDatastoreType.OPERATIONAL, e.getKey()));
                    this.linkHashMap.remove(e.getValue());
                    break;
                }
            }
        }
    }

    /**
     * Returns the Values from this local HashMap.
     *
     * @return the Values from this local HashMap.
     */
    public synchronized Collection<Link> values() {
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
    public synchronized Link removeLocally(InstanceIdentifier<Link> iiL) {
        LinkId linkId = this.instanceIDs.remove(iiL);
        if (linkId != null) {
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
    public synchronized Link removeLocally(LinkId key) {
        Iterator<Entry<InstanceIdentifier<Link>, LinkId>> iterator = this.instanceIDs.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().equals(key)) {
                iterator.remove();
                break;
            }
        }
        return linkHashMap.remove(key);
    }
}
