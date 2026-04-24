import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DeckShoe {
    private final Random random;
    private final List<Card> cards = new ArrayList<>();
    private final int deckCount;

    public DeckShoe(int deckCount) {
        this(deckCount, new Random());
    }

    public DeckShoe(int deckCount, Random random) {
        this.deckCount = Math.max(1, deckCount);
        this.random = random;
        refillAndShuffle();
    }

    public void refillAndShuffle() {
        cards.clear();
        for (int deck = 0; deck < deckCount; deck++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    cards.add(new Card(suit, rank));
                }
            }
        }
        Collections.shuffle(cards, random);
    }

    public Card deal() {
        if (cards.size() < 15) {
            refillAndShuffle();
        }
        return cards.remove(cards.size() - 1);
    }

    public Card dealCheatCard() {
        Rank[] goodRanks = {Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE};
        return new Card(Suit.values()[random.nextInt(Suit.values().length)],
            goodRanks[random.nextInt(goodRanks.length)]);
    }

    public int remainingCards() {
        return cards.size();
    }

    void stackCardsForNextDeals(Card... nextCards) {
        for (int i = nextCards.length - 1; i >= 0; i--) {
            cards.add(nextCards[i]);
        }
    }
}
