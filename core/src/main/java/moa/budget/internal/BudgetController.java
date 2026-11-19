package moa.budget.internal;

import java.time.YearMonth;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import moa.budget.BudgetLineInput;
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

    // 다시 설정 = 덮어쓰기라 PUT. 응답은 현황 조회와 같은 모양 (04 설계 5.2, 5.7)
    @PutMapping("/{yearMonth}")
    BudgetStatus set(@PathVariable Long storeId, @PathVariable YearMonth yearMonth,
            @RequestBody BudgetRequest request) {
        return budgetService.set(storeId, yearMonth, request.total(), request.lines());
    }

    // 금액은 숫자 하나 (04 설계 D4-10). 빠진 칸은 null로 들어와 MonthlyBudget이 400으로 거부한다
    // 빈칸 · 0 항목은 프론트가 빼고 보낸다 (기획서 5.4)
    record BudgetRequest(Long total, List<BudgetLineInput> lines) {
    }
}
