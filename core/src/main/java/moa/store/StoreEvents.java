package moa.store;

import java.time.LocalDate;

public final class StoreEvents {

    private StoreEvents() {
    }

    // 등록이 커밋된 뒤 category(기본 비용 분류)와 sales(기본 결제수단)가 받는다 (04 설계 1.7)
    public record StoreRegistered(Long storeId, StoreType type, LocalDate registeredAt) {
    }
}
