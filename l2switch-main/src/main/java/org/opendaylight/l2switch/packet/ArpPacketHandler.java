package org.opendaylight.l2switch.packet;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ArpPacketHandler implements ArpPacketListener {

  private final static Logger _logger = LoggerFactory.getLogger(ArpPacketHandler.class);
  private final String IPV4_IP_TO_IGNORE ="0.0.0.0";
  private final String IPV6_IP_TO_IGNORE ="0:0:0:0:0:0:0:0";

  private PacketDispatcher packetDispatcher;

  public ArpPacketHandler(PacketDispatcher packetDispatcher) {
    this.packetDispatcher = packetDispatcher;
  }

  /**
   * The handler function for ARP packets.
   * @param packetReceived  The incoming packet.
   */
  @Override
  public void onArpPacketReceived(ArpPacketReceived packetReceived) {
    if(packetReceived==null || packetReceived.getPacketChain()==null) {
      return;
    }

    RawPacket rawPacket = null;
    EthernetPacket ethernetPacket = null;
    ArpPacket arpPacket = null;
    for (PacketChain packetChain : packetReceived.getPacketChain()) {
      if (packetChain.getPacket() instanceof RawPacket) {
        rawPacket = (RawPacket)packetChain.getPacket();
      }
      else if (packetChain.getPacket() instanceof EthernetPacket) {
        ethernetPacket = (EthernetPacket)packetChain.getPacket();
      }
      else if (packetChain.getPacket() instanceof ArpPacket) {
        arpPacket = (ArpPacket)packetChain.getPacket();
      }
    }
    if (rawPacket==null || ethernetPacket==null || arpPacket==null) {
      return;
    }

    packetDispatcher.dispatchPacket(packetReceived.getPayload(),
        rawPacket.getIngress(),
        ethernetPacket.getSourceMac(),
        ethernetPacket.getDestinationMac());
  }

}
