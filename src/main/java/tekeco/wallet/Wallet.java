package tekeco.wallet;

public class Wallet implements IWallet {
    private int coins;
    private long lastPaidDay = NEVER;
    private int lastWage;
    private long tavernDay = NEVER;
    private boolean tavernBlocked;
    private boolean unpaid;
    private boolean broke;

    @Override
    public int getCoins() {
        return coins;
    }

    @Override
    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
    }

    @Override
    public void add(int amount) {
        if (amount > 0) {
            coins += amount;
        }
    }

    @Override
    public boolean take(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
    }

    @Override
    public long getLastPaidDay() {
        return lastPaidDay;
    }

    @Override
    public void setLastPaidDay(long day) {
        this.lastPaidDay = day;
    }

    @Override
    public int getLastWage() {
        return lastWage;
    }

    @Override
    public void setLastWage(int wage) {
        this.lastWage = wage;
    }

    @Override
    public long getTavernDay() {
        return tavernDay;
    }

    @Override
    public void setTavernDay(long day) {
        this.tavernDay = day;
    }

    @Override
    public boolean isTavernBlocked() {
        return tavernBlocked;
    }

    @Override
    public void setTavernBlocked(boolean blocked) {
        this.tavernBlocked = blocked;
    }

    @Override
    public boolean wasUnpaid() {
        return unpaid;
    }

    @Override
    public boolean wasBroke() {
        return broke;
    }

    @Override
    public void setPaydayResult(boolean unpaid, boolean broke) {
        this.unpaid = unpaid;
        this.broke = broke;
    }
}
