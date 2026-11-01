package moa.store.internal;

import java.time.LocalDate;

import moa.store.StoreType;

public class Store {

    private String name;
    private StoreType type;
    private LocalDate registeredAt;

    private Store(String name, StoreType type, LocalDate registeredAt) {
        this.name = name;
        this.type = type;
        this.registeredAt = registeredAt;
    }

    public static Store register(String name, StoreType type, LocalDate today) {
        return new Store(validName(name), validType(type), today);
    }

    // 이름 규칙: 앞뒤 공백을 자른 뒤 1~50자 (04 설계 1.3). 등록과 수정이 함께 쓴다
    private static String validName(String name) {
        String stripped = name == null ? "" : name.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (stripped.length() > 50) {
            throw new IllegalArgumentException("name must be at most 50 characters");
        }
        return stripped;
    }

    private static StoreType validType(StoreType type) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        return type;
    }

    public String getName() {
        return name;
    }

    public StoreType getType() {
        return type;
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }
}
