import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameController {
    private static final int MIN_BET = 10;

    private final Random random = new Random();
    private final DeckShoe shoe = new DeckShoe(6);
    private final Player player;
    private final Dealer dealer = new Dealer("Dealer");
    private final SaveManager saveManager = new SaveManager();
    private final List<OpponentProfile> opponents = List.of(
        new OpponentProfile("Dusty Mae", "Cards on the table. Eyes where I can see 'em.", 6, 1, 180),
        new OpponentProfile("The Drunk Miner", "Hah! I hit on hope and whiskey.", 4, 3, 260),
        new OpponentProfile("The Shady Gambler", "Three hot hands in a row and I start askin' questions.", 9, 2, 170),
        new OpponentProfile("Sheriff Boone", "Play it clean and we won't have trouble.", 7, 2, 145)
    );
    private final List<RailPatron> railPatrons = List.of(
        new RailPatron("Lucky Len", 280, 20, RailStyle.BOLD),
        new RailPatron("Velvet Ruth", 360, 35, RailStyle.CONSERVATIVE),
        new RailPatron("Tin Cup Nora", 240, 15, RailStyle.OPPORTUNIST),
        new RailPatron("Doc Mercer", 320, 25, RailStyle.BALANCED)
    );

    private OpponentProfile currentOpponent;
    private int wins;
    private int losses;
    private int activeHandIndex;
    private boolean roundActive;
    private boolean roundResolved;
    private boolean loading = true;
    private String statusText = "Booting the saloon...";
    private String eventText = "DON'T GAMBLE. This simulation uses fake money.";
    private long duelDrawOpenedAt;

    public GameController() {
        SaveManager.SaveData saveData = saveManager.load();
        player = new Player("Player", saveData.bankroll());
        player.setBestStreak(saveData.bestStreak());
        wins = saveData.wins();
        losses = saveData.losses();
        currentOpponent = randomOpponent();
    }

    public void finishLoading() {
        loading = false;
        statusText = "The table is ready.";
        eventText = currentOpponent.getName()
            + " sits down across from you.\n\n"
            + currentOpponent.getIntroLine()
            + "\n\nFour regulars are already splashing chips around the rail."
            + "\n\nFake money only. Real ego damage still applies.";
    }

    public GameSnapshot getSnapshot() {
        List<String> handDescriptions = new ArrayList<>();
        List<Integer> handValues = new ArrayList<>();
        List<Integer> handBets = new ArrayList<>(player.getHandBets());
        List<RailSeatSnapshot> railSeats = new ArrayList<>();
        for (Hand hand : player.getHands()) {
            handDescriptions.add(hand.describe(true));
            handValues.add(hand.getValue());
        }
        for (RailPatron railPatron : railPatrons) {
            railSeats.add(railPatron.snapshot());
        }
        while (handBets.size() < handDescriptions.size()) {
            handBets.add(0);
        }

        Hand dealerHand = dealer.getPrimaryHand();
        boolean revealDealer = !roundActive || roundResolved;
        String dealerCards = dealerHand.describe(revealDealer);
        int dealerVisibleValue = dealerHand.getCards().isEmpty() ? 0 : dealerHand.getCards().get(0).getValue();

        return new GameSnapshot(
            currentOpponent.getName(),
            currentOpponent.getIntroLine(),
            dealerCards,
            dealerVisibleValue,
            dealerHand.getValue(),
            railSeats,
            handDescriptions,
            handValues,
            handBets,
            activeHandIndex,
            player.getBankroll(),
            player.getStreak(),
            player.getBestStreak(),
            player.getSuspicion(),
            wins,
            losses,
            shoe.remainingCards(),
            statusText,
            eventText,
            roundActive,
            roundResolved,
            !loading && !roundActive,
            canHit(),
            canStand(),
            canDouble(),
            canSplit(),
            canCheat(),
            false,
            false,
            loading
        );
    }

    public void setBetAndDeal(int bet) {
        if (loading || roundActive) {
            return;
        }
        if (bet < MIN_BET) {
            statusText = "Minimum bet is $" + MIN_BET + ".";
            return;
        }
        if (bet > player.getBankroll()) {
            statusText = "That bet is too rich for this table.";
            return;
        }

        player.resetHands();
        dealer.resetHands();
        activeHandIndex = 0;
        roundActive = true;
        roundResolved = false;
        prepareRailRound();

        Hand playerHand = player.getPrimaryHand();
        Hand dealerHand = dealer.getPrimaryHand();
        player.placeBaseBet(bet);
        for (int i = 0; i < 2; i++) {
            for (RailPatron railPatron : railPatrons) {
                railPatron.deal(shoe);
            }
            playerHand.addCard(shoe.deal());
            dealerHand.addCard(shoe.deal());
        }
        int dealerUpCard = dealerHand.getCards().get(0).getValue();
        for (RailPatron railPatron : railPatrons) {
            railPatron.playRound(shoe, dealerUpCard, random);
        }

        statusText = "Cards dealt with a hard wooden clack.";
        eventText = "Dealer shows " + dealerHand.getCards().get(0) + ".\n\nRail watch: " + buildRailSummary();

        if (playerHand.isBlackjack() || dealerHand.isBlackjack()) {
            resolveRound();
        }
    }

    public void hit() {
        if (!canHit()) {
            return;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        hand.addCard(shoe.deal());
        statusText = "Card dealt with a hard wooden clack.";

        if (hand.isBust() || hand.getValue() == 21 || hand.isDoubledDown()) {
            moveToNextHandOrDealer();
        }
    }

    public void stand() {
        if (!canStand()) {
            return;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        hand.stand();
        statusText = "You hold on hand " + (activeHandIndex + 1) + ".";
        eventText = player.getHands().size() > 1
            ? "Hand " + (activeHandIndex + 1) + " is locked in."
            : "Dealer reaches for the shoe.";
        moveToNextHandOrDealer();
    }

    public void doubleDown() {
        if (!canDouble()) {
            return;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        player.doubleHandBet(activeHandIndex);
        hand.doubleDown();
        hand.addCard(shoe.deal());
        statusText = "You doubled down and let fate do the rest.";
        moveToNextHandOrDealer();
    }

    public void split() {
        if (!canSplit()) {
            return;
        }
        Hand first = player.getHands().get(activeHandIndex);
        Card moved = first.removeSecondCard();
        Hand second = new Hand();
        second.addCard(moved);
        player.addHand(activeHandIndex + 1, second);
        player.splitBet();
        first.addCard(shoe.deal());
        second.addCard(shoe.deal());
        statusText = "Two hands. Twice the trouble.";
        eventText = "Split complete. Play hand " + (activeHandIndex + 1)
            + " of " + player.getHands().size() + ".";
    }

    public void cheat() {
        if (!canCheat()) {
            return;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        Hand.CheatSwap cheatSwap = hand.findBestCheatSwap();
        if (cheatSwap == null) {
            return;
        }
        hand.replaceCard(cheatSwap.index(), cheatSwap.replacement());
        int suspicionSpike = 14 + currentOpponent.getSuspicionSensitivity();
        player.addSuspicion(suspicionSpike);
        statusText = "You lean the hand toward 21.";
        eventText = "You swap "
            + cheatSwap.original()
            + " for "
            + cheatSwap.replacement()
            + ", moving the hand from "
            + cheatSwap.valueBefore()
            + " to "
            + cheatSwap.valueAfter()
            + ". Suspicion rises by "
            + suspicionSpike
            + ".";
    }

    public void startDuelSequence() {
        statusText = "Duels are disabled for now.";
        eventText = "The table stays calm while we tighten up the rest of the game.";
    }

    public void armDuel() {
        statusText = "Duels are disabled for now.";
    }

    public void drawDuel() {
        statusText = "Duels are disabled for now.";
    }

    public void resetSave() {
        player.setBankroll(500);
        player.setBestStreak(0);
        player.coolSuspicion(100);
        wins = 0;
        losses = 0;
        currentOpponent = randomOpponent();
        statusText = "Fresh bankroll. Fresh lies.";
        eventText = "High scores wiped from the chalkboard.";
        for (RailPatron railPatron : railPatrons) {
            railPatron.resetSession();
        }
        saveManager.save(player, wins, losses);
    }

    private void moveToNextHandOrDealer() {
        while (activeHandIndex < player.getHands().size()) {
            Hand current = player.getHands().get(activeHandIndex);
            if (!current.hasStood() && !current.isBust() && !current.isDoubledDown() && current.getValue() < 21) {
                if (player.getHands().size() > 1) {
                    statusText = "Hand " + (activeHandIndex + 1) + " is live.";
                    eventText = "Play hand " + (activeHandIndex + 1) + " of " + player.getHands().size() + ".";
                }
                return;
            }
            activeHandIndex++;
        }

        while (dealer.shouldHit()) {
            dealer.getPrimaryHand().addCard(shoe.deal());
        }
        resolveRound();
    }

    private void resolveRound() {
        roundActive = false;
        roundResolved = true;
        activeHandIndex = 0;

        Hand dealerHand = dealer.getPrimaryHand();
        int dealerValue = dealerHand.getValue();
        boolean dealerBust = dealerHand.isBust();
        StringBuilder summary = new StringBuilder();
        boolean anyLoss = false;
        for (RailPatron railPatron : railPatrons) {
            railPatron.settleAgainst(dealerHand);
        }

        for (int i = 0; i < player.getHands().size(); i++) {
            Hand hand = player.getHands().get(i);
            int bet = player.getBetForHand(i);

            if (summary.length() > 0) {
                summary.append('\n');
            }
            summary.append("Hand ").append(i + 1);

            if (hand.isBust()) {
                summary.append(": bust.");
                player.recordLoss();
                losses++;
                anyLoss = true;
                continue;
            }

            int playerValue = hand.getValue();
            if (hand.isBlackjack() && !dealerHand.isBlackjack()) {
                player.adjustBankroll((int) Math.round(bet * 2.5));
                summary.append(": blackjack.");
                player.recordWin();
                wins++;
            } else if (dealerBust || playerValue > dealerValue) {
                player.adjustBankroll(bet * 2);
                summary.append(": win.");
                player.recordWin();
                wins++;
            } else if (playerValue == dealerValue) {
                player.adjustBankroll(bet);
                summary.append(": push.");
                player.recordPush();
            } else {
                summary.append(": loss.");
                player.recordLoss();
                losses++;
                anyLoss = true;
            }
        }

        statusText = summary.toString();
        eventText = "Press Deal for the next hand.\n\nRail watch: " + buildRailSummary();
        player.coolSuspicion(anyLoss ? 8 : 4);

        if (player.getBankroll() < MIN_BET) {
            player.adjustBankroll(150);
            eventText += "\n\nThe bartender fronts you $150 to keep the game alive.";
        } else if (player.getStreak() >= 3) {
            eventText += "\n\nA fresh opponent steps in while the old one drags the table upright.";
            currentOpponent = randomOpponent();
        }
        saveManager.save(player, wins, losses);
    }

    private boolean shouldTriggerAccusation(int suspicion) {
        int chance = suspicion + currentOpponent.getSuspicionSensitivity() * 7 + currentOpponent.getAggression() * 8;
        if (player.getStreak() >= 3) {
            chance += 20;
        }
        return random.nextInt(100) < Math.min(chance, 95);
    }

    private boolean canDouble() {
        if (!roundActive || activeHandIndex >= player.getHands().size()) {
            return false;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        return hand.getCards().size() == 2 && player.getBankroll() >= player.getBetForHand(activeHandIndex);
    }

    private boolean canSplit() {
        if (!roundActive || activeHandIndex >= player.getHands().size()) {
            return false;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        return hand.canSplit() && player.getBankroll() >= player.getBetForHand(activeHandIndex);
    }

    private boolean canCheat() {
        return roundActive && activeHandIndex < player.getHands().size() && player.getSuspicion() < 100;
    }

    private boolean canHit() {
        return roundActive && activeHandIndex < player.getHands().size();
    }

    private boolean canStand() {
        return canHit();
    }

    private OpponentProfile randomOpponent() {
        return opponents.get(random.nextInt(opponents.size()));
    }

    private void prepareRailRound() {
        for (RailPatron railPatron : railPatrons) {
            railPatron.openRound(random);
        }
    }

    private String buildRailSummary() {
        StringBuilder builder = new StringBuilder();
        for (RailPatron railPatron : railPatrons) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(railPatron.getSummary());
        }
        return builder.toString();
    }

}
