package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packethandler.packet.rev140528.packet.PacketPayloadType;

import java.util.HashMap;
import java.util.Map;

/**
 * DecoderRegistry maintains mapping of decoders to EtherType
 */
public class DecoderRegistry {
  private Map<PacketPayloadType, PacketDecoder> etherTypeToDecoderMap = new HashMap<PacketPayloadType, PacketDecoder>();

  public void addDecoder(PacketPayloadType packetPayloadType, PacketDecoder decoder) {
    if(packetPayloadType == null || decoder == null) return;

    synchronized(this) {
      etherTypeToDecoderMap.put(packetPayloadType, decoder);
    }

  }

  public synchronized PacketDecoder getDecoder(PacketPayloadType packetPayloadType) {
    return etherTypeToDecoderMap.get(packetPayloadType);
  }
}
