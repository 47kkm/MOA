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
        if (name.strip().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return new Store(name, type, today);
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
