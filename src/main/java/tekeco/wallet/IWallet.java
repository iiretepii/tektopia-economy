package tekeco.wallet;

/** Emeralds held by one villager, plus when they were last paid. */
public interface IWallet {
    /** Day number for a villager the payroll hasn't seen yet. */
    long NEVER = -1L;

    int getCoins();

    void setCoins(int coins);

    void add(int amount);

    /** Removes {@code amount} if the wallet holds that much; otherwise leaves it unchanged. */
    boolean take(int amount);

    long getLastPaidDay();

    void setLastPaidDay(long day);

    int getLastWage();

    void setLastWage(int wage);

    /** The last day this villager paid a Tavern tab or was turned away, or {@link #NEVER}. */
    long getTavernDay();

    void setTavernDay(long day);

    /** True while this mod has switched off the villager's Tavern visits because they couldn't pay. */
    boolean isTavernBlocked();

    void setTavernBlocked(boolean blocked);

    /** True if the treasury couldn't pay this villager at their last payday. */
    boolean wasUnpaid();

    /** True if this villager couldn't afford rent and food at their last payday. */
    boolean wasBroke();

    void setPaydayResult(boolean unpaid, boolean broke);

    default boolean isNew() {
        return getLastPaidDay() == NEVER;
    }

    default void copyFrom(IWallet other) {
        setCoins(other.getCoins());
        setLastPaidDay(other.getLastPaidDay());
        setLastWage(other.getLastWage());
        setTavernDay(other.getTavernDay());
        setTavernBlocked(other.isTavernBlocked());
        setPaydayResult(other.wasUnpaid(), other.wasBroke());
    }
}
