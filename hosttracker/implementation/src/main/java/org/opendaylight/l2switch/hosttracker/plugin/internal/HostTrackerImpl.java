/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All
 * rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.l2switch.hosttracker.plugin.inventory.Host;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTrackerImpl implements DataChangeListener {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    /**
     * As defined on
     * controller/opendaylight/md-sal/topology-manager/src/main/java/org/opendaylight/md/controller/topology/manager/FlowCapableTopologyProvider.java
     */
    private static final String TOPOLOGY_NAME = "flow:1";

    private static final Logger log = LoggerFactory.getLogger(HostTrackerImpl.class);

    private final DataBroker dataService;
    private final String topologyId;

    ExecutorService exec = Executors.newFixedThreadPool(CPUS);

    private final ConcurrentClusterAwareHostHashMap<HostId, Host> hosts;
    private ListenerRegistration<DataChangeListener> addrsNodeListerRegistration;
    private ListenerRegistration<DataChangeListener> hostNodeListerRegistration;

    public HostTrackerImpl(DataBroker dataService, String topologyId) {
        Preconditions.checkNotNull(dataService, "dataBrokerService should not be null.");
        this.dataService = dataService;
        if (topologyId == null || topologyId.isEmpty()) {
            this.topologyId = TOPOLOGY_NAME;
        } else {
            this.topologyId = topologyId;
        }
        this.hosts = new ConcurrentClusterAwareHostHashMap<>(dataService, this.topologyId);
    }

    public void registerAsDataChangeListener() {
        InstanceIdentifier<Addresses> addrCapableNodeConnectors = //
                InstanceIdentifier.builder(Nodes.class) //
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class) //
                .child(NodeConnector.class) //
                .augmentation(AddressCapableNodeConnector.class)//
                .child(Addresses.class).build();
        this.addrsNodeListerRegistration = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, addrCapableNodeConnectors, this, DataChangeScope.SUBTREE);

        InstanceIdentifier<HostNode> hostNodes = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Node.class)
                .augmentation(HostNode.class).build();
        this.hostNodeListerRegistration = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, hostNodes, this, DataChangeScope.SUBTREE);

        InstanceIdentifier<Link> lIID = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Link.class).build();

        this.addrsNodeListerRegistration = dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, lIID, this, DataChangeScope.BASE);

        //Processing addresses that existed before we register as a data change listener.
