import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.Timer;

public class GameFrame extends JFrame {
    private final GameController controller;
    private final GamePanel panel;

    public GameFrame() {
        controller = new GameController();
        setTitle("Blackjack Saloon");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1040, 780));
        setResizable(false);

        panel = new GamePanel(controller, this);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_SPACE && controller.getSnapshot().duelActive()) {
                    panel.fireDuelDraw();
                }
            }
        });

        Timer bootTimer = new Timer(4200, event -> {
            controller.finishLoading();
            panel.refresh(true);
        });
        bootTimer.setRepeats(false);
        bootTimer.start();
    }
}
