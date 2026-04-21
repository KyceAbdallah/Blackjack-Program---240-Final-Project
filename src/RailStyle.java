import java.util.Random;

enum RailStyle implements RailPlayStrategy {
    CONSERVATIVE {
        @Override
        public int chooseBet(int stack, int tableMinimum, int preferredBet, Random random) {
            return boundedBet(stack, tableMinimum, preferredBet, random, 0.55, 0.90);
        }

        @Override
        public RailTurnPlan createTurnPlan(Hand hand, int dealerUpCard, int stack, int currentBet, Random random) {
            int value = hand.getValue();
            boolean doubleDown = canDouble(hand, stack, currentBet)
                && value >= 10 && value <= 11
                && dealerUpCard >= 4 && dealerUpCard <= 6;
            int standThreshold = dealerUpCard >= 7 ? 18 : 17;
            int pressAmount = canPress(stack, currentBet) && random.nextDouble() < 0.08 ? 5 : 0;
            boolean cheat = value <= 12 && random.nextDouble() < 0.03;
            return new RailTurnPlan(standThreshold, doubleDown, pressAmount, cheat);
        }
    },
    BALANCED {
        @Override
        public int chooseBet(int stack, int tableMinimum, int preferredBet, Random random) {
            return boundedBet(stack, tableMinimum, preferredBet, random, 0.85, 1.15);
        }

        @Override
        public RailTurnPlan createTurnPlan(Hand hand, int dealerUpCard, int stack, int currentBet, Random random) {
            int value = hand.getValue();
            boolean doubleDown = canDouble(hand, stack, currentBet)
                && value >= 9 && value <= 11
                && dealerUpCard >= 3 && dealerUpCard <= 6;
            int standThreshold = dealerUpCard >= 7 ? 17 : 16;
            int pressAmount = canPress(stack, currentBet) && random.nextDouble() < 0.18 ? 10 : 0;
            boolean cheat = (value <= 11 || value > 21) && random.nextDouble() < 0.06;
            return new RailTurnPlan(standThreshold, doubleDown, pressAmount, cheat);
        }
    },
    BOLD {
        @Override
        public int chooseBet(int stack, int tableMinimum, int preferredBet, Random random) {
            return boundedBet(stack, tableMinimum, preferredBet, random, 0.95, 1.45);
        }

        @Override
        public RailTurnPlan createTurnPlan(Hand hand, int dealerUpCard, int stack, int currentBet, Random random) {
            int value = hand.getValue();
            boolean doubleDown = canDouble(hand, stack, currentBet)
                && value >= 9 && value <= 11
                && (dealerUpCard >= 2 && dealerUpCard <= 6 || random.nextDouble() < 0.24);
            int standThreshold = dealerUpCard >= 7 ? 16 : 15;
            int pressAmount = canPress(stack, currentBet) && random.nextDouble() < 0.35 ? 10 : 0;
            boolean cheat = (value <= 13 || value > 21) && random.nextDouble() < 0.11;
            return new RailTurnPlan(standThreshold, doubleDown, pressAmount, cheat);
        }
    },
    OPPORTUNIST {
        @Override
        public int chooseBet(int stack, int tableMinimum, int preferredBet, Random random) {
            return boundedBet(stack, tableMinimum, preferredBet, random, 0.70, 1.35);
        }

        @Override
        public RailTurnPlan createTurnPlan(Hand hand, int dealerUpCard, int stack, int currentBet, Random random) {
            int value = hand.getValue();
            boolean doubleDown = canDouble(hand, stack, currentBet)
                && value >= 9 && value <= 11
                && (dealerUpCard >= 4 && dealerUpCard <= 6 || random.nextDouble() < 0.14);
            int standThreshold = dealerUpCard >= 7 ? 17 : 15;
            int pressAmount = canPress(stack, currentBet) && random.nextDouble() < 0.24 ? 15 : 0;
            boolean cheat = (value <= 14 || value > 21) && random.nextDouble() < 0.18;
            return new RailTurnPlan(standThreshold, doubleDown, pressAmount, cheat);
        }
    };

    protected int boundedBet(int stack, int tableMinimum, int preferredBet, Random random,
                             double lowMultiplier, double highMultiplier) {
        int low = Math.max(tableMinimum, (int) Math.floor(preferredBet * lowMultiplier));
        int high = Math.max(low, (int) Math.ceil(preferredBet * highMultiplier));
        int candidate = low + random.nextInt(high - low + 1);
        int maxAffordable = Math.max(tableMinimum, stack / 3);
        int capped = Math.min(candidate, Math.min(stack, maxAffordable));
        capped = Math.max(tableMinimum, capped);
        return Math.max(tableMinimum, Math.max(5, (capped / 5) * 5));
    }

    protected boolean canDouble(Hand hand, int stack, int currentBet) {
        return hand.getCards().size() == 2 && currentBet * 2 <= stack;
    }

    protected boolean canPress(int stack, int currentBet) {
        return currentBet + 5 <= stack;
    }
}
