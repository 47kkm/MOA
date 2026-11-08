package moa.expense.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

// 쓰는 메서드만 적는다. 구현은 Spring Data가 메서드 이름을 읽고 만든다
public interface ExpenseRepository extends Repository<Expense, Long> {

    Expense save(Expense expense);

    void delete(Expense expense);

    // 다른 매장의 비용은 없는 것과 같다
    Optional<Expense> findByIdAndStoreId(Long id, Long storeId);

    // 한 달 목록. 날짜 역순, 같은 날은 나중 입력(id 큰 것)이 위 (04 설계 4.2)
    List<Expense> findByStoreIdAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(Long storeId, LocalDate from,
            LocalDate to);
}
