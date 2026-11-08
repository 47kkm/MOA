package moa.expense.internal;

import java.time.LocalDate;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import moa.shared.Money;

// 애그리거트는 비용 한 건 (04 설계 4.4). 비용 분류는 categoryId로만 가리킨다
@Entity
@Table(name = "expenses", catalog = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long storeId;

    private Long categoryId;

    // Money의 amount가 amount 칸에 들어간다. DECIMAL(15,0) 지정은 Money 쪽에 있다 (04 설계 0.8)
    @Embedded
    private Money amount;

    private LocalDate expenseDate;

    private String memo;

    @Enumerated(EnumType.STRING)
    private ClassifiedBy classifiedBy;

    private boolean approved;

    // JPA가 DB에서 읽어 온 값을 채울 때 쓰는 빈 생성자. 우리 코드는 register()로만 만든다
    protected Expense() {
    }

    private Expense(Long storeId, Long categoryId, Money amount, LocalDate expenseDate, String memo) {
        this.storeId = storeId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.memo = memo;
        this.classifiedBy = ClassifiedBy.USER;
        this.approved = true;
    }

    // Phase 1은 사장님이 직접 고르므로 USER · 승인됨 (04 설계 D4-7)
    public static Expense register(Long storeId, Long categoryId, Long amount, LocalDate date, String memo,
            LocalDate today) {
        return new Expense(storeId, categoryId, Money.create(amount), date, memo);
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Money getAmount() {
        return amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getMemo() {
        return memo;
    }

    public ClassifiedBy getClassifiedBy() {
        return classifiedBy;
    }

    public boolean isApproved() {
        return approved;
    }
}
