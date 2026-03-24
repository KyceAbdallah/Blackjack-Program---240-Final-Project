import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundManager {
    private static final Path ASSET_DIR = Path.of("assets");

    private final Map<String, Long> lastPlayTimes = new HashMap<>();
    private final Map<String, List<Clip>> clipCache = new HashMap<>();
    private final Random random = new Random();
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

    public void playVoiceSnippet(String fileName) {
        long now = System.currentTimeMillis();
        Long previous = lastPlayTimes.get(fileName + "#voice");
        if (previous != null && now - previous < 35) {
            return;
        }
        lastPlayTimes.put(fileName + "#voice", now);

        Clip clip = getReusableClip(fileName);
        if (clip == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        clip.stop();
        long length = clip.getMicrosecondLength();
        long snippetLength = 40_000L + random.nextInt(35_000);
        long maxStart = Math.max(0, length - snippetLength - 1);
        long start = maxStart == 0 ? 0 : (long) (random.nextDouble() * maxStart * 0.85);
        clip.setMicrosecondPosition(start);
        clip.start();

        Thread stopper = new Thread(() -> {
            try {
                Thread.sleep(Math.max(20L, snippetLength / 1_000L));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            clip.stop();
        }, "voice-snippet-stop");
        stopper.setDaemon(true);
        stopper.start();
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

    private Clip getReusableClip(String fileName) {
        List<Clip> clips = clipCache.computeIfAbsent(fileName, this::buildClipPool);
        for (Clip clip : clips) {
            if (!clip.isRunning()) {
                return clip;
            }
        }
        return clips.isEmpty() ? null : clips.get(random.nextInt(clips.size()));
    }

    private List<Clip> buildClipPool(String fileName) {
        List<Clip> pool = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Clip clip = loadClip(fileName);
            if (clip != null) {
                pool.add(clip);
            }
        }
        return pool.isEmpty() ? Collections.emptyList() : pool;
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
