/*
 * Copyright (c) 2014 André Martins, Colin Dixon, Evan Zeller and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.eclipse.jdt.annotation.NonNull;
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
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.ExactDataObjectStep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class Utilities {
    private Utilities() {
        // Hidden on purpose
    }

    public static List<Link> createLinks(NodeId srcNId, TpId srcTpId, NodeId dstNId, TpId dstTpId) {
        List<Link> links = new ArrayList<>();
        LinkBuilder srcdst = new LinkBuilder()
            .setSource(new SourceBuilder()
                .setSourceNode(srcNId)
                .setSourceTp(srcTpId).build())
            .setDestination(new DestinationBuilder()
                .setDestNode(dstNId).setDestTp(dstTpId).build())
            .setLinkId(new LinkId(srcTpId.getValue() + "/" + dstTpId.getValue()));
        srcdst.withKey(new LinkKey(srcdst.getLinkId()));
        LinkBuilder dstsrc = new LinkBuilder()
            .setSource(new SourceBuilder().setSourceNode(dstNId).setSourceTp(dstTpId).build())
            .setDestination(new DestinationBuilder().setDestNode(srcNId).setDestTp(srcTpId).build())
            .setLinkId(new LinkId(dstTpId.getValue() + "/" + srcTpId.getValue()));
        dstsrc.withKey(new LinkKey(dstsrc.getLinkId()));
        links.add(dstsrc.build());
        links.add(srcdst.build());
        return links;
    }

    public static DataObjectIdentifier<Node> buildNodeIID(NodeKey nk, String topologyId) {
        return DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
            .child(Node.class, nk).build();
    }

    public static InstanceIdentifier<Link> buildLinkIID(LinkKey lk, String topologyId) {
        return InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(topologyId)))
            .child(Link.class, lk).build();
    }

    public static AttachmentPointsBuilder createAPsfromNodeConnector(NodeConnector nc) {
        return createAPsfromTP(new TpId(nc.getId().getValue()));
    }

    public static AttachmentPointsBuilder createAPsfromTP(TpId tpId) {
        return new AttachmentPointsBuilder().setTpId(tpId).withKey(new AttachmentPointsKey(tpId));
    }

    public static <I extends DataObject>  DataObjectIdentifier<I> firstIdentifierOfN(  DataObjectIdentifier<?> identifier, Class<I> type) {
        List <ExactDataObjectStep<?>> collected = new ArrayList<>();
        for (ExactDataObjectStep<?> step : identifier.steps()) {
            collected.add(step);
            if (type.equals(step.type())) {
                @SuppressWarnings("unchecked")
                DataObjectIdentifier<I> result =  (DataObjectIdentifier<I>)  DataObjectIdentifier.ofUnsafeSteps(List.copyOf(collected));
                return result;
            }
        }
        return null;
    }
    public static <T extends DataObject> DataObjectIdentifier<T> firstIdentifierOf(@NonNull final DataObjectIdentifier<?> sourceId,
                                                                                   @NonNull final Class<T> targetType) {
        final List<ExactDataObjectStep<?>> newSteps = new ArrayList<>();
        boolean found = false;
        for (final ExactDataObjectStep<?> step : sourceId.steps()) {
            newSteps.add(step);
            if (targetType.equals(step.type())) {
                found = true;
                break; // Found the target, stop iterating.
            }
        }
        if (!found) {
            return null;
        }
        @SuppressWarnings("unchecked")
        final DataObjectIdentifier<T> result = (DataObjectIdentifier<T>) DataObjectIdentifier.ofUnsafeSteps(
                ImmutableList.copyOf(newSteps));
        return result;
    }
}
