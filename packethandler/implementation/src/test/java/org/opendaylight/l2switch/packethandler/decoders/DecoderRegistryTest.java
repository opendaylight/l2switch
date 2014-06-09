package org.opendaylight.l2switch.packethandler.decoders;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;


/**
 * Created by amitmandke on 6/5/14.
 */
public class DecoderRegistryTest {
  private DecoderRegistry decoderRegistry = null;
  private PacketDecoder packetDecoder;

  @Before
  public void init() {
    decoderRegistry = new DecoderRegistry();
    packetDecoder = mock(PacketDecoder.class);
  }

  @Test
  public void testAddDecoderSunnyDay() {
    decoderRegistry.addDecoder(KnownEtherType.Arp, packetDecoder);
    assertEquals(packetDecoder, decoderRegistry.getDecoder(KnownEtherType.Arp));
  }

  public void testGetDecoderWithoutAdding() {
    PacketDecoder decoder = decoderRegistry.getDecoder(KnownEtherType.Arp);
    assertEquals(null, decoder);
  }
}
