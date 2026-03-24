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

    private OpponentProfile currentOpponent;
    private int wins;
    private int losses;
    private int activeHandIndex;
    private boolean roundActive;
    private boolean roundResolved;
    private boolean duelActive;
    private boolean duelCanDraw;
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
        statusText = currentOpponent.getName() + " settles into the far chair.";
        eventText = "A dusty saloon wakes up around you.\n\n"
            + currentOpponent.getIntroLine()
            + "\n\nFake money only. Real ego damage still applies.";
    }

    public GameSnapshot getSnapshot() {
        List<String> handDescriptions = new ArrayList<>();
        List<Integer> handValues = new ArrayList<>();
        List<Integer> handBets = new ArrayList<>(player.getHandBets());
        for (Hand hand : player.getHands()) {
            handDescriptions.add(hand.describe(true));
            handValues.add(hand.getValue());
        }
        while (handBets.size() < handDescriptions.size()) {
            handBets.add(0);
        }

        Hand dealerHand = dealer.getPrimaryHand();
        boolean revealDealer = !roundActive || duelActive || roundResolved;
        String dealerCards = dealerHand.describe(revealDealer);
        int dealerVisibleValue = dealerHand.getCards().isEmpty() ? 0 : dealerHand.getCards().get(0).getValue();

        return new GameSnapshot(
            currentOpponent.getName(),
            currentOpponent.getIntroLine(),
            dealerCards,
            dealerVisibleValue,
            dealerHand.getValue(),
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
            !loading && !roundActive && !duelActive,
            roundActive && !duelActive,
            roundActive && !duelActive,
            canDouble(),
            canSplit(),
            canCheat(),
            duelActive,
            duelCanDraw,
            loading
        );
    }

    public void setBetAndDeal(int bet) {
        if (loading || duelActive || roundActive) {
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

        Hand playerHand = player.getPrimaryHand();
        Hand dealerHand = dealer.getPrimaryHand();
        player.placeBaseBet(bet);
        playerHand.addCard(shoe.deal());
        dealerHand.addCard(shoe.deal());
        playerHand.addCard(shoe.deal());
        dealerHand.addCard(shoe.deal());

        statusText = "Cards dealt with a hard wooden clack.";
        eventText = "Dealer shows " + dealerHand.getCards().get(0) + ".";

        if (playerHand.isBlackjack() || dealerHand.isBlackjack()) {
            resolveRound();
        }
    }

    public void hit() {
        if (!roundActive || duelActive) {
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
        if (!roundActive || duelActive) {
            return;
        }
        player.getHands().get(activeHandIndex).stand();
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
        player.addHand(second);
        player.splitBet();
        first.addCard(shoe.deal());
        second.addCard(shoe.deal());
        statusText = "Two hands. Twice the trouble.";
    }

    public void cheat() {
        if (!canCheat()) {
            return;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        hand.replaceLowestCard(shoe.dealCheatCard());
        int suspicionSpike = 14 + currentOpponent.getSuspicionSensitivity();
        player.addSuspicion(suspicionSpike);
        statusText = "Ace up the sleeve.";
        eventText = "The room notices. Suspicion rises by " + suspicionSpike + ".";
    }

    public void startDuelSequence() {
        duelActive = true;
        duelCanDraw = false;
        roundActive = false;
        roundResolved = false;
        statusText = currentOpponent.getName() + " flips the table and calls you a cheat.";
        eventText = "Wait for DRAW, then hit the duel button or press SPACE.\nHands off the iron until then.";
    }

    public void armDuel() {
        if (!duelActive) {
            return;
        }
        duelCanDraw = true;
        duelDrawOpenedAt = System.currentTimeMillis();
        statusText = "DRAW!";
        eventText = "Now.";
    }

    public void drawDuel() {
        if (!duelActive) {
            return;
        }
        if (!duelCanDraw) {
            statusText = "Too early. Hands off the iron.";
            loseDuel();
            return;
        }

        long reactionTime = System.currentTimeMillis() - duelDrawOpenedAt;
        int graceWindow = Math.max(250, 950 - currentOpponent.getDuelDifficulty());
        if (reactionTime <= graceWindow) {
            statusText = "You won the duel. The room goes quiet.";
            eventText = "Glass shatters somewhere behind you.";
            duelActive = false;
            duelCanDraw = false;
            player.coolSuspicion(30);
            currentOpponent = randomOpponent();
            saveManager.save(player, wins, losses);
        } else {
            loseDuel();
        }
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
        saveManager.save(player, wins, losses);
    }

    private void moveToNextHandOrDealer() {
        while (activeHandIndex < player.getHands().size()) {
            Hand current = player.getHands().get(activeHandIndex);
            if (!current.hasStood() && !current.isBust() && !current.isDoubledDown() && current.getValue() < 21) {
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
        eventText = "Press Deal for the next hand.";
        player.coolSuspicion(anyLoss ? 8 : 4);

        if (shouldTriggerAccusation(player.getSuspicion())) {
            startDuelSequence();
        } else {
            if (player.getBankroll() < MIN_BET) {
                player.adjustBankroll(150);
                eventText = "The bartender fronts you $150 to keep the game alive.";
            } else if (player.getStreak() >= 3) {
                eventText = "A fresh opponent steps in while the old one drags the table upright.";
                currentOpponent = randomOpponent();
            }
            saveManager.save(player, wins, losses);
        }
    }

    private boolean shouldTriggerAccusation(int suspicion) {
        int chance = suspicion + currentOpponent.getSuspicionSensitivity() * 7 + currentOpponent.getAggression() * 8;
        if (player.getStreak() >= 3) {
            chance += 20;
        }
        return random.nextInt(100) < Math.min(chance, 95);
    }

    private boolean canDouble() {
        if (!roundActive || duelActive || activeHandIndex >= player.getHands().size()) {
            return false;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        return hand.getCards().size() == 2 && player.getBankroll() >= player.getBetForHand(activeHandIndex);
    }

    private boolean canSplit() {
        if (!roundActive || duelActive || activeHandIndex >= player.getHands().size()) {
            return false;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        return hand.canSplit() && player.getBankroll() >= player.getCurrentBet();
    }

    private boolean canCheat() {
        return roundActive && !duelActive && activeHandIndex < player.getHands().size() && player.getSuspicion() < 100;
    }

    private OpponentProfile randomOpponent() {
        return opponents.get(random.nextInt(opponents.size()));
    }

    private void loseDuel() {
        duelActive = false;
        duelCanDraw = false;
        int penalty = Math.min(120, Math.max(40, player.getCurrentBet()));
        player.adjustBankroll(-penalty);
        player.recordLoss();
        losses++;
        player.coolSuspicion(100);
        statusText = currentOpponent.getName() + " wins the duel.";
        eventText = "You lost the duel and pay a bruised-pride tax of $" + penalty + ".";
        currentOpponent = randomOpponent();
        if (player.getBankroll() < MIN_BET) {
            player.adjustBankroll(150);
        }
        saveManager.save(player, wins, losses);
    }
}
