import java.util.Random;

interface RailPlayStrategy {
    int chooseBet(int stack, int tableMinimum, int preferredBet, Random random);

    RailTurnPlan createTurnPlan(Hand hand, int dealerUpCard, int stack, int currentBet, Random random);
}
