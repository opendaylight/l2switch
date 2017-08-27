/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.internal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.host.tracker.config.rev140528.HostTrackerConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
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

@SuppressWarnings("rawtypes")
public class HostTrackerImpl implements DataTreeChangeListener<DataObject> {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    /**
     * As defined on
     * controller/opendaylight/md-sal/topology-manager/src/main/java/org/opendaylight/md/controller/topology/manager/FlowCapableTopologyProvider.java
     */
    private static final String TOPOLOGY_NAME = "flow:1";

    private static final Logger LOG = LoggerFactory.getLogger(HostTrackerImpl.class);

    private final DataBroker dataService;
    private final String topologyId;
    private final long hostPurgeInterval;
    private final long hostPurgeAge;
    private static int numHostsPurged;

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(CPUS);

    private final ConcurrentClusterAwareHostHashMap<HostId, Host> hosts;
    private final ConcurrentClusterAwareLinkHashMap<LinkId, Link> links;
    private final OperationProcessor opProcessor;
    private ListenerRegistration<DataTreeChangeListener> addrsNodeListenerRegistration;
    private ListenerRegistration<DataTreeChangeListener> hostNodeListenerRegistration;
    private ListenerRegistration<DataTreeChangeListener> linkNodeListenerRegistration;

    /**
     * It creates hosts using reference to MD-SAl / toplogy module. For every hostPurgeIntervalInput time interval
     * it requests to purge hosts that are not seen for hostPurgeAgeInput time interval.
     *
     * @param dataService A reference to the MD-SAL
     * @param config Default configuration
     */
    public HostTrackerImpl(final DataBroker dataService, final HostTrackerConfig config) {
        Preconditions.checkNotNull(dataService, "dataBrokerService should not be null.");
        Preconditions.checkArgument(config.getHostPurgeAge() >= 0, "hostPurgeAgeInput must be non-negative");
        Preconditions.checkArgument(config.getHostPurgeInterval() >= 0, "hostPurgeIntervalInput must be non-negative");
        this.dataService = dataService;
        this.hostPurgeAge = config.getHostPurgeAge();
        this.hostPurgeInterval = config.getHostPurgeInterval();
        this.opProcessor = new OperationProcessor(dataService);
        Thread processorThread = new Thread(opProcessor);
        processorThread.start();
        final String topologyId = config.getTopologyId();
        if (topologyId == null || topologyId.isEmpty()) {
            this.topologyId = TOPOLOGY_NAME;
        } else {
            this.topologyId = topologyId;
        }
        this.hosts = new ConcurrentClusterAwareHostHashMap<>(opProcessor, this.topologyId);
        this.links = new ConcurrentClusterAwareLinkHashMap<>(opProcessor, this.topologyId);

        if (hostPurgeInterval > 0) {
            exec.scheduleWithFixedDelay(() -> purgeHostsNotSeenInLast(hostPurgeAge), 0, hostPurgeInterval, TimeUnit.SECONDS);
        }
    }

