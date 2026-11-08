package moa.expense.internal;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import moa.expense.ExpenseInfo;
import moa.expense.ExpenseList;
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

    // yearMonth는 필수 "2026-10". 없거나 형식이 틀리면 공통 핸들러가 400 VALIDATION (04 설계 4.7)
    // categoryId는 없거나 빈 값이면 필터 없음
    @GetMapping
    ExpenseList list(@PathVariable Long storeId, @RequestParam YearMonth yearMonth,
            @RequestParam(required = false) Long categoryId) {
        return expenseService.list(storeId, yearMonth, categoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseInfo register(@PathVariable Long storeId, @RequestBody ExpenseRequest request) {
        return expenseService.register(storeId, request.categoryId(), request.amount(), request.date(), request.memo());
    }

    // 수정과 재분류는 같은 경로. 비용 분류가 바뀌었는지는 서비스가 본다 (04 설계 D4-1)
    @PutMapping("/{id}")
    ExpenseInfo update(@PathVariable Long storeId, @PathVariable Long id, @RequestBody ExpenseRequest request) {
        return expenseService.update(storeId, id, request.categoryId(), request.amount(), request.date(),
                request.memo());
    }

    // 금액은 숫자 하나 (04 설계 D4-10). 빠진 칸은 null로 들어와 Expense가 400으로 거부한다
    record ExpenseRequest(Long categoryId, Long amount, LocalDate date, String memo) {
    }
}
