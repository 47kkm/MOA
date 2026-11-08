package moa.shared;

public class Money {
    private long amount;

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

    public long getAmount() {
        return amount;
    }
}
