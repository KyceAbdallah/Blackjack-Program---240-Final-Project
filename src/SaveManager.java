import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SaveManager {
    private static final Path DEFAULT_SAVE_PATH = Path.of(
        System.getProperty("user.home"),
        ".blackjack-saloon",
        "blackjack-save.properties"
    );

    private final Path savePath;
    private String lastErrorMessage = "";

    public SaveManager() {
        this(DEFAULT_SAVE_PATH);
    }

    public SaveManager(Path savePath) {
        this.savePath = savePath;
    }

    public SaveData load() {
        Properties properties = new Properties();
        lastErrorMessage = "";
        if (Files.exists(savePath)) {
            try (InputStream inputStream = Files.newInputStream(savePath)) {
                properties.load(inputStream);
            } catch (IOException exception) {
                lastErrorMessage = "Couldn't load save data. Starting a fresh table instead.";
            }
        }
        return new SaveData(
            parseInt(properties.getProperty("bankroll"), 500),
            parseInt(properties.getProperty("bestStreak"), 0),
            parseInt(properties.getProperty("wins"), 0),
            parseInt(properties.getProperty("losses"), 0),
            parseInt(properties.getProperty("streak"), 0),
            parseInt(properties.getProperty("suspicion"), 0)
        );
    }

    public void save(Player player, int wins, int losses) {
        Properties properties = new Properties();
        properties.setProperty("bankroll", String.valueOf(player.getBankroll()));
        properties.setProperty("bestStreak", String.valueOf(player.getBestStreak()));
        properties.setProperty("wins", String.valueOf(wins));
        properties.setProperty("losses", String.valueOf(losses));
        properties.setProperty("streak", String.valueOf(player.getStreak()));
        properties.setProperty("suspicion", String.valueOf(player.getSuspicion()));

        lastErrorMessage = "";
        try {
            Path parent = savePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream outputStream = Files.newOutputStream(savePath)) {
                properties.store(outputStream, "Blackjack Saloon save");
            }
        } catch (IOException exception) {
            lastErrorMessage = "Couldn't save your progress to " + savePath + ".";
        }
    }

    public Path getSavePath() {
        return savePath;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public record SaveData(int bankroll, int bestStreak, int wins, int losses, int streak, int suspicion) {
    }
}
