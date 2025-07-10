/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.arphandler.core;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.opendaylight.l2switch.arphandler.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.binding.BindingInstanceIdentifier;
import org.opendaylight.yangtools.binding.BindingInstanceIdentifier.Step;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.KeyStep;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * PacketDispatcher sends packets out to the network.
 */
public class PacketDispatcher {
	private static final Logger LOG = LoggerFactory.getLogger(PacketDispatcher.class);

	private final InventoryReader inventoryReader;
	private final TransmitPacket transmitPacket;

	public PacketDispatcher(InventoryReader inventoryReader, TransmitPacket transmitPacket) {
		this.inventoryReader = requireNonNull(inventoryReader);
		this.transmitPacket = requireNonNull(transmitPacket);
	}

	/**
	 * Dispatches the packet in the appropriate way - flood or unicast.
	 *
	 * @param payload The payload to be sent.
	 * @param ingress The NodeConnector where the payload came from.
	 * @param srcMac  The source MacAddress of the packet.
	 * @param destMac The destination MacAddress of the packet.
	 */
	public void dispatchPacket(byte[] payload, NodeConnectorRef ingress, MacAddress srcMac, MacAddress destMac) {
		inventoryReader.readInventory();

		String nodeId = ingress.getValue().toString();
		NodeConnectorRef srcConnectorRef = inventoryReader.getControllerSwitchConnectors().get(nodeId);

		if (srcConnectorRef == null) {
			refreshInventoryReader();
			srcConnectorRef = inventoryReader.getControllerSwitchConnectors().get(nodeId);
		}
		NodeConnectorRef destNodeConnector = inventoryReader
				.getNodeConnector((DataObjectIdentifier<Node>) ingress.getValue(), destMac);
		if (srcConnectorRef != null) {
			if (destNodeConnector != null) {
				sendPacketOut(payload, srcConnectorRef, destNodeConnector);
			} else {
				floodPacket(nodeId, payload, ingress, srcConnectorRef);
			}
		} else {
			LOG.info("Cannot send packet out or flood as controller node connector is not available for node {}.",
					nodeId);
		}
	}

	/**
	 * Floods the packet.
	 *
	 * @param nodeId      The node id
	 * @param payload     The payload to be sent.
	 * @param origIngress The NodeConnector where the payload came from.
	 */
	public void floodPacket(String nodeId, byte[] payload, NodeConnectorRef origIngress,
			NodeConnectorRef controllerNodeConnector) {

		List<NodeConnectorRef> nodeConnectors = inventoryReader.getSwitchNodeConnectors().get(nodeId);

		if (nodeConnectors == null) {
			refreshInventoryReader();
			nodeConnectors = inventoryReader.getSwitchNodeConnectors().get(nodeId);
			if (nodeConnectors == null) {
				LOG.info("Cannot flood packets, as inventory doesn't have any node connectors for node {}", nodeId);
				return;
			}
		}
		String origId = extractNodeConnectorId(origIngress);
		if (origId == null) {
			LOG.warn("Cannot extract original ingress NodeConnector ID");
			return;
		}

		for (NodeConnectorRef ncRef : nodeConnectors) {
			String ncId = extractNodeConnectorId(ncRef);
			if (ncId != null && !ncId.equals(origId)) {
				sendPacketOut(payload, origIngress, ncRef);
			}
		}

	}

	private String extractNodeConnectorId(NodeConnectorRef ref) {
		BindingInstanceIdentifier instanceId = ref.getValue();
		for (Step step : instanceId.steps()) {
			if (step instanceof KeyStep keyStep && keyStep.key() instanceof NodeConnectorKey) {
				return ((NodeConnectorKey) keyStep.key()).getId().getValue();
			}
		}
		return null;
	}

	/**
	 * Sends the specified packet on the specified port.
	 *
	 * @param payload The payload to be sent.
	 * @param ingress The NodeConnector where the payload came from.
	 * @param egress  The NodeConnector where the payload will go.
	 */
	public void sendPacketOut(byte[] payload, NodeConnectorRef ingress, NodeConnectorRef egress) {
		if (ingress == null || egress == null) {
			return;
		}
		BindingInstanceIdentifier egressNodePath = egress.getValue();
		TransmitPacketInput input = new TransmitPacketInputBuilder().setPayload(payload)
				.setNode(new NodeRef(egressNodePath)).setEgress(egress).setIngress(ingress).build();

		Futures.addCallback(transmitPacket.invoke(input), new FutureCallback<RpcResult<?>>() {
			@Override
			public void onSuccess(RpcResult<?> result) {
				LOG.debug("transmitPacket was successful");
			}

			@Override
			public void onFailure(Throwable failure) {
				LOG.debug("transmitPacket for {} failed", input, failure);
			}
		}, MoreExecutors.directExecutor());
	}

	private void refreshInventoryReader() {
		inventoryReader.setRefreshData(true);
		inventoryReader.readInventory();
	}

	//private static InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
	//	return nodeChild.firstIdentifierOf(Node.class);
	//}
}
