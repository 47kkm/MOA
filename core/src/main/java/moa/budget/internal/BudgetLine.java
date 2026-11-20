package moa.budget.internal;

import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import moa.shared.Money;

// 예산 항목 = 월 예산 안의 값 (04 설계 5.4, D5-2). id가 없고, 같은 값이면 같다
// 월 예산의 @ElementCollection으로 budget_lines 테이블에 저장된다. 고치면 그 월 예산의 줄을 전부 지우고 다시 넣는다
@Embeddable
public class BudgetLine {

    // 상위 비용 분류. category.categories.id를 가리킨다 (FK 없음, 04 설계 0.4)
    private Long categoryId;

    @Embedded
    private Money amount;

    // JPA가 DB에서 읽어 온 값을 채울 때 쓰는 빈 생성자
    protected BudgetLine() {
    }

    private BudgetLine(Long categoryId, Money amount) {
        this.categoryId = categoryId;
        this.amount = amount;
    }

    public static BudgetLine of(Long categoryId, long amount) {
        return new BudgetLine(categoryId, Money.create(amount));
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Money getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BudgetLine line = (BudgetLine) obj;
        return Objects.equals(categoryId, line.categoryId) && Objects.equals(amount, line.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, amount);
    }
}
