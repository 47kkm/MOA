package moa.store;

import java.time.LocalDate;

public record StoreInfo(Long id, String name, StoreType type, LocalDate registeredAt) {
}
