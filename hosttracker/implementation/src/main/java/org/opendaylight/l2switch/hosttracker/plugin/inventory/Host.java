/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All
 * rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.l2switch.hosttracker.plugin.util.Compare;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public class Host {

    private static final Logger log = LoggerFactory.getLogger(Host.class);
    /**
     * HostNodeBuilder stores a mutable version of the HostNodeBuilder which can
     * be turned into immutable ones on demand.
     *
     * @see #getHostNode()
     */
    private HostNodeBuilder hostNodeBuilder;
    /**
     * NodeBuilder stores a mutable version of the NodeBuilder which can be
     * turned into immutable ones on demand.
     *
     * @see #getHostNode()
     */
    private NodeBuilder nodeBuilder;
    /**
     * In essence this stores the host-switch links with the AttachmentPoints
     * being the switch-side and InternaTP being the host side.
     */
    private HashMap<AttachmentPoints, InternalTP> switchToHostConnectors;
    /**
     * Hosttracker's prefix for nodes stored on MD-SAL.
     */
    public static final String NODE_PREFIX = "host:";

    private static AtomicLong lastHostId = new AtomicLong();
    private AtomicLong lastTPId = new AtomicLong(0);

    /**
     * Creates a host with the given Addresses and the attached nodeConnector.
     *
     * @param addrs Addresses that belong to this host.
     * @param nodeConnector The NodeConnector where the addresses where listen.
     */
    public Host(Addresses addrs, NodeConnector nodeConnector) {
        this.switchToHostConnectors = new HashMap<>();
        this.hostNodeBuilder = createHostNodeBuilder(addrs, nodeConnector);
        this.nodeBuilder = createNodeBuilder(this.hostNodeBuilder);
        updateSwitchToHost();
    }

    /**
     * Updates the switchToHostConnectors hashmap. Starts by checking if every
     * attachment point have a termination point, if it does it sets as active.
     */
    private void updateSwitchToHost() {
        List<TerminationPoint> terminationPoints = this.nodeBuilder.getTerminationPoint();
        List<AttachmentPoints> attachmentPoints = this.hostNodeBuilder.getAttachmentPoints();
        for (AttachmentPoints at : attachmentPoints) {
            if (switchToHostConnectors.containsKey(at)) {
                log.trace("host " + getId() + " HashMap contains" + at);
                if (!terminationPoints.contains(switchToHostConnectors.get(at).getTp())) {
                    log.trace("terminationPoint DOES NOT contains " + switchToHostConnectors.get(at).getTp());
                    TerminationPoint tp = createTerminationPoint(hostNodeBuilder);
                    terminationPoints.add(tp);
                    switchToHostConnectors.put(at, new InternalTP(tp));
                }
                switchToHostConnectors.get(at).setActive(true);
            } else {
                log.trace("host " + getId() + " HashMap DOES NOT contains " + at);
                TerminationPoint tp = createTerminationPoint(hostNodeBuilder);
                terminationPoints.add(tp);
                switchToHostConnectors.put(at, new InternalTP(tp));
            }
        }
    }

    /**
     * Removes the given AttachmentPoints from this Host and returns a list of
     * Links that should be removed on MD-SAL.
     *
     * @param atps The AttachmentPoints to remove.
     * @param nId The NodeId of those AttachementPoints.
     * @return A list of links that should be removed.
     */
    public synchronized List<Link> removeAttachmentPoints(AttachmentPoints atps, org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId nId) {
        List<Link> linksToRemove = new ArrayList();
        if (switchToHostConnectors.containsKey(atps)) {
            switchToHostConnectors.get(atps).setActive(false);
            hostNodeBuilder.getAttachmentPoints().remove(atps);
            List<Link> links = Utilities.createLinks(nodeBuilder.getNodeId(),
                    switchToHostConnectors.get(atps).getTp().getTpId(),
                    new NodeId(nId.getValue()),
                    atps.getTpId());
            linksToRemove.addAll(links);
        }
        return linksToRemove;
    }

    /**
     * Gets an immutable Node with the HostNode augmentation of this Host.
     *
     * @return An immutable Node with the HostNode augmentation of this Host.
     * @see HostNode
     */
    public synchronized Node getHostNode() {
        return nodeBuilder.addAugmentation(HostNode.class, hostNodeBuilder.build()).build();
    }

    /**
     * Returns this HostId
     *
     * @return this HostId.
     */
    public synchronized HostId getId() {
        return hostNodeBuilder.getId();
    }

    /**
     * Updates this Host with the given Host.
     *
     * @param newHost The new Host to merge information with.
     */
    public synchronized void mergeHostWith(Host newHost) {
        updateHostNodeBuilder(this.hostNodeBuilder, newHost.hostNodeBuilder);
        updateSwitchToHost();
    }

    /**
     * Merge the newHostNodeBuilder information into the hostToUpdate. Merges
     * the list of addresses and the list of attachment points into the current
     * host.
     *
     * @param hostToUpdate HostNode with to merge the information.
     * @param newHostNodeBuilder HostNode with new information.
     */
    private static void updateHostNodeBuilder(HostNodeBuilder hostToUpdate, HostNodeBuilder newHostNodeBuilder) {
        ListIterator<Addresses> oldLIAddrs;
        for (Addresses newAddrs : newHostNodeBuilder.getAddresses()) {
            oldLIAddrs = hostToUpdate.getAddresses().listIterator();
            while (oldLIAddrs.hasNext()) {
                Addresses oldAddrs = oldLIAddrs.next();
                if (Compare.Addresses(oldAddrs, newAddrs)) {
                    oldLIAddrs.remove();
                    break;
                }
            }
            hostToUpdate.getAddresses().add(newAddrs);
        }

        ListIterator<AttachmentPoints> oldLIAPs;
        for (AttachmentPoints newAPs : newHostNodeBuilder.getAttachmentPoints()) {
            oldLIAPs = hostToUpdate.getAttachmentPoints().listIterator();
            while (oldLIAPs.hasNext()) {
                AttachmentPoints oldAPs = oldLIAPs.next();
                if (Compare.AttachmentPoints(oldAPs, newAPs)) {
                    oldLIAPs.remove();
                    break;
                }
            }
            hostToUpdate.getAttachmentPoints().add(newAPs);
        }
    }

    /**
     * Creates a HostNodeBuilder based on the given Addresses and NodeConnector.
     *
     * @param addrs Addresses that belong to this host.
     * @param nodeConnector The NodeConnector where the addresses where listen.
     * @return A HostNodeBuilder with an Id, AttachmentPoints and Addresses.
     */
    private HostNodeBuilder createHostNodeBuilder(Addresses addrs, NodeConnector nodeConnector) {
        HostNodeBuilder host = new HostNodeBuilder();
        List<Addresses> setAddrs = new ArrayList<>();
        if (addrs != null) {
            setAddrs.add(addrs);
        }
        host.setAddresses(setAddrs);
        HostId hId = createHostId(addrs);
        if (hId == null) {
            hId = new HostId(Long.toString(Host.lastHostId.getAndIncrement()));
        }
        host.setId(hId);
        List<AttachmentPoints> attachmentPoints = new ArrayList<>();
        if (nodeConnector != null) {
            attachmentPoints.add(Utilities.createAPsfromNodeConnector(nodeConnector));
        }
        host.setAttachmentPoints(attachmentPoints);
        return host;
    }

    /**
     * Creates a NodeBuilder based on the given HostNodeBuilder.
     *
     * @param hostNode The HostNodeBuilder where the AttachmentPoints and Id
     * are.
     * @return A NodeBuilder with the same Id of HostNodeBuilder and a list of
     * TerminationPoint corresponding to each HostNodeBuilder's
     * AttachmentPoints.
     */
    private NodeBuilder createNodeBuilder(HostNodeBuilder hostNode) {
        List<TerminationPoint> tps = new ArrayList<>();
        for (AttachmentPoints at : hostNode.getAttachmentPoints()) {
            TerminationPoint tp = createTerminationPoint(hostNode);
            switchToHostConnectors.put(at, new InternalTP(tp));
            tps.add(tp);
        }
        NodeBuilder node = new NodeBuilder().setNodeId(createNodeId(hostNode))//
                .setTerminationPoint(tps);
        node.setKey(new NodeKey(node.getNodeId()));

        return node;
    }

    /**
     * Creates a new TerminationPoint for this Host.
     *
     * @param hn HostNodeBuilder containing an Id.
     * @return A new TerminationPoint with an unique TpId.
     */
    private TerminationPoint createTerminationPoint(HostNodeBuilder hn) {
        TerminationPoint tp = new TerminationPointBuilder()//
                .setTpId(new TpId(NODE_PREFIX + hn.getId().getValue() + ":" + lastTPId.getAndIncrement()))//
                .build();
        return tp;
    }

    /**
     * Creates a NodeId based on the Id stored on the given HostNodeBuilder
     * adding the NODE_PREFIX.
     *
     * @param host HostNodeBuilder that contains an Id
     * @return A new NodeId.
     */
    private static NodeId createNodeId(HostNodeBuilder host) {
        return new NodeId(NODE_PREFIX + host.getId().getValue());
    }

    /**
     * Creates a HostId based on the MAC values present in Addresses, if MAC is
     * null then returns null.
     *
     * @param addrs Address containing a MAC address.
     * @return A new HostId based on the MAC address present in addrs, null if
     * addrs is null or MAC is null.
     */
    public synchronized static HostId createHostId(Addresses addrs) {
        if (addrs != null && addrs.getMac() != null) {
            return new HostId(addrs.getMac().getValue());
        } else {
            return null;
        }
    }

    /**
     * Creates a HostId based on the MAC values present in Addresses, if MAC is
     * null then returns null.
     *
     * @param addrs Address containing a MAC address.
     * @return A new HostId based on the MAC address present in addrs, null if
     * addrs is null or MAC is null.
     */
    /**
     *
     * @param nodeId
     * @return
     */
    public synchronized static HostId createHostId(NodeId nodeId) {
        if (nodeId.getValue().startsWith(NODE_PREFIX)) {
            return new HostId(nodeId.getValue());
        } else {
            return null;
        }
    }

    /**
     * Creates links that have this Host's AttachmentPoints in the given
     * dstNode.
     *
     * @param dstNode Node that could have Host's AttachmentPoints.
     * @return A list of links containing a link from this Host's
     * TerminationPoint to the given dstNode and vice-versa.
     */
    public synchronized List<Link> createLinks(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node dstNode) {
        for (Map.Entry<AttachmentPoints, InternalTP> next : switchToHostConnectors.entrySet()) {
            if (next.getValue().isActive()) {
                for (NodeConnector nc : dstNode.getNodeConnector()) {
                    if (nc.getId().getValue().equals(next.getKey().getTpId().getValue())) {
                        return Utilities.createLinks(nodeBuilder.getNodeId(),
                                next.getValue().getTp().getTpId(),
                                new NodeId(dstNode.getId().getValue()),
                                next.getKey().getTpId());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the list of TerminationPoint from this host.
     *
     * @return the list of TerminationPoint from this host.
     */
    public synchronized List<TerminationPoint> getTerminationPoints() {
        return this.nodeBuilder.getTerminationPoint();
    }

    /**
     * If a host does not have any AttachmentPoints active it means it is an
     * orphan.
     *
     * @return true if a host is an orphan, false otherwise.
     */
    public synchronized boolean isOrphan() {
        for (Map.Entry<AttachmentPoints, InternalTP> next : switchToHostConnectors.entrySet()) {
            if (next.getValue().isActive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Internal class that contains a TerminationPoint and if it is or is not
     * active. Being active means that have an online AttachmentPoint that links
     * to this TerminationPoint.
     */
    private class InternalTP {

        public InternalTP(TerminationPoint tp) {
            this.tp = tp;
            this.active = true;
        }

        public InternalTP(TerminationPoint tp, boolean active) {
            this.tp = tp;
            this.active = active;
        }

        public TerminationPoint getTp() {
            return this.tp;
        }

        public boolean isActive() {
            return this.active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
        TerminationPoint tp;
        boolean active;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.hostNodeBuilder.getId());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Host other = (Host) obj;
        if (!Objects.equals(this.hostNodeBuilder.getId(), other.hostNodeBuilder.getId())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Host{" + "hostNodeBuilder=" + hostNodeBuilder + ", nodeBuilder=" + nodeBuilder + ", att=" + switchToHostConnectors + '}';
    }

}
