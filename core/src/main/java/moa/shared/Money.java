package moa.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Money {
    // 없으면 validate에서 앱이 뜨지 않는다. 자바 long은 BIGINT로 보는데 DB 컬럼은 DECIMAL이라서 (04 설계 0.8)
    @Column(columnDefinition = "DECIMAL(15, 0)")
    private long amount;

    // JPA가 DB에서 읽어 온 값을 채울 때 쓰는 빈 생성자. 우리 코드는 create()로만 만든다
    protected Money() {
    }

    private Money(long amount) {
        this.amount = amount;
    }

    public static Money create(long amount) {
        return new Money(validAmount(amount));
    }

    // 금액 규칙: 음수 불가, 0은 허용 (04 설계 0.8). 9자리 상한은 여기 없다 → 합계는 9자리를 넘을 수 있어서 SalesLine·Expense에서 검사한다
    private static long validAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }

        return amount;
    }

    public Money plus(Money money) {
        return create(this.amount + money.getAmount());
    }

    public boolean isZero() {
        return this.amount == 0;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public boolean equals (Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Money money = (Money) obj;
        return amount == money.amount;
    }

    @Override
    public int hashCode () {
        return Long.hashCode(amount);
    }
}
