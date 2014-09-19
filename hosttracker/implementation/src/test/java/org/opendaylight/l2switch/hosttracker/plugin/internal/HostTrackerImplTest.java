package org.opendaylight.l2switch.hosttracker.plugin.internal;

import org.junit.Before;
import org.junit.Test;
//import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;

public class HostTrackerImplTest {

  @MockitoAnnotations.Mock private DataBroker dataBroker;
  @MockitoAnnotations.Mock private NotificationService notificationService;
  private HostTrackerImpl hostTracker;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    hostTracker = new HostTrackerImpl(dataBroker, notificationService);
  }

  @Test(expected=UnsupportedOperationException.class)
  public void testClose() throws Exception {
    hostTracker.close();
  }

  @Test
  public void testRegisterAsDataChangeListener() throws Exception {
    hostTracker.registerAsDataChangeListener();
    verify(dataBroker, times(1)).registerDataChangeListener(
      any(LogicalDatastoreType.class),
      any(InstanceIdentifier.class),
      any(HostTrackerImpl.class),
      any(AsyncDataBroker.DataChangeScope.class));
  }

  @Test
  public void testPacketReceived() throws Exception {
    /*InstanceIdentifier<NodeConnector> ncInsId = InstanceIdentifier.builder(Nodes.class)
      .child(Node.class)
      .child(NodeConnector.class)
      .toInstance();

    ReadOnlyTransaction readOnlyTransaction = Mockito.mock(ReadOnlyTransaction.class);
    when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);


    NodeConnector nc = new NodeConnectorBuilder()
      .setKey(new NodeConnectorKey(new NodeConnectorId("1")))
      .build();
    Optional<NodeConnector> optionalNc = Optional.of(nc);
    CheckedFuture checkedFuture1 = Mockito.mock(CheckedFuture.class);
    when(checkedFuture1.get()).thenReturn(optionalNc);

    Node node = new NodeBuilder()
      .build();
    Optional<Node> optionalNode = Optional.of(node);
    CheckedFuture checkedFuture2 = Mockito.mock(CheckedFuture.class);
    when(checkedFuture2.get()).thenReturn(optionalNode);

    when(readOnlyTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(checkedFuture1);

    hostTracker.packetReceived(null, ncInsId);
    verify(dataBroker, times(1)).newReadOnlyTransaction();*/
  }

  @Test
  public void testOnDataChanged() throws Exception {
  }


}
