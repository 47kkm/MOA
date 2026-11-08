package moa.expense.internal;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import moa.expense.ExpenseInfo;
import moa.expense.ExpenseService;

// JSON ↔ 레코드 변환만 한다. 규칙 검사는 Expense와 ExpenseService에 있다 (04 설계 0.5)
// storeId는 store의 매장 접근 검사를 지나온 값이라 여기서 다시 확인하지 않는다 (04 설계 1.2)
@RestController
@RequestMapping("/api/stores/{storeId}/expenses")
class ExpenseController {

    private final ExpenseService expenseService;

    ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseInfo register(@PathVariable Long storeId, @RequestBody ExpenseRequest request) {
        return expenseService.register(storeId, request.categoryId(), request.amount(), request.date(), request.memo());
    }

    // 금액은 숫자 하나 (04 설계 D4-10). 빠진 칸은 null로 들어와 Expense가 400으로 거부한다
    record ExpenseRequest(Long categoryId, Long amount, LocalDate date, String memo) {
    }
}
