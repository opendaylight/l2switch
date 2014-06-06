package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.EthernetPacketGrp;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * ARP (Address Resolution Protocol) Packet Decoder
 */
public class ArpDecoder implements PacketDecoder {

  /**
   * Decode an EthernetPacket into an ArpPacket
   */
  @Override
  public EthernetPacketGrp decode(EthernetPacket ethernetPacket) {
  // Create helper method to transfer input ethernetPacket => ArpPacket
    // Possibly make the "PacketDecoder" interface into an ABSTRACT CLASS, and put this method into it
    return null;
  }

  @Override
  public Notification buildPacketNotification(EthernetPacketGrp decodedEthernetPacket) {
    return null;
  }
}
