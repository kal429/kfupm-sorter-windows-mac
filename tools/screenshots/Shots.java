import io.github.kal429.kfupmsorter.AppDirs;
import io.github.kal429.kfupmsorter.Prefs;
import io.github.kal429.kfupmsorter.Settings;
import io.github.kal429.kfupmsorter.ui.MainWindow;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Takes the website screenshots: opens the real window with sample courses and filters,
 * and paints every tab at twice the size, so the pictures stay sharp on high-resolution screens.
 * Run by .github/workflows/screenshots.yml on Windows (the look most students have):
 *   java -Dsun.java2d.uiScale=2 -Dkfupmsorter.data=<empty dir> -cp build/classes;build/shots Shots en site/shots
 * (uiScale=2 lays the window out at twice the size, so every label is measured for the sharp picture.)
 * Not part of the app.
 */
public final class Shots {
    static final int W = 980, H = 700, SCALE = 2;
    static final String[] TABS = {"courses", "filters", "sorting", "status", "about"};

    public static void main(String[] args) throws Exception {
        String lang = args[0];
        Path out = Path.of(args[1]);
        Files.createDirectories(out);
        prepareSampleData(lang);

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        MainWindow[] w = new MainWindow[1];
        SwingUtilities.invokeAndWait(() -> {
            w[0] = new MainWindow();
            // no title bar: Windows keeps a normal window within the screen, which on a CI machine
            // (1024x768 at twice the size) would squeeze the app; a borderless one may be larger
            w[0].setUndecorated(true);
            w[0].setSize(W, H);
            w[0].setLocation(10, 10);
            w[0].setVisible(true);
        });
        Thread.sleep(1500);
        // make the inside of the window exactly W x H, whatever the title bar and borders take
        SwingUtilities.invokeAndWait(() -> {
            java.awt.Insets in = w[0].getInsets();
            w[0].setSize(W + in.left + in.right, H + in.top + in.bottom);
            w[0].validate();
        });
        Thread.sleep(1500);

        // show the student's own department in the course list
        JList<?> depts = (JList<?>) field(w[0], "deptList");
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < depts.getModel().getSize(); i++) {
                if (String.valueOf(depts.getModel().getElementAt(i)).startsWith("COE ")) { depts.setSelectedIndex(i); depts.ensureIndexIsVisible(i); break; }
            }
        });
        Thread.sleep(800);
        SwingUtilities.invokeAndWait(() -> {
            int i = depts.getSelectedIndex();
            if (i >= 0) depts.ensureIndexIsVisible(Math.min(depts.getModel().getSize() - 1, i + 6));
        });
        Thread.sleep(300);

        JComponent rootPane = w[0].getRootPane();
        if (rootPane.getWidth() != W || rootPane.getHeight() != H)
            throw new IllegalStateException("the window is " + rootPane.getWidth() + "x" + rootPane.getHeight() + ", not " + W + "x" + H);

        JTabbedPane tabs = (JTabbedPane) field(w[0], "tabs");
        BufferedImage first = null;
        for (int t = 0; t < TABS.length; t++) {
            int idx = t;
            SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(idx));
            Thread.sleep(900);
            BufferedImage img = paint(w[0]);
            ImageIO.write(img, "png", out.resolve(lang + "-" + TABS[t] + ".png").toFile());
            if (t == 0) first = img;
            System.out.println("saved " + lang + "-" + TABS[t] + ".png " + img.getWidth() + "x" + img.getHeight());
        }
        if (lang.equals("en")) {
            BufferedImage card = card(first);
            ImageIO.write(card, "png", out.resolveSibling("og-image.png").toFile());
            System.out.println("saved og-image.png");
        }
        System.exit(0);
    }

    /** The window content (below the system title bar), painted at twice the size. */
    static BufferedImage paint(JFrame f) throws Exception {
        JComponent root = f.getRootPane();
        int w = root.getWidth(), h = root.getHeight();
        BufferedImage img = new BufferedImage(w * SCALE, h * SCALE, BufferedImage.TYPE_INT_RGB);
        SwingUtilities.invokeAndWait(() -> {
            Graphics2D g = img.createGraphics();
            g.scale(SCALE, SCALE);
            root.printAll(g);
            g.dispose();
        });
        return img;
    }

    /** The 1200x630 picture that WhatsApp, Telegram and X show for a shared link. */
    static BufferedImage card(BufferedImage shot) {
        int cw = 1200, ch = 630;
        BufferedImage c = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = c.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setPaint(new GradientPaint(0, 0, new Color(0, 112, 64), 0, ch, new Color(0, 82, 52)));
        g.fillRect(0, 0, cw, ch);
        g.setColor(new Color(218, 201, 97));
        g.fillRect(0, ch - 10, cw, 10);
        String face = fontFace();
        g.setColor(Color.WHITE);
        g.setFont(new Font(face, Font.BOLD, 50));
        g.drawString("KFUPM Sorter", 60, 120);
        g.setFont(new Font(face, Font.BOLD, 36));
        String[] lines = {"Sorts your Downloads", "by KFUPM course,", "automatically."};
        for (int i = 0; i < lines.length; i++) g.drawString(lines[i], 60, 235 + i * 50);
        g.setColor(new Color(206, 234, 216));
        g.setFont(new Font(face, Font.PLAIN, 26));
        g.drawString("Free · Windows and Mac", 60, 420);
        g.drawString("English and Arabic", 60, 458);
        g.setColor(new Color(218, 201, 97));
        g.setFont(new Font(face, Font.BOLD, 24));
        g.drawString("kfupm-sorter-windows-mac.vercel.app", 60, 560);
        // the app, slightly tilted into the right side
        int sw = 600, sh = shot.getHeight() * sw / shot.getWidth();
        int x = cw - sw - 40, y = (ch - sh) / 2 - 5;
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new RoundRectangle2D.Float(x + 6, y + 10, sw, sh, 14, 14));
        g.setClip(new RoundRectangle2D.Float(x, y, sw, sh, 14, 14));
        g.drawImage(shot, x, y, sw, sh, null);
        g.setClip(null);
        g.dispose();
        return c;
    }

    static String fontFace() {
        String[] have = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String want : new String[]{"Segoe UI", "Helvetica Neue", "DejaVu Sans"})
            for (String h : have) if (h.equalsIgnoreCase(want)) return h;
        return Font.SANS_SERIF;
    }

    /** A believable student setup: this term's courses, two filters and a few sorted files. */
    static void prepareSampleData(String lang) throws Exception {
        Path data = AppDirs.dataDir();
        Files.createDirectories(data);
        Prefs p = new Prefs();
        p.language = lang;
        p.auto = true;
        p.imported = true;
        p.checkUpdates = true;
        p.save();
        // as if GitHub was just asked and this version is the newest, so the window does not go online
        io.github.kal429.kfupmsorter.Updates.State u = new io.github.kal429.kfupmsorter.Updates.State();
        u.lastCheck = System.currentTimeMillis();
        u.save();
        // the Status tab shows "On" only when the sign-in entry exists; on the throw-away CI machine, create it
        if (System.getenv("GITHUB_ACTIONS") != null) io.github.kal429.kfupmsorter.Autostart.enable();

        Settings s = new Settings();
        s.watchFolder = AppDirs.OS_TYPE == AppDirs.OS.WINDOWS ? "C:\\Users\\Student\\Downloads" : "/Users/student/Downloads";
        s.term = "261";
        s.termFolder = true;
        s.courses.addAll(List.of("COE 301", "COE 292", "EE 236", "MATH 201", "PHYS 102", "ENGL 214"));
        s.filters.add(new Settings.Filter("Internship", List.of("internship", "coop", "training"), true));
        s.filters.add(new Settings.Filter("CIE Club", List.of("CIE", "IEEE"), true));
        s.save();

        String sep = AppDirs.OS_TYPE == AppDirs.OS.WINDOWS ? "\\" : "/";
        String[][] moves = {
                {"Lecture_T261_COE_301_Ch3.pdf", "261" + sep + "COE 301"},
                {"261-EE236-L2-Resistive Circuits.pdf", "261" + sep + "EE 236"},
                {"MATH201_HW4.pdf", "261" + sep + "MATH 201"},
                {"Internship_Offer_Letter.pdf", "Internship"},
                {"PHYS102 Lab 3 Report.docx", "261" + sep + "PHYS 102"},
                {"CIE_General_Meeting.pptx", "CIE Club"},
                {"COE292_Project_Guidelines.pdf", "261" + sep + "COE 292"},
                {"ENGL214_Progress_Report.docx", "261" + sep + "ENGL 214"},
                {"IMG_2041.jpg", "Images"},
                {"lab-setup.zip", "Archives"},
        };
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime t = LocalDateTime.now().minusMinutes(moves.length * 7L);
        StringBuilder log = new StringBuilder();
        for (String[] m : moves) {
            t = t.plusMinutes(7);
            log.append(f.format(t)).append("  MOVED   ").append(m[0]).append("  ->  ").append(m[1]).append(System.lineSeparator());
        }
        Files.writeString(AppDirs.logFile(), log.toString(), StandardCharsets.UTF_8);
        Files.writeString(AppDirs.lastRunFile(), "last run : " + f.format(LocalDateTime.now().minusSeconds(20)) + System.lineSeparator() + "moved    : 1" + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }
}
