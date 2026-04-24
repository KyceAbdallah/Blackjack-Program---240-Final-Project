import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player extends Participant implements Gambleable {
    private int bankroll;
    private int currentBet;
    private int streak;
    private int bestStreak;
    private int suspicion;
    private final List<Integer> handBets = new ArrayList<>();

    public Player(String name, int bankroll) {
        super(name);
        this.bankroll = bankroll;
    }

    @Override
    public void resetHands() {
        super.resetHands();
        handBets.clear();
        currentBet = 0;
    }

    public void placeBaseBet(int bet) {
        currentBet = bet;
        handBets.clear();
        handBets.add(bet);
        adjustBankroll(-bet);
    }

    public void splitBet() {
        handBets.add(currentBet);
        adjustBankroll(-currentBet);
    }

    public void doubleHandBet(int handIndex) {
        int extra = handBets.get(handIndex);
        handBets.set(handIndex, extra * 2);
        adjustBankroll(-extra);
    }

    public List<Integer> getHandBets() {
        return Collections.unmodifiableList(handBets);
    }

    public int getBetForHand(int handIndex) {
        return handBets.get(handIndex);
    }

    public void setBankroll(int bankroll) {
        this.bankroll = bankroll;
    }

    @Override
    public void adjustBankroll(int amount) {
        bankroll += amount;
    }

    @Override
    public int getBankroll() {
        return bankroll;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = Math.max(0, streak);
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void recordWin() {
        streak++;
        bestStreak = Math.max(bestStreak, streak);
    }

    public void recordLoss() {
        streak = 0;
    }

    public void recordPush() {
    }

    public int getSuspicion() {
        return suspicion;
    }

    public void setSuspicion(int suspicion) {
        this.suspicion = Math.max(0, Math.min(100, suspicion));
    }

    public void addSuspicion(int amount) {
        suspicion = Math.min(100, suspicion + amount);
    }

    public void coolSuspicion(int amount) {
        suspicion = Math.max(0, suspicion - amount);
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }
}
