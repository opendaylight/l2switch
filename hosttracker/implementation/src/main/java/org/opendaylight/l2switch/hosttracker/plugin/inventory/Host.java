/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All
 * rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.inventory;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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

public class Host {

    public static Host createHost(Node node) {
        HostNode hostNode = node.getAugmentation(HostNode.class);
        return new Host(hostNode.getId(), hostNode.getAddresses(), hostNode.getAttachmentPoints());
    }

    private List<AttachmentPointsBuilder> apbs;
    private HostNodeBuilder hostNodeBuilder;
    private NodeBuilder nodeBuilder;

    /**
     * Hosttracker's prefix for nodes stored on MD-SAL.
     */
    public static final String NODE_PREFIX = "host:";

    private Host() {
        apbs = new ArrayList<>();
        hostNodeBuilder = new HostNodeBuilder();
    }

    public Host(HostId hId, List<Addresses> addrs, List<AttachmentPoints> aps) throws InvalidParameterException {
        this();
        hostNodeBuilder.setAddresses(addrs);
        if (hId == null) {
            throw new InvalidParameterException("A host must have a HostId");
        }
        hostNodeBuilder.setId(hId);
        for (AttachmentPoints ap : aps) {
            apbs.add(new AttachmentPointsBuilder(ap));
        }
        nodeBuilder = createNodeBuilder(hostNodeBuilder, apbs);
    }

    public Host(Addresses addrs, NodeConnector nodeConnector) throws InvalidParameterException {
        this();
        List<Addresses> setAddrs = new ArrayList<>();
        if (addrs != null) {
            setAddrs.add(addrs);
        }
        hostNodeBuilder.setAddresses(setAddrs);
        HostId hId = createHostId(addrs);
        if (hId == null) {
            throw new InvalidParameterException("This host doesn't contain a valid MAC address to assign a valid HostId");
        }
        hostNodeBuilder.setId(hId);
        if (nodeConnector != null) {
            AttachmentPointsBuilder apb = Utilities.createAPsfromNodeConnector(nodeConnector);
            apb.setActive(Boolean.TRUE);
            apbs.add(apb);
        }
        nodeBuilder = createNodeBuilder(hostNodeBuilder, apbs);
    }

