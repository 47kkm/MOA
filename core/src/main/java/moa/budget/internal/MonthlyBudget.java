package moa.budget.internal;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import moa.budget.BudgetLineInput;
import moa.shared.Money;

// 애그리거트는 월 예산 한 장 (04 설계 5.4). 예산 항목은 그 안의 값이고, 소진은 여기에 없다 (D5-1)
@Entity
@Table(name = "monthly_budgets", catalog = "budget")
public class MonthlyBudget {

    static final long MAX_AMOUNT = 999_999_999L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long storeId;

    // 그 달 1일. YEAR_MONTH는 MySQL 예약어라 칸 이름은 budget_month (04 설계 D5-10)
    @Column(name = "budget_month")
    private LocalDate month;

    // 총액. Money의 amount가 amount 칸에 들어간다 (04 설계 5.8)
    @Embedded
    private Money total;

    // 값 컬렉션 (04 설계 D5-2). cascade 없이 월 예산 save 한 번에 budget_lines도 함께 저장된다
    @ElementCollection
    @CollectionTable(name = "budget_lines", catalog = "budget", joinColumns = @JoinColumn(name = "monthly_budget_id"))
    private List<BudgetLine> lines = new ArrayList<>();

    // JPA가 DB에서 읽어 온 값을 채울 때 쓰는 빈 생성자. 우리 코드는 set()으로만 만든다
    protected MonthlyBudget() {
    }

    private MonthlyBudget(Long storeId, YearMonth yearMonth, Money total, List<BudgetLine> lines) {
        this.storeId = storeId;
        this.month = yearMonth.atDay(1);
        this.total = total;
        this.lines.addAll(lines);
    }

    public static MonthlyBudget set(Long storeId, YearMonth yearMonth, Long total, List<BudgetLineInput> lines,
            YearMonth thisMonth) {
        Money validTotal = validAmount(total);
        return new MonthlyBudget(storeId, validMonth(yearMonth, thisMonth), validTotal, validLines(lines, validTotal));
    }

    // 고칠 수 있는 달은 이번 달 · 다음 달 (04 설계 5.3, 기획서 P5-1). thisMonth는 응용 서비스가 Clock으로 구해 넘긴다
    private static YearMonth validMonth(YearMonth yearMonth, YearMonth thisMonth) {
        if (!yearMonth.equals(thisMonth) && !yearMonth.equals(thisMonth.plusMonths(1))) {
            throw new IllegalArgumentException("only this month or next month can be set");
        }
        return yearMonth;
    }

    // 항목 규칙: 금액은 총액과 같은 범위, 한 비용 분류에 항목 하나, 합 ≤ 총액 (같으면 허용). 차이는 미배정 (04 설계 5.3)
    private static List<BudgetLine> validLines(List<BudgetLineInput> lines, Money total) {
        List<BudgetLine> result = lines.stream()
                .map(l -> BudgetLine.of(l.categoryId(), validAmount(l.amount()).getAmount()))
                .toList();
        if (result.stream().map(BudgetLine::getCategoryId).distinct().count() < result.size()) {
            throw new IllegalArgumentException("one line per category");
        }
        if (sum(result) > total.getAmount()) {
            throw new IllegalArgumentException("sum of lines must not exceed total");
        }
        return result;
    }

    private static long sum(List<BudgetLine> lines) {
        return lines.stream().mapToLong(l -> l.getAmount().getAmount()).sum();
    }

    // 금액 규칙: 총액 · 항목 금액 모두 1 이상 999,999,999 이하 (04 설계 5.3). 상한은 입력 규칙이라 Money가 아닌 여기서 본다 (0.8)
    private static Money validAmount(Long amount) {
        if (amount < 1 || amount > MAX_AMOUNT) {
            throw new IllegalArgumentException("amount must be between 1 and " + MAX_AMOUNT);
        }
        return Money.create(amount);
    }

    // 그 상위의 항목. 없으면 빈 값
    public Optional<BudgetLine> lineOf(Long categoryId) {
        return lines.stream().filter(l -> l.getCategoryId().equals(categoryId)).findFirst();
    }

    // 미배정 = 총액 − 항목 합 (04 설계 5.4)
    public Money unassigned() {
        return Money.create(total.getAmount() - sum(lines));
    }

    public YearMonth yearMonth() {
        return YearMonth.from(month);
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Money getTotal() {
        return total;
    }

    public List<BudgetLine> getLines() {
        return List.copyOf(lines);
    }
}
