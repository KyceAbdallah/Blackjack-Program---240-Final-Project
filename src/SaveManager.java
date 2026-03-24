import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SaveManager {
    private static final Path SAVE_PATH = Path.of("blackjack-save.properties");

    public SaveData load() {
        Properties properties = new Properties();
        if (Files.exists(SAVE_PATH)) {
            try (InputStream inputStream = Files.newInputStream(SAVE_PATH)) {
                properties.load(inputStream);
            } catch (IOException ignored) {
            }
        }
        return new SaveData(
            parseInt(properties.getProperty("bankroll"), 500),
            parseInt(properties.getProperty("bestStreak"), 0),
            parseInt(properties.getProperty("wins"), 0),
            parseInt(properties.getProperty("losses"), 0)
        );
    }

    public void save(Player player, int wins, int losses) {
        Properties properties = new Properties();
        properties.setProperty("bankroll", String.valueOf(player.getBankroll()));
        properties.setProperty("bestStreak", String.valueOf(player.getBestStreak()));
        properties.setProperty("wins", String.valueOf(wins));
        properties.setProperty("losses", String.valueOf(losses));

        try (OutputStream outputStream = Files.newOutputStream(SAVE_PATH)) {
            properties.store(outputStream, "Blackjack Saloon save");
        } catch (IOException ignored) {
        }
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

    public record SaveData(int bankroll, int bestStreak, int wins, int losses) {
    }
}
