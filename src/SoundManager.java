import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
        Path path = ASSET_DIR.resolve(fileName);
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
}
