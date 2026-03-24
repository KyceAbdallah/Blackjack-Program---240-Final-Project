public class Dealer extends Participant {
    public Dealer(String name) {
        super(name);
    }

    public boolean shouldHit() {
        return getPrimaryHand().getValue() < 17;
    }
}