    public synchronized Node getHostNode() {
        List<AttachmentPoints> attachmentPoints = new ArrayList<>();
        for (AttachmentPointsBuilder apb : apbs) {
            attachmentPoints.add(apb.build());
        }
        hostNodeBuilder.setAttachmentPoints(attachmentPoints);
        return nodeBuilder.addAugmentation(HostNode.class, hostNodeBuilder.build()).build();
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
    private NodeBuilder createNodeBuilder(HostNodeBuilder hostNode, List<AttachmentPointsBuilder> apbs) {
        List<TerminationPoint> tps = new ArrayList<>();
        for (AttachmentPointsBuilder atb : apbs) {
            TerminationPoint tp = createTerminationPoint(hostNode, atb);
            tps.add(tp);
            atb.setCorrespondingTp(tp.getTpId());
        }
        NodeBuilder node = new NodeBuilder().setNodeId(createNodeId(hostNode))//
                .setTerminationPoint(tps);
        node.setKey(new NodeKey(node.getNodeId()));

        return node;
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
     * Creates a new TerminationPoint for this Host. The TerminationPoint will
     * have the form of TP + ":" + AttachmentPoint
     *
     * @param hn HostNodeBuilder containing an Id
     * @param atb AttachmentPointsBuilder containing a TpId
     * @return A new TerminationPoint with an unique TpId
     */
    private TerminationPoint createTerminationPoint(HostNodeBuilder hn, AttachmentPointsBuilder atb) {
        TerminationPoint tp = new TerminationPointBuilder()//
                .setTpId(new TpId(NODE_PREFIX + hn.getId().getValue() + ":" + atb.getTpId().getValue()))//
                .build();
        return tp;
    }

    /**
     * Creates a HostId based on the MAC values present in Addresses, if MAC is
     * null then returns null.
     *
     * @param addrs Address containing a MAC address.
     * @return A new HostId based on the MAC address present in addrs, null if
     * addrs is null or MAC is null.
     */
    public static HostId createHostId(Addresses addrs) {
        if (addrs != null && addrs.getMac() != null) {
            return new HostId(addrs.getMac().getValue());
        } else {
            return null;
        }
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
     * Creates links that have this Host's AttachmentPoints in the given
     * dstNode.
     *
     * @param dstNode Node that could have Host's AttachmentPoints.
     * @return A list of links containing a link from this Host's
     * TerminationPoint to the given dstNode and vice-versa.
     */
    public synchronized List<Link> createLinks(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node dstNode) {
        for (AttachmentPointsBuilder apb : apbs) {
            if (apb.isActive()) {
                for (NodeConnector nc : dstNode.getNodeConnector()) {
                    if (nc.getId().getValue().equals(apb.getTpId().getValue())) {
                        return Utilities.createLinks(nodeBuilder.getNodeId(),
                                apb.getCorrespondingTp(),
                                new NodeId(dstNode.getId().getValue()),
                                apb.getTpId());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Updates this Host with the given Host. Merge the newHostNodeBuilder
     * information into the hostToUpdate. Merges the list of addresses and the
     * list of attachment points into the current host.
     *
     *
     * @param newHost The new Host to merge information with.
     */
    public synchronized void mergeHostWith(Host newHost) {
        ListIterator<Addresses> oldLIAddrs;
        for (Addresses newAddrs : newHost.hostNodeBuilder.getAddresses()) {
            oldLIAddrs = this.hostNodeBuilder.getAddresses().listIterator();
            while (oldLIAddrs.hasNext()) {
                Addresses oldAddrs = oldLIAddrs.next();
                if (Compare.Addresses(oldAddrs, newAddrs)) {
                    oldLIAddrs.remove();
                    break;
                }
            }
            this.hostNodeBuilder.getAddresses().add(newAddrs);
        }

        ListIterator<AttachmentPointsBuilder> oldLIAPs;
        for (AttachmentPointsBuilder newAPs : newHost.apbs) {
            oldLIAPs = this.apbs.listIterator();
            while (oldLIAPs.hasNext()) {
                AttachmentPointsBuilder oldAPs = oldLIAPs.next();
                if (Compare.AttachmentPointsBuilder(oldAPs, newAPs)) {
                    oldLIAPs.remove();
                    break;
                }
            }
            this.apbs.add(newAPs);
        }
    }

    /**
     * Sets the given AttachmentPointsBuilder to inactive from the list of this
     * Host's AttachmentPoints.
     *
     * @param apb The AttachmentPointsBuilder to set inactive
     */
    public synchronized void removeAttachmentPoints(AttachmentPointsBuilder apb) {
        for (Iterator<AttachmentPointsBuilder> it = apbs.iterator(); it.hasNext();) {
            AttachmentPointsBuilder apbi = it.next();
            if (apbi.getKey().equals(apb.getKey())) {
                apbi.setActive(Boolean.FALSE);
            }
        }
    }

    /**
     * Sets the AttachmentPointsBuilder that have the given TerminationPoint to
     * inactive from the list of this Host's AttachmentPoints.
     *
     * @param tp The TerminationPoint to set inactive
     */
    public synchronized void removeTerminationPoint(TpId tp) {
        for (Iterator<AttachmentPointsBuilder> it = apbs.iterator(); it.hasNext();) {
            AttachmentPointsBuilder apbi = it.next();
            if (apbi.getCorrespondingTp().equals(tp)) {
                apbi.setActive(Boolean.FALSE);
            }
        }
    }

    /**
     * If a host does not have any AttachmentPoints active it means it is an
     * orphan.
     *
     * @return true if a host is an orphan, false otherwise.
     */
    public synchronized boolean isOrphan() {
        for (Iterator<AttachmentPointsBuilder> it = apbs.iterator(); it.hasNext();) {
            if (it.next().isActive()) {
                return false;
            }
        }
        return true;
    }
}
