package moa.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Money {
    @Column(columnDefinition = "DECIMAL(15, 0)")
    private long amount;

    protected Money() {
    }

    private Money(long amount) {
        this.amount = amount;
    }

    public static Money create(long amount) {
        return new Money(validAmount(amount));
    }

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