    @SuppressWarnings("unchecked")
    public void init() {
        InstanceIdentifier<Addresses> addrCapableNodeConnectors = //
                InstanceIdentifier.builder(Nodes.class) //
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class) //
                        .child(NodeConnector.class) //
                        .augmentation(AddressCapableNodeConnector.class)//
                        .child(Addresses.class).build();
        this.addrsNodeListenerRegistration = dataService.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, addrCapableNodeConnectors), (DataTreeChangeListener)this);

        InstanceIdentifier<HostNode> hostNodes = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Node.class)
                .augmentation(HostNode.class).build();
        this.hostNodeListenerRegistration = dataService.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, hostNodes), (DataTreeChangeListener)this);

        InstanceIdentifier<Link> lIID = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Link.class).build();

        this.linkNodeListenerRegistration = dataService.registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, lIID), (DataTreeChangeListener)this);

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
    public void onDataTreeChanged(Collection<DataTreeModification<DataObject>> changes) {
        for (DataTreeModification<?> change: changes) {
            DataObjectModification<?> rootNode = change.getRootNode();
            final InstanceIdentifier<?> identifier = change.getRootPath().getRootIdentifier();
            switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    onModifiedData(identifier, rootNode);
                    break;
                case DELETE:
                    onDeletedData(identifier, rootNode);
                    break;
                default:
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void onModifiedData(InstanceIdentifier<?> iid, DataObjectModification<?> rootNode) {
        final DataObject dataObject = rootNode.getDataAfter();
        if (dataObject instanceof Addresses) {
            packetReceived((Addresses) dataObject, iid);
        } else if (dataObject instanceof Node) {
            synchronized (hosts) {
                hosts.putLocally((InstanceIdentifier<Node>) iid, Host.createHost((Node) dataObject));
            }
        } else if (dataObject instanceof  Link) {
            synchronized (links) {
                links.putLocally((InstanceIdentifier<Link>) iid, (Link) dataObject);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void onDeletedData(InstanceIdentifier<?> iid, DataObjectModification<?> rootNode) {
        if (iid.getTargetType().equals(Node.class)) {
            Node node = (Node) rootNode.getDataBefore();
            InstanceIdentifier<Node> iiN = (InstanceIdentifier<Node>) iid;
            HostNode hostNode = node.getAugmentation(HostNode.class);
            if (hostNode != null) {
                synchronized (hosts) {
                    try {
                        hosts.removeLocally(iiN);
                    } catch (ClassCastException ex1) {
                        LOG.debug("Exception occurred while remove host locally", ex1);
                    }
                }
            } else if (iid.getTargetType().equals(Link.class)) {
                // TODO performance improvement here
                InstanceIdentifier<Link> iiL = (InstanceIdentifier<Link>) iid;
                synchronized (links) {
                    try {
                        links.removeLocally(iiL);
                    } catch (ClassCastException ex2) {
                        LOG.debug("Exception occurred while remove link locally", ex2);
                    }
                }
                linkRemoved((InstanceIdentifier<Link>) iid, (Link) rootNode.getDataBefore());
            }
        }

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
            LOG.warn(ex.getLocalizedMessage());
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
            LOG.trace("Processing nodeConnector: {} ", nodeConnector.getId().toString());
            HostId hId = Host.createHostId(addrs);
            if (hId != null) {
                if (isNodeConnectorInternal(nodeConnector)) {
                    LOG.trace("NodeConnector is internal: {} ", nodeConnector.getId().toString());

                    removeNodeConnectorFromHost(hostsToMod, hostsToRem, nodeConnector);
                    hosts.removeAll(hostsToRem);
                    hosts.putAll(hostsToMod);
                } else {
                    LOG.trace("NodeConnector is NOT internal {} ", nodeConnector.getId().toString());
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
            LOG.warn(ex.getLocalizedMessage());
            return false;
        }
        if (oNT != null && oNT.isPresent()) {
            NetworkTopology networkTopo = oNT.get();
            for (Topology t : networkTopo.getTopology()) {
                if (t.getLink() != null) {
                    for (Link l : t.getLink()) {
                        if (l.getSource().getSourceTp().equals(tpId)
                                && !l.getDestination().getDestTp().getValue().startsWith(Host.NODE_PREFIX)
                                || l.getDestination().getDestTp().equals(tpId)
                                && !l.getSource().getSourceTp().getValue().startsWith(Host.NODE_PREFIX)) {
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
        LOG.trace("linkRemoved");
        List<Host> hostsToMod = new ArrayList<>();
        List<Host> hostsToRem = new ArrayList<>();
        synchronized (hosts) {
            removeLinksFromHosts(hostsToMod, hostsToRem, linkRemoved);
            hosts.removeAll(hostsToRem);
            hosts.putAll(hostsToMod);
        }
    }

    private void writeDatatoMDSAL(List<Link> linksToAdd, List<Link> linksToRemove) {
        if (linksToAdd != null) {
            for (final Link l : linksToAdd) {
                final InstanceIdentifier<Link> lIID = Utilities.buildLinkIID(l.getKey(), topologyId);
                LOG.trace("Writing link from MD_SAL: {}", lIID.toString());
                opProcessor.enqueueOperation(tx -> tx.merge(LogicalDatastoreType.OPERATIONAL, lIID, l, true));
            }
        }
        if (linksToRemove != null) {
            for (Link l : linksToRemove) {
                final InstanceIdentifier<Link> lIID = Utilities.buildLinkIID(l.getKey(), topologyId);
                LOG.trace("Removing link from MD_SAL: {}", lIID.toString());
                opProcessor.enqueueOperation(tx -> tx.delete(LogicalDatastoreType.OPERATIONAL,  lIID));
            }
        }
    }

    /**
     * Remove all hosts that haven't been observed more recently than the specified number of
     * hostsPurgeAgeInSeconds.
     *
     * @param hostsPurgeAgeInSeconds remove hosts that haven't been observed in longer than this number of
     *               hostsPurgeAgeInSeconds.
     * @return the number of purged hosts
     */
    protected int purgeHostsNotSeenInLast(final long hostsPurgeAgeInSeconds) {
        numHostsPurged = 0;
        final long nowInMillis = System.currentTimeMillis();
        final long nowInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMillis);
        // iterate through all hosts in the local cache
        for (Host h : hosts.values()) {
            final HostNode hn = h.getHostNode().getAugmentation(HostNode.class);
            if (hn == null) {
                LOG.warn("Encountered non-host node {} in hosts during purge", hn);
            } else if (hn.getAddresses() != null) {
                boolean purgeHosts = false;
                // if the node is a host and has addresses, check to see if it's been seen recently
                purgeHosts = hostReadyForPurge( hn, nowInSeconds,hostsPurgeAgeInSeconds);
                if (purgeHosts) {
                    removeHosts(h);
                }
            } else {
                LOG.warn("Encountered host node {} with no address in hosts during purge", hn);
            }
        }
        LOG.debug("Number of purged hosts during current purge interval - {}. ", numHostsPurged);
        return numHostsPurged;
    }

    /**
     * Checks if hosts need to be purged
     *
     * @param hostNode reference to HostNode class
     * @param currentTimeInSeconds current time in seconds
     * @param expirationPeriod timelimit set to hosts for expiration
     * @return boolean - whether the hosts are ready to be purged
     */
    private boolean hostReadyForPurge(final HostNode hostNode,final long currentTimeInSeconds,final long expirationPeriod) {
        // checks if hosts need to be purged
        for (Addresses addrs : hostNode.getAddresses()) {
            long lastSeenTimeInSeconds = addrs.getLastSeen()/1000;
            if (lastSeenTimeInSeconds > currentTimeInSeconds - expirationPeriod) {
                LOG.debug("Host node {} NOT ready for purge", hostNode);
                return false;
            }
        }
        LOG.debug("Host node {} ready for purge", hostNode);
        return true;
    }

    /**
     * Removes hosts from locally and MD-SAL. Throws warning message if not removed successfully
     *
     * @param host  reference to Host node
     */
    private void removeHosts(final Host host){
        // remove associated links with the host before removing hosts
        removeAssociatedLinksFromHosts(host);
        // purge hosts from local & MD-SAL database
        if (hosts.remove(host.getId()) != null) {
            numHostsPurged++;
            LOG.debug("Removed host with id {} during purge.", host.getId());
        } else {
            LOG.warn("Unexpected error encountered - Failed to remove host {} during purge", host);
        }
    }

    /**
     * Removes links associated with the given hosts from local and MD-SAL database.
     * Throws warning message if not removed successfully.
     *
     * @param host  reference to Host node
     */
    private void removeAssociatedLinksFromHosts(final Host host) {
        if (host != null) {
            if (host.getId() != null) {
                List<Link> linksToRemove = new ArrayList<>();
                for (Link link: links.values()) {
                    if (link.toString().contains(host.getId().getValue())) {
                        linksToRemove.add(link);
                    }
                }
                links.removeAll(linksToRemove);
            } else {
                LOG.warn("Encountered host with no id , Unexpected host id {}. ", host);
            }
        } else {
            LOG.warn("Encountered Host with no value, Unexpected host {}. ", host);
        }
    }

    public void close() {
        this.addrsNodeListenerRegistration.close();
        this.hostNodeListenerRegistration.close();
        this.linkNodeListenerRegistration.close();
        this.exec.shutdownNow();
        synchronized (hosts) {
            this.hosts.clear();
        }
    }
}
