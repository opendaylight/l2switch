/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;

class NetworkGraphImplTest {
    private final NetworkGraphService networkGraphImpl = new NetworkGraphImpl();

    @Test
    void testClear() {
        // FIXME: test effects
        networkGraphImpl.clear();
    }

    @Test
    void testGetLinksInMst_NullGraph() {
        var mstLinks = networkGraphImpl.getLinksInMst();
        assertEquals(0, mstLinks.size());
    }

    @Test
    void testAllLinks_NullGraph() {
        var allLinks = networkGraphImpl.getAllLinks();
        assertEquals(0, allLinks.size());
    }

    @Test
    void testAddLinks_NullInput() {
        networkGraphImpl.clear();
        networkGraphImpl.addLinks(null);
        var mstLinks = networkGraphImpl.getLinksInMst();
        assertEquals(0, mstLinks.size());
        var allLinks = networkGraphImpl.getAllLinks();
        assertEquals(0, allLinks.size());
    }

    @Test
    void testAddLinks_ValidInput() {
        final var link1 = newLink("openflow:1", "openflow:1", "openflow:1", "openflow:2", "openflow:2");
        final var link2 = newLink("openflow:2", "openflow:2", "openflow:2", "openflow:3", "openflow:3");

        networkGraphImpl.addLinks(List.of(link1, link2));
        var mstLinks = networkGraphImpl.getLinksInMst();
        assertEquals(2, mstLinks.size());
        var allLinks = networkGraphImpl.getAllLinks();
        assertEquals(2, allLinks.size());
    }

    @Test
    void testRemoveLinks_NullInput() {
        assertThrows(NullPointerException.class, () -> networkGraphImpl.removeLinks(null));
    }

    @Test
    void testRemoveLinks_ValidInput() {
        final var link1 = newLink("openflow:1", "openflow:1", "openflow:1", "openflow:2", "openflow:2");
        final var link2 = newLink("openflow:2", "openflow:2", "openflow:2", "openflow:3", "openflow:3");
        final var link3 = newLink("openflow:3", "openflow:3", "openflow:3", "openflow:4", "openflow:4");

        networkGraphImpl.addLinks(List.of(link1, link2, link3));
        var mstLinks = networkGraphImpl.getLinksInMst();
        assertEquals(3, mstLinks.size());
        var allLinks = networkGraphImpl.getAllLinks();
        assertEquals(3, allLinks.size());

        networkGraphImpl.removeLinks(List.of(link1));
        allLinks = networkGraphImpl.getAllLinks();
        assertEquals(2, allLinks.size());
    }

    @Test
    void testMstRemovesLoops() {
        final var link1 = newLink("openflow:1:1", "openflow:1", "openflow:1:1", "openflow:2", "openflow:2:1");
        final var link2 = newLink("openflow:2:1", "openflow:2", "openflow:2:1", "openflow:1", "openflow:1:1");
        final var link3 = newLink("openflow:2:2", "openflow:2", "openflow:2:2", "openflow:3", "openflow:3:1");
        final var link4 = newLink("openflow:3:1", "openflow:3", "openflow:3:1", "openflow:2", "openflow:2:2");
        final var link5 = newLink("openflow:3:2", "openflow:3", "openflow:3:2", "openflow:1", "openflow:1:2");
        final var link6 = newLink("openflow:1:2", "openflow:1", "openflow:1:2", "openflow:3", "openflow:3:2");
        // parallel link betn nodes 1 & 2
        final var link7 = newLink("openflow:1:3", "openflow:1", "openflow:1:3", "openflow:2", "openflow:2:3");
        final var link8 = newLink("openflow:2:3", "openflow:2", "openflow:2:3", "openflow:1", "openflow:1:3");

        networkGraphImpl.addLinks(List.of(link1, link2, link3, link4, link5, link6, link7, link8));
        var mstLinks = networkGraphImpl.getLinksInMst();
        assertEquals(2, mstLinks.size());
        var allLinks = networkGraphImpl.getAllLinks();
        assertEquals(4, allLinks.size());
    }

    @NonNullByDefault
    private static Link newLink(final String linkId, final String srcNode, final String srcTp, final String dstNode,
            final String dstTp) {
        return new LinkBuilder()
            .setLinkId(new LinkId(linkId))
            .setSource(new SourceBuilder()
                .setSourceNode(new NodeId(srcNode))
                .setSourceTp(new TpId(srcTp))
                .build())
            .setDestination(new DestinationBuilder()
                .setDestNode(new NodeId(dstNode))
                .setDestTp(new TpId(dstTp))
                .build())
            .build();
    }
}
