package io.github.kal429.kfupmsorter;

import io.github.kal429.kfupmsorter.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * KFUPM Sorter Desktop - keeps your Downloads folder sorted by KFUPM course.
 *
 *   (no arguments)   open the window
 *   --background     sort every minute until automatic sorting is turned off
 *   --sort-once      sort once and print what moved (handy for testing)
 *   --preview        print what would move, without moving anything
 */
public final class Main {
    public static final String VERSION = "1.1.1";

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "";
        switch (mode) {
            case "--background":
                System.setProperty("apple.awt.UIElement", "true");   // macOS: no Dock icon for the background sorter
                Background.run();
                return;
            case "--sort-once":
            case "--preview": {
                boolean dry = mode.equals("--preview");
                Engine.Result r = Engine.runSaved(dry);
                for (Engine.Move m : r.moves) System.out.println((dry ? "WOULD MOVE  " : "MOVED  ") + m.file + "  ->  " + m.folder);
                for (String e : r.errors) System.out.println("ERROR  " + e);
                if (r.skippedBusy) System.out.println("Another sort is running; try again in a moment.");
                System.out.println((dry ? "Would move " : "Moved ") + r.moves.size() + " file(s).");
                return;
            }
            default:
                openWindow();
        }
    }

    private static void openWindow() {
        System.setProperty("apple.awt.application.name", "KFUPM Sorter");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            if (AppDirs.OS_TYPE == AppDirs.OS.OTHER) UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            else UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
