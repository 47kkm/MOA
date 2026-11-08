package moa.shared;

public class Money {
    private long amount;

    private Money(long amount) {
        this.amount = amount;
    }

    public static Money create(long amount) {
        return new Money(amount);
    }

    public long getAmount() {
        return amount;
    }
}
