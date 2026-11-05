package moa.store.internal;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import moa.store.StoreType;

// MySQL에서는 스키마(= DB)를 Hibernate가 catalog로 다룬다. schema로 적으면 테이블을 못 찾는다 (04 설계 0.4)
@Entity
@Table(name = "stores", catalog = "store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_type")
    private StoreType type;

    private LocalDate registeredAt;

    // JPA가 DB에서 읽어 온 값을 채울 때 쓰는 빈 생성자. 우리 코드는 register()로만 만든다
    protected Store() {
    }

    private Store(String name, StoreType type, LocalDate registeredAt) {
        this.name = name;
        this.type = type;
        this.registeredAt = registeredAt;
    }

    public static Store register(String name, StoreType type, LocalDate today) {
        return new Store(validName(name), validType(type), today);
    }

    public void update (String name, StoreType type) {
        this.name = validName(name);
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

    public Long getId() {
        return id;
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
