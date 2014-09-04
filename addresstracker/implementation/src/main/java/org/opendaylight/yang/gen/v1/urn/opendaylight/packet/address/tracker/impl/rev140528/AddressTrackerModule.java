package org.opendaylight.yang.gen.v1.urn.opendaylight.packet.address.tracker.impl.rev140528;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObservationWriter;
import org.opendaylight.l2switch.addresstracker.addressobserver.AddressObserver;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressTrackerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.packet.address.tracker.impl.rev140528.AbstractAddressTrackerModule {

  private final static Logger _logger = LoggerFactory.getLogger(AddressTrackerModule.class);
  private Registration listenerRegistration;

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
    AddressObserver addressObserver = new AddressObserver(addressObservationWriter);

    // Register AddressObserver for notifications
    this.listenerRegistration = notificationService.registerNotificationListener(addressObserver);

    final class CloseResources implements AutoCloseable {
      @Override
      public void close() throws Exception {
        if(listenerRegistration != null) {
          listenerRegistration.close();
        }
        _logger.info("AddressTracker (instance {}) torn down.", this);
      }
    }
    AutoCloseable ret = new CloseResources();
    _logger.info("AddressTracker (instance {}) initialized.", ret);
    return ret;

  }

}