//        ReadOnlyTransaction newReadOnlyTransaction = dataService.newReadOnlyTransaction();
//        InstanceIdentifier<NodeConnector> iinc = addrCapableNodeConnectors.firstIdentifierOf(NodeConnector.class);
//        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> iin//
//                = addrCapableNodeConnectors.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class);
//        ListenableFuture<Optional<NodeConnector>> dataFuture = newReadOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, iinc);
//        try {
//            NodeConnector get = dataFuture.get().get();
//            log.trace("test "+get);
//        } catch (InterruptedException | ExecutionException ex) {
//            java.util.logging.Logger.getLogger(HostTrackerImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        Futures.addCallback(dataFuture, new FutureCallback<Optional<NodeConnector>>() {
//            @Override
//            public void onSuccess(final Optional<NodeConnector> result) {
//                if (result.isPresent()) {
//                    log.trace("Processing NEW NODE? " + result.get().getId().getValue());
////                    processHost(result, dataObject, node);
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable arg0) {
//            }
//        });
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {

        exec.submit(new Runnable() {
            @Override
            public void run() {
                if (change == null) {
                    log.info("In onDataChanged: No processing done as change even is null.");
                    return;
                }
                Map<InstanceIdentifier<?>, DataObject> updatedData = change.getUpdatedData();
                Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
                Map<InstanceIdentifier<?>, DataObject> originalData = change.getOriginalData();
                Set<InstanceIdentifier<?>> deletedData = change.getRemovedPaths();

                for (InstanceIdentifier<?> iid : deletedData) {
                    if (iid.getTargetType().equals(Node.class)) {
                        Node node = ((Node) originalData.get(iid));
                        InstanceIdentifier<Node> iiN = (InstanceIdentifier<Node>) iid;
                        HostNode hostNode = node.getAugmentation(HostNode.class);
                        if (hostNode != null) {
                            synchronized (hosts) {
                                try {
                                    hosts.removeLocally(iiN);
                                } catch (ClassCastException ex) {
                                }
                            }
                        }
                    } else if (iid.getTargetType().equals(Link.class)) {
                        // TODO performance improvement here
                        linkRemoved((InstanceIdentifier<Link>) iid, (Link) originalData.get(iid));
                    }
                }

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : updatedData.entrySet()) {
                    InstanceIdentifier<?> iiD = entrySet.getKey();
                    final DataObject dataObject = entrySet.getValue();
                    if (dataObject instanceof Addresses) {
                        packetReceived((Addresses) dataObject, iiD);
                    } else if (dataObject instanceof Node) {
                        synchronized (hosts) {
                            hosts.putLocally((InstanceIdentifier<Node>) iiD, Host.createHost((Node) dataObject));
                        }
                    }
                }

                for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : createdData.entrySet()) {
                    InstanceIdentifier<?> iiD = entrySet.getKey();
                    final DataObject dataObject = entrySet.getValue();
                    if (dataObject instanceof Addresses) {
                        packetReceived((Addresses) dataObject, iiD);
                    } else if (dataObject instanceof Node) {
                        synchronized (hosts) {
                            hosts.putLocally((InstanceIdentifier<Node>) iiD, Host.createHost((Node) dataObject));
                        }
                    }
                }
            }
        });
    }

    public void packetReceived(Addresses addrs, InstanceIdentifier<?> ii) {
        InstanceIdentifier<NodeConnector> iinc = ii.firstIdentifierOf(NodeConnector.class);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> iin//
                = ii.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class);

        ListenableFuture<Optional<NodeConnector>> futureNodeConnector;
        ListenableFuture<Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>> futureNode;
        try (ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction()) {
            futureNodeConnector = readTx.read(LogicalDatastoreType.OPERATIONAL, iinc);
            futureNode = readTx.read(LogicalDatastoreType.OPERATIONAL, iin);
            readTx.close();
        }
        Optional<NodeConnector> opNodeConnector = null;
        Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> opNode = null;
        try {
            opNodeConnector = futureNodeConnector.get();
            opNode = futureNode.get();
        } catch (ExecutionException | InterruptedException ex) {
            log.warn(ex.getLocalizedMessage());
        }
        if (opNode != null && opNode.isPresent()
                && opNodeConnector != null && opNodeConnector.isPresent()) {
            processHost(opNode.get(), opNodeConnector.get(), addrs);
        }
    }

    private void processHost(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node,
            NodeConnector nodeConnector,
            Addresses addrs) {
        List<Host> hostsToMod = new ArrayList<>();
        List<Host> hostsToRem = new ArrayList<>();
        List<Link> linksToRem = new ArrayList<>();
        List<Link> linksToAdd = new ArrayList<>();
        synchronized (hosts) {
            log.trace("Processing nodeConnector " + nodeConnector.getId().toString());
            HostId hId = Host.createHostId(addrs);
            if (hId != null) {
                if (isNodeConnectorInternal(nodeConnector)) {
                    log.trace("NodeConnector is internal " + nodeConnector.getId().toString());

                    removeNodeConnectorFromHost(hostsToMod, hostsToRem, nodeConnector);
                    hosts.removeAll(hostsToRem);
                    hosts.putAll(hostsToMod);
                } else {
                    log.trace("NodeConnector is NOT internal " + nodeConnector.getId().toString());
                    Host host = new Host(addrs, nodeConnector);
                    if (hosts.containsKey(host.getId())) {
                        hosts.get(host.getId()).mergeHostWith(host);
                    } else {
                        hosts.put(host.getId(), host);
                    }
                    List<Link> newLinks = hosts.get(host.getId()).createLinks(node);
                    if (newLinks != null) {
                        linksToAdd.addAll(newLinks);
                    }
                    hosts.submit(host.getId());
                }
            }
        }
        writeDatatoMDSAL(linksToAdd, linksToRem);
    }

    /**
     * It verifies if a given NodeConnector is *internal*. An *internal*
     * NodeConnector is considered to be all NodeConnetors that are NOT attached
     * to hosts created by hosttracker.
     *
     * @param nodeConnector the nodeConnector to check if it is internal or not.
     * @return true if it was found a host connected to this nodeConnetor, false
     * if it was not found a network topology or it was not found a host
     * connected to this nodeConnetor.
     */
    private boolean isNodeConnectorInternal(NodeConnector nodeConnector) {
        TpId tpId = new TpId(nodeConnector.getKey().getId().getValue());
        InstanceIdentifier<NetworkTopology> ntII
                = InstanceIdentifier.builder(NetworkTopology.class).build();
        ListenableFuture<Optional<NetworkTopology>> lfONT;
        try (ReadOnlyTransaction rot = dataService.newReadOnlyTransaction()) {
            lfONT = rot.read(LogicalDatastoreType.OPERATIONAL, ntII);
            rot.close();
        }
        Optional<NetworkTopology> oNT;
        try {
            oNT = lfONT.get();
        } catch (InterruptedException | ExecutionException ex) {
            log.warn(ex.getLocalizedMessage());
            return false;
        }
        if (oNT != null && oNT.isPresent()) {
            NetworkTopology networkTopo = oNT.get();
            for (Topology t : networkTopo.getTopology()) {
                if (t.getLink() != null) {
                    for (Link l : t.getLink()) {
                        if ((l.getSource().getSourceTp().equals(tpId)
                                && !l.getDestination().getDestTp().getValue().startsWith(Host.NODE_PREFIX))
                                || (l.getDestination().getDestTp().equals(tpId)
                                && !l.getSource().getSourceTp().getValue().startsWith(Host.NODE_PREFIX))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void removeLinksFromHosts(List<Host> hostsToMod, List<Host> hostsToRem, Link linkRemoved) {
        for (Host h : hosts.values()) {
            h.removeTerminationPoint(linkRemoved.getSource().getSourceTp());
            h.removeTerminationPoint(linkRemoved.getDestination().getDestTp());
            if (h.isOrphan()) {
                hostsToRem.add(h);
            } else {
                hostsToMod.add(h);
            }
        }
    }

    private void removeNodeConnectorFromHost(List<Host> hostsToMod, List<Host> hostsToRem, NodeConnector nc) {
        AttachmentPointsBuilder atStD = Utilities.createAPsfromNodeConnector(nc);
        for (Host h : hosts.values()) {
            h.removeAttachmentPoints(atStD);
            if (h.isOrphan()) {
                hostsToRem.add(h);
            } else {
                hostsToMod.add(h);
            }
        }
    }

    private void linkRemoved(InstanceIdentifier<Link> iiLink, Link linkRemoved) {
        log.trace("linkRemoved");
        List<Host> hostsToMod = new ArrayList<>();
        List<Host> hostsToRem = new ArrayList<>();
        synchronized (hosts) {
            removeLinksFromHosts(hostsToMod, hostsToRem, linkRemoved);
            hosts.removeAll(hostsToRem);
            hosts.putAll(hostsToMod);
        }
    }

    private void writeDatatoMDSAL(List<Link> linksToAdd, List<Link> linksToRemove) {

        final WriteTransaction writeTx = dataService.newWriteOnlyTransaction();
        if (linksToAdd != null) {
            for (Link l : linksToAdd) {
                InstanceIdentifier<Link> lIID = Utilities.buildLinkIID(l.getKey(), topologyId);
                log.trace("Writing link from MD_SAL: " + lIID.toString());
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, lIID, l, true);
            }
        }
        if (linksToRemove != null) {
            for (Link l : linksToRemove) {
                InstanceIdentifier<Link> lIID = Utilities.buildLinkIID(l.getKey(), topologyId);
                log.trace("Removing link from MD_SAL: " + lIID.toString());
                writeTx.delete(LogicalDatastoreType.OPERATIONAL, lIID);
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
    }

    public void close() {
        this.addrsNodeListerRegistration.close();
        this.hostNodeListerRegistration.close();
        synchronized (hosts) {
            this.hosts.clear();
        }
    }
}
