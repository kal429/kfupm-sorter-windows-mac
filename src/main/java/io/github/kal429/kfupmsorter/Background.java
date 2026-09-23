package io.github.kal429.kfupmsorter;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;

/**
 * The background sorter: checks the folder every minute until automatic sorting is
 * turned off. Shows a small tray / menu-bar icon with "Open", "Sort now" and "Quit".
 */
public final class Background {
    private Background() {}

    private static volatile boolean quit = false;
    private static volatile boolean sortRequested = false;

    public static void run() {
        // only one background sorter per user
        FileLock lock;
        try {
            FileChannel ch = new RandomAccessFile(AppDirs.dataDir().resolve("background.lock").toFile(), "rw").getChannel();
            lock = ch.tryLock();
        } catch (Exception e) {
            lock = null;
        }
        if (lock == null) return;

        Prefs prefs = Prefs.load();
        installTrayIcon(prefs.language);
        SortLog.note("Background sorter started.");

        while (!quit) {
            prefs = Prefs.load();
            if (!prefs.auto) break;                    // turned off from the window
            try {
                Engine.Result r = Engine.runSaved(false);
                for (String e : r.errors) SortLog.note("ERROR: " + e);
            } catch (Throwable t) {
                SortLog.note("ERROR: " + t);
            }
            for (int s = 0; s < 60 && !quit && !sortRequested; s++) sleep(1000);
            sortRequested = false;
        }
        SortLog.note("Background sorter stopped.");
        System.exit(0);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }

    private static void installTrayIcon(String lang) {
        try {
            if (!SystemTray.isSupported()) return;
            Image img = Toolkit.getDefaultToolkit().getImage(Background.class.getResource("/kfupmsorter/icon.png"));
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem(Strings.get(lang, "tray.open"));
            MenuItem sort = new MenuItem(Strings.get(lang, "tray.sort"));
            MenuItem stop = new MenuItem(Strings.get(lang, "tray.quit"));
            open.addActionListener(e -> openWindow());
            sort.addActionListener(e -> sortRequested = true);
            stop.addActionListener(e -> quit = true);
            menu.add(open);
            menu.add(sort);
            menu.addSeparator();
            menu.add(stop);
            TrayIcon icon = new TrayIcon(img, "KFUPM Sorter", menu);
            icon.setImageAutoSize(true);
            icon.addActionListener(e -> openWindow());
            SystemTray.getSystemTray().add(icon);
        } catch (Throwable ignored) {
            // no tray on this desktop: keep sorting anyway
        }
    }

    private static void openWindow() {
        try {
            List<String> cmd = Autostart.backgroundCommand();
            cmd.remove(cmd.size() - 1);   // same program, without --background
            new ProcessBuilder(cmd).start();
        } catch (IOException ignored) { }
    }
}
