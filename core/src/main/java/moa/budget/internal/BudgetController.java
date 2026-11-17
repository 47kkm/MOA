package moa.budget.internal;

import java.time.YearMonth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import moa.budget.BudgetService;
import moa.budget.BudgetStatus;

// JSON ↔ 레코드 변환만 한다. 규칙 검사는 MonthlyBudget과 BudgetService에 있다 (04 설계 0.5)
// storeId는 store의 매장 접근 검사를 지나온 값이라 여기서 다시 확인하지 않는다 (04 설계 1.2)
@RestController
@RequestMapping("/api/stores/{storeId}/budgets")
class BudgetController {

    private final BudgetService budgetService;

    BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    // yearMonth는 "2026-11". 형식이 틀리면 공통 핸들러가 400 VALIDATION (04 설계 5.7)
    @GetMapping("/{yearMonth}")
    BudgetStatus status(@PathVariable Long storeId, @PathVariable YearMonth yearMonth) {
        return budgetService.status(storeId, yearMonth);
    }
}
