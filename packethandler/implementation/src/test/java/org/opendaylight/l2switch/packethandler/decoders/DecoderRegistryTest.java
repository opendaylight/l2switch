package org.opendaylight.l2switch.packethandler.decoders;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.PacketType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.PacketPayloadTypeBuilder;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;


/**
 * Created by amitmandke on 6/5/14.
 */
public class DecoderRegistryTest {
  private DecoderRegistry decoderRegistry = null;
  private PacketDecoder packetDecoder;
  private PacketPayloadType packetPayloadType;

  @Before
  public void init() {
    decoderRegistry = new DecoderRegistry();
    packetDecoder = mock(PacketDecoder.class);
    packetPayloadType = new PacketPayloadTypeBuilder().setPacketType(PacketType.Raw).setPayloadType(1).build();
  }

  @Test
  public void testAddDecoderSunnyDay() {
    decoderRegistry.addDecoder(packetPayloadType, packetDecoder);
    assertEquals(packetDecoder, decoderRegistry.getDecoder(packetPayloadType));
  }

  public void testGetDecoderWithoutAdding() {
    PacketDecoder decoder = decoderRegistry.getDecoder(packetPayloadType);
    assertEquals(null, decoder);
  }
}
