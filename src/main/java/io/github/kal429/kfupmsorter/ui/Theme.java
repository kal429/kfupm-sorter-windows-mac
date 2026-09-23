package io.github.kal429.kfupmsorter.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** KFUPM colors (official style board) and a few small styled components. */
public final class Theme {
    private Theme() {}

    public static final Color GREEN  = new Color(0, 133, 64);
    public static final Color FOREST = new Color(0, 87, 63);
    public static final Color PETROL = new Color(0, 62, 81);
    public static final Color GOLD   = new Color(218, 201, 97);
    public static final Color STONE  = new Color(170, 138, 0);
    public static final Color GRAY   = new Color(217, 218, 228);
    public static final Color DARK   = new Color(55, 57, 56);
    public static final Color PALE   = new Color(206, 234, 216);
    public static final Color BG     = new Color(245, 246, 248);
    public static final Color MUTED  = new Color(105, 108, 112);
    public static final Color BAD    = new Color(176, 32, 32);

    public static Font base() {
        Font f = UIManager.getFont("Label.font");
        return f != null ? f : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    public static Font size(float pts, int style) { return base().deriveFont(style, pts); }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(size(base().getSize2D() + 3f, Font.BOLD));
        l.setForeground(FOREST);
        l.setHorizontalAlignment(SwingConstants.LEADING);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(size(base().getSize2D() - 1f, Font.PLAIN));
        l.setForeground(MUTED);
        l.setHorizontalAlignment(SwingConstants.LEADING);
        return l;
    }

    public static void pad(JComponent c, int top, int side, int bottom) {
        c.setBorder(BorderFactory.createEmptyBorder(top, side, bottom, side));
    }

    /** A flat, rounded button: green for the main action, white for the others. */
    public static final class KButton extends JButton {
        private final boolean primary;

        public KButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.size(Theme.base().getSize2D(), primary ? Font.BOLD : Font.PLAIN));
            setForeground(primary ? Color.WHITE : PETROL);
            setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = fm.stringWidth(getText()) + 36;
            int h = fm.getHeight() + 16;
            return new Dimension(Math.max(w, 90), Math.max(h, 32));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled();
            boolean hover = getModel().isRollover() && on;
            boolean down = getModel().isPressed() && on;
            if (primary) {
                g2.setColor(!on ? GRAY : (down || hover) ? FOREST : GREEN);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            } else {
                g2.setColor(hover ? BG : Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(GRAY);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText();
            int tx = (w - fm.stringWidth(t)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(!on ? MUTED : primary ? Color.WHITE : getForeground());
            g2.drawString(t, tx, ty);
            g2.dispose();
        }
    }
}
