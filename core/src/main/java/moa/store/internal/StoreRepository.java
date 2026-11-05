package moa.store.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

// 쓰는 메서드만 적는다. 구현은 Spring Data가 만든다
public interface StoreRepository extends Repository<Store, Long> {

    Store save(Store store);

    long count();

    Optional<Store> findById(Long id);

    List<Store> findAll();

    boolean existsById(Long id);
}
