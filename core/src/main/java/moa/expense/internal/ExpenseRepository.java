package moa.expense.internal;

import org.springframework.data.repository.Repository;

// 쓰는 메서드만 적는다. 구현은 Spring Data가 메서드 이름을 읽고 만든다
public interface ExpenseRepository extends Repository<Expense, Long> {

    Expense save(Expense expense);
}
