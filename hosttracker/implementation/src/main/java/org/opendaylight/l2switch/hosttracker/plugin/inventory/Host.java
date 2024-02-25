/*
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.inventory;

import static java.util.Objects.requireNonNull;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.opendaylight.l2switch.hosttracker.plugin.util.Compare;
import org.opendaylight.l2switch.hosttracker.plugin.util.Utilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.AddressesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.HostNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
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
        HostNode hostNode = node.augmentation(HostNode.class);
        return new Host(hostNode.getId(), hostNode.getAddresses(), hostNode.nonnullAttachmentPoints());
    }

    public Host(HostId hostId, Map<AddressesKey, Addresses> addrs, Map<AttachmentPointsKey, AttachmentPoints> aps) {
        requireNonNull(hostId);
        hostNodeBuilder.setAddresses(addrs);
        hostNodeBuilder.setId(hostId);
        for (AttachmentPoints ap : aps.values()) {
            attachmentPointsBuilders.add(new AttachmentPointsBuilder(ap));
        }
        nodeBuilder = createNodeBuilder(hostNodeBuilder, attachmentPointsBuilders);
    }

    public Host(Addresses addrs, NodeConnector nodeConnector) throws InvalidParameterException {
        requireNonNull(addrs, "This host doesn't have a valid address");
        hostNodeBuilder.setAddresses(BindingMap.of(addrs));

        HostId hostId = requireNonNull(createHostId(addrs),
            "This host doesn't contain a valid MAC address to assign a valid HostId");
        hostNodeBuilder.setId(hostId);
        if (nodeConnector != null) {
            AttachmentPointsBuilder apb = Utilities.createAPsfromNodeConnector(nodeConnector);
            apb.setActive(Boolean.TRUE);
            attachmentPointsBuilders.add(apb);
        }
        nodeBuilder = createNodeBuilder(hostNodeBuilder, attachmentPointsBuilders);
    }

    public synchronized Node getHostNode() {
        Map<AttachmentPointsKey, AttachmentPoints> attachmentPointsMap =
                new HashMap<AttachmentPointsKey, AttachmentPoints>();
        for (AttachmentPointsBuilder apb : attachmentPointsBuilders) {
            AttachmentPoints builtAttachmentPoints = apb.build();
            attachmentPointsMap.put(builtAttachmentPoints.key(), builtAttachmentPoints);
        }
        hostNodeBuilder.setAttachmentPoints(attachmentPointsMap);
        return nodeBuilder.addAugmentation(hostNodeBuilder.build()).build();
    }

    /**
     * Creates a NodeBuilder based on the given HostNodeBuilder.
     *
     * @param hostNode The HostNodeBuilder where the AttachmentPoints and Id are.
     * @return A NodeBuilder with the same Id of HostNodeBuilder and a list of TerminationPoint corresponding to
     *     each HostNodeBuilder's AttachmentPoints.
     */
    private static NodeBuilder createNodeBuilder(HostNodeBuilder hostNode, List<AttachmentPointsBuilder> apbs) {
        Map<TerminationPointKey, TerminationPoint> tpsMap =
                new HashMap<TerminationPointKey, TerminationPoint>();
        for (AttachmentPointsBuilder apb : apbs) {
            TerminationPoint tp = createTerminationPoint(hostNode);
            tpsMap.put(tp.key(), tp);
            apb.setCorrespondingTp(tp.getTpId());
        }
        NodeBuilder node = new NodeBuilder().setNodeId(createNodeId(hostNode)).setTerminationPoint(tpsMap);
        node.withKey(new NodeKey(node.getNodeId()));

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
    private static TerminationPoint createTerminationPoint(HostNodeBuilder hn) {
        return new TerminationPointBuilder()
            .setTpId(new TpId(NODE_PREFIX + hn.getId().getValue()))
            .build();
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
            if (apb.getActive()) {
                for (NodeConnector nc : dstNode.nonnullNodeConnector().values()) {
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
     * Updates this Host with the given Host. Merge the newHostNodeBuilder information into the hostToUpdate. Merges
     * the list of addresses and the list of attachment points into the current host.
     *
     * @param newHost The new Host to merge information with.
     */
    public synchronized void mergeHostWith(Host newHost) {
        // TODO: this algorithm is rather naive and based on former assumption of List. Now we have a Map, can we
        //       perhaps do better?
        Map<AddressesKey, Addresses> newAddresses = newHost.hostNodeBuilder.getAddresses();
        if (newAddresses == null) {
            newAddresses = Map.of();
        }

        for (Addresses newAddrs : newAddresses.values()) {
            Map<AddressesKey, Addresses> oldAddresses = hostNodeBuilder.getAddresses();
            oldAddresses = oldAddresses == null ? Map.of() : new HashMap<>(oldAddresses);

            final var oldLIAddrs = oldAddresses.values().iterator();
            while (oldLIAddrs.hasNext()) {
                Addresses oldAddrs = oldLIAddrs.next();
                if (Compare.addresses(oldAddrs, newAddrs)) {
                    oldLIAddrs.remove();
                    break;
                }
            }

            Map<AddressesKey, Addresses> newAddrsMap = new HashMap<>();
            for (AddressesKey key : oldAddresses.keySet()) {
                newAddrsMap.put(key, newAddrs);
            }
            this.hostNodeBuilder.setAddresses(newAddrsMap);
        }

        for (AttachmentPointsBuilder newAPs : newHost.attachmentPointsBuilders) {
            Iterator<AttachmentPointsBuilder> oldLIAPs = this.attachmentPointsBuilders.iterator();
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
            if (apbi.key().equals(apb.key())) {
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
            if (attachmentPointsBuilder.getActive()) {
                return false;
            }
        }
        return true;
    }
}
