/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;

/**
 * Service that adds packet forwarding flows to configuration data store.
 */
public interface FlowWriterService {
    /**
     * Writes a flow that forwards packets to destPort if destination mac in
     * packet is destMac and source Mac in packet is sourceMac. If sourceMac is
     * null then flow would not set any source mac, resulting in all packets
     * with destMac being forwarded to destPort.
     *
     * @param sourceMac the source MAC
     * @param destMac the destination MAC
     * @param destNodeConnectorRef the destination port NodeConnectorRef
     */
    void addMacToMacFlow(MacAddress sourceMac, MacAddress destMac, NodeConnectorRef destNodeConnectorRef);

    /**
     * Writes mac-to-mac flow on all ports that are in the path between given
     * source and destination ports. It uses path provided by
     * org.opendaylight.l2switch.loopremover.topology.NetworkGraphService to
     * find a links between given ports. And then writes appropriate flow on each
     * port that is covered in that path.
     *
     * @param sourceMac the source MAC
     * @param sourceNodeConnectorRef the the source port NodeConnectorRef
     * @param destMac the destination MAC
     * @param destNodeConnectorRef the destination port NodeConnectorRef
     */
    void addBidirectionalMacToMacFlows(MacAddress sourceMac, NodeConnectorRef sourceNodeConnectorRef,
        MacAddress destMac, NodeConnectorRef destNodeConnectorRef);
}
