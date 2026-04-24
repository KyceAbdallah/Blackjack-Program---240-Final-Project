import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.LongSupplier;

public class GameController {
    private static final int MIN_BET = 10;
    private static final int DEFAULT_BANKROLL = 500;

    private final Random random;
    private final DeckShoe shoe;
    private final Player player;
    private final Dealer dealer = new Dealer("Dealer");
    private final SaveManager saveManager;
    private final LongSupplier clock;
    private final String loadWarning;
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
    private boolean playerCheatedThisRound;
    private boolean railTurnsActive;
    private boolean duelActive;
    private boolean duelCanDraw;
    private String statusText = "Booting the saloon...";
    private String eventText = "DON'T GAMBLE. This simulation uses fake money.";
    private long duelDrawOpenedAt;
    private int nextRailPatronIndex;
    private String pendingRoundSummary = "";
    private boolean pendingAnyLoss;

    public GameController() {
        this(new Random(), new SaveManager(), System::currentTimeMillis);
    }

    GameController(Random random, SaveManager saveManager, LongSupplier clock) {
        this(random, new DeckShoe(6, random), saveManager, clock);
    }

    GameController(Random random, DeckShoe shoe, SaveManager saveManager, LongSupplier clock) {
        this.random = random;
        this.shoe = shoe;
        this.saveManager = saveManager;
        this.clock = clock;

        SaveManager.SaveData saveData = saveManager.load();
        loadWarning = saveManager.getLastErrorMessage();
        player = new Player("Player", saveData.bankroll());
        player.setBestStreak(saveData.bestStreak());
        player.setStreak(saveData.streak());
        player.setSuspicion(saveData.suspicion());
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
            + "\n\nFake money only. Real ego damage still applies."
            + appendNotice(loadWarning);
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
            !loading && !roundActive && !railTurnsActive && !duelActive,
            canHit(),
            canStand(),
            canDouble(),
            canSplit(),
            canCheat(),
            railTurnsActive,
            duelActive,
            duelCanDraw,
            loading
        );
    }

    public void setBetAndDeal(int bet) {
        if (loading || roundActive || duelActive) {
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
        playerCheatedThisRound = false;
        railTurnsActive = false;
        pendingRoundSummary = "";
        pendingAnyLoss = false;
        nextRailPatronIndex = 0;
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
        playerCheatedThisRound = true;
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
        if (player.getSuspicion() >= 85) {
            eventText += "\n\n" + currentOpponent.getName() + " is watching your hands like a hawk.";
        }
    }

    public void startDuelSequence() {
        startDuelSequence(
            "A hush rolls across the felt.",
            "Rail watch: " + buildRailSummary()
        );
    }

    public void armDuel() {
        if (!duelActive || duelCanDraw) {
            return;
        }
        duelCanDraw = true;
        duelDrawOpenedAt = clock.getAsLong();
        statusText = "Hands hover above the holsters.";
        eventText = currentOpponent.getName()
            + " twitches first.\n\nDraw now.\n\n"
            + "Beat " + currentOpponent.getName() + "'s pace or lose the table.";
    }

    public void drawDuel() {
        if (!duelActive) {
            statusText = "No one is drawing on you right now.";
            return;
        }

        if (!duelCanDraw) {
            resolveDuel(false, -1, true);
            return;
        }

        int reactionTime = (int) Math.max(0L, clock.getAsLong() - duelDrawOpenedAt);
        int opponentWindow = Math.max(
            120,
            currentOpponent.getDuelDifficulty() + random.nextInt(121) - currentOpponent.getAggression() * 12
        );
        resolveDuel(reactionTime <= opponentWindow, reactionTime, false);
    }

    public boolean hasPendingRailTurns() {
        return railTurnsActive;
    }

    public void advanceRailTurn() {
        if (!railTurnsActive) {
            return;
        }

        if (nextRailPatronIndex >= railPatrons.size()) {
            railTurnsActive = false;
            finalizeRound(pendingRoundSummary, pendingAnyLoss);
            return;
        }

        RailPatron railPatron = railPatrons.get(nextRailPatronIndex++);
        int dealerUpCard = dealer.getPrimaryHand().getCards().isEmpty()
            ? 0
            : dealer.getPrimaryHand().getCards().get(0).getValue();
        railPatron.playRound(shoe, dealerUpCard, random);
        railPatron.settleAgainst(dealer.getPrimaryHand());

        statusText = railPatron.snapshot().name() + " plays out the hand.";
        eventText = railPatron.getSummary();

        if (nextRailPatronIndex >= railPatrons.size()) {
            eventText += "\n\nThe rail settles down and the next hand can begin.";
        } else {
            eventText += "\n\nNext up: " + railPatrons.get(nextRailPatronIndex).snapshot().name() + ".";
        }
    }

    public void resetSave() {
        player.setBankroll(DEFAULT_BANKROLL);
        player.setBestStreak(0);
        player.setStreak(0);
        player.setSuspicion(0);
        wins = 0;
        losses = 0;
        currentOpponent = randomOpponent();
        roundActive = false;
        roundResolved = false;
        railTurnsActive = false;
        duelActive = false;
        duelCanDraw = false;
        playerCheatedThisRound = false;
        nextRailPatronIndex = 0;
        pendingRoundSummary = "";
        pendingAnyLoss = false;
        dealer.resetHands();
        player.resetHands();
        statusText = "Fresh bankroll. Fresh lies.";
        eventText = "High scores wiped from the chalkboard.";
        for (RailPatron railPatron : railPatrons) {
            railPatron.resetSession();
        }
        persistProgress();
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
        if (shouldStartDuelAfterRound()) {
            startDuelSequence(summary.toString(), "Rail watch: " + buildRailSummary());
            return;
        }
        beginRailTurns(summary.toString(), anyLoss, dealerValue, dealerBust);
    }

    private void startDuelSequence(String roundSummary, String railSummary) {
        duelActive = true;
        duelCanDraw = false;
        duelDrawOpenedAt = 0L;
        railTurnsActive = false;
        roundActive = false;
        roundResolved = true;
        activeHandIndex = 0;
        statusText = currentOpponent.getName() + " calls you out.";
        eventText = roundSummary
            + "\n\n"
            + currentOpponent.getName()
            + " slams a palm on the rail and calls cheat."
            + "\n\n"
            + "Wait for the draw cue, then hit Draw or SPACE."
            + "\n\n"
            + railSummary;
    }

    private void resolveDuel(boolean playerWon, int reactionTime, boolean earlyDraw) {
        duelActive = false;
        duelCanDraw = false;
        railTurnsActive = false;
        int purse = 60 + currentOpponent.getAggression() * 15;
        int penalty = Math.min(Math.max(75, player.getBankroll() / 5), Math.max(75, player.getBankroll()));

        if (playerWon) {
            player.adjustBankroll(purse);
            player.recordWin();
            player.coolSuspicion(40);
            wins++;
            statusText = "You outdraw " + currentOpponent.getName() + ".";
            eventText = "Your hand clears leather in " + reactionTime + "ms."
                + "\n\nThe room backs down and a pouch with $" + purse + " slides your way."
                + "\n\nA new opponent takes the empty chair.";
        } else {
            int actualPenalty = Math.min(penalty, player.getBankroll());
            player.adjustBankroll(-actualPenalty);
            player.recordLoss();
            player.setSuspicion(0);
            losses++;
            statusText = earlyDraw ? "You flinch early." : currentOpponent.getName() + " wins the draw.";
            eventText = earlyDraw
                ? currentOpponent.getName() + " catches your jump before the signal."
                    + "\n\nThe house scoops $" + actualPenalty + " off your stack."
                : currentOpponent.getName() + " beats your " + reactionTime + "ms draw."
                    + "\n\nThe house scoops $" + actualPenalty + " off your stack.";
        }

        if (player.getBankroll() < MIN_BET) {
            player.adjustBankroll(150);
            eventText += "\n\nThe bartender fronts you $150 to keep the game alive.";
        }

        currentOpponent = randomOpponent();
        persistProgress();
    }

    private void finalizeRound(String roundSummary, boolean anyLoss) {
        railTurnsActive = false;
        eventText = "Press Deal for the next hand.\n\nRail watch: " + buildRailSummary();
        player.coolSuspicion(anyLoss ? 8 : 4);

        if (player.getBankroll() < MIN_BET) {
            player.adjustBankroll(150);
            eventText += "\n\nThe bartender fronts you $150 to keep the game alive.";
        } else if (player.getStreak() >= 3) {
            eventText += "\n\nA fresh opponent steps in while the old one drags the table upright.";
            currentOpponent = randomOpponent();
        }

        if (!roundSummary.isBlank()) {
            statusText = roundSummary;
        }
        persistProgress();
    }

    private void beginRailTurns(String roundSummary, boolean anyLoss, int dealerValue, boolean dealerBust) {
        pendingRoundSummary = roundSummary;
        pendingAnyLoss = anyLoss;
        nextRailPatronIndex = 0;

        if (railPatrons.isEmpty()) {
            finalizeRound(roundSummary, anyLoss);
            return;
        }

        railTurnsActive = true;
        statusText = roundSummary;
        eventText = "Dealer settles on "
            + dealerValue
            + (dealerBust ? " and busts." : ".")
            + "\n\nRail turns begin with "
            + railPatrons.get(0).snapshot().name()
            + ".";
    }

    private boolean shouldStartDuelAfterRound() {
        return playerCheatedThisRound
            && player.getSuspicion() >= 35
            && shouldTriggerAccusation(player.getSuspicion());
    }

    private boolean shouldTriggerAccusation(int suspicion) {
        int chance = suspicion + currentOpponent.getSuspicionSensitivity() * 7 + currentOpponent.getAggression() * 8;
        if (player.getStreak() >= 3) {
            chance += 20;
        }
        return random.nextInt(100) < Math.min(chance, 95);
    }

    private boolean canDouble() {
        if (!roundActive || duelActive || railTurnsActive || activeHandIndex >= player.getHands().size()) {
            return false;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        return hand.getCards().size() == 2 && player.getBankroll() >= player.getBetForHand(activeHandIndex);
    }

    private boolean canSplit() {
        if (!roundActive || duelActive || railTurnsActive || activeHandIndex >= player.getHands().size()) {
            return false;
        }
        Hand hand = player.getHands().get(activeHandIndex);
        return hand.canSplit() && player.getBankroll() >= player.getBetForHand(activeHandIndex);
    }

    private boolean canCheat() {
        return roundActive && !duelActive && !railTurnsActive
            && activeHandIndex < player.getHands().size() && player.getSuspicion() < 100;
    }

    private boolean canHit() {
        return roundActive && !duelActive && !railTurnsActive && activeHandIndex < player.getHands().size();
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

    private void persistProgress() {
        saveManager.save(player, wins, losses);
        String saveWarning = saveManager.getLastErrorMessage();
        if (!saveWarning.isBlank()) {
            eventText += appendNotice(saveWarning);
        }
    }

    private String appendNotice(String notice) {
        return notice == null || notice.isBlank() ? "" : "\n\n" + notice;
    }
}
