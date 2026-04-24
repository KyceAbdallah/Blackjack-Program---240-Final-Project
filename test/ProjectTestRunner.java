import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.LongSupplier;

public class ProjectTestRunner {
    public static void main(String[] args) throws Exception {
        run("save manager persists streak and suspicion", ProjectTestRunner::testSaveManagerPersistsExpandedState);
        run("blackjack round pays correctly", ProjectTestRunner::testBlackjackPayout);
        run("push returns the original bet", ProjectTestRunner::testPushRound);
        run("double down win pays doubled bet", ProjectTestRunner::testDoubleDownWin);
        run("split creates two playable hands", ProjectTestRunner::testSplitCreatesHands);
        run("duel flow resolves when the player draws in time", ProjectTestRunner::testDuelResolution);
        System.out.println("All tests passed.");
    }

    private static void testSaveManagerPersistsExpandedState() throws IOException {
        Path dir = Files.createTempDirectory("bj-save-test");
        Path savePath = dir.resolve("save.properties");
        SaveManager saveManager = new SaveManager(savePath);

        Player player = new Player("Tester", 640);
        player.setBestStreak(5);
        player.setStreak(3);
        player.setSuspicion(42);
        saveManager.save(player, 8, 2);

        SaveManager.SaveData loaded = saveManager.load();
        assertEquals(640, loaded.bankroll(), "bankroll should round-trip");
        assertEquals(5, loaded.bestStreak(), "best streak should round-trip");
        assertEquals(8, loaded.wins(), "wins should round-trip");
        assertEquals(2, loaded.losses(), "losses should round-trip");
        assertEquals(3, loaded.streak(), "current streak should round-trip");
        assertEquals(42, loaded.suspicion(), "suspicion should round-trip");
    }

    private static void testBlackjackPayout() throws IOException {
        GameController controller = freshController();
        DeckShoe shoe = new DeckShoe(1, new Random(0));
        shoe.stackCardsForNextDeals(
            card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE), card(Rank.ACE), card(Rank.NINE),
            card(Rank.SIX), card(Rank.SEVEN), card(Rank.EIGHT), card(Rank.NINE), card(Rank.KING), card(Rank.SEVEN)
        );
        controller = controllerFor(shoe, 0L);

        controller.finishLoading();
        controller.setBetAndDeal(50);

        GameSnapshot snapshot = controller.getSnapshot();
        assertEquals(575, snapshot.bankroll(), "blackjack should pay 3:2 plus return the bet");
        assertEquals(1, snapshot.wins(), "blackjack should record a win");
        assertFalse(snapshot.roundActive(), "blackjack should resolve the round immediately");
    }

    private static void testPushRound() throws IOException {
        DeckShoe shoe = new DeckShoe(1, new Random(0));
        shoe.stackCardsForNextDeals(
            card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE), card(Rank.TEN), card(Rank.NINE),
            card(Rank.SIX), card(Rank.SEVEN), card(Rank.EIGHT), card(Rank.NINE), card(Rank.EIGHT), card(Rank.NINE)
        );
        GameController controller = controllerFor(shoe, 0L);

        controller.finishLoading();
        controller.setBetAndDeal(40);
        controller.stand();
        drainRailTurns(controller);

        GameSnapshot snapshot = controller.getSnapshot();
        assertEquals(500, snapshot.bankroll(), "push should return the original bet");
        assertEquals(0, snapshot.wins(), "push should not count as a win");
        assertEquals(0, snapshot.losses(), "push should not count as a loss");
    }

    private static void testDoubleDownWin() throws IOException {
        DeckShoe shoe = new DeckShoe(1, new Random(0));
        shoe.stackCardsForNextDeals(
            card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE), card(Rank.FIVE), card(Rank.SIX),
            card(Rank.SEVEN), card(Rank.EIGHT), card(Rank.NINE), card(Rank.TEN), card(Rank.SIX), card(Rank.TEN)
        );
        GameController controller = controllerFor(shoe, 0L);

        controller.finishLoading();
        controller.setBetAndDeal(25);
        shoe.stackCardsForNextDeals(card(Rank.TEN), card(Rank.TWO));
        controller.doubleDown();
        drainRailTurns(controller);

        GameSnapshot snapshot = controller.getSnapshot();
        assertEquals(550, snapshot.bankroll(), "double-down win should pay against the doubled stake");
        assertEquals(1, snapshot.wins(), "double-down win should count as a win");
        assertFalse(snapshot.roundActive(), "double-down win should resolve after dealer play");
    }

    private static void testSplitCreatesHands() throws IOException {
        DeckShoe shoe = new DeckShoe(1, new Random(0));
        shoe.stackCardsForNextDeals(
            card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE), card(Rank.EIGHT), card(Rank.SIX),
            card(Rank.SEVEN), card(Rank.NINE), card(Rank.TEN), card(Rank.JACK), card(Rank.EIGHT), card(Rank.TEN)
        );
        GameController controller = controllerFor(shoe, 0L);

        controller.finishLoading();
        controller.setBetAndDeal(30);
        shoe.stackCardsForNextDeals(card(Rank.TWO), card(Rank.THREE));
        controller.split();

        GameSnapshot snapshot = controller.getSnapshot();
        assertEquals(2, snapshot.playerHands().size(), "split should create two hands");
        assertEquals(2, snapshot.handBets().size(), "split should track a bet for both hands");
        assertEquals(440, snapshot.bankroll(), "split should reserve the second bet from bankroll");
    }

    private static void testDuelResolution() throws IOException {
        MutableClock clock = new MutableClock(1_000L);
        GameController controller = controllerFor(new DeckShoe(1, new Random(0)), clock);

        controller.finishLoading();
        controller.startDuelSequence();
        controller.armDuel();
        clock.set(1_060L);
        controller.drawDuel();

        GameSnapshot snapshot = controller.getSnapshot();
        assertFalse(snapshot.duelActive(), "duel should resolve after drawing");
        assertTrue(snapshot.bankroll() > 500, "winning the duel should award money");
        assertEquals(1, snapshot.wins(), "winning the duel should record a win");
    }

    private static void drainRailTurns(GameController controller) {
        while (controller.hasPendingRailTurns()) {
            controller.advanceRailTurn();
        }
    }

    private static GameController controllerFor(DeckShoe shoe, long now) throws IOException {
        return controllerFor(shoe, new MutableClock(now));
    }

    private static GameController controllerFor(DeckShoe shoe, LongSupplier clock) throws IOException {
        Path dir = Files.createTempDirectory("bj-controller-test");
        SaveManager saveManager = new SaveManager(dir.resolve("save.properties"));
        return new GameController(new Random(0), shoe, saveManager, clock);
    }

    private static GameController freshController() throws IOException {
        return controllerFor(new DeckShoe(1, new Random(0)), 0L);
    }

    private static Card card(Rank rank) {
        return new Card(Suit.SPADES, rank);
    }

    private static void run(String name, ThrowingRunnable test) throws Exception {
        try {
            test.run();
            System.out.println("PASS: " + name);
        } catch (AssertionError error) {
            System.err.println("FAIL: " + name);
            throw error;
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " Expected " + expected + " but was " + actual + ".");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class MutableClock implements LongSupplier {
        private long now;

        private MutableClock(long now) {
            this.now = now;
        }

        @Override
        public long getAsLong() {
            return now;
        }

        private void set(long now) {
            this.now = now;
        }
    }
}
