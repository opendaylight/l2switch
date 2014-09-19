/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.hosttracker.plugin.util;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UtilitiesTest {

  @Test
  public void testCreateLinks() throws Exception {
    final String SRC_NODE_ID = "src";
    final String SRC_TP_ID = "src-tp";
    final String DST_NODE_ID = "dst";
    final String DST_TP_ID = "dst-tp";
    List<Link> linkList = Utilities.createLinks(new NodeId(SRC_NODE_ID),
      new TpId(SRC_TP_ID),
      new NodeId(DST_NODE_ID),
      new TpId(DST_TP_ID));
    assertEquals(2, linkList.size());
    Link dstSrcLink = linkList.get(0);
    assertEquals(DST_NODE_ID, dstSrcLink.getSource().getSourceNode().getValue());
    assertEquals(DST_TP_ID, dstSrcLink.getSource().getSourceTp().getValue());
    assertEquals(SRC_NODE_ID, dstSrcLink.getDestination().getDestNode().getValue());
    assertEquals(SRC_TP_ID, dstSrcLink.getDestination().getDestTp().getValue());
    Link srcDstLink = linkList.get(1);
    assertEquals(SRC_NODE_ID, srcDstLink.getSource().getSourceNode().getValue());
    assertEquals(SRC_TP_ID, srcDstLink.getSource().getSourceTp().getValue());
    assertEquals(DST_NODE_ID, srcDstLink.getDestination().getDestNode().getValue());
    assertEquals(DST_TP_ID, srcDstLink.getDestination().getDestTp().getValue());
  }

  @Test
  public void testBuildNodeIID() throws Exception {
    final String NODE_ID = "id1";
    InstanceIdentifier<Node> insId = Utilities.buildNodeIID(new NodeKey(new NodeId(NODE_ID)));
    assertNotNull(insId.firstIdentifierOf(NetworkTopology.class));
    assertNotNull(insId.firstIdentifierOf(Topology.class));
    assertEquals("flow:1", insId.firstKeyOf(Topology.class, TopologyKey.class).getTopologyId().getValue());
    assertNotNull(insId.firstIdentifierOf(Node.class));
    assertEquals(NODE_ID, insId.firstKeyOf(Node.class, NodeKey.class).getNodeId().getValue());
  }

  @Test
  public void testBuildLinkIID() throws Exception {
    final String NODE_ID = "id1";
    InstanceIdentifier<Link> insId = Utilities.buildLinkIID(new LinkKey(new LinkId(NODE_ID)));
    assertNotNull(insId.firstIdentifierOf(NetworkTopology.class));
    assertNotNull(insId.firstIdentifierOf(Topology.class));
    assertEquals("flow:1", insId.firstKeyOf(Topology.class, TopologyKey.class).getTopologyId().getValue());
    assertNotNull(insId.firstIdentifierOf(Link.class));
    assertEquals(NODE_ID, insId.firstKeyOf(Link.class, LinkKey.class).getLinkId().getValue());
  }

}
