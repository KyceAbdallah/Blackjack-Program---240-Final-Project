import java.util.Random;

class RailPatron {
    private final String name;
    private final int openingStack;
    private final int tableMinimum;
    private final int preferredBet;
    // Strategy pattern: each rail player delegates betting and table decisions to a play style.
    private final RailPlayStrategy strategy;

    private Hand hand = new Hand();
    private int stack;
    private int currentBet;
    private String status = "Waiting";
    private String summary;

    RailPatron(String name, int openingStack, int preferredBet, RailPlayStrategy strategy) {
        this(name, openingStack, 10, preferredBet, strategy);
    }

    RailPatron(String name, int openingStack, int tableMinimum, int preferredBet, RailPlayStrategy strategy) {
        this.name = name;
        this.openingStack = openingStack;
        this.tableMinimum = tableMinimum;
        this.preferredBet = preferredBet;
        this.strategy = strategy;
        this.stack = openingStack;
        clearTable();
    }

    void resetSession() {
        stack = openingStack;
        clearTable();
    }

    void clearTable() {
        hand = new Hand();
        currentBet = 0;
        status = "Waiting";
        summary = name + " waits for the next hand.";
    }

    void openRound(Random random) {
        if (stack < tableMinimum) {
            stack += 90 + random.nextInt(91);
        }
        hand = new Hand();
        currentBet = strategy.chooseBet(stack, tableMinimum, preferredBet, random);
        status = "Bet $" + currentBet;
        summary = name + " splashes $" + currentBet + " onto the felt.";
    }

    void deal(Card card) {
        hand.addCard(card);
    }

    void deal(DeckShoe shoe) {
        deal(shoe.deal());
    }

    void playRound(DeckShoe shoe, int dealerUpCard, Random random) {
        if (hand.getCards().isEmpty()) {
            clearTable();
            return;
        }

        if (hand.isBlackjack()) {
            status = "Blackjack";
            summary = name + " flips a blackjack.";
            return;
        }

        RailTurnPlan turnPlan = strategy.createTurnPlan(hand, dealerUpCard, stack, currentBet, random);
        StringBuilder actionBuilder = new StringBuilder();

        if (turnPlan.pressAmount() > 0 && currentBet + turnPlan.pressAmount() <= stack) {
            currentBet += turnPlan.pressAmount();
            actionBuilder.append(name).append(" presses the bet to $").append(currentBet).append(". ");
        }

        if (turnPlan.cheat()) {
            Hand.CheatSwap cheatSwap = hand.findBestCheatSwap();
            if (cheatSwap != null && cheatSwap.valueAfter() != cheatSwap.valueBefore()) {
                hand.replaceCard(cheatSwap.index(), cheatSwap.replacement());
                actionBuilder.append(name)
                    .append(" palms a ")
                    .append(cheatSwap.replacement())
                    .append(" and shifts from ")
                    .append(cheatSwap.valueBefore())
                    .append(" to ")
                    .append(cheatSwap.valueAfter())
                    .append(". ");
            }
        }

        if (turnPlan.doubleDown()) {
            currentBet *= 2;
            hand.doubleDown();
            deal(shoe);
            if (hand.isBust()) {
                status = "Bust " + hand.getValue();
                summary = actionBuilder + name + " doubles and burns out with " + hand.getValue() + ".";
            } else {
                status = "Double " + hand.getValue();
                summary = actionBuilder + name + " doubles down to " + hand.getValue() + ".";
            }
            return;
        }

        int target = turnPlan.standThreshold();
        while (hand.getValue() < target && !hand.isBust()) {
            deal(shoe);
            if (hand.getValue() >= 21) {
                break;
            }
        }

        if (hand.isBust()) {
            status = "Bust " + hand.getValue();
            summary = actionBuilder + name + " busts with " + hand.getValue() + ".";
        } else if (hand.getValue() == 21) {
            hand.stand();
            status = "Stand 21";
            summary = actionBuilder + name + " lands clean on 21.";
        } else {
            hand.stand();
            status = "Stand " + hand.getValue();
            summary = actionBuilder + name + " stands on " + hand.getValue() + ".";
        }
    }

    void settleAgainst(Hand dealerHand) {
        if (currentBet <= 0 || hand.getCards().isEmpty()) {
            return;
        }

        int value = hand.getValue();
        int dealerValue = dealerHand.getValue();
        boolean dealerBust = dealerHand.isBust();

        if (hand.isBust()) {
            stack -= currentBet;
            status = "Lost " + value;
            summary = name + " loses after busting " + value + ".";
            return;
        }

        if (hand.isBlackjack() && !dealerHand.isBlackjack()) {
            stack += (int) Math.round(currentBet * 1.5);
            status = "BJ wins";
            summary = name + " gets paid on blackjack.";
            return;
        }

        if (dealerBust || value > dealerValue) {
            stack += currentBet;
            status = "Wins " + value;
            summary = name + " beats the house with " + value + ".";
            return;
        }

        if (value == dealerValue) {
            status = "Push " + value;
            summary = name + " pushes with " + value + ".";
            return;
        }

        stack -= currentBet;
        status = "Lost " + value;
        summary = name + " loses with " + value + ".";
    }

    String getSummary() {
        return summary;
    }

    RailSeatSnapshot snapshot() {
        return new RailSeatSnapshot(name, hand.describe(true), hand.getValue(), currentBet, status);
    }
}
