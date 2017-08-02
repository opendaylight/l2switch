/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.loopremover.topology;



import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.shortestpath.rev170717.PathComputationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.shortestpath.rev170717.ShortestPathInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.shortestpath.rev170717.ShortestPathOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.shortestpath.rev170717.ShortestPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.Future;


/**
 * Implementation of PathComputationService
 * It implements shortestPath RPC method to return shortest path between two switches
 * using Dijkstra algorithm.
 */
public class ShortestPathImpl implements PathComputationService {

    private static final Logger LOG = LoggerFactory.getLogger(ShortestPathImpl.class);

    /**
     * RPC function to find the shortest path between two Switches (OpenFlow)
     * TODO - host to host shortest path implementation by Host Name/IP
     * @param input ShortestPathInput
     */
    @Override
    public Future<RpcResult<ShortestPathOutput>> shortestPath(ShortestPathInput input) {

        NetworkGraphImpl networkGraph = new NetworkGraphImpl();
        ShortestPathOutputBuilder shortestPathOutputBuilder = new ShortestPathOutputBuilder();
        try {
            // Source and Destination should not be NULL
            if (input.getSource() != null && input.getDestination() != null) {
                // Source and Destination should not be equal
                if (input.getSource().equals(input.getDestination())) {
                    throw new Exception("Source & Destination are same");
                }
                NodeId src = new NodeId(String.valueOf(input.getSource()));
                NodeId dst = new NodeId(String.valueOf(input.getDestination()));

                //Get Shortest Path link details from NetworkGraph
                List<Link> shortestPath = networkGraph.getPath(src, dst);

                //Iterate the shortestPath  List<Link> and add the details to shortestPathOutputBuilder
                for (Link link : shortestPath) {
                    shortestPathOutputBuilder.setSource(link.getSource());
                    shortestPathOutputBuilder.setDestination(link.getDestination());
                    shortestPathOutputBuilder.setLinkId(link.getLinkId());
                    shortestPathOutputBuilder.setSupportingLink(link.getSupportingLink());
                }
            } else {
                throw new Exception("Source/Destination is Null");
            }
        } catch (Exception exception) {
            LOG.error("Exception : " + exception.getMessage());
            return (Future<RpcResult<ShortestPathOutput>>) RpcResultBuilder.failed()
                    .withError(RpcError.ErrorType.RPC, exception.getMessage()).build();
        }
        return RpcResultBuilder.success(shortestPathOutputBuilder.build()).buildFuture();
    }
}
