package org.opendaylight.yang.gen.v1.urn.opendaylight.packet.address.tracker.impl.rev140528;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObservationWriter;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObserverUsingArp;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObserverUsingIpv4;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObserverUsingIpv6;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddressTrackerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.packet.address.tracker.impl.rev140528.AbstractAddressTrackerModule {

  private final static Logger _logger = LoggerFactory.getLogger(AddressTrackerModule.class);
  private List<Registration> listenerRegistrations = new ArrayList<>();
  private static String ARP_PACKET_TYPE = "arp", IPV4_PACKET_TYPE = "ipv4", IPV6_PACKET_TYPE = "ipv6";

  public AddressTrackerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
    super(identifier, dependencyResolver);
  }

  public AddressTrackerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.packet.address.tracker.impl.rev140528.AddressTrackerModule oldModule, java.lang.AutoCloseable oldInstance) {
    super(identifier, dependencyResolver, oldModule, oldInstance);
  }

  @Override
  public void customValidation() {
    // add custom validation form module attributes here.
  }

  @Override
  public java.lang.AutoCloseable createInstance() {
    NotificationProviderService notificationService = getNotificationServiceDependency();
    DataBroker dataService = getDataBrokerDependency();

    // Setup AddressObserver & AddressObservationWriter
    AddressObservationWriter addressObservationWriter = new AddressObservationWriter(dataService);
    addressObservationWriter.setTimestampUpdateInterval(getTimestampUpdateInterval());
    Set<String> packetTypes = processObserveAddressesFrom(getObserveAddressesFrom());

    if(packetTypes == null || packetTypes.isEmpty()) { //set default to arp
      packetTypes = new HashSet<>();
      packetTypes.add(ARP_PACKET_TYPE);
    }

    if(packetTypes.contains(ARP_PACKET_TYPE)) {
      AddressObserverUsingArp addressObserverUsingArp = new AddressObserverUsingArp(addressObservationWriter);
      // Register AddressObserver for notifications
      this.listenerRegistrations.add(notificationService.registerNotificationListener(addressObserverUsingArp));
    }

    if(packetTypes.contains(IPV4_PACKET_TYPE)) {
      AddressObserverUsingIpv4 addressObserverUsingIpv4 = new AddressObserverUsingIpv4(addressObservationWriter);
      // Register AddressObserver for notifications
      this.listenerRegistrations.add(notificationService.registerNotificationListener(addressObserverUsingIpv4));
    }
    if(packetTypes.contains(IPV6_PACKET_TYPE)) {
      AddressObserverUsingIpv6 addressObserverUsingIpv6 = new AddressObserverUsingIpv6(addressObservationWriter);
      // Register AddressObserver for notifications
      this.listenerRegistrations.add(notificationService.registerNotificationListener(addressObserverUsingIpv6));
    }


    final class CloseResources implements AutoCloseable {
      @Override
      public void close() throws Exception {
        if(listenerRegistrations != null && !listenerRegistrations.isEmpty()) {
          for(Registration listenerRegistration : listenerRegistrations)
            listenerRegistration.close();
        }
        _logger.info("AddressTracker (instance {}) torn down.", this);
      }
    }
    AutoCloseable ret = new CloseResources();
    _logger.info("AddressTracker (instance {}) initialized.", ret);
    return ret;

  }

  private Set<String> processObserveAddressesFrom(String observeAddressesFrom) {
    Set<String> packetTypes = new HashSet<>();
    if(observeAddressesFrom == null || observeAddressesFrom.isEmpty()) {
      packetTypes.add(ARP_PACKET_TYPE);
      return packetTypes;
    }
    String[] observeAddressFromSplit = observeAddressesFrom.split(",");
    if(observeAddressFromSplit == null || observeAddressFromSplit.length == 0) {
      packetTypes.add(ARP_PACKET_TYPE);
      return packetTypes;
    }
    for(String packetType : observeAddressFromSplit) {
      packetTypes.add(packetType);
    }
    return packetTypes;
  }

}
