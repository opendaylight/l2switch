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
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.shortestpath.rev170717.result.Linkdetails;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.loopremover.shortestpath.rev170717.result.LinkdetailsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class ShortestPathImpl implements PathComputationService{


    private static final Logger LOG = LoggerFactory.getLogger(ShortestPathImpl.class);
    /**
     * RPC function to find the shortest path between two Switches
     *
     * @param input
     */
    @Override
    public Future<RpcResult<ShortestPathOutput>> shortestPath(ShortestPathInput input) {

        NetworkGraphImpl networkGraph = new NetworkGraphImpl();
        LinkdetailsBuilder linkdetailsBuilder = new LinkdetailsBuilder();
        ShortestPathOutputBuilder shortestPathOutputBuilder = new ShortestPathOutputBuilder();
        List<Linkdetails> pathdetails = new ArrayList<>();
        try {
            if(input.getSource() != null || input.getDestination() !=null
                    || !(input.getSource().equals(input.getDestination()))) {
                NodeId src = new NodeId(input.getSource());
                NodeId dst = new NodeId(input.getDestination());

                //Get Shortest Path link details from NetworkGraph
                List<Link> shortestPath = networkGraph.getPath(src, dst);

                //Iterate the shortestPath  List<Link> and add the details to linkdetailsBuilder
                for (Link link : shortestPath) {
                    linkdetailsBuilder.setSource(link.getSource().getSourceNode().getValue());
                    linkdetailsBuilder.setSourcetp(link.getSource().getSourceTp().getValue());
                    linkdetailsBuilder.setDestination(link.getDestination().getDestNode().getValue());
                    linkdetailsBuilder.setDestinationtp(link.getDestination().getDestTp().getValue());
                    linkdetailsBuilder.setLinkid(link.getLinkId().getValue());
                    pathdetails.add((linkdetailsBuilder.build()));
                }
            } else {
                LOG.error("Source or Destination is Null");
                return (Future<RpcResult<ShortestPathOutput>>) RpcResultBuilder.failed()
                        .withError(RpcError.ErrorType.RPC, "Source / Destination is Null").build();
            }
        } catch (Exception exception) {
            LOG.error("Exception : ",exception);
        }

        shortestPathOutputBuilder.setLinkdetails(pathdetails);
        return RpcResultBuilder.success(shortestPathOutputBuilder.build()).buildFuture();
    }
}
