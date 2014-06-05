package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;

/**
 * DecoderRegistry maintains mapping of decoders to EtherType
 */
public class DecoderRegistry {
  public void addDecoder(KnownEtherType etherType, PacketDecoder decoder) {

  }

  public PacketDecoder getDecoder(KnownEtherType etherType) {
    return null;
  }
}
