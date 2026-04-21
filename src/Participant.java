import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Participant {
    private final String name;
    private final List<Hand> hands = new ArrayList<>();

    protected Participant(String name) {
        this.name = name;
        hands.add(new Hand());
    }

    public String getName() {
        return name;
    }

    public List<Hand> getHands() {
        return Collections.unmodifiableList(hands);
    }

    public Hand getPrimaryHand() {
        return hands.get(0);
    }

    public void resetHands() {
        hands.clear();
        hands.add(new Hand());
    }

    public void addHand(Hand hand) {
        addHand(hands.size(), hand);
    }

    public void addHand(int index, Hand hand) {
        hands.add(index, hand);
    }
}
