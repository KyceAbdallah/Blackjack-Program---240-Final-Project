import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    private static final Color BACKDROP = new Color(36, 21, 12);
    private static final Color PANEL = new Color(86, 53, 33);
    private static final Color ACCENT = new Color(219, 173, 88);
    private static final Color DUST = new Color(242, 218, 170);
    private static final Color FELT = new Color(31, 82, 54);
    private static final Color DANGER = new Color(167, 48, 48);

    private final GameController controller;
    private final JFrame frame;
    private final Random random = new Random();
    private final SoundManager soundManager = new SoundManager();

    private final JLabel titleLabel = new JLabel("BLACKJACK SALOON", SwingConstants.CENTER);
    private final JLabel opponentLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JTextArea eventArea = new JTextArea();
    private final JLabel dealerLabel = new JLabel();
    private final JLabel dealerValueLabel = new JLabel();
    private final JPanel handsPanel = new JPanel();
    private final JLabel bankrollLabel = new JLabel();
    private final JLabel streakLabel = new JLabel();
    private final JLabel recordLabel = new JLabel();
    private final JLabel shoeLabel = new JLabel();
    private final JProgressBar suspicionBar = new JProgressBar(0, 100);
    private final JTextField betField = new JTextField("25");
    private final JButton dealButton = new JButton("DEAL");
    private final JButton hitButton = new JButton("HIT");
    private final JButton standButton = new JButton("STAND");
    private final JButton doubleButton = new JButton("DOUBLE");
    private final JButton splitButton = new JButton("SPLIT");
    private final JButton cheatButton = new JButton("CHEAT");
    private final JButton duelButton = new JButton("DRAW");
    private final JButton resetButton = new JButton("RESET SAVE");
    private final JLabel loadingLabel = new JLabel("DON'T GAMBLE", SwingConstants.CENTER);

    private Timer typeTimer;
    private Timer duelTimer;
    private String fullEventText = "";
    private int typeIndex;
    private Point baseLocation;
    private boolean musicStarted;
    private boolean duelSequenceQueued;

    public GamePanel(GameController controller, JFrame frame) {
        this.controller = controller;
        this.frame = frame;
        buildUi();
        bindActions();
        refresh(false);
    }

    private void buildUi() {
        setLayout(new BorderLayout(18, 18));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setBackground(BACKDROP);

        titleLabel.setFont(new Font("Monospaced", Font.BOLD, 30));
        titleLabel.setForeground(ACCENT);
        add(titleLabel, BorderLayout.NORTH);

        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildSidebarPanel(), BorderLayout.EAST);

        loadingLabel.setOpaque(true);
        loadingLabel.setBackground(new Color(18, 10, 5, 220));
        loadingLabel.setForeground(ACCENT);
        loadingLabel.setFont(new Font("Monospaced", Font.BOLD, 28));
        loadingLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 4),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));
        add(loadingLabel, BorderLayout.SOUTH);
    }

    private JPanel buildTablePanel() {
        JPanel table = styledPanel();
        table.setBackground(FELT);
        table.setLayout(new BorderLayout(12, 12));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        opponentLabel.setForeground(DUST);
        opponentLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        dealerLabel.setForeground(DUST);
        dealerLabel.setFont(new Font("Monospaced", Font.PLAIN, 19));
        dealerValueLabel.setForeground(ACCENT);
        dealerValueLabel.setFont(new Font("Monospaced", Font.BOLD, 17));
        header.add(opponentLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(dealerLabel);
        header.add(dealerValueLabel);

        handsPanel.setOpaque(false);
        handsPanel.setLayout(new GridLayout(0, 1, 0, 10));

        table.add(header, BorderLayout.NORTH);
        table.add(handsPanel, BorderLayout.CENTER);
        return table;
    }

    private JPanel buildSidebarPanel() {
        JPanel sidebar = styledPanel();
        sidebar.setPreferredSize(new Dimension(320, 680));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        statusLabel.setForeground(ACCENT);
        statusLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        eventArea.setEditable(false);
        eventArea.setLineWrap(true);
        eventArea.setWrapStyleWord(true);
        eventArea.setForeground(DUST);
        eventArea.setBackground(BACKDROP);
        eventArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        eventArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane eventScroll = new JScrollPane(eventArea);
        eventScroll.setAlignmentX(LEFT_ALIGNMENT);
        eventScroll.setPreferredSize(new Dimension(280, 180));
        eventScroll.setMaximumSize(new Dimension(280, 180));
        eventScroll.setBorder(BorderFactory.createLineBorder(ACCENT, 2));

        bankrollLabel.setForeground(DUST);
        streakLabel.setForeground(DUST);
        recordLabel.setForeground(DUST);
        shoeLabel.setForeground(DUST);
        for (JLabel label : new JLabel[]{bankrollLabel, streakLabel, recordLabel, shoeLabel}) {
            label.setFont(new Font("Monospaced", Font.BOLD, 14));
            label.setAlignmentX(LEFT_ALIGNMENT);
        }

        suspicionBar.setForeground(DANGER);
        suspicionBar.setBackground(BACKDROP);
        suspicionBar.setStringPainted(true);
        suspicionBar.setFont(new Font("Monospaced", Font.BOLD, 12));
        suspicionBar.setAlignmentX(LEFT_ALIGNMENT);

        betField.setMaximumSize(new Dimension(280, 32));
        betField.setFont(new Font("Monospaced", Font.BOLD, 14));

        JPanel buttonGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        buttonGrid.setOpaque(false);
        buttonGrid.setAlignmentX(LEFT_ALIGNMENT);
        JButton[] buttons = {
            dealButton, hitButton, standButton, doubleButton,
            splitButton, cheatButton, duelButton, resetButton
        };
        for (JButton button : buttons) {
            styleButton(button, button == cheatButton || button == duelButton ? DANGER : PANEL);
            buttonGrid.add(button);
        }

        sidebar.add(sectionLabel("Status"));
        sidebar.add(statusLabel);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(sectionLabel("Story"));
        sidebar.add(eventScroll);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(sectionLabel("Table"));
        sidebar.add(bankrollLabel);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(streakLabel);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(recordLabel);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(shoeLabel);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(sectionLabel("Suspicion"));
        sidebar.add(suspicionBar);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(sectionLabel("Bet"));
        sidebar.add(betField);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(buttonGrid);
        return sidebar;
    }

    private JPanel styledPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 3),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));
        return panel;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT);
        label.setFont(new Font("Monospaced", Font.BOLD, 15));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(DUST);
        button.setFocusPainted(false);
        button.setFont(new Font("Monospaced", Font.BOLD, 13));
        button.setBorder(BorderFactory.createLineBorder(ACCENT, 2));
    }

    private void bindActions() {
        dealButton.addActionListener(event -> {
            controller.setBetAndDeal(parseBet());
            soundManager.playEffect("card-clack.wav");
            refresh(true);
        });
        hitButton.addActionListener(event -> {
            controller.hit();
            soundManager.playEffect("card-clack.wav");
            refresh(true);
        });
        standButton.addActionListener(event -> {
            controller.stand();
            refresh(true);
        });
        doubleButton.addActionListener(event -> {
            controller.doubleDown();
            soundManager.playEffect("card-clack.wav");
            refresh(true);
        });
        splitButton.addActionListener(event -> {
            controller.split();
            soundManager.playEffect("card-clack.wav");
            refresh(true);
        });
        cheatButton.addActionListener(event -> {
            controller.cheat();
            soundManager.playEffect("card-clack.wav");
            refresh(true);
        });
        duelButton.addActionListener(event -> fireDuelDraw());
        resetButton.addActionListener(event -> {
            controller.resetSave();
            refresh(true);
        });
    }

    public void fireDuelDraw() {
        controller.drawDuel();
        refresh(true);
    }

    public void refresh(boolean animateText) {
        if (!musicStarted) {
            soundManager.loopMusic("a_quiet_time.wav");
            musicStarted = true;
        }

        GameSnapshot snapshot = controller.getSnapshot();
        loadingLabel.setVisible(snapshot.showLoading());
        opponentLabel.setText("Opponent: " + snapshot.opponentName());
        dealerLabel.setText("Dealer: " + snapshot.dealerCards());
        dealerValueLabel.setText(snapshot.roundActive()
            ? "Dealer showing: " + snapshot.dealerVisibleValue()
            : "Dealer value: " + snapshot.dealerFinalValue());

        bankrollLabel.setText("Bankroll: $" + snapshot.bankroll());
        streakLabel.setText("Streak: " + snapshot.streak() + "   Best: " + snapshot.bestStreak());
        recordLabel.setText("Record: " + snapshot.wins() + " - " + snapshot.losses());
        shoeLabel.setText("Cards in shoe: " + snapshot.shoeCards());
        suspicionBar.setValue(snapshot.suspicion());
        suspicionBar.setString(snapshot.suspicion() + "%");

        String status = snapshot.statusText().replace("\n", " | ");
        statusLabel.setText("<html><body style='width:260px'>" + status + "</body></html>");
        if (animateText) {
            typewrite(snapshot.eventText());
        } else {
            eventArea.setText(snapshot.eventText());
        }

        renderHands(snapshot);

        dealButton.setEnabled(snapshot.canDeal());
        hitButton.setEnabled(snapshot.canHit());
        standButton.setEnabled(snapshot.canStand());
        doubleButton.setEnabled(snapshot.canDouble());
        splitButton.setEnabled(snapshot.canSplit());
        cheatButton.setEnabled(snapshot.canCheat());
        duelButton.setEnabled(snapshot.duelActive());

        if (snapshot.duelActive() && !snapshot.duelCanDraw() && !duelSequenceQueued) {
            duelSequenceQueued = true;
            soundManager.playEffect("glass-break.wav");
            shakeFrame();
            beginDuelTimer();
        }
        if (snapshot.duelCanDraw()) {
            duelSequenceQueued = false;
            stopDuelTimer();
            soundManager.playEffect("glass-break.wav");
        }
        if (!snapshot.duelActive()) {
            duelSequenceQueued = false;
            stopDuelTimer();
        }

        revalidate();
        repaint();
    }

    private void renderHands(GameSnapshot snapshot) {
        handsPanel.removeAll();
        for (int i = 0; i < snapshot.playerHands().size(); i++) {
            int bet = i < snapshot.handBets().size() ? snapshot.handBets().get(i) : 0;
            JPanel handPanel = new JPanel(new BorderLayout());
            handPanel.setOpaque(true);
            handPanel.setBackground(i == snapshot.activeHandIndex() && snapshot.roundActive()
                ? new Color(74, 32, 19)
                : new Color(57, 37, 23));
            handPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(i == snapshot.activeHandIndex() ? ACCENT : DUST, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            JLabel title = new JLabel("Hand " + (i + 1) + "   Bet $" + bet);
            title.setForeground(ACCENT);
            title.setFont(new Font("Monospaced", Font.BOLD, 16));

            JLabel cards = new JLabel(snapshot.playerHands().get(i));
            cards.setForeground(DUST);
            cards.setFont(new Font("Monospaced", Font.BOLD, 20));

            JLabel value = new JLabel("Value: " + snapshot.playerValues().get(i));
            value.setForeground(DUST);
            value.setFont(new Font("Monospaced", Font.PLAIN, 14));

            handPanel.add(title, BorderLayout.NORTH);
            handPanel.add(cards, BorderLayout.CENTER);
            handPanel.add(value, BorderLayout.SOUTH);
            handsPanel.add(handPanel);
        }
    }

    private int parseBet() {
        try {
            return Integer.parseInt(betField.getText().trim());
        } catch (NumberFormatException exception) {
            return 10;
        }
    }

    private void typewrite(String text) {
        fullEventText = text;
        typeIndex = 0;
        eventArea.setText("");
        if (typeTimer != null) {
            typeTimer.stop();
        }
        typeTimer = new Timer(18, event -> {
            if (typeIndex >= fullEventText.length()) {
                typeTimer.stop();
                return;
            }
            char next = fullEventText.charAt(typeIndex);
            eventArea.append(String.valueOf(next));
            if (!Character.isWhitespace(next) && typeIndex % 3 == 0) {
                soundManager.playEffect("typewriter.wav");
            }
            typeIndex++;
        });
        typeTimer.start();
    }

    private void beginDuelTimer() {
        if (duelTimer != null && duelTimer.isRunning()) {
            return;
        }
        duelTimer = new Timer(1200 + random.nextInt(1800), event -> {
            controller.armDuel();
            refresh(true);
        });
        duelTimer.setRepeats(false);
        duelTimer.start();
    }

    private void stopDuelTimer() {
        if (duelTimer != null) {
            duelTimer.stop();
        }
    }

    private void shakeFrame() {
        if (baseLocation == null) {
            baseLocation = frame.getLocation();
        }
        Point anchor = baseLocation;
        final int[] shakes = {0};
        Timer shake = new Timer(22, null);
        shake.addActionListener(event -> {
            if (shakes[0] >= 12) {
                frame.setLocation(anchor);
                shake.stop();
                return;
            }
            int offsetX = random.nextInt(13) - 6;
            int offsetY = random.nextInt(9) - 4;
            frame.setLocation(anchor.x + offsetX, anchor.y + offsetY);
            shakes[0]++;
        });
        shake.start();
    }
}
