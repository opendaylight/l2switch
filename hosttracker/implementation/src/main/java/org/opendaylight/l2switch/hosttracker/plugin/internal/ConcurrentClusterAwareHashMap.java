/**
 * Copyright (c) 2014 André Martins and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.hosttracker.plugin.inventory.Host;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConcurrentClusterAwareHashMap<K, V> extends ConcurrentHashMap<K, V> {

    private final DataBroker dataService;

    private final ConcurrentHashMap<InstanceIdentifier<Node>, K> instanceIDs;

    public ConcurrentClusterAwareHashMap(DataBroker dataService) {
        super();
        this.dataService = dataService;
        instanceIDs = new ConcurrentHashMap<>();
    }

    public synchronized V removeLocally(InstanceIdentifier<Node> iiN) {
        K hostId = instanceIDs.get(iiN);
        if (hostId != null) {
            instanceIDs.remove(iiN);
            return super.remove(hostId);
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
        return super.remove(key);
    }

    public synchronized V putLocally(InstanceIdentifier<Node> ii, V value) {
        Host h = ((Host) value);
        instanceIDs.put(ii, (K) h.getId());
        return super.put((K) h.getId(), value);
    }

    @Override
    public synchronized V put(K hostId, V host) {
        V oldValue = super.put(hostId, host);
        Node hostNode = ((Host) host).getHostNode();
        InstanceIdentifier<Node> buildNodeIID = Utilities.buildNodeIID(hostNode.getKey());
        WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, buildNodeIID, hostNode, true);
        instanceIDs.put(buildNodeIID, hostId);
        writeTx.submit();
        return oldValue;
    }

    @Override
    public synchronized V remove(Object hostId) {
        V removedValue = super.remove(hostId);
        if (removedValue != null) {
            Node hostNode = ((Host) removedValue).getHostNode();
            InstanceIdentifier<Node> hnIID = Utilities.buildNodeIID(hostNode.getKey());
            WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, hnIID);
            writeTx.submit();
        }
        return removedValue;
    }

}
