package moa.expense.internal;

import java.util.Optional;

import org.springframework.data.repository.Repository;

// 쓰는 메서드만 적는다. 구현은 Spring Data가 메서드 이름을 읽고 만든다
public interface ExpenseRepository extends Repository<Expense, Long> {

    Expense save(Expense expense);

    // 다른 매장의 비용은 없는 것과 같다
    Optional<Expense> findByIdAndStoreId(Long id, Long storeId);
}
