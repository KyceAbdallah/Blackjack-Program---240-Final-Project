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
import java.awt.event.KeyEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
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
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
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
    private Timer railTurnTimer;
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
    private String narrativeHistory = "";
    private String lastNarrativeText = "";
    private String currentEntryPrefix = "";
    private String currentEntryText = "";
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
        add(buildControlDock(), BorderLayout.SOUTH);
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
        JPanel controlTipCard = createCardPanel(PANEL_ALT);
        controlTipCard.add(sectionLabel("Controls"));
        controlTipCard.add(Box.createVerticalStrut(10));
        JLabel controlTip = new JLabel(
            "<html><div style='width:290px'>Gameplay controls stay docked below the table so you can always deal, hit, or hold without hunting through the sidebar.</div></html>"
        );
        controlTip.setForeground(MUTED);
        controlTip.setFont(new Font("SansSerif", Font.PLAIN, 13));
        controlTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel shortcutTip = new JLabel("Enter deal. H hit. S hold. D double. P split. C cheat.");
        shortcutTip.setForeground(GOLD);
        shortcutTip.setFont(new Font("SansSerif", Font.BOLD, 12));
        shortcutTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlTipCard.add(controlTip);
        controlTipCard.add(Box.createVerticalStrut(10));
        controlTipCard.add(shortcutTip);
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(controlTipCard);
    }

    private JPanel buildControlDock() {
        JPanel dock = new JPanel(new BorderLayout(18, 12));
        dock.setOpaque(true);
        dock.setBackground(PANEL);
        dock.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 190, 108, 110), 1),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        JLabel controlTitle = new JLabel("TABLE CONTROLS");
        controlTitle.setForeground(GOLD);
        controlTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel controlHint = new JLabel("Enter = Deal   H = Hit   S = Hold   D = Double   P = Split   C = Cheat");
        controlHint.setForeground(MUTED);
        controlHint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        styleButton(tutorialButton, new Color(38, 46, 68), CREAM);
        tutorialButton.setText("How To Play");
        tutorialButton.setPreferredSize(new Dimension(148, 40));
        header.add(controlTitle, BorderLayout.WEST);
        header.add(controlHint, BorderLayout.CENTER);
        header.add(tutorialButton, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout(18, 0));
        content.setOpaque(false);

        JPanel betPanel = new JPanel();
        betPanel.setOpaque(false);
        betPanel.setLayout(new BoxLayout(betPanel, BoxLayout.Y_AXIS));
        JLabel wagerLabel = sectionLabel("Wager");
        wagerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        betField.setMaximumSize(new Dimension(220, 40));
        betField.setPreferredSize(new Dimension(220, 40));
        betField.setFont(new Font("SansSerif", Font.BOLD, 18));
        betField.setForeground(new Color(28, 23, 18));
        betField.setBackground(new Color(246, 239, 226));
        betField.setCaretColor(new Color(28, 23, 18));
        betField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(228, 190, 108), 2),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        betField.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel betHint = new JLabel("Type a bet, then press Enter or Deal In.");
        betHint.setForeground(MUTED);
        betHint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        betHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        betPanel.add(wagerLabel);
        betPanel.add(Box.createVerticalStrut(8));
        betPanel.add(betField);
        betPanel.add(Box.createVerticalStrut(8));
        betPanel.add(betHint);

        JPanel actionGrid = new JPanel(new GridLayout(2, 4, 10, 10));
        actionGrid.setOpaque(false);
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
            actionGrid.add(button);
        }

        content.add(betPanel, BorderLayout.WEST);
        content.add(actionGrid, BorderLayout.CENTER);

        dock.add(header, BorderLayout.NORTH);
        dock.add(content, BorderLayout.CENTER);
        return dock;
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
        dealButton.addActionListener(event -> handleDeal());
        hitButton.addActionListener(event -> handleHit());
        standButton.addActionListener(event -> handleStand());
        doubleButton.addActionListener(event -> handleDoubleDown());
        splitButton.addActionListener(event -> handleSplit());
        cheatButton.addActionListener(event -> handleCheat());
        tutorialButton.addActionListener(event -> showTutorialDialog());
        duelButton.addActionListener(event -> fireDuelDraw());
        resetButton.addActionListener(event -> handleReset());
        betField.addActionListener(event -> handleDeal());
        registerKeyboardShortcuts();
        if (frame.getRootPane() != null) {
            frame.getRootPane().setDefaultButton(dealButton);
        }
    }

    public boolean hasPendingNarrative() {
        return typeTimer != null && typeTimer.isRunning()
            || narrativeAwaitingContinue
            || narrativePageIndex < narrativePages.size() - 1;
    }

    public void advanceNarrative() {
        if (typeTimer != null && typeTimer.isRunning()) {
            typeTimer.stop();
            appendCurrentPage();
            if (narrativePageIndex < narrativePages.size() - 1) {
                narrativeAwaitingContinue = true;
                updateContinuePrompt();
            } else {
                finishNarrativeEntry();
            }
            return;
        }
        if (narrativePageIndex < narrativePages.size() - 1) {
            narrativeAwaitingContinue = false;
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
        tableCanvas.setSnapshot(currentSnapshot);
        opponentLabel.setText(currentSnapshot.opponentName());
        opponentLineLabel.setText(wrapHtml(currentSnapshot.opponentLine(), sidebarWrapWidth() - 20));
        statusLabel.setText(wrapHtml(currentSnapshot.statusText(), sidebarWrapWidth()));
        subtitleLabel.setText(buildSubtitle(currentSnapshot));
        headerMetaLabel.setText("<html><div style='text-align:right'>Facing "
            + escapeHtml(currentSnapshot.opponentName())
            + "<br>Shoe " + currentSnapshot.shoeCards()
            + " / Suspicion " + currentSnapshot.suspicion() + "%"
            + "</div></html>");

        int playerPot = currentSnapshot.handBets().stream().mapToInt(Integer::intValue).sum();
        int railPot = currentSnapshot.railSeats().stream().mapToInt(RailSeatSnapshot::bet).sum();
        int tablePot = playerPot + railPot;
        bankrollLabel.setText("Bankroll: $" + currentSnapshot.bankroll());
        potLabel.setText("Pot on felt: $" + tablePot + "   You: $" + playerPot + "   Rail: $" + railPot);
        recordLabel.setText("Record: " + currentSnapshot.wins() + " - " + currentSnapshot.losses());
        streakLabel.setText("Current streak: " + currentSnapshot.streak() + "   Best: " + currentSnapshot.bestStreak());
        shoeLabel.setText("Cards left in shoe: " + currentSnapshot.shoeCards());
        suspicionBar.setValue(currentSnapshot.suspicion());
        suspicionBar.setString(currentSnapshot.suspicion() + "% watched");

        if (animateText) {
            prepareNarrative(currentSnapshot.eventText());
        } else {
            appendNarrativeImmediately(currentSnapshot.eventText());
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

        if (currentSnapshot.railTurnsActive()) {
            beginRailTurnTimer();
        } else {
            stopRailTurnTimer();
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
        if (snapshot.railTurnsActive()) {
            return "The rail is playing out the hand one seat at a time.";
        }
        if (snapshot.roundActive()) {
            return "The hand is live. Use the docked controls below the table or the keyboard shortcuts.";
        }
        return "Set your wager in the controls below, then deal into the next hand.";
    }

    private void prepareNarrative(String text) {
        String normalized = normalizeNarrativeText(text);
        if (normalized.equals(lastNarrativeText)) {
            return;
        }
        lastNarrativeText = normalized;
        narrativePages = splitNarrative(normalized);
        narrativePageIndex = 0;
        currentEntryPrefix = narrativeHistory.isBlank() ? "" : narrativeHistory + "\n\n";
        currentEntryText = "";
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
        renderNarrative(currentEntryText);
        if (typeTimer != null) {
            typeTimer.stop();
        }
        typeTimer = new Timer(28, event -> {
            if (currentCharIndex >= currentPageText.length()) {
                typeTimer.stop();
                appendCurrentPage();
                if (narrativePageIndex < narrativePages.size() - 1) {
                    narrativeAwaitingContinue = true;
                    updateContinuePrompt();
                } else {
                    finishNarrativeEntry();
                }
                return;
            }
            char next = currentPageText.charAt(currentCharIndex);
            renderNarrative(currentEntryText + currentPageText.substring(0, currentCharIndex + 1));
            if (!Character.isWhitespace(next) && currentCharIndex % 3 == 0) {
                soundManager.playVoiceSnippet("Papyrus Dialogue Sound Effect");
            }
            currentCharIndex++;
        });
        typeTimer.start();
    }

    private void updateContinuePrompt() {
        String prompt = narrativePageIndex < narrativePages.size() - 1 ? "\n\n[Press SPACE to continue]" : "";
        renderNarrative(currentEntryText + prompt);
    }

    private void appendNarrativeImmediately(String text) {
        String normalized = normalizeNarrativeText(text);
        if (normalized.equals(lastNarrativeText)) {
            return;
        }
        lastNarrativeText = normalized;
        narrativePages = List.of(normalized);
        narrativePageIndex = 0;
        currentPageText = normalized;
        currentEntryPrefix = narrativeHistory.isBlank() ? "" : narrativeHistory + "\n\n";
        currentEntryText = normalized;
        narrativeAwaitingContinue = false;
        finishNarrativeEntry();
    }

    private void appendCurrentPage() {
        if (currentPageText.isBlank()) {
            return;
        }
        currentEntryText = currentEntryText.isBlank()
            ? currentPageText
            : currentEntryText + "\n\n" + currentPageText;
    }

    private void finishNarrativeEntry() {
        narrativeAwaitingContinue = false;
        narrativeHistory = currentEntryPrefix + currentEntryText;
        renderNarrative(currentEntryText);
    }

    private void renderNarrative(String activeEntryText) {
        StringBuilder builder = new StringBuilder();
        builder.append(currentEntryPrefix);
        builder.append(activeEntryText);
        eventArea.setText(builder.toString());
        eventArea.setCaretPosition(eventArea.getDocument().getLength());
    }

    private String normalizeNarrativeText(String text) {
        return text == null ? "" : text.trim();
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
            The live gameplay controls stay docked below the table so they remain visible.

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

            Keyboard shortcuts:
            Enter deals using the current wager.
            H hits, S holds, D doubles, P splits, and C cheats.

            How winning works:
            If your final value is higher than the dealer's without busting, you win.
            If the dealer busts and you do not, you win.
            If you and the dealer tie, it is a push and your bet is returned.
            A natural blackjack pays better than a normal win.

            Tavern systems:
            Bankroll shows how much fake money you still have.
            Pot on felt shows how much is currently committed at the whole table, including the rail regulars.
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
            The right-side dashboard is your readout for story, stats, and suspicion.
            The dock below the table is your control booth for bets and actions.

            If you ever want this guide again, press the How To Play button in the control dock.
            """;
    }

    private void handleDeal() {
        controller.setBetAndDeal(parseBet());
        soundManager.playEffect("card-clack.wav");
        refresh(true);
    }

    private void handleHit() {
        controller.hit();
        soundManager.playEffect("card-clack.wav");
        refresh(true);
    }

    private void handleStand() {
        controller.stand();
        refresh(true);
    }

    private void handleDoubleDown() {
        controller.doubleDown();
        soundManager.playEffect("card-clack.wav");
        refresh(true);
    }

    private void handleSplit() {
        controller.split();
        soundManager.playEffect("card-clack.wav");
        refresh(true);
    }

    private void handleCheat() {
        controller.cheat();
        soundManager.playEffect("card-clack.wav");
        refresh(true);
    }

    private void handleReset() {
        controller.resetSave();
        refresh(true);
    }

    private void registerKeyboardShortcuts() {
        registerShortcut("deal-hand", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), this::handleDeal);
        registerShortcut("hit-hand", KeyStroke.getKeyStroke(KeyEvent.VK_H, 0), this::handleHit);
        registerShortcut("stand-hand", KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), this::handleStand);
        registerShortcut("double-hand", KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), this::handleDoubleDown);
        registerShortcut("split-hand", KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), this::handleSplit);
        registerShortcut("cheat-hand", KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), this::handleCheat);
        registerShortcut("reset-saloon", KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), this::handleReset);
        registerShortcut("show-tutorial", KeyStroke.getKeyStroke(KeyEvent.VK_T, 0), this::showTutorialDialog);
    }

    private void registerShortcut(String key, KeyStroke stroke, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                action.run();
            }
        });
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

    private void beginRailTurnTimer() {
        if (railTurnTimer != null && railTurnTimer.isRunning()) {
            return;
        }
        railTurnTimer = new Timer(900, event -> {
            controller.advanceRailTurn();
            soundManager.playEffect("card-clack.wav");
            refresh(true);
        });
        railTurnTimer.setRepeats(true);
        railTurnTimer.start();
    }

    private void stopRailTurnTimer() {
        if (railTurnTimer != null) {
            railTurnTimer.stop();
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
}
