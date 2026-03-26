import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class GamePanel extends JPanel {
    private static final Color BACKDROP_TOP = new Color(15, 22, 36);
    private static final Color BACKDROP_BOTTOM = new Color(47, 19, 27);
    private static final Color PANEL = new Color(19, 24, 36);
    private static final Color PANEL_ALT = new Color(31, 21, 31);
    private static final Color GOLD = new Color(231, 194, 105);
    private static final Color CREAM = new Color(247, 239, 226);
    private static final Color MUTED = new Color(187, 173, 155);
    private static final Color TABLE_EDGE = new Color(108, 62, 34);
    private static final Color TABLE_TOP = new Color(17, 79, 64);
    private static final Color TABLE_BOTTOM = new Color(8, 43, 36);
    private static final Color DANGER = new Color(203, 85, 64);

    private final GameController controller;
    private final JFrame frame;
    private final Random random = new Random();
    private final SoundManager soundManager = new SoundManager();

    private final JLabel titleLabel = new JLabel("BLACKJACK SALOON");
    private final JLabel subtitleLabel = new JLabel();
    private final JLabel displayNoteLabel = new JLabel("Layout scales with the window. Fullscreen still gives the cleanest table view.");
    private final JLabel headerMetaLabel = new JLabel("", SwingConstants.RIGHT);
    private final JLabel opponentLabel = new JLabel();
    private final JLabel opponentLineLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JTextArea eventArea = new JTextArea();
    private final JLabel bankrollLabel = new JLabel();
    private final JLabel potLabel = new JLabel();
    private final JLabel recordLabel = new JLabel();
    private final JLabel streakLabel = new JLabel();
    private final JLabel shoeLabel = new JLabel();
    private final JProgressBar suspicionBar = new JProgressBar(0, 100);
    private final JTextField betField = new JTextField("25");
    private final JButton dealButton = new JButton("Deal In");
    private final JButton hitButton = new JButton("Take Card");
    private final JButton standButton = new JButton("Hold");
    private final JButton doubleButton = new JButton("Double");
    private final JButton splitButton = new JButton("Split");
    private final JButton cheatButton = new JButton("Cheat");
    private final JButton duelButton = new JButton("Draw");
    private final JButton resetButton = new JButton("Reset");
    private final JButton tutorialButton = new JButton("How To Play");
    private final TableCanvas tableCanvas = new TableCanvas();
    private JPanel sidebarPanel;
    private JScrollPane sidebarScrollPane;
    private JScrollPane storyPane;

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
    private boolean tutorialShown;

    public GamePanel(GameController controller, JFrame frame) {
        this.controller = controller;
        this.frame = frame;
        buildUi();
        bindActions();
        startLoadingOverlayTimer();
        refresh(false);
    }

    private void buildUi() {
        setOpaque(false);
        setLayout(new BorderLayout(24, 24));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 6, 22));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(22, 0));
        body.setOpaque(true);
        body.setBackground(new Color(24, 18, 30));
        tableCanvas.setPreferredSize(new Dimension(820, 620));
        body.add(tableCanvas, BorderLayout.CENTER);
        sidebarPanel = buildSidebar();
        sidebarScrollPane = new JScrollPane(
            sidebarPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        sidebarScrollPane.setOpaque(true);
        sidebarScrollPane.setBackground(new Color(24, 18, 30));
        sidebarScrollPane.getViewport().setOpaque(true);
        sidebarScrollPane.getViewport().setBackground(new Color(24, 18, 30));
        sidebarScrollPane.setBorder(BorderFactory.createEmptyBorder());
        sidebarScrollPane.setPreferredSize(new Dimension(380, 760));
        styleScrollPane(sidebarScrollPane);
        body.add(sidebarScrollPane, BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 190, 108, 110), 1),
            BorderFactory.createEmptyBorder(16, 18, 16, 18)
        ));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        titleLabel.setForeground(CREAM);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 42));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        displayNoteLabel.setForeground(GOLD);
        displayNoteLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        displayNoteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerMetaLabel.setForeground(GOLD);
        headerMetaLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(subtitleLabel);
        titleBlock.add(Box.createVerticalStrut(6));
        titleBlock.add(displayNoteLabel);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(headerMetaLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(true);
        sidebar.setBackground(new Color(24, 18, 30));
        sidebar.setPreferredSize(new Dimension(380, 760));
        sidebar.setMinimumSize(new Dimension(320, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel opponentCard = createCardPanel(PANEL_ALT);
        opponentLabel.setForeground(CREAM);
        opponentLabel.setFont(new Font("Serif", Font.BOLD, 28));
        opponentLineLabel.setForeground(MUTED);
        opponentLineLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        statusLabel.setForeground(GOLD);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        opponentCard.add(sectionLabel("Opponent"));
        opponentCard.add(Box.createVerticalStrut(10));
        opponentCard.add(opponentLabel);
        opponentCard.add(Box.createVerticalStrut(8));
        opponentCard.add(opponentLineLabel);
        opponentCard.add(Box.createVerticalStrut(12));
        opponentCard.add(statusLabel);

        JPanel storyCard = createCardPanel(PANEL);
        eventArea.setEditable(false);
        eventArea.setLineWrap(true);
        eventArea.setWrapStyleWord(true);
        eventArea.setForeground(CREAM);
        eventArea.setBackground(new Color(10, 14, 22));
        eventArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
        eventArea.setRows(9);
        eventArea.setColumns(26);
        eventArea.setMargin(new Insets(14, 14, 14, 14));
        storyPane = new JScrollPane(eventArea);
        storyPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        storyPane.setPreferredSize(new Dimension(336, 250));
        storyPane.setMinimumSize(new Dimension(280, 220));
        storyPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        storyPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 190, 108), 1),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        styleScrollPane(storyPane);
        JLabel hintLabel = new JLabel("SPACE advances story and still handles quick-draw timing.");
        hintLabel.setForeground(MUTED);
        hintLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        storyCard.add(sectionLabel("Story"));
        storyCard.add(Box.createVerticalStrut(10));
        storyCard.add(storyPane);
        storyCard.add(Box.createVerticalStrut(10));
        storyCard.add(hintLabel);

        JPanel statsCard = createCardPanel(PANEL);
        statsCard.add(sectionLabel("Table Readout"));
        statsCard.add(Box.createVerticalStrut(10));
        sidebar.add(opponentCard);
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(storyCard);
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(statsCard);
        finishSidebar(statsCard, sidebar);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel createCardPanel(Color background) {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(background);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 190, 108, 120), 1),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(GOLD);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void finishSidebar(JPanel statsCard, JPanel sidebar) {
        JPanel statGrid = new JPanel(new GridLayout(0, 1, 0, 8));
        statGrid.setOpaque(false);
        for (JLabel label : new JLabel[]{bankrollLabel, potLabel, recordLabel, streakLabel, shoeLabel}) {
            label.setForeground(CREAM);
            label.setFont(new Font("SansSerif", Font.BOLD, 15));
            statGrid.add(label);
        }
        statsCard.add(statGrid);
        statsCard.add(Box.createVerticalStrut(12));
        statsCard.add(sectionLabel("Suspicion"));
        statsCard.add(Box.createVerticalStrut(8));

        suspicionBar.setForeground(DANGER);
        suspicionBar.setBackground(new Color(16, 21, 31));
        suspicionBar.setStringPainted(true);
        suspicionBar.setFont(new Font("SansSerif", Font.BOLD, 12));
        suspicionBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 190, 108), 1),
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
        statsCard.add(suspicionBar);

        JPanel betCard = createCardPanel(PANEL_ALT);
        betCard.add(sectionLabel("Betting Window"));
        betCard.add(Box.createVerticalStrut(10));
        betField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        betField.setFont(new Font("SansSerif", Font.BOLD, 18));
        betField.setForeground(new Color(28, 23, 18));
        betField.setBackground(new Color(246, 239, 226));
        betField.setCaretColor(new Color(28, 23, 18));
        betField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 190, 108), 2),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        betField.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel betHint = new JLabel("Set the wager, then deal into the next hand.");
        betHint.setForeground(MUTED);
        betHint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        betHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        betCard.add(betField);
        betCard.add(Box.createVerticalStrut(10));
        betCard.add(betHint);

        JPanel actionsCard = createCardPanel(PANEL);
        actionsCard.add(sectionLabel("Actions"));
        actionsCard.add(Box.createVerticalStrut(10));
        styleButton(tutorialButton, new Color(38, 46, 68), CREAM);
        tutorialButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        tutorialButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        actionsCard.add(tutorialButton);
        actionsCard.add(Box.createVerticalStrut(10));
        JPanel buttonGrid = new JPanel(new GridLayout(4, 2, 10, 10));
        buttonGrid.setOpaque(false);
        styleButton(dealButton, GOLD, new Color(41, 29, 18));
        styleButton(hitButton, new Color(60, 109, 191), CREAM);
        styleButton(standButton, new Color(72, 131, 99), CREAM);
        styleButton(doubleButton, new Color(111, 76, 161), CREAM);
        styleButton(splitButton, new Color(79, 88, 108), CREAM);
        styleButton(cheatButton, new Color(171, 69, 61), CREAM);
        styleButton(duelButton, new Color(214, 96, 61), CREAM);
        styleButton(resetButton, new Color(84, 57, 42), CREAM);
        for (JButton button : new JButton[]{
            dealButton, hitButton, standButton, doubleButton,
            splitButton, cheatButton, duelButton, resetButton
        }) {
            buttonGrid.add(button);
        }
        actionsCard.add(buttonGrid);

        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(betCard);
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(actionsCard);
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 45), 1),
            BorderFactory.createEmptyBorder(12, 10, 12, 10)
        ));
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(48);
        scrollPane.getVerticalScrollBar().setOpaque(true);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setOpaque(true);
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getViewport().setBackground(new Color(10, 14, 22));
    }

    @Override
    public void doLayout() {
        applyResponsiveLayout();
        super.doLayout();
    }

    private void applyResponsiveLayout() {
        int width = Math.max(1080, getWidth());
        int height = Math.max(760, getHeight());
        int sidebarWidth = clamp(width / 4, 330, 430);
        int storyHeight = clamp(height / 4, 220, 320);

        if (sidebarPanel != null) {
            sidebarPanel.setPreferredSize(new Dimension(sidebarWidth, Math.max(620, height - 120)));
        }
        if (sidebarScrollPane != null) {
            sidebarScrollPane.setPreferredSize(new Dimension(sidebarWidth + 12, Math.max(540, height - 120)));
        }
        if (storyPane != null) {
            int paneWidth = Math.max(280, sidebarWidth - 40);
            storyPane.setPreferredSize(new Dimension(paneWidth, storyHeight));
            storyPane.setMinimumSize(new Dimension(paneWidth, 210));
            storyPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, storyHeight));
        }

        int titleSize = clamp(width / 34, 32, 46);
        int subtitleSize = clamp(width / 96, 13, 16);
        int noteSize = clamp(width / 120, 11, 13);
        int sideTitleSize = clamp(width / 54, 24, 30);
        int bodyTextSize = clamp(width / 98, 14, 16);
        int buttonSize = clamp(width / 104, 13, 15);

        titleLabel.setFont(new Font("Serif", Font.BOLD, titleSize));
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, subtitleSize));
        displayNoteLabel.setFont(new Font("SansSerif", Font.BOLD, noteSize));
        headerMetaLabel.setFont(new Font("SansSerif", Font.BOLD, noteSize + 1));
        opponentLabel.setFont(new Font("Serif", Font.BOLD, sideTitleSize));
        opponentLineLabel.setFont(new Font("SansSerif", Font.PLAIN, bodyTextSize));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, bodyTextSize));
        eventArea.setFont(new Font("SansSerif", Font.PLAIN, bodyTextSize));
        betField.setFont(new Font("SansSerif", Font.BOLD, Math.max(17, bodyTextSize + 2)));

        for (JButton button : new JButton[]{
            dealButton, hitButton, standButton, doubleButton,
            splitButton, cheatButton, duelButton, resetButton, tutorialButton
        }) {
            button.setFont(new Font("SansSerif", Font.BOLD, buttonSize));
        }
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
        tutorialButton.addActionListener(event -> showTutorialDialog());
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
            eventArea.setCaretPosition(0);
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
        opponentLineLabel.setText(wrapHtml(currentSnapshot.opponentLine(), sidebarWrapWidth() - 20));
        statusLabel.setText(wrapHtml(currentSnapshot.statusText(), sidebarWrapWidth()));
        subtitleLabel.setText(buildSubtitle(currentSnapshot));
        headerMetaLabel.setText("<html><div style='text-align:right'>Facing "
            + escapeHtml(currentSnapshot.opponentName())
            + "<br>Shoe " + currentSnapshot.shoeCards()
            + " / Suspicion " + currentSnapshot.suspicion() + "%"
            + "</div></html>");

        bankrollLabel.setText("Bankroll: $" + currentSnapshot.bankroll());
        potLabel.setText("Pot on felt: $" + currentSnapshot.handBets().stream().mapToInt(Integer::intValue).sum());
        recordLabel.setText("Record: " + currentSnapshot.wins() + " - " + currentSnapshot.losses());
        streakLabel.setText("Current streak: " + currentSnapshot.streak() + "   Best: " + currentSnapshot.bestStreak());
        shoeLabel.setText("Cards left in shoe: " + currentSnapshot.shoeCards());
        suspicionBar.setValue(currentSnapshot.suspicion());
        suspicionBar.setString(currentSnapshot.suspicion() + "% watched");

        if (animateText) {
            prepareNarrative(currentSnapshot.eventText());
        } else {
            eventArea.setText(currentSnapshot.eventText());
            eventArea.setCaretPosition(0);
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
        duelButton.setEnabled(currentSnapshot.duelActive());

        if (currentSnapshot.duelActive() && !currentSnapshot.duelCanDraw() && !duelSequenceQueued) {
            duelSequenceQueued = true;
            soundManager.playEffect("glass-break.wav");
            shakeFrame();
            beginDuelTimer();
        } else if (currentSnapshot.duelCanDraw()) {
            duelSequenceQueued = false;
            stopDuelTimer();
        } else if (!currentSnapshot.duelActive()) {
            duelSequenceQueued = false;
            stopDuelTimer();
        }

        if (!currentSnapshot.showLoading() && !tutorialShown) {
            tutorialShown = true;
            SwingUtilities.invokeLater(this::showTutorialDialog);
        }

        tableCanvas.repaint();
        revalidate();
        repaint();
    }

    private int sidebarWrapWidth() {
        int width = sidebarPanel == null || sidebarPanel.getWidth() <= 0 ? 360 : sidebarPanel.getWidth();
        return clamp(width - 56, 240, 360);
    }

    private String buildSubtitle(GameSnapshot snapshot) {
        if (snapshot.showLoading()) {
            return "The lamps are warming up and the felt is still settling.";
        }
        if (snapshot.duelActive()) {
            return snapshot.duelCanDraw() ? "Steel is out. Move now." : "The room freezes before the draw.";
        }
        if (snapshot.roundActive()) {
            return "The hand is live. Read the table and press the right edge.";
        }
        return "The next hand is yours to start when the bet window looks right.";
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
            if (page.length() + part.length() > 150 && page.length() > 0) {
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
        eventArea.setCaretPosition(0);
        if (typeTimer != null) {
            typeTimer.stop();
        }
        typeTimer = new Timer(28, event -> {
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
        String prompt = narrativePageIndex < narrativePages.size() - 1 ? "\n\n[Press SPACE to continue]" : "";
        eventArea.setText(currentPageText + prompt);
        eventArea.setCaretPosition(0);
    }

    private int parseBet() {
        try {
            return Integer.parseInt(betField.getText().trim());
        } catch (NumberFormatException exception) {
            return 10;
        }
    }

    private void showTutorialDialog() {
        JTextArea tutorialArea = new JTextArea(buildTutorialText());
        tutorialArea.setEditable(false);
        tutorialArea.setLineWrap(true);
        tutorialArea.setWrapStyleWord(true);
        tutorialArea.setCaretPosition(0);
        tutorialArea.setMargin(new Insets(14, 14, 14, 14));
        tutorialArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
        tutorialArea.setForeground(new Color(28, 24, 21));
        tutorialArea.setBackground(new Color(248, 242, 231));

        JScrollPane scrollPane = new JScrollPane(tutorialArea);
        scrollPane.setPreferredSize(new Dimension(680, 520));
        styleScrollPane(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(228, 190, 108), 1));
        scrollPane.getViewport().setBackground(new Color(248, 242, 231));

        JOptionPane.showMessageDialog(
            frame,
            scrollPane,
            "Blackjack Saloon Tutorial",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String buildTutorialText() {
        return """
            WELCOME TO BLACKJACK SALOON

            Display note:
            The layout now scales with the window, though fullscreen still gives the cleanest view of the whole table.

            Your goal:
            Beat the dealer without going over 21. Number cards are worth their number. Face cards count as 10. Aces count as 11 unless that would bust your hand, then they count as 1.

            How a round works:
            1. Enter a bet in the Betting Window.
            2. Press Deal In to receive your opening hand.
            3. The dealer gets cards too, but during a live hand only part of the dealer value is shown.
            4. Choose actions until you stand, bust, or finish the hand.
            5. When the hand ends, winnings or losses are applied to your bankroll.

            Core actions:
            Deal In: Starts the next round using the bet you entered.
            Take Card: Adds one card to your active hand.
            Hold: Ends your play and lets the dealer finish.
            Double: Doubles the current hand's bet, deals one final card, and locks the hand.
            Split: If available, breaks a matching pair into two hands.
            Cheat: Swaps in a better card, but raises suspicion.
            Draw: Used only during duel moments, if that system is active.
            Reset: Wipes your save progress and bankroll back to the default table state.

            How winning works:
            If your final value is higher than the dealer's without busting, you win.
            If the dealer busts and you do not, you win.
            If you and the dealer tie, it is a push and your bet is returned.
            A natural blackjack pays better than a normal win.

            Tavern systems:
            Bankroll shows how much fake money you still have.
            Pot on felt shows how much is currently committed in the hand.
            Suspicion rises when you cheat. High suspicion is dangerous in the saloon and can affect how the room reacts.
            Record and streak track your performance across hands.
            The Story panel delivers tavern flavor, warnings, and event text. Press SPACE to move through longer story beats.

            Good beginner strategy:
            Stay under 21.
            Use Hold when you are happy with your total and want the dealer to take the risk.
            Be careful with Double because it commits more money immediately.
            Avoid Cheat until you understand the normal flow of the table.

            Tavern flavor guide:
            The named opponent is the personality across the table.
            The center felt shows the live hand and the chips in play.
            The lower hand panel is your active hand area.
            The right-side dashboard is your control booth for bets, status, and actions.

            If you ever want this guide again, press the How To Play button in the Actions panel.
            """;
    }

    private List<String> parseCards(String description) {
        List<String> cards = new ArrayList<>();
        if (description == null || description.isBlank()) {
            return cards;
        }
        int start = 0;
        while (start < description.length()) {
            int open = description.indexOf('[', start);
            if (open < 0) {
                break;
            }
            int close = description.indexOf(']', open + 1);
            if (close < 0) {
                break;
            }
            cards.add(description.substring(open + 1, close).trim());
            start = close + 1;
        }
        return cards;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String wrapHtml(String text, int width) {
        return "<html><div style='width:" + width + "px'>" + escapeHtml(text).replace("\n", "<br>") + "</div></html>";
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(122, 154, 214);
            thumbDarkShadowColor = thumbColor;
            thumbHighlightColor = thumbColor;
            thumbLightShadowColor = thumbColor;
            trackColor = new Color(16, 21, 31);
            trackHighlightColor = trackColor;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorder(BorderFactory.createEmptyBorder());
            return button;
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setColor(new Color(16, 21, 31));
            g2.fillRoundRect(trackBounds.x + 2, trackBounds.y, trackBounds.width - 4, trackBounds.height, 10, 10);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(122, 154, 214));
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 12, 12);
            g2.setColor(new Color(228, 190, 108, 120));
            g2.drawRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 5, thumbBounds.height - 5, 12, 12);
            g2.dispose();
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(10, 36);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setPaint(new GradientPaint(0, 0, BACKDROP_TOP, 0, getHeight(), BACKDROP_BOTTOM));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setComposite(AlphaComposite.SrcOver.derive(0.2f));
        g2.setPaint(new GradientPaint(0, 0, new Color(205, 77, 60), getWidth(), getHeight(), new Color(0, 0, 0, 0)));
        g2.fillOval(-180, -120, 520, 360);
        g2.fillOval(getWidth() - 320, -90, 390, 280);
        g2.dispose();
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        if (loadingOverlayAlpha <= 0f) {
            return;
        }

        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, loadingOverlayAlpha))));
        g2.setColor(new Color(5, 8, 14));
        g2.fillRect(0, 0, getWidth(), getHeight());

        float pulse = Math.max(0.42f, Math.min(1f, 0.72f + (float) Math.sin(loadingTextPulse) * 0.2f));
        g2.setComposite(AlphaComposite.SrcOver.derive(pulse));
        g2.setColor(GOLD);
        g2.setFont(new Font("Serif", Font.BOLD, 44));
        String title = "SALOON OPENING";
        int centerY = getHeight() / 2 - 22;
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (getWidth() - titleWidth) / 2, centerY);
        g2.setColor(CREAM);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String line = "The layout scales with the window. The room settles in a moment.";
        int lineWidth = g2.getFontMetrics().stringWidth(line);
        g2.drawString(line, (getWidth() - lineWidth) / 2, centerY + 40);
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
            g2.setColor(new Color(24, 18, 30));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int w = getWidth();
            int h = getHeight();
            double scale = Math.min((w - 40) / 930.0, (h - 78) / 760.0);
            scale = Math.max(0.58, scale);
            int stageW = (int) Math.round(930 * scale);
            int stageH = (int) Math.round(760 * scale);
            int stageX = (w - stageW) / 2;
            int stageY = Math.max(8, h - stageH - 10);

            g2.translate(stageX, stageY);
            g2.scale(scale, scale);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.22f));
            g2.setColor(Color.BLACK);
            g2.fillRoundRect(10, 16, 930, 760, 54, 54);
            g2.setComposite(AlphaComposite.SrcOver);

            drawRoomBackdrop(g2, 0, 0, 930, 760);
            drawTablePerspective(g2, 0, 0, 930, 760);
            g2.dispose();
        }

        private void drawRoomBackdrop(Graphics2D g2, int x, int y, int width, int height) {
            g2.setPaint(new GradientPaint(x, y, new Color(35, 25, 31), x, y + height, new Color(18, 14, 20)));
            g2.fillRoundRect(x, y, width, height, 48, 48);

            int curtainWidth = Math.max(120, width / 5);
            g2.setColor(new Color(74, 28, 32, 165));
            g2.fillArc(x - curtainWidth / 2, y - 30, curtainWidth * 2, 220, 90, 180);
            g2.fillArc(x + width - curtainWidth * 3 / 2, y - 20, curtainWidth * 2, 220, 270, 180);

            g2.setColor(new Color(255, 216, 137, 28));
            g2.fillOval(x + width / 2 - 180, y + 10, 360, 160);
            g2.fillOval(x + width / 2 - 260, y + 40, 520, 110);

            int backBarY = y + 36;
            g2.setColor(new Color(22, 17, 25, 170));
            g2.fillRoundRect(x + 130, backBarY, width - 260, 54, 24, 24);
            g2.setColor(new Color(232, 193, 108, 95));
            g2.drawRoundRect(x + 130, backBarY, width - 260, 54, 24, 24);
            g2.setFont(new Font("Serif", Font.BOLD, 20));
            g2.setColor(CREAM);
            String banner = "Facing " + currentSnapshot.opponentName();
            int bannerWidth = g2.getFontMetrics().stringWidth(banner);
            g2.drawString(banner, x + width / 2 - bannerWidth / 2, backBarY + 35);
        }

        private void drawTablePerspective(Graphics2D g2, int x, int y, int width, int height) {
            int railX = x + 42;
            int railY = y + 132;
            int railW = width - 84;
            int railH = height + 300;
            int feltInset = 20;

            g2.setPaint(new GradientPaint(railX, railY, new Color(132, 77, 41), railX, railY + 180, TABLE_EDGE));
            g2.fillArc(railX, railY, railW, railH, 0, 180);

            g2.setPaint(new GradientPaint(railX + feltInset, railY + feltInset,
                new Color(24, 101, 80), railX + feltInset, railY + feltInset + 260, TABLE_BOTTOM));
            g2.fillArc(railX + feltInset, railY + feltInset, railW - feltInset * 2, railH - feltInset * 2, 0, 180);

            g2.setColor(new Color(244, 207, 121, 120));
            g2.setStroke(new BasicStroke(3f));
            g2.drawArc(railX + feltInset + 14, railY + feltInset + 14, railW - (feltInset + 14) * 2, railH - (feltInset + 14) * 2, 0, 180);

            int centerX = x + width / 2;
            int centerY = railY + railH / 2;
            int radiusX = railW / 2 - 36;
            int radiusY = railH / 2 - 48;

            drawTableBranding(g2, centerX, y + 350);
            drawRailPlayers(g2, centerX, centerY, radiusX, radiusY);
            drawDealerReach(g2, centerX, y + 150);
            drawPotZone(g2, centerX, y + 396);
            drawHandZone(g2, x, y, width, height, centerX);
            drawPrompt(g2, centerX, y + height - 74);
        }

        private void drawTableBranding(Graphics2D g2, int centerX, int centerY) {
            g2.setColor(new Color(241, 225, 179, 155));
            g2.setFont(new Font("Serif", Font.BOLD, 36));
            String title = "BLACKJACK";
            int titleWidth = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, centerX - titleWidth / 2, centerY);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            String sub = "INSURANCE PAYS 2 TO 1";
            int subWidth = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, centerX - subWidth / 2, centerY + 26);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new Arc2D.Double(centerX - 260, centerY - 48, 520, 110, 18, 144, Arc2D.OPEN));
        }

        private void drawRailPlayers(Graphics2D g2, int centerX, int centerY, int radiusX, int radiusY) {
            drawSeatScene(g2, centerX, centerY, radiusX, radiusY, 154, "Miner", new Color(71, 52, 45), 2, false, false);
            drawSeatScene(g2, centerX, centerY, radiusX, radiusY, 128, "Mae", new Color(77, 49, 41), 3, false, false);
            drawSeatScene(g2, centerX, centerY, radiusX, radiusY, 90, currentSnapshot.opponentName(), new Color(53, 61, 82), 4, true, true);
            drawSeatScene(g2, centerX, centerY, radiusX, radiusY, 52, "Boone", new Color(70, 52, 43), 2, false, false);
            drawSeatScene(g2, centerX, centerY, radiusX, radiusY, 26, "Doc", new Color(58, 54, 70), 3, false, false);
        }

        private void drawSeatScene(Graphics2D g2, int centerX, int centerY, int radiusX, int radiusY,
                                   int angleDegrees, String name, Color coatColor, int chipCount,
                                   boolean dealerCards, boolean highlight) {
            Point seat = pointOnEllipse(centerX, centerY, radiusX, radiusY, angleDegrees);
            Point chipSpot = pointOnEllipse(centerX, centerY, radiusX - 130, radiusY - 110, angleDegrees);
            Point cardSpot = pointOnEllipse(centerX, centerY, radiusX - 80, radiusY - 70, angleDegrees);

            drawRailCharacter(g2, seat.x, seat.y - 20, coatColor);
            drawBetRing(g2, chipSpot.x, chipSpot.y, highlight ? 30 : 24);
            drawChipStack(g2, chipSpot.x - 18, chipSpot.y - 8, highlight ? new Color(222, 183, 88) : new Color(194, 72, 56), chipCount);

            List<String> cards = dealerCards ? parseCards(currentSnapshot.dealerCards()) : List.of("10", "8");
            drawCardFan(g2, cards, cardSpot.x, cardSpot.y, dealerCards ? 72 : 60, dealerCards ? 98 : 84, 18);
        }

        private void drawRailCharacter(Graphics2D g2, int x, int y, Color coatColor) {
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(x - 52, y - 18, 104, 30);

            g2.setColor(new Color(52, 34, 26));
            g2.fillOval(x - 20, y - 74, 40, 44);
            g2.setColor(coatColor);
            g2.fillRoundRect(x - 58, y - 38, 116, 64, 28, 28);
            g2.setColor(new Color(235, 209, 179));
            g2.fillOval(x - 16, y - 64, 32, 34);
            g2.setColor(new Color(28, 20, 17));
            g2.drawOval(x - 16, y - 64, 32, 34);
        }

        private void drawDealerReach(Graphics2D g2, int centerX, int topY) {
            Path2D.Double leftArm = new Path2D.Double();
            leftArm.moveTo(centerX - 70, topY);
            leftArm.curveTo(centerX - 120, topY + 36, centerX - 68, topY + 106, centerX - 16, topY + 156);
            leftArm.lineTo(centerX + 4, topY + 144);
            leftArm.curveTo(centerX - 44, topY + 98, centerX - 84, topY + 46, centerX - 38, topY + 2);
            leftArm.closePath();
            g2.setColor(new Color(70, 91, 130));
            g2.fill(leftArm);

            Path2D.Double rightArm = new Path2D.Double();
            rightArm.moveTo(centerX + 74, topY + 4);
            rightArm.curveTo(centerX + 112, topY + 48, centerX + 66, topY + 112, centerX + 14, topY + 170);
            rightArm.lineTo(centerX - 8, topY + 158);
            rightArm.curveTo(centerX + 40, topY + 100, centerX + 78, topY + 42, centerX + 42, topY);
            rightArm.closePath();
            g2.fill(rightArm);

            g2.setColor(new Color(225, 196, 172));
            g2.fillOval(centerX - 22, topY + 150, 44, 26);
            g2.fillRoundRect(centerX - 12, topY + 118, 24, 44, 10, 10);
        }

        private void drawPotZone(Graphics2D g2, int centerX, int centerY) {
            int pot = currentSnapshot.handBets().stream().mapToInt(Integer::intValue).sum();
            g2.setColor(new Color(11, 16, 24, 185));
            g2.fillRoundRect(centerX - 128, centerY - 54, 256, 118, 28, 28);
            g2.setColor(new Color(255, 220, 137, 110));
            g2.drawRoundRect(centerX - 128, centerY - 54, 256, 118, 28, 28);
            g2.setColor(MUTED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            String stake = currentSnapshot.roundActive() ? "Pot on felt" : "Table stake";
            int sw = g2.getFontMetrics().stringWidth(stake);
            g2.drawString(stake, centerX - sw / 2, centerY - 22);
            g2.setColor(GOLD);
            g2.setFont(new Font("Serif", Font.BOLD, 34));
            String potText = "$" + pot;
            int pw = g2.getFontMetrics().stringWidth(potText);
            g2.drawString(potText, centerX - pw / 2, centerY + 12);
            drawChipStack(g2, centerX - 86, centerY + 42, new Color(193, 72, 56), Math.max(2, Math.min(6, pot / 40 + 2)));
            drawChipStack(g2, centerX - 14, centerY + 32, new Color(44, 46, 56), Math.max(3, Math.min(7, pot / 35 + 3)));
            drawChipStack(g2, centerX + 58, centerY + 42, new Color(75, 109, 211), Math.max(2, Math.min(6, pot / 50 + 2)));
        }

        private void drawChipStack(Graphics2D g2, int x, int y, Color color, int count) {
            for (int i = 0; i < count; i++) {
                int yy = y - i * 8;
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(x + 3, yy + 3, 36, 16);
                g2.setColor(color);
                g2.fillOval(x, yy, 36, 16);
                g2.setColor(CREAM);
                g2.drawOval(x, yy, 36, 16);
                g2.drawLine(x + 6, yy + 8, x + 30, yy + 8);
            }
        }

        private Point pointOnEllipse(int centerX, int centerY, int radiusX, int radiusY, int angleDegrees) {
            double radians = Math.toRadians(angleDegrees);
            int px = centerX + (int) Math.round(Math.cos(radians) * radiusX);
            int py = centerY - (int) Math.round(Math.sin(radians) * radiusY);
            return new Point(px, py);
        }

        private void drawHandZone(Graphics2D g2, int x, int y, int width, int height, int centerX) {
            drawPlayerHands(g2, x, y, width, height, centerX);
            drawPlayerFists(g2, centerX, y + height - 124);
        }

        private void drawPlayerHands(Graphics2D g2, int x, int y, int width, int height, int centerX) {
            List<String> hands = currentSnapshot.playerHands();
            int count = Math.max(1, hands.size());
            int cardZoneY = y + height - 224;
            int spread = count == 1 ? 0 : 210;

            for (int i = 0; i < count; i++) {
                int handCenter = centerX + (i - (count - 1) / 2) * spread;
                boolean active = i < hands.size() && i == currentSnapshot.activeHandIndex() && currentSnapshot.roundActive();
                int bet = i < currentSnapshot.handBets().size() ? currentSnapshot.handBets().get(i) : 0;
                int value = i < currentSnapshot.playerValues().size() ? currentSnapshot.playerValues().get(i) : 0;
                List<String> cards = i < hands.size() ? parseCards(hands.get(i)) : List.of();

                drawBetRing(g2, handCenter, y + height - 276, active ? 36 : 30);
                drawCardFan(g2, cards, handCenter, cardZoneY, 74, 102, 18);
                drawBadge(g2, "Bet $" + bet, handCenter - 56, y + height - 334, 112, 26,
                    active ? new Color(223, 186, 104) : new Color(77, 50, 31), active ? new Color(32, 24, 18) : GOLD);
                drawBadge(g2, "Value " + value, handCenter - 56, y + height - 154, 112, 24,
                    new Color(13, 20, 28, 215), CREAM);
            }
        }

        private void drawPlayerFists(Graphics2D g2, int centerX, int baselineY) {
            drawPlayerFist(g2, centerX - 162, baselineY, true);
            drawPlayerFist(g2, centerX + 162, baselineY, false);
        }

        private void drawPlayerFist(Graphics2D g2, int x, int y, boolean left) {
            Path2D.Double sleeve = new Path2D.Double();
            if (left) {
                sleeve.moveTo(x - 84, y + 8);
                sleeve.lineTo(x - 18, y - 10);
                sleeve.lineTo(x + 4, y + 46);
                sleeve.lineTo(x - 66, y + 66);
            } else {
                sleeve.moveTo(x + 84, y + 8);
                sleeve.lineTo(x + 18, y - 10);
                sleeve.lineTo(x - 4, y + 46);
                sleeve.lineTo(x + 66, y + 66);
            }
            sleeve.closePath();
            g2.setColor(new Color(63, 42, 27));
            g2.fill(sleeve);

            Path2D.Double fist = new Path2D.Double();
            if (left) {
                fist.moveTo(x - 8, y - 4);
                fist.curveTo(x - 40, y - 8, x - 54, y + 22, x - 46, y + 42);
                fist.curveTo(x - 36, y + 62, x - 8, y + 70, x + 18, y + 52);
                fist.curveTo(x + 28, y + 42, x + 28, y + 12, x + 4, y + 0);
            } else {
                fist.moveTo(x + 8, y - 4);
                fist.curveTo(x + 40, y - 8, x + 54, y + 22, x + 46, y + 42);
                fist.curveTo(x + 36, y + 62, x + 8, y + 70, x - 18, y + 52);
                fist.curveTo(x - 28, y + 42, x - 28, y + 12, x - 4, y + 0);
            }
            fist.closePath();
            g2.setColor(new Color(118, 88, 58));
            g2.fill(fist);
            g2.setColor(new Color(55, 37, 23));
            g2.draw(fist);

            for (int i = 0; i < 4; i++) {
                int knuckleX = left ? x - 34 + i * 12 : x - 14 + i * 12;
                g2.setColor(new Color(155, 123, 88));
                g2.fillOval(knuckleX, y + 6, 12, 12);
            }
        }

        private void drawBetRing(Graphics2D g2, int centerX, int centerY, int radius) {
            g2.setColor(new Color(239, 230, 204, 185));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval(centerX - radius, centerY - radius / 2, radius * 2, radius);
        }

        private void drawBadge(Graphics2D g2, String text, int x, int y, int width, int height, Color fill, Color textColor) {
            g2.setColor(fill);
            g2.fillRoundRect(x, y, width, height, 14, 14);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.drawRoundRect(x, y, width, height, 14, 14);
            g2.setColor(textColor);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, x + (width - tw) / 2, y + 16);
        }

        private void drawPrompt(Graphics2D g2, int centerX, int y) {
            String prompt;
            if (currentSnapshot.showLoading()) {
                prompt = "The room is still setting the table.";
            } else if (currentSnapshot.duelActive()) {
                prompt = currentSnapshot.duelCanDraw() ? "Draw now." : "Hands off the iron.";
            } else if (currentSnapshot.roundActive()) {
                prompt = "The hand is live. Play from your seat.";
            } else if (currentSnapshot.canDeal()) {
                prompt = "Set a wager, then deal into the next round.";
            } else {
                prompt = "The table is paused.";
            }
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            int tw = g2.getFontMetrics().stringWidth(prompt);
            int bw = tw + 34;
            int bx = centerX - bw / 2;
            g2.setColor(new Color(11, 14, 21, 170));
            g2.fillRoundRect(bx, y, bw, 28, 16, 16);
            g2.setColor(new Color(255, 220, 135, 120));
            g2.drawRoundRect(bx, y, bw, 28, 16, 16);
            g2.setColor(CREAM);
            g2.drawString(prompt, centerX - tw / 2, y + 19);
        }

        private void drawCardFan(Graphics2D g2, List<String> cards, int centerX, int y, int cardWidth, int cardHeight, int overlap) {
            if (cards.isEmpty()) {
                drawGhostCard(g2, centerX - cardWidth / 2, y - cardHeight / 2, cardWidth, cardHeight);
                return;
            }
            int totalWidth = cards.size() * cardWidth - Math.max(0, cards.size() - 1) * overlap;
            int startX = centerX - totalWidth / 2;
            for (int i = 0; i < cards.size(); i++) {
                int cardX = startX + i * (cardWidth - overlap);
                drawCard(g2, cardX, y - cardHeight / 2, cardWidth, cardHeight, cards.get(i));
            }
        }

        private void drawGhostCard(Graphics2D g2, int x, int y, int width, int height) {
            g2.setColor(new Color(255, 255, 255, 24));
            g2.fillRoundRect(x, y, width, height, 18, 18);
            g2.setColor(new Color(255, 255, 255, 58));
            g2.drawRoundRect(x, y, width, height, 18, 18);
            g2.setColor(MUTED);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            String text = "WAITING";
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, x + (width - tw) / 2, y + height / 2 + 4);
        }

        private void drawCard(Graphics2D g2, int x, int y, int width, int height, String token) {
            if ("?".equals(token)) {
                drawHiddenCard(g2, x, y, width, height);
                return;
            }

            Color accent = switch (token) {
                case "A", "K", "Q", "J" -> new Color(164, 59, 70);
                case "10" -> new Color(197, 145, 57);
                default -> new Color(50, 77, 128);
            };

            g2.setColor(new Color(0, 0, 0, 72));
            g2.fillRoundRect(x + 4, y + 6, width, height, 18, 18);
            g2.setPaint(new GradientPaint(x, y, new Color(255, 249, 240), x, y + height, new Color(235, 225, 210)));
            g2.fillRoundRect(x, y, width, height, 18, 18);
            g2.setColor(new Color(72, 51, 35));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, width, height, 18, 18);
            g2.setColor(accent);
            g2.fillRoundRect(x + 10, y + 10, width - 20, 22, 12, 12);
            g2.setColor(CREAM);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("CARD", x + width / 2 - 17, y + 25);
            g2.setColor(new Color(38, 30, 23));
            g2.setFont(new Font("Serif", Font.BOLD, 34));
            int tw = g2.getFontMetrics().stringWidth(token);
            g2.drawString(token, x + (width - tw) / 2, y + 70);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 62));
            g2.setFont(new Font("Serif", Font.BOLD, 46));
            int mw = g2.getFontMetrics().stringWidth(token);
            g2.drawString(token, x + (width - mw) / 2, y + height - 22);
        }

        private void drawHiddenCard(Graphics2D g2, int x, int y, int width, int height) {
            g2.setColor(new Color(0, 0, 0, 72));
            g2.fillRoundRect(x + 4, y + 6, width, height, 18, 18);
            g2.setPaint(new GradientPaint(x, y, new Color(38, 44, 63), x, y + height, new Color(17, 24, 39)));
            g2.fillRoundRect(x, y, width, height, 18, 18);
            g2.setColor(GOLD);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, width, height, 18, 18);
            g2.setColor(new Color(255, 255, 255, 35));
            for (int offset = -height; offset < width; offset += 12) {
                g2.drawLine(x + offset, y + height, x + offset + height, y);
            }
            g2.setColor(CREAM);
            g2.setFont(new Font("Serif", Font.BOLD, 38));
            g2.drawString("?", x + width / 2 - 9, y + height / 2 + 14);
        }
    }
}
