package org.opendaylight.l2switch.packethandler.decoders;

import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;

import java.util.HashMap;
import java.util.Map;

/**
 * DecoderRegistry maintains mapping of decoders to PacketPayloadType
 */
public class DecoderRegistry {
  private Map<PacketPayloadType, PacketDecoder> packetPayloadTypeToDecoderMap = new HashMap<PacketPayloadType, PacketDecoder>();

  public void addDecoder(PacketPayloadType packetPayloadType, PacketDecoder decoder) {
    if(packetPayloadType == null || decoder == null) return;

    synchronized(this) {
      packetPayloadTypeToDecoderMap.put(packetPayloadType, decoder);
    }

  }

  public synchronized PacketDecoder getDecoder(PacketPayloadType packetPayloadType) {
    return packetPayloadTypeToDecoderMap.get(packetPayloadType);
  }
}
