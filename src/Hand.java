import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {
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
}
