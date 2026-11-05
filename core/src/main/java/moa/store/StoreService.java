package moa.store;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import moa.shared.ErrorCode;
import moa.shared.MoaException;
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
        // Phase 1은 시스템 전체에 매장 1개. Phase 2에서 세는 범위만 "계정별"로 바뀐다 (04 설계 D1-1)
        if (stores.count() >= 1) {
            throw new MoaException(ErrorCode.STORE_ALREADY_EXISTS, "store already exists");
        }
        Store store = stores.save(Store.register(name, type, LocalDate.now(clock)));
        events.publishEvent(new StoreRegistered(store.getId(), store.getType(), store.getRegisteredAt()));
        return toInfo(store);
    }

    // save를 부르지 않는다. 읽어 온 store를 고치면 커밋 때 변경 감지가 UPDATE를 보낸다
    @Transactional
    public StoreInfo update(Long storeId, String name, StoreType type) {
        Store store = stores.findById(storeId)
                .orElseThrow(() -> new MoaException(ErrorCode.NOT_FOUND, "store not found: " + storeId));
        store.update(name, type);
        return toInfo(store);
    }

    @Transactional(readOnly = true)
    public StoreInfo get(Long storeId) {
        return stores.findById(storeId)
                .map(StoreService::toInfo)
                .orElseThrow(() -> new MoaException(ErrorCode.NOT_FOUND, "store not found: " + storeId));
    }

    // Phase 1에서는 0개 또는 1개. 프론트가 "매장이 있나"를 이걸로 판단한다
    @Transactional(readOnly = true)
    public List<StoreInfo> findAll() {
        return stores.findAll().stream().map(StoreService::toInfo).toList();
    }

    private static StoreInfo toInfo(Store store) {
        return new StoreInfo(store.getId(), store.getName(), store.getType(), store.getRegisteredAt());
    }
}
