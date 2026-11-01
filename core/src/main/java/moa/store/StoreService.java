package moa.store;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.store.StoreEvents.StoreRegistered;
import moa.store.internal.Store;
import moa.store.internal.StoreRepository;

@Service
public class StoreService {

    private final StoreRepository stores;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public StoreService(StoreRepository stores, Clock clock, ApplicationEventPublisher events) {
        this.stores = stores;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public StoreInfo register(String name, StoreType type) {
        Store store = stores.save(Store.register(name, type, LocalDate.now(clock)));
        events.publishEvent(new StoreRegistered(store.getId(), store.getType(), store.getRegisteredAt()));
        return toInfo(store);
    }

    private static StoreInfo toInfo(Store store) {
        return new StoreInfo(store.getId(), store.getName(), store.getType(), store.getRegisteredAt());
    }
}
