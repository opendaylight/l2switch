package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;

import java.util.EnumMap;
import java.util.Map;

/**
 * DecoderRegistry maintains mapping of decoders to EtherType
 */
public class DecoderRegistry {
  private Map<KnownEtherType, PacketDecoder> etherTypeToDecoderMap = new EnumMap<KnownEtherType, PacketDecoder>(KnownEtherType.class);

  public void addDecoder(KnownEtherType etherType, PacketDecoder decoder) {
    if(etherType == null || decoder == null) return;

    synchronized(this) {
      etherTypeToDecoderMap.put(etherType, decoder);
    }

  }

  public synchronized PacketDecoder getDecoder(KnownEtherType etherType) {
    return etherTypeToDecoderMap.get(etherType);
  }
}
