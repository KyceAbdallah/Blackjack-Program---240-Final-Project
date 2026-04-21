public class OpponentProfile {
    private final String name;
    private final String introLine;
    private final int suspicionSensitivity;
    private final int aggression;
    private final int duelDifficulty;

    public OpponentProfile(String name, String introLine, int suspicionSensitivity, int aggression, int duelDifficulty) {
        this.name = name;
        this.introLine = introLine;
        this.suspicionSensitivity = suspicionSensitivity;
        this.aggression = aggression;
        this.duelDifficulty = duelDifficulty;
    }

    public String getName() {
        return name;
    }

    public String getIntroLine() {
        return introLine;
    }

    public int getSuspicionSensitivity() {
        return suspicionSensitivity;
    }

    public int getAggression() {
        return aggression;
    }

    public int getDuelDifficulty() {
        return duelDifficulty;
    }
}
