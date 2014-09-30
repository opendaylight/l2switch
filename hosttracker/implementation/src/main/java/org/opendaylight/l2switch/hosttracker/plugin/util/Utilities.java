/**
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All
 * rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.util;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.host.tracker.rev140624.host.AttachmentPointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Utilities {

    public static List<Link> createLinks(NodeId srcNId, TpId srcTpId, NodeId dstNId, TpId dstTpId) {
        List<Link> links = new ArrayList();
        LinkBuilder srcdst = new LinkBuilder()//
                .setSource(new SourceBuilder()//
                        .setSourceNode(srcNId)//
                        .setSourceTp(srcTpId).build())//
                .setDestination(new DestinationBuilder()//
                        .setDestNode(dstNId)
                        .setDestTp(dstTpId).build())//
                .setLinkId(new LinkId(srcTpId.getValue() + "/" + dstTpId.getValue()));
        srcdst.setKey(new LinkKey(srcdst.getLinkId()));
        LinkBuilder dstsrc = new LinkBuilder()//
                .setSource(new SourceBuilder()//
                        .setSourceNode(dstNId)//
                        .setSourceTp(dstTpId).build())//
                .setDestination(new DestinationBuilder()//
                        .setDestNode(srcNId)
                        .setDestTp(srcTpId).build())//
                .setLinkId(new LinkId(dstTpId.getValue() + "/" + srcTpId.getValue()));
        dstsrc.setKey(new LinkKey(dstsrc.getLinkId()));
        links.add(dstsrc.build());
        links.add(srcdst.build());
        return links;
    }

    public static InstanceIdentifier<Node> buildNodeIID(NodeKey nk, String topologyId) {
        InstanceIdentifier<Node> nIID = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Node.class, nk).build();
        return nIID;
    }

    public static InstanceIdentifier<Link> buildLinkIID(LinkKey lk, String topologyId) {
        InstanceIdentifier<Link> lIID = InstanceIdentifier.builder(NetworkTopology.class)//
                .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))//
                .child(Link.class, lk).build();
        return lIID;
    }

    public static AttachmentPointsBuilder createAPsfromNodeConnector(NodeConnector nc) {
        TpId tpId = new TpId(nc.getId().getValue());
        return createAPsfromTP(tpId);
    }

    public static AttachmentPointsBuilder createAPsfromTP(TpId tpId) {
        AttachmentPointsBuilder at = new AttachmentPointsBuilder()//
                .setTpId(tpId)//
                .setKey(new AttachmentPointsKey(tpId));
        return at;
    }
}
