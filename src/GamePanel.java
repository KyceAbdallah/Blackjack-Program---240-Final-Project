import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
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
    private static final Color BACKDROP = new Color(44, 30, 20);
    private static final Color FELT = new Color(28, 101, 71);
    private static final Color FELT_DARK = new Color(15, 62, 43);
    private static final Color WOOD = new Color(113, 73, 43);
    private static final Color GOLD = new Color(239, 201, 104);
    private static final Color CREAM = new Color(247, 236, 213);
    private static final Color CHIP_RED = new Color(181, 58, 48);
    private static final Color CHIP_BLUE = new Color(57, 94, 186);
    private static final Color CHIP_WHITE = new Color(236, 236, 231);
    private static final Color DANGER = new Color(189, 70, 47);

    private final GameController controller;
    private final JFrame frame;
    private final Random random = new Random();
    private final SoundManager soundManager = new SoundManager();

    private final JLabel titleLabel = new JLabel("BLACKJACK SALOON", SwingConstants.CENTER);
    private final JLabel opponentLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JTextArea eventArea = new JTextArea();
    private final JLabel bankrollLabel = new JLabel();
    private final JLabel potLabel = new JLabel();
    private final JLabel recordLabel = new JLabel();
    private final JLabel shoeLabel = new JLabel();
    private final JProgressBar suspicionBar = new JProgressBar(0, 100);
    private final JTextField betField = new JTextField("25");
    private final JButton dealButton = new JButton("Deal");
    private final JButton hitButton = new JButton("Hit");
    private final JButton standButton = new JButton("Stand");
    private final JButton doubleButton = new JButton("Double");
    private final JButton splitButton = new JButton("Split");
    private final JButton cheatButton = new JButton("Cheat");
    private final JButton duelButton = new JButton("Draw");
    private final JButton resetButton = new JButton("Reset");
    private final TableCanvas tableCanvas = new TableCanvas();

    private Timer typeTimer;
    private Timer duelTimer;
    private Timer loadingOverlayTimer;
    private Point baseLocation;
    private boolean musicStarted;
    private boolean duelSequenceQueued;
    private float loadingOverlayAlpha = 1f;
    private float loadingTextPulse;
    private GameSnapshot currentSnapshot;
    private List<String> narrativePages = List.of("");
    private int narrativePageIndex;
    private String currentPageText = "";
    private int currentCharIndex;
    private boolean narrativeAwaitingContinue;

    public GamePanel(GameController controller, JFrame frame) {
        this.controller = controller;
        this.frame = frame;
        buildUi();
        bindActions();
        startLoadingOverlayTimer();
        refresh(false);
    }

    private void buildUi() {
        setLayout(new BorderLayout(18, 18));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setBackground(BACKDROP);

        titleLabel.setFont(new Font("Dialog", Font.BOLD, 34));
        titleLabel.setForeground(CREAM);
        add(titleLabel, BorderLayout.NORTH);

        tableCanvas.setPreferredSize(new Dimension(700, 650));
        add(tableCanvas, BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(320, 680));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(60, 41, 29));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 3),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

        opponentLabel.setForeground(GOLD);
        opponentLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        opponentLabel.setAlignmentX(LEFT_ALIGNMENT);

        statusLabel.setForeground(CREAM);
        statusLabel.setFont(new Font("Dialog", Font.BOLD, 15));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        eventArea.setEditable(false);
        eventArea.setLineWrap(true);
        eventArea.setWrapStyleWord(true);
        eventArea.setForeground(CREAM);
        eventArea.setBackground(new Color(31, 23, 18));
        eventArea.setFont(new Font("Dialog", Font.PLAIN, 15));
        eventArea.setMargin(new Insets(12, 12, 12, 12));
        eventArea.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane storyPane = new JScrollPane(eventArea);
        storyPane.setAlignmentX(LEFT_ALIGNMENT);
        storyPane.setPreferredSize(new Dimension(280, 220));
        storyPane.setMaximumSize(new Dimension(280, 220));
        storyPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WOOD, 2),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));

        bankrollLabel.setForeground(CREAM);
        potLabel.setForeground(CREAM);
        recordLabel.setForeground(CREAM);
        shoeLabel.setForeground(CREAM);
        for (JLabel label : new JLabel[]{bankrollLabel, potLabel, recordLabel, shoeLabel}) {
            label.setFont(new Font("Dialog", Font.BOLD, 15));
            label.setAlignmentX(LEFT_ALIGNMENT);
        }

        suspicionBar.setForeground(DANGER);
        suspicionBar.setBackground(new Color(34, 27, 20));
        suspicionBar.setStringPainted(true);
        suspicionBar.setFont(new Font("Dialog", Font.BOLD, 12));
        suspicionBar.setAlignmentX(LEFT_ALIGNMENT);
        suspicionBar.setBorder(BorderFactory.createLineBorder(GOLD, 1));

        betField.setMaximumSize(new Dimension(280, 34));
        betField.setFont(new Font("Dialog", Font.BOLD, 16));
        betField.setBackground(new Color(248, 239, 225));
        betField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WOOD, 2),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        JPanel buttonGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        buttonGrid.setOpaque(false);
        buttonGrid.setAlignmentX(LEFT_ALIGNMENT);
        for (JButton button : new JButton[]{
            dealButton, hitButton, standButton, doubleButton,
            splitButton, cheatButton, duelButton, resetButton
        }) {
            styleButton(button);
            buttonGrid.add(button);
        }

        sidebar.add(opponentLabel);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(statusLabel);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(sectionLabel("Story"));
        sidebar.add(storyPane);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(sectionLabel("Table"));
        sidebar.add(bankrollLabel);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(potLabel);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(recordLabel);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(shoeLabel);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(sectionLabel("Suspicion"));
        sidebar.add(suspicionBar);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(sectionLabel("Bet"));
        sidebar.add(betField);
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(buttonGrid);
        return sidebar;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(GOLD);
        label.setFont(new Font("Dialog", Font.BOLD, 15));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(90, 62, 37));
        button.setForeground(CREAM);
        button.setFocusPainted(false);
        button.setFont(new Font("Dialog", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 2),
            BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
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

    public boolean hasPendingNarrative() {
        return typeTimer != null && typeTimer.isRunning()
            || narrativeAwaitingContinue
            || narrativePageIndex < narrativePages.size() - 1;
    }

    public void advanceNarrative() {
        if (typeTimer != null && typeTimer.isRunning()) {
            typeTimer.stop();
            eventArea.setText(currentPageText);
            narrativeAwaitingContinue = true;
            updateContinuePrompt();
            return;
        }

        if (narrativePageIndex < narrativePages.size() - 1) {
            narrativePageIndex++;
            typePage(narrativePages.get(narrativePageIndex));
        }
    }

    public void fireDuelDraw() {
        controller.drawDuel();
        refresh(true);
    }

    public void refresh(boolean animateText) {
        if (!musicStarted) {
            soundManager.loopMusic("quiet time.wav");
            musicStarted = true;
        }

        currentSnapshot = controller.getSnapshot();
        opponentLabel.setText(currentSnapshot.opponentName());
        statusLabel.setText("<html><body style='width:250px'>" + currentSnapshot.statusText().replace("\n", "<br>") + "</body></html>");

        bankrollLabel.setText("Bankroll: $" + currentSnapshot.bankroll());
        potLabel.setText("Pot: $" + currentSnapshot.handBets().stream().mapToInt(Integer::intValue).sum());
        recordLabel.setText("Record: " + currentSnapshot.wins() + " - " + currentSnapshot.losses() + "   Best streak: " + currentSnapshot.bestStreak());
        shoeLabel.setText("Cards left in shoe: " + currentSnapshot.shoeCards());
        suspicionBar.setValue(currentSnapshot.suspicion());
        suspicionBar.setString(currentSnapshot.suspicion() + "%");

        if (animateText) {
            prepareNarrative(currentSnapshot.eventText());
        } else {
            eventArea.setText(currentSnapshot.eventText());
            narrativePages = List.of(currentSnapshot.eventText());
            narrativePageIndex = 0;
            currentPageText = currentSnapshot.eventText();
            narrativeAwaitingContinue = false;
        }

        dealButton.setEnabled(currentSnapshot.canDeal());
        hitButton.setEnabled(currentSnapshot.canHit());
        standButton.setEnabled(currentSnapshot.canStand());
        doubleButton.setEnabled(currentSnapshot.canDouble());
        splitButton.setEnabled(currentSnapshot.canSplit());
        cheatButton.setEnabled(currentSnapshot.canCheat());
        duelButton.setEnabled(false);

        duelSequenceQueued = false;
        stopDuelTimer();

        tableCanvas.repaint();
        revalidate();
        repaint();
    }

    private void prepareNarrative(String text) {
        narrativePages = splitNarrative(text);
        narrativePageIndex = 0;
        typePage(narrativePages.get(0));
    }

    private List<String> splitNarrative(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return List.of("");
        }

        List<String> pages = new ArrayList<>();
        String[] paragraphs = normalized.split("\\n+");
        StringBuilder page = new StringBuilder();
        for (String paragraph : paragraphs) {
            String part = paragraph.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (page.length() + part.length() > 120 && page.length() > 0) {
                pages.add(page.toString().trim());
                page.setLength(0);
            }
            if (page.length() > 0) {
                page.append("\n\n");
            }
            page.append(part);
        }
        if (page.length() > 0) {
            pages.add(page.toString().trim());
        }
        return pages.isEmpty() ? List.of(normalized) : pages;
    }

    private void typePage(String pageText) {
        currentPageText = pageText;
        currentCharIndex = 0;
        narrativeAwaitingContinue = false;
        eventArea.setText("");
        if (typeTimer != null) {
            typeTimer.stop();
        }
        typeTimer = new Timer(34, event -> {
            if (currentCharIndex >= currentPageText.length()) {
                typeTimer.stop();
                narrativeAwaitingContinue = true;
                updateContinuePrompt();
                return;
            }
            char next = currentPageText.charAt(currentCharIndex);
            eventArea.append(String.valueOf(next));
            if (!Character.isWhitespace(next) && currentCharIndex % 3 == 0) {
                soundManager.playVoiceSnippet("Papyrus Dialogue Sound Effect");
            }
            currentCharIndex++;
        });
        typeTimer.start();
    }

    private void updateContinuePrompt() {
        String prompt = narrativePageIndex < narrativePages.size() - 1
            ? "\n\n[Press SPACE to continue]"
            : "";
        eventArea.setText(currentPageText + prompt);
    }

    private int parseBet() {
        try {
            return Integer.parseInt(betField.getText().trim());
        } catch (NumberFormatException exception) {
            return 10;
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(BACKDROP);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);

        if (loadingOverlayAlpha <= 0f) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, loadingOverlayAlpha))));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        float pulse = Math.max(0.35f, Math.min(1f, 0.65f + (float) Math.sin(loadingTextPulse) * 0.25f));
        g2.setComposite(AlphaComposite.SrcOver.derive(pulse));
        g2.setColor(CREAM);
        g2.setFont(new Font("Monospaced", Font.BOLD, 38));
        String title = "DON'T GAMBLE";
        int centerY = getHeight() / 2 - 24;
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (getWidth() - titleWidth) / 2, centerY);
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        String line = "FAKE MONEY. REAL CONSEQUENCES.";
        int lineWidth = g2.getFontMetrics().stringWidth(line);
        g2.drawString(line, (getWidth() - lineWidth) / 2, centerY + 42);
        g2.dispose();
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
            frame.setLocation(anchor.x + random.nextInt(13) - 6, anchor.y + random.nextInt(9) - 4);
            shakes[0]++;
        });
        shake.start();
    }

    private void startLoadingOverlayTimer() {
        loadingOverlayTimer = new Timer(33, event -> {
            boolean stillLoading = controller.getSnapshot().showLoading();
            if (stillLoading) {
                loadingOverlayAlpha = 0.86f + 0.14f * (float) ((Math.sin(loadingTextPulse) + 1.0) / 2.0);
                loadingTextPulse += 0.16f;
            } else if (loadingOverlayAlpha > 0f) {
                loadingOverlayAlpha = Math.max(0f, loadingOverlayAlpha - 0.05f);
                loadingTextPulse += 0.10f;
            } else {
                loadingOverlayTimer.stop();
            }
            repaint();
        });
        loadingOverlayTimer.start();
    }

    private class TableCanvas extends JPanel {
        TableCanvas() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (currentSnapshot == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setColor(new Color(93, 60, 34));
            g2.fillRoundRect(10, 16, width - 20, height - 32, 36, 36);
            g2.setColor(FELT_DARK);
            g2.fillRoundRect(24, 30, width - 48, height - 60, 28, 28);

            Arc2D halfCircle = new Arc2D.Double(70, 60, width - 140, height - 150, 0, 180, Arc2D.OPEN);
            g2.setStroke(new BasicStroke(4f));
            g2.setColor(GOLD);
            g2.draw(halfCircle);

            drawDealer(g2, width);
            drawDealerCards(g2, width);
            drawPot(g2, width, height);
            drawPlayerArea(g2, width, height);
            g2.dispose();
        }

        private void drawDealer(Graphics2D g2, int width) {
            int centerX = width / 2;
            g2.setColor(new Color(63, 40, 27));
            g2.fill(new Ellipse2D.Double(centerX - 24, 80, 48, 48));
            g2.fillRoundRect(centerX - 40, 120, 80, 70, 18, 18);
            g2.setColor(CREAM);
            g2.setFont(new Font("Dialog", Font.BOLD, 18));
            String name = "Dealer";
            int nameWidth = g2.getFontMetrics().stringWidth(name);
            g2.drawString(name, centerX - nameWidth / 2, 215);
        }

        private void drawDealerCards(Graphics2D g2, int width) {
            drawCardRow(g2, parseCards(currentSnapshot.dealerCards()), width / 2, 255);
            g2.setFont(new Font("Dialog", Font.BOLD, 15));
            String value = currentSnapshot.roundActive()
                ? "Showing " + currentSnapshot.dealerVisibleValue()
                : "Value " + currentSnapshot.dealerFinalValue();
            int valueWidth = g2.getFontMetrics().stringWidth(value);
            g2.setColor(CREAM);
            g2.drawString(value, width / 2 - valueWidth / 2, 315);
        }

        private void drawPot(Graphics2D g2, int width, int height) {
            int pot = currentSnapshot.handBets().stream().mapToInt(Integer::intValue).sum();
            int centerX = width / 2;
            int centerY = height / 2 + 8;

            g2.setColor(GOLD);
            g2.setFont(new Font("Dialog", Font.BOLD, 20));
            String potText = "Pot: $" + pot;
            int potWidth = g2.getFontMetrics().stringWidth(potText);
            g2.drawString(potText, centerX - potWidth / 2, centerY - 8);

            drawChipStack(g2, centerX - 76, centerY + 24, CHIP_RED, 4);
            drawChipStack(g2, centerX - 17, centerY + 16, CHIP_WHITE, 5);
            drawChipStack(g2, centerX + 42, centerY + 24, CHIP_BLUE, 4);
        }

        private void drawChipStack(Graphics2D g2, int x, int y, Color color, int count) {
            for (int i = 0; i < count; i++) {
                int yy = y - i * 8;
                g2.setColor(color);
                g2.fillOval(x, yy, 34, 14);
                g2.setColor(CREAM);
                g2.drawOval(x, yy, 34, 14);
            }
        }

        private void drawPlayerArea(Graphics2D g2, int width, int height) {
            List<String> hands = currentSnapshot.playerHands();
            int areaY = height - 184;
            int spacing = hands.size() == 1 ? 0 : 220;
            int startX = width / 2 - (hands.size() - 1) * spacing / 2;

            for (int i = 0; i < hands.size(); i++) {
                boolean active = i == currentSnapshot.activeHandIndex() && currentSnapshot.roundActive();
                int x = startX + i * spacing;
                int bet = i < currentSnapshot.handBets().size() ? currentSnapshot.handBets().get(i) : 0;
                int panelX = x - 110;
                int panelY = areaY - 6;
                int panelWidth = 220;
                int panelHeight = 116;

                g2.setColor(active ? new Color(255, 228, 164) : new Color(232, 222, 201));
                g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);
                g2.setColor(active ? GOLD : WOOD);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

                g2.setColor(new Color(54, 35, 24));
                g2.setFont(new Font("Dialog", Font.BOLD, 14));
                g2.drawString("Hand " + (i + 1) + "  Bet $" + bet, panelX + 18, panelY + 24);
                drawCardRow(g2, parseCards(hands.get(i)), x, panelY + 56);
                g2.setFont(new Font("Dialog", Font.BOLD, 14));
                g2.drawString("Value " + currentSnapshot.playerValues().get(i), panelX + 18, panelY + 98);
            }
        }

        private void drawCardRow(Graphics2D g2, List<String> cards, int centerX, int baselineY) {
            if (cards.isEmpty()) {
                g2.setColor(CREAM);
                g2.setFont(new Font("Dialog", Font.BOLD, 16));
                String text = "No cards yet";
                int width = g2.getFontMetrics().stringWidth(text);
                g2.drawString(text, centerX - width / 2, baselineY);
                return;
            }

            int cardWidth = 52;
            int cardHeight = 70;
            int gap = 12;
            int totalWidth = cards.size() * cardWidth + (cards.size() - 1) * gap;
            int startX = centerX - totalWidth / 2;
            int y = baselineY - 35;

            for (int i = 0; i < cards.size(); i++) {
                int x = startX + i * (cardWidth + gap);
                g2.setColor(new Color(248, 242, 231));
                g2.fillRoundRect(x, y, cardWidth, cardHeight, 10, 10);
                g2.setColor(WOOD);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, cardWidth, cardHeight, 10, 10);
                g2.setFont(new Font("Dialog", Font.BOLD, 22));
                String value = cards.get(i);
                int textWidth = g2.getFontMetrics().stringWidth(value);
                g2.drawString(value, x + (cardWidth - textWidth) / 2, y + 41);
            }
        }

        private List<String> parseCards(String description) {
            List<String> cards = new ArrayList<>();
            if (description == null || description.isBlank()) {
                return cards;
            }
            String[] parts = description.trim().split("\\s+");
            for (String part : parts) {
                String cleaned = part.replace("[", "").replace("]", "").trim();
                if (!cleaned.isEmpty() && !cleaned.equals("?")) {
                    cards.add(cleaned);
                }
            }
            return cards;
        }
    }
}
