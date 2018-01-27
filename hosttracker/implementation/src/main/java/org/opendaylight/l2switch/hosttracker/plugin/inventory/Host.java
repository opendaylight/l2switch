/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.inventory;

import java.security.InvalidParameterException;
import java.util.ArrayList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Host {
    private static final Logger LOG = LoggerFactory.getLogger(Host.class);

    /**
     * Hosttracker's prefix for nodes stored on MD-SAL.
     */
    public static final String NODE_PREFIX = "host:";

    private final List<AttachmentPointsBuilder> attachmentPointsBuilders = new ArrayList<>();
    private final HostNodeBuilder hostNodeBuilder = new HostNodeBuilder();
    private final NodeBuilder nodeBuilder;

    public static Host createHost(Node node) {
        HostNode hostNode = node.getAugmentation(HostNode.class);
        return new Host(hostNode.getId(), hostNode.getAddresses(), hostNode.getAttachmentPoints());
    }

    public Host(HostId hostId, List<Addresses> addrs, List<AttachmentPoints> aps) throws InvalidParameterException {
        hostNodeBuilder.setAddresses(addrs);
        if (hostId == null) {
            throw new InvalidParameterException("A host must have a HostId");
        }
        hostNodeBuilder.setId(hostId);
        for (AttachmentPoints ap : aps) {
            attachmentPointsBuilders.add(new AttachmentPointsBuilder(ap));
        }
        nodeBuilder = createNodeBuilder(hostNodeBuilder, attachmentPointsBuilders);
    }

    public Host(Addresses addrs, NodeConnector nodeConnector) throws InvalidParameterException {
        List<Addresses> setAddrs = new ArrayList<>();
        if (addrs != null) {
            setAddrs.add(addrs);
        }
        hostNodeBuilder.setAddresses(setAddrs);
        HostId hostId = createHostId(addrs);
        if (hostId == null) {
            throw new InvalidParameterException(
                    "This host doesn't contain a valid MAC address to assign a valid HostId");
        }
        hostNodeBuilder.setId(hostId);
        if (nodeConnector != null) {
            AttachmentPointsBuilder apb = Utilities.createAPsfromNodeConnector(nodeConnector);
            apb.setActive(Boolean.TRUE);
            attachmentPointsBuilders.add(apb);
        }
        nodeBuilder = createNodeBuilder(hostNodeBuilder, attachmentPointsBuilders);
    }

    public synchronized Node getHostNode() {
        List<AttachmentPoints> attachmentPoints = new ArrayList<>();
        for (AttachmentPointsBuilder apb : attachmentPointsBuilders) {
            attachmentPoints.add(apb.build());
        }
        hostNodeBuilder.setAttachmentPoints(attachmentPoints);
        return nodeBuilder.addAugmentation(HostNode.class, hostNodeBuilder.build()).build();
    }

    /**
     * Creates a NodeBuilder based on the given HostNodeBuilder.
     *
     * @param hostNode The HostNodeBuilder where the AttachmentPoints and Id are.
     * @return A NodeBuilder with the same Id of HostNodeBuilder and a list of TerminationPoint corresponding to
     *     each HostNodeBuilder's AttachmentPoints.
     */
    private NodeBuilder createNodeBuilder(HostNodeBuilder hostNode, List<AttachmentPointsBuilder> apbs) {
        List<TerminationPoint> tps = new ArrayList<>();
        for (AttachmentPointsBuilder apb : apbs) {
            TerminationPoint tp = createTerminationPoint(hostNode);
            tps.add(tp);
            apb.setCorrespondingTp(tp.getTpId());
        }
        NodeBuilder node = new NodeBuilder().setNodeId(createNodeId(hostNode))
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
     * Creates a new TerminationPoint for this Host.
     *
     * @param hn HostNodeBuilder containing an Id
     * @return A new TerminationPoint with an unique TpId
     */
    private TerminationPoint createTerminationPoint(HostNodeBuilder hn) {
        TerminationPoint tp = new TerminationPointBuilder()
                .setTpId(new TpId(NODE_PREFIX + hn.getId().getValue()))
                .build();
        return tp;
    }

    /**
     * Creates a HostId based on the MAC values present in Addresses, if MAC is null then returns null.
     *
     * @param addrs Address containing a MAC address.
     * @return A new HostId based on the MAC address present in addrs, null ifcaddrs is null or MAC is null.
     */
    public static HostId createHostId(Addresses addrs) {
        if (addrs != null && addrs.getMac() != null) {
            return new HostId(addrs.getMac().getValue());
        } else {
            return null;
        }
    }

    /**
     * Returns this HostId.
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
     * @return A list of links containing a link from this Host's TerminationPoint to the given dstNode and vice-versa.
     */
    public synchronized List<Link> createLinks(
            org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node dstNode) {
        for (AttachmentPointsBuilder apb : attachmentPointsBuilders) {
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
                if (Compare.addresses(oldAddrs, newAddrs)) {
                    oldLIAddrs.remove();
                    break;
                }
            }
            this.hostNodeBuilder.getAddresses().add(newAddrs);
        }

        ListIterator<AttachmentPointsBuilder> oldLIAPs;
        for (AttachmentPointsBuilder newAPs : newHost.attachmentPointsBuilders) {
            oldLIAPs = this.attachmentPointsBuilders.listIterator();
            while (oldLIAPs.hasNext()) {
                AttachmentPointsBuilder oldAPs = oldLIAPs.next();
                if (Compare.attachmentPointsBuilder(oldAPs, newAPs)) {
                    oldLIAPs.remove();
                    break;
                }
            }
            this.attachmentPointsBuilders.add(newAPs);
        }
    }

    /**
     * Sets the given AttachmentPointsBuilder to inactive from the list of this
     * Host's AttachmentPoints.
     *
     * @param apb The AttachmentPointsBuilder to set inactive
     */
    public synchronized void removeAttachmentPoints(AttachmentPointsBuilder apb) {
        LOG.debug("Setting attachment points {} to inactive state", apb);
        for (AttachmentPointsBuilder apbi : attachmentPointsBuilders) {
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
        LOG.debug("Setting termination point {} to inactive state", tp);
        for (AttachmentPointsBuilder apbi : attachmentPointsBuilders) {
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
        for (AttachmentPointsBuilder attachmentPointsBuilder : attachmentPointsBuilders) {
            if (attachmentPointsBuilder.isActive()) {
                return false;
            }
        }
        return true;
    }
}
