/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;

import com.google.common.base.Preconditions;
import edu.uci.ics.jung.algorithms.shortestpath.PrimMinimumSpanningTree;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of NetworkGraphService{@link org.opendaylight.l2switch.loopremover.topology.NetworkGraphService}.
 * It uses Jung graph library internally to maintain a graph and optimum way to return shortest path using
 * Dijkstra algorithm.
 */
public class NetworkGraphImpl implements NetworkGraphService {

  private static final Logger _logger = LoggerFactory.getLogger(NetworkGraphImpl.class);

  Graph<NodeId, Link> networkGraph = null;
  Set<String> linkAdded = new HashSet<>();

  //Enable following lines when shortest path functionality is required.
  //DijkstraShortestPath<NodeId, Link> shortestPath = null;

  /**
   * Adds links to existing graph or creates new directed graph with given links if graph was not initialized.
   *
   * @param links The links to add.
   */
  @Override
  public synchronized void addLinks(List<Link> links) {
    if(links == null || links.isEmpty()) {
      _logger.info("In addLinks: No link added as links is null or empty.");
      return;
    }

    if(networkGraph == null) {
      networkGraph = new SparseMultigraph<>();
    }

    for(Link link : links) {
      if(linkAlreadyAdded(link)) {
        continue;
      }
      NodeId sourceNodeId = link.getSource().getSourceNode();
      NodeId destinationNodeId = link.getDestination().getDestNode();
      networkGraph.addVertex(sourceNodeId);
      networkGraph.addVertex(destinationNodeId);
      networkGraph.addEdge(link, sourceNodeId, destinationNodeId, EdgeType.UNDIRECTED);
    }

    /*if(shortestPath == null) {
      shortestPath = new DijkstraShortestPath<>(networkGraph);
    } else {
      shortestPath.reset();
    }*/
  }

  private boolean linkAlreadyAdded(Link link) {
    String linkAddedKey = null;
    if(link.getDestination().getDestTp().hashCode() > link.getSource().getSourceTp().hashCode()) {
      linkAddedKey = link.getSource().getSourceTp().getValue() + link.getDestination().getDestTp().getValue();
    } else {
      linkAddedKey = link.getDestination().getDestTp().getValue() + link.getSource().getSourceTp().getValue();
    }
    if(linkAdded.contains(linkAddedKey)) {
      return true;
    } else {
      linkAdded.add(linkAddedKey);
      return false;
    }
  }

  /**
   * Removes links from existing graph.
   *
   * @param links The links to remove.
   */
  @Override
  public synchronized void removeLinks(List<Link> links) {
    Preconditions.checkNotNull(networkGraph, "Graph is not initialized, add links first.");

    if(links == null || links.isEmpty()) {
      _logger.info("In removeLinks: No link removed as links is null or empty.");
      return;
    }

    for(Link link : links) {
      networkGraph.removeEdge(link);
    }
    /*if(shortestPath == null) {
      shortestPath = new DijkstraShortestPath<>(networkGraph);
    } else {
      shortestPath.reset();
    }*/

  }

  /**
   * returns a path between 2 nodes. Uses Dijkstra's algorithm to return shortest path.
   * @param sourceNodeId
   * @param destinationNodeId
   * @return
   */
  //@Override
  /*public synchronized List<Link> getPath(NodeId sourceNodeId, NodeId destinationNodeId) {
    Preconditions.checkNotNull(shortestPath, "Graph is not initialized, add links first.");

    if(sourceNodeId == null || destinationNodeId == null) {
      _logger.info("In getPath: returning null, as sourceNodeId or destinationNodeId is null.");
      return null;
    }

    return shortestPath.getPath(sourceNodeId, destinationNodeId);
  }*/

  /**
   * Clears the prebuilt graph, in case same service instance is required to process a new graph.
   */
  @Override
  public synchronized void clear() {
    networkGraph = null;
    //shortestPath = null;
  }

  /**
   * Forms MST(minimum spanning tree) from network graph and returns links that are not in MST.
   *
   * @return The links in the MST (minimum spanning tree)
   */
  @Override
  public synchronized List<Link> getLinksInMst() {
    List<Link> linksInMst = new ArrayList<>();
    if(networkGraph != null) {
      PrimMinimumSpanningTree<NodeId, Link> networkMst = new PrimMinimumSpanningTree<>(DelegateTree.<NodeId, Link>getFactory());
      Graph<NodeId, Link> mstGraph = networkMst.transform(networkGraph);
      Collection<Link> mstLinks = mstGraph.getEdges();
      linksInMst.addAll(mstLinks);
    }
    return linksInMst;
  }

  /**
   * Get all the links in the network.
   *
   * @return The links in the network.
   */
  @Override
  public List<Link> getAllLinks() {
    List<Link> allLinks = new ArrayList<>();
    if(networkGraph != null) {
      allLinks.addAll(networkGraph.getEdges());
    }
    return allLinks;
  }
}
