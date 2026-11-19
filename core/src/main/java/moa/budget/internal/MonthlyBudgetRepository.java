package moa.budget.internal;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.repository.Repository;

// 쓰는 메서드만 적는다. 구현은 Spring Data가 메서드 이름을 읽고 만든다
public interface MonthlyBudgetRepository extends Repository<MonthlyBudget, Long> {

    MonthlyBudget save(MonthlyBudget budget);

    // 매장 · 연월당 하나 (유니크 인덱스). month는 그 달 1일 (04 설계 D5-10)
    Optional<MonthlyBudget> findByStoreIdAndMonth(Long storeId, LocalDate month);
}
