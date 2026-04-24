import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class TableCanvas extends JPanel {
    private static final Color GOLD = new Color(231, 194, 105);
    private static final Color CREAM = new Color(247, 239, 226);
    private static final Color MUTED = new Color(187, 173, 155);
    private static final Color TABLE_EDGE = new Color(108, 62, 34);
    private static final Color TABLE_BOTTOM = new Color(8, 43, 36);

    private GameSnapshot snapshot;

    public TableCanvas() {
        setOpaque(false);
    }

    public void setSnapshot(GameSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (snapshot == null) {
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
        String banner = "Facing " + snapshot.opponentName();
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
        List<RailSeatSnapshot> railSeats = snapshot.railSeats();
        int[] angles = {154, 128, 52, 26};
        Color[] coatColors = {
            new Color(71, 52, 45),
            new Color(77, 49, 41),
            new Color(70, 52, 43),
            new Color(58, 54, 70)
        };

        for (int i = 0; i < Math.min(angles.length, railSeats.size()); i++) {
            drawRailSeatScene(g2, centerX, centerY, radiusX, radiusY, angles[i], railSeats.get(i), coatColors[i]);
        }

        drawDealerSeatScene(g2, centerX, centerY, radiusX, radiusY, 90, snapshot.opponentName(), new Color(53, 61, 82));
    }

    private void drawRailSeatScene(Graphics2D g2, int centerX, int centerY, int radiusX, int radiusY,
                                   int angleDegrees, RailSeatSnapshot seat, Color coatColor) {
        Point seatPoint = pointOnEllipse(centerX, centerY, radiusX, radiusY, angleDegrees);
        Point chipSpot = pointOnEllipse(centerX, centerY, radiusX - 130, radiusY - 110, angleDegrees);
        Point cardSpot = pointOnEllipse(centerX, centerY, radiusX - 80, radiusY - 70, angleDegrees);

        drawRailCharacter(g2, seatPoint.x, seatPoint.y - 20, coatColor);
        drawBetRing(g2, chipSpot.x, chipSpot.y, 24);
        drawChipStack(g2, chipSpot.x - 18, chipSpot.y - 8, new Color(194, 72, 56), chipCountForBet(seat.bet()));

        List<String> cards = parseCards(seat.cards());
        drawCardFan(g2, cards, cardSpot.x, cardSpot.y, 60, 84, 18);
        drawBadge(g2, seat.name() + (seat.bet() > 0 ? " $" + seat.bet() : ""), cardSpot.x, cardSpot.y - 74,
            74, 128, 24, new Color(18, 22, 32, 210), GOLD);
        drawBadge(g2, seat.status(), cardSpot.x, cardSpot.y + 54,
            82, 140, 24, new Color(10, 14, 22, 215), CREAM);
    }

    private void drawDealerSeatScene(Graphics2D g2, int centerX, int centerY, int radiusX, int radiusY,
                                     int angleDegrees, String name, Color coatColor) {
        Point seat = pointOnEllipse(centerX, centerY, radiusX, radiusY, angleDegrees);
        Point chipSpot = pointOnEllipse(centerX, centerY, radiusX - 130, radiusY - 110, angleDegrees);
        Point cardSpot = pointOnEllipse(centerX, centerY, radiusX - 80, radiusY - 70, angleDegrees);

        drawRailCharacter(g2, seat.x, seat.y - 20, coatColor);
        drawBetRing(g2, chipSpot.x, chipSpot.y, 30);
        drawChipStack(g2, chipSpot.x - 18, chipSpot.y - 8, new Color(222, 183, 88), 4);

        List<String> cards = parseCards(snapshot.dealerCards());
        drawCardFan(g2, cards, cardSpot.x, cardSpot.y, 72, 98, 18);
        String dealerStatus = snapshot.roundActive()
            ? "Showing " + snapshot.dealerVisibleValue()
            : "Dealer " + snapshot.dealerFinalValue();
        drawBadge(g2, name, cardSpot.x, cardSpot.y - 80, 100, 164, 24,
            new Color(36, 30, 33, 218), CREAM);
        drawBadge(g2, dealerStatus, cardSpot.x, cardSpot.y + 62, 92, 148, 24,
            new Color(58, 43, 27, 220), GOLD);
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
        int playerPot = snapshot.handBets().stream().mapToInt(Integer::intValue).sum();
        int railPot = snapshot.railSeats().stream().mapToInt(RailSeatSnapshot::bet).sum();
        int pot = playerPot + railPot;
        g2.setColor(new Color(11, 16, 24, 185));
        g2.fillRoundRect(centerX - 128, centerY - 54, 256, 118, 28, 28);
        g2.setColor(new Color(255, 220, 137, 110));
        g2.drawRoundRect(centerX - 128, centerY - 54, 256, 118, 28, 28);
        g2.setColor(MUTED);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        String stake = snapshot.roundActive() ? "Pot on felt" : "Table stake";
        int sw = g2.getFontMetrics().stringWidth(stake);
        g2.drawString(stake, centerX - sw / 2, centerY - 22);
        g2.setColor(GOLD);
        g2.setFont(new Font("Serif", Font.BOLD, 34));
        String potText = "$" + pot;
        int pw = g2.getFontMetrics().stringWidth(potText);
        g2.drawString(potText, centerX - pw / 2, centerY + 12);
        g2.setColor(CREAM);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        String splitText = "You $" + playerPot + "  |  Rail $" + railPot;
        int splitWidth = g2.getFontMetrics().stringWidth(splitText);
        g2.drawString(splitText, centerX - splitWidth / 2, centerY + 34);
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
        List<String> hands = snapshot.playerHands();
        int count = Math.max(1, hands.size());
        int cardZoneY = y + height - 224;
        int spread = count == 1 ? 0 : 210;

        for (int i = 0; i < count; i++) {
            int handCenter = centerX + (i - (count - 1) / 2) * spread;
            boolean active = i < hands.size() && i == snapshot.activeHandIndex() && snapshot.roundActive();
            int bet = i < snapshot.handBets().size() ? snapshot.handBets().get(i) : 0;
            int value = i < snapshot.playerValues().size() ? snapshot.playerValues().get(i) : 0;
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

    private void drawBadge(Graphics2D g2, String text, int centerX, int y, int minWidth, int maxWidth,
                           int height, Color fill, Color textColor) {
        if (text == null || text.isBlank()) {
            return;
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        int width = clamp(g2.getFontMetrics().stringWidth(text) + 24, minWidth, maxWidth);
        drawBadge(g2, text, centerX - width / 2, y, width, height, fill, textColor);
    }

    private int chipCountForBet(int bet) {
        if (bet <= 0) {
            return 0;
        }
        return clamp(bet / 15 + 1, 1, 6);
    }

    private void drawPrompt(Graphics2D g2, int centerX, int y) {
        String prompt;
        if (snapshot.showLoading()) {
            prompt = "The room is still setting the table.";
        } else if (snapshot.railTurnsActive()) {
            prompt = "Rail seats are finishing their turns.";
        } else if (snapshot.duelActive()) {
            prompt = snapshot.duelCanDraw() ? "Draw now." : "Hands off the iron.";
        } else if (snapshot.roundActive()) {
            prompt = "The hand is live. Play from your seat.";
        } else if (snapshot.canDeal()) {
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

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
