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
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.l2switch.hosttracker.plugin.inventory.Host;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.AddressCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6Packet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTrackerImpl implements DataChangeListener, ArpPacketListener, Ipv4PacketListener, Ipv6PacketListener {

    private static final int CPUS = Runtime.getRuntime().availableProcessors();

    private static final Logger log = LoggerFactory.getLogger(HostTrackerImpl.class);

    private DataBroker dataService;

    private NotificationService notificationService;

    private ConcurrentHashMap<HostId, Host> hosts;

    private final String IPV4_IP_TO_IGNORE = "0.0.0.0";
    private final String IPV6_IP_TO_IGNORE = "0:0:0:0:0:0:0:0";

    HostTrackerImpl(DataBroker dataService, NotificationService notificationProviderService) {
        Preconditions.checkNotNull(dataService, "dataBrokerService should not be null.");
        this.dataService = dataService;
        this.notificationService = notificationProviderService;
        this.hosts = new ConcurrentHashMap<>();
    }

    private void packetReceived(Addresses addrs, InstanceIdentifier<?> ii) {
        InstanceIdentifier<NodeConnector> iinc = ii.firstIdentifierOf(NodeConnector.class);
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> iin//
                = ii.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class);

        ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction();
        ListenableFuture<Optional<NodeConnector>> futureNodeConnector = readTx.read(LogicalDatastoreType.OPERATIONAL, iinc);
        ListenableFuture<Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>> futureNode //
                = readTx.read(LogicalDatastoreType.OPERATIONAL, iin);
        try {
            if (futureNodeConnector.get().isPresent()
                    && futureNode.get().isPresent()) {
                processHost(futureNode.get().get(),
                        futureNodeConnector.get().get(),
                        addrs);
            }
        } catch (ExecutionException | InterruptedException ex) {

        }
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        ExecutorService exec = Executors.newFixedThreadPool(CPUS);
        exec.submit(new Runnable() {
            public void run() {
                if (change == null) {
                    log.info("In onDataChanged: No processing done as change even is null.");
                }
                Map<InstanceIdentifier<?>, DataObject> linkOriginalData = (Map<InstanceIdentifier<?>, DataObject>) change.getOriginalData();
                Map<InstanceIdentifier<?>, DataObject> linkUpdatedData = change.getUpdatedData();
                for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : linkUpdatedData.entrySet()) {
                    InstanceIdentifier<?> key = entrySet.getKey();
                    final DataObject dataObject = entrySet.getValue();
                    if (dataObject instanceof Addresses) {
                        packetReceived((Addresses) dataObject, key);
                    }
                }
            }
        });
    }

    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void registerAsDataChangeListener() {
        InstanceIdentifier<Addresses> addrCapableNodeConnectors = //
                InstanceIdentifier.builder(Nodes.class) //
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class) //
                .child(NodeConnector.class) //
                .augmentation(AddressCapableNodeConnector.class)//
                .child(Addresses.class).build();
        dataService.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, addrCapableNodeConnectors, this, DataChangeScope.SUBTREE);

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

    private void processHost(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node,
            NodeConnector nodeConnector,
            Addresses addrs) {
        List<Host> hostsToMod = new ArrayList<>();
        List<Link> linksToRem = new ArrayList<>();
        List<Link> linksToAdd = new ArrayList<>();
        synchronized (hosts) {
            log.trace("Processing nodeConnector " + nodeConnector.getId().toString());
            HostId hId = Host.createHostId(addrs);
            if (hId != null) {
                if (isNodeConnectorInternal(nodeConnector)) {
                    log.trace("NodeConnector is internal " + nodeConnector.getId().toString());
                    removeLinksAttachedToNodeConnector(hostsToMod, linksToRem, node.getId(), nodeConnector);
                } else {
                    log.trace("NodeConnector is NOT internal " + nodeConnector.getId().toString());
                    Host host = new Host(addrs, nodeConnector);
                    if (hosts.containsKey(host.getId())) {
                        hosts.get(host.getId()).updateHostWith(host);
                    } else {
                        hosts.put(host.getId(), host);
                    }
                    List<Link> newLinks = host.createLinks(node);
                    if (newLinks != null) {
                        linksToAdd.addAll(newLinks);
                    }
                    hostsToMod.add(hosts.get(host.getId()));
                }
            }
        }
        writeDatatoMDSAL(hostsToMod, linksToRem, linksToAdd);
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
        ReadOnlyTransaction rot = dataService.newReadOnlyTransaction();
        ListenableFuture<Optional<NetworkTopology>> lfONT
                = rot.read(LogicalDatastoreType.OPERATIONAL, ntII);
        Optional<NetworkTopology> oNT;
        try {
            oNT = lfONT.get();
        } catch (InterruptedException | ExecutionException ex) {
            return false;
        }
        if (oNT != null && oNT.isPresent()) {
            NetworkTopology networkTopo = oNT.get();
            for (Topology t : networkTopo.getTopology()) {
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
        return false;
    }

    private void writeDatatoMDSAL(List<Host> hosts, List<Link> linksToRemove, List<Link> linksToAdd) {

        ReadWriteTransaction writeTx = dataService.newReadWriteTransaction();
        if (linksToAdd != null) {
            for (Link l : linksToAdd) {
                InstanceIdentifier<Link> lIID = Utilities.buildLinkIID(l.getKey());
                writeTx.put(LogicalDatastoreType.OPERATIONAL, lIID, l, true);
            }
        }
        if (linksToRemove != null) {
            for (Link l : linksToRemove) {
                InstanceIdentifier<Link> lIID = Utilities.buildLinkIID(l.getKey());
                writeTx.delete(LogicalDatastoreType.OPERATIONAL, lIID);
            }
        }
        if (hosts != null) {
            for (Host h : hosts) {
                Node ourHostNode = h.getHostNode();
                InstanceIdentifier<Node> hnIID = Utilities.buildNodeIID(ourHostNode.getKey());
                log.trace("Writing host node to MD_SAL: " + ourHostNode.toString());
                writeTx.put(LogicalDatastoreType.OPERATIONAL, hnIID, ourHostNode, true);
            }
        }
        writeTx.submit();
    }

    private void removeLinksAttachedToNodeConnector(List<Host> hostsToMod, List<Link> linksToRem, NodeId nodeId, NodeConnector nodeConnector) {
        AttachmentPoints at = new AttachmentPointsBuilder()//
                .setTpId(new TpId(nodeConnector.getId().getValue()))//
                .build();
        for (Host h : hosts.values()) {
            List<Link> linksToRemove = h.removeAttachmentPoints(at, nodeId);
            if (!linksToRemove.isEmpty()) {
                hostsToMod.add(h);
                linksToRem.addAll(linksToRemove);
            }
        }
    }

    void registerAsNotificationProvider() {
        this.notificationService.registerNotificationListener(this);
    }

    @Override
    public void onArpPacketReceived(ArpPacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }

        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        ArpPacket arpPacket = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof ArpPacket) {
                arpPacket = (ArpPacket) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || arpPacket == null) {
            return;
        }
        VlanId vlanId = null;
        if (ethernetPacket.getEthertype().equals(KnownEtherType.VlanTagged)) {
            vlanId = ethernetPacket.getHeader8021q().get(0).getVlan();
        }
        MacAddress sourceMac = ethernetPacket.getSourceMac();
        IpAddress ipAddress = null;
        if (arpPacket.getProtocolType().equals(KnownEtherType.Ipv4)) {
            ipAddress = new IpAddress(new Ipv4Address(arpPacket.getSourceProtocolAddress()));
        }
        Addresses addrs = createAddresses(sourceMac, vlanId, ipAddress, ethernetPacket.getEthertype());
        if (addrs == null) {
            return;
        }
        NodeConnectorRef ingress = rawPacket.getIngress();
        packetReceived(addrs, ingress.getValue());
    }

    @Override
    public void onIpv4PacketReceived(Ipv4PacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }

        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        Ipv4Packet ipv4Packet = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof Ipv4Packet) {
                ipv4Packet = (Ipv4Packet) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || ipv4Packet == null) {
            return;
        }

        if (IPV4_IP_TO_IGNORE.equals(ipv4Packet.getSourceIpv4().getValue())) {
            return;
        }

        VlanId vlanId = null;
        if (ethernetPacket.getEthertype().equals(KnownEtherType.VlanTagged)) {
            vlanId = ethernetPacket.getHeader8021q().get(0).getVlan();
        }
        MacAddress sourceMac = ethernetPacket.getSourceMac();
        IpAddress ipAddress = new IpAddress(ipv4Packet.getSourceIpv4());

        Addresses addrs = createAddresses(sourceMac, vlanId, ipAddress, ethernetPacket.getEthertype());
        if (addrs == null) {
            return;
        }
        NodeConnectorRef ingress = rawPacket.getIngress();
        packetReceived(addrs, ingress.getValue());
    }

    @Override
    public void onIpv6PacketReceived(Ipv6PacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }

        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        Ipv6Packet ipv6Packet = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof Ipv6Packet) {
                ipv6Packet = (Ipv6Packet) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || ipv6Packet == null) {
            return;
        }
        if (IPV6_IP_TO_IGNORE.equals(ipv6Packet.getSourceIpv6().getValue())) {
            return;
        }

        VlanId vlanId = null;
        if (ethernetPacket.getEthertype().equals(KnownEtherType.VlanTagged)) {
            vlanId = ethernetPacket.getHeader8021q().get(0).getVlan();
        }
        MacAddress sourceMac = ethernetPacket.getSourceMac();
        IpAddress ipAddress = new IpAddress(ipv6Packet.getSourceIpv6());

        Addresses addrs = createAddresses(sourceMac, vlanId, ipAddress, ethernetPacket.getEthertype());
        if (addrs == null) {
            return;
        }
        NodeConnectorRef ingress = rawPacket.getIngress();
        packetReceived(addrs, ingress.getValue());
    }

    private Addresses createAddresses(MacAddress srcMacAddr, VlanId vlanId, IpAddress srcIpAddr, KnownEtherType ketype) {
        AddressesBuilder addrs = new AddressesBuilder();
        if (srcMacAddr == null || srcIpAddr == null) {
            return null;
        }
        //We really need to discuss this
        BigInteger id = BigInteger.valueOf(ketype.getIntValue()).abs().add(BigInteger.valueOf(srcMacAddr.hashCode()).abs().shiftLeft(16));
        addrs.setId(id);
        addrs.setKey(new AddressesKey(addrs.getId()));
        addrs.setVlan(vlanId);
        addrs.setIp(srcIpAddr);
        addrs.setMac(srcMacAddr);
        addrs.setLastSeen(new Date().getTime());
        return addrs.build();
    }
}
