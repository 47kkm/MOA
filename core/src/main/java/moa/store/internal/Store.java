package moa.store.internal;

import java.time.LocalDate;

import moa.store.StoreType;

public class Store {

    private Store() {
    }

    public static Store register(String name, StoreType type, LocalDate today) {
        if (name.strip().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return new Store();
    }
}
