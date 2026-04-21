import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {
    public record CheatSwap(
        int index,
        Card original,
        Card replacement,
        int valueBefore,
        int valueAfter
    ) {
    }

    private final List<Card> cards = new ArrayList<>();
    private boolean stood;
    private boolean doubledDown;

    public void addCard(Card card) {
        cards.add(card);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public int getValue() {
        int total = 0;
        int aces = 0;
        for (Card card : cards) {
            total += card.getValue();
            if (card.getRank() == Rank.ACE) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }

    public boolean isBust() {
        return getValue() > 21;
    }

    public boolean canSplit() {
        return cards.size() == 2 && cards.get(0).getRank() == cards.get(1).getRank();
    }

    public Card removeSecondCard() {
        return cards.remove(1);
    }

    public void replaceCard(int index, Card replacement) {
        cards.set(index, replacement);
    }

    public void replaceLowestCard(Card replacement) {
        if (cards.isEmpty()) {
            cards.add(replacement);
            return;
        }
        int lowestIndex = 0;
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i).getValue() < cards.get(lowestIndex).getValue()) {
                lowestIndex = i;
            }
        }
        cards.set(lowestIndex, replacement);
    }

    public CheatSwap findBestCheatSwap() {
        if (cards.isEmpty()) {
            return null;
        }

        int currentValue = getValue();
        CheatSwap best = null;
        for (int i = 0; i < cards.size(); i++) {
            Card original = cards.get(i);
            for (Rank rank : Rank.values()) {
                if (rank == original.getRank()) {
                    continue;
                }
                Card replacement = new Card(original.getSuit(), rank);
                int candidateValue = valueWithReplacement(i, replacement);
                CheatSwap candidate = new CheatSwap(i, original, replacement, currentValue, candidateValue);
                if (isBetterCheat(candidate, best)) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    public boolean hasStood() {
        return stood;
    }

    public void stand() {
        stood = true;
    }

    public boolean isDoubledDown() {
        return doubledDown;
    }

    public void doubleDown() {
        doubledDown = true;
    }

    public String describe(boolean revealAll) {
        if (cards.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            if (!revealAll && i == 1) {
                builder.append("[?]");
            } else {
                builder.append('[').append(cards.get(i)).append(']');
            }
        }
        return builder.toString();
    }

    private int valueWithReplacement(int replaceIndex, Card replacement) {
        int total = 0;
        int aces = 0;
        for (int i = 0; i < cards.size(); i++) {
            Card card = i == replaceIndex ? replacement : cards.get(i);
            total += card.getValue();
            if (card.getRank() == Rank.ACE) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    private boolean isBetterCheat(CheatSwap candidate, CheatSwap best) {
        if (best == null) {
            return true;
        }

        int candidateDiff = Math.abs(21 - candidate.valueAfter());
        int bestDiff = Math.abs(21 - best.valueAfter());
        if (candidateDiff != bestDiff) {
            return candidateDiff < bestDiff;
        }

        boolean candidateBust = candidate.valueAfter() > 21;
        boolean bestBust = best.valueAfter() > 21;
        if (candidateBust != bestBust) {
            return !candidateBust;
        }

        if (!candidateBust && candidate.valueAfter() != best.valueAfter()) {
            return candidate.valueAfter() > best.valueAfter();
        }

        if (candidateBust && candidate.valueAfter() != best.valueAfter()) {
            return candidate.valueAfter() < best.valueAfter();
        }

        return candidate.replacement().getValue() > best.replacement().getValue();
    }
}
