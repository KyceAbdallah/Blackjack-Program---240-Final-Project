import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundManager {
    private static final Path ASSET_DIR = Path.of("assets");

    private final Map<String, Long> lastPlayTimes = new HashMap<>();
    private Clip musicClip;

    public void playEffect(String fileName) {
        long now = System.currentTimeMillis();
        Long previous = lastPlayTimes.get(fileName);
        if (previous != null && now - previous < 60) {
            return;
        }
        lastPlayTimes.put(fileName, now);

        Clip clip = loadClip(fileName);
        if (clip == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        clip.setFramePosition(0);
        clip.start();
    }

    public void loopMusic(String fileName) {
        if (musicClip != null && musicClip.isRunning()) {
            return;
        }
        musicClip = loadClip(fileName);
        if (musicClip != null) {
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private Clip loadClip(String fileName) {
        Path path = resolveAudioPath(fileName);
        if (!Files.exists(path)) {
            return null;
        }
        try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(path.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(inputStream);
            return clip;
        } catch (Exception exception) {
            return null;
        }
    }

    private Path resolveAudioPath(String fileName) {
        List<Path> directCandidates = List.of(
            ASSET_DIR.resolve(fileName),
            Path.of(fileName)
        );
        for (Path candidate : directCandidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        String normalizedTarget = normalize(fileName);
        List<Path> directories = new ArrayList<>();
        directories.add(ASSET_DIR);
        directories.add(Path.of("."));

        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.list(directory)) {
                Path match = files
                    .filter(Files::isRegularFile)
                    .filter(path -> normalize(path.getFileName().toString()).contains(normalizedTarget))
                    .findFirst()
                    .orElse(null);
                if (match != null) {
                    return match;
                }
            } catch (IOException ignored) {
            }
        }

        return ASSET_DIR.resolve(fileName);
    }

    private String normalize(String value) {
        return value.toLowerCase()
            .replace(".wav", "")
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
    }
}
