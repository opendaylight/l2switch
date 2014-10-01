/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NetworkGraphImplTest {

  private NetworkGraphImpl networkGraphImpl;

  @Before
  public void initMocks() {
    networkGraphImpl = new NetworkGraphImpl();
  }


  @Test
  public void testClear() throws Exception {
    networkGraphImpl.clear();
  }

  @Test
  public void testGetLinksInMst_NullGraph() throws Exception {
    networkGraphImpl.clear();
    List<Link> mstLinks = networkGraphImpl.getLinksInMst();
    assertEquals(0, mstLinks.size());
  }

  @Test
  public void testAllLinks_NullGraph() throws Exception {
    networkGraphImpl.clear();
    List<Link> allLinks = networkGraphImpl.getAllLinks();
    assertEquals(0, allLinks.size());
  }

  @Test
  public void testAddLinks_NullInput() throws Exception {
    networkGraphImpl.clear();
    networkGraphImpl.addLinks(null);
    List<Link> mstLinks = networkGraphImpl.getLinksInMst();
    assertEquals(0, mstLinks.size());
    List<Link> allLinks = networkGraphImpl.getAllLinks();
    assertEquals(0, allLinks.size());
  }

  @Test
  public void testAddLinks_ValidInput() throws Exception {
    Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2")).build())
        .build();
    Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:3")).setDestTp(new TpId("openflow:3")).build())
        .build();
    List<Link> links = new ArrayList<Link>();
    links.add(link1);
    links.add(link2);

    networkGraphImpl.addLinks(links);
    List<Link> mstLinks = networkGraphImpl.getLinksInMst();
    assertEquals(2, mstLinks.size());
    List<Link> allLinks = networkGraphImpl.getAllLinks();
    assertEquals(2, allLinks.size());
  }

  @Test(expected = NullPointerException.class)
  public void testRemoveLinks_NullInput() throws Exception {
    networkGraphImpl.clear();
    networkGraphImpl.removeLinks(null);
  }


  @Test
  public void testRemoveLinks_ValidInput() throws Exception {
    Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2")).build())
        .build();
    Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:3")).setDestTp(new TpId("openflow:3")).build())
        .build();
    Link link3 = new LinkBuilder().setLinkId(new LinkId("openflow:3"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:3")).setSourceTp(new TpId("openflow:3")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:4")).setDestTp(new TpId("openflow:4")).build())
        .build();
    List<Link> links = new ArrayList<Link>();
    links.add(link1);
    links.add(link2);
    links.add(link3);
    List<Link> removeLinks = new ArrayList<Link>();
    removeLinks.add(link1);

    networkGraphImpl.addLinks(links);
    List<Link> mstLinks = networkGraphImpl.getLinksInMst();
    assertEquals(3, mstLinks.size());
    List<Link> allLinks = networkGraphImpl.getAllLinks();
    assertEquals(3, allLinks.size());

    networkGraphImpl.removeLinks(removeLinks);
    allLinks = networkGraphImpl.getAllLinks();
    assertEquals(2, allLinks.size());
  }

  @Test
  public void testMstRemovesLoops() throws Exception {
    Link link1 = new LinkBuilder().setLinkId(new LinkId("openflow:1:1"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1:1")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2:1")).build())
        .build();
    Link link2 = new LinkBuilder().setLinkId(new LinkId("openflow:2:1"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2:1")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1")).setDestTp(new TpId("openflow:1:1")).build())
        .build();
    Link link3 = new LinkBuilder().setLinkId(new LinkId("openflow:2:2"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2:2")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:3")).setDestTp(new TpId("openflow:3:1")).build())
        .build();
    Link link4 = new LinkBuilder().setLinkId(new LinkId("openflow:3:1"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:3")).setSourceTp(new TpId("openflow:3:1")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2:2")).build())
        .build();
    Link link5 = new LinkBuilder().setLinkId(new LinkId("openflow:3:2"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:3")).setSourceTp(new TpId("openflow:3:2")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1")).setDestTp(new TpId("openflow:1:2")).build())
        .build();
    Link link6 = new LinkBuilder().setLinkId(new LinkId("openflow:1:2"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1:2")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:3")).setDestTp(new TpId("openflow:3:2")).build())
        .build();
    //parallel link betn nodes 1 & 2
    Link link7 = new LinkBuilder().setLinkId(new LinkId("openflow:1:3"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:1")).setSourceTp(new TpId("openflow:1:3")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:2")).setDestTp(new TpId("openflow:2:3")).build())
        .build();
    Link link8 = new LinkBuilder().setLinkId(new LinkId("openflow:2:3"))
        .setSource(new SourceBuilder().setSourceNode(new NodeId("openflow:2")).setSourceTp(new TpId("openflow:2:3")).build())
        .setDestination(new DestinationBuilder().setDestNode(new NodeId("openflow:1")).setDestTp(new TpId("openflow:1:3")).build())
        .build();
    List<Link> links = new ArrayList<Link>();
    links.add(link1);
    links.add(link2);
    links.add(link3);
    links.add(link4);
    links.add(link5);
    links.add(link6);
    links.add(link7);
    links.add(link8);

    networkGraphImpl.addLinks(links);
    List<Link> mstLinks = networkGraphImpl.getLinksInMst();
    assertEquals(2, mstLinks.size());
    List<Link> allLinks = networkGraphImpl.getAllLinks();
    assertEquals(4, allLinks.size());
  }
}
