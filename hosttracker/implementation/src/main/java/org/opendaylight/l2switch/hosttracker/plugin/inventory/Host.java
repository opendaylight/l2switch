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

import org.opendaylight.l2switch.hosttracker.plugin.util.Compare;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;
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

    public static final String NODE_PREFIX = "host:";
    private static AtomicLong lastHostId = new AtomicLong();
    private AtomicLong lastTPId = new AtomicLong(0);

    public Host(Addresses addrs, NodeConnector nodeConnector) {
        this.switchToHostConnectors = new HashMap<>();
        this.hostNodeBuilder = createHostNodeBuilder(addrs, nodeConnector);
        this.nodeBuilder = createNodeBuilder(this.hostNodeBuilder);
        updateSwitchToHost();
    }

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
     * Removes the list of attachment points from this Host and returns a list
     * of Links that should be removed on MD-SAL.
     *
     * @param latps
     * @return the list of links that need to be removed.
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

    public synchronized Node getHostNode() {
        return nodeBuilder.addAugmentation(HostNode.class, hostNodeBuilder.build()).build();
    }

    public synchronized HostId getId() {
        return hostNodeBuilder.getId();
    }

    public synchronized void updateHostWith(Host newHost) {
        updateHostNodeBuilder(this.hostNodeBuilder, newHost.hostNodeBuilder);
        updateSwitchToHost();
    }

    private static void updateHostNodeBuilder(HostNodeBuilder hostToUpdate, HostNodeBuilder newInformation) {
        ListIterator<Addresses> oldLIAddrs;
        for (Addresses newAddrs : newInformation.getAddresses()) {
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
        for (AttachmentPoints newAPs : newInformation.getAttachmentPoints()) {
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
            attachmentPoints.add(new AttachmentPointsBuilder()//
                    .setTpId(new TpId(nodeConnector.getId().getValue())).build());
        }
        host.setAttachmentPoints(attachmentPoints);
        return host;
    }

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

    private TerminationPoint createTerminationPoint(HostNodeBuilder hn) {
        TerminationPoint tp = new TerminationPointBuilder()//
                .setTpId(new TpId(NODE_PREFIX + hn.getId().getValue() + ":" + lastTPId.getAndIncrement()))//
                .build();
        return tp;
    }

    private static NodeId createNodeId(HostNodeBuilder host) {
        return new NodeId(NODE_PREFIX + host.getId().getValue());
    }

    public synchronized static HostId createHostId(Addresses addrs) {
        if (addrs != null && addrs.getMac() != null) {
            return new HostId(addrs.getMac().getValue());
        } else {
            return null;
        }
    }

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
