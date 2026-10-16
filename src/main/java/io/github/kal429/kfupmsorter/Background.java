package io.github.kal429.kfupmsorter;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;

/**
 * The background sorter: checks the folder every minute until automatic sorting is
 * turned off. Shows a small tray / menu-bar icon with "Open", "Sort now" and "Quit".
 * Once a day it also asks GitHub whether a newer version exists, and says so once per version.
 */
public final class Background {
    private Background() {}

    private static volatile boolean quit = false;
    private static volatile boolean sortRequested = false;
    private static TrayIcon trayIcon;
    private static PopupMenu trayMenu;
    private static MenuItem updateItem;
    private static volatile boolean checking = false;

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
            if (prefs.checkUpdates) maybeCheckForUpdate(prefs.language);
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

    /** At most once a day, in its own thread so a slow network never delays sorting. */
    private static void maybeCheckForUpdate(String lang) {
        if (checking || !Updates.dueInBackground(Updates.State.load(), System.currentTimeMillis())) return;
        checking = true;
        Thread t = new Thread(() -> {
            try {
                Updates.Info info = Updates.check();
                if (info != null) announce(info, lang);
            } catch (Throwable e) {
                SortLog.note("Could not check for a new version: " + e.getMessage());
            } finally {
                checking = false;
            }
        }, "update-check");
        t.setDaemon(true);
        t.start();
    }

    /** A tray menu item for the new version, and one notification per version. */
    private static synchronized void announce(Updates.Info info, String lang) {
        SortLog.note("A new version is available: " + info.version);
        if (trayMenu != null) {
            if (updateItem == null) {
                updateItem = new MenuItem();
                trayMenu.insert(updateItem, 0);
                trayMenu.insertSeparator(1);
            }
            for (java.awt.event.ActionListener l : updateItem.getActionListeners()) updateItem.removeActionListener(l);
            updateItem.setLabel(Strings.fmt(Strings.get(lang, "tray.update"), info.version));
            updateItem.addActionListener(e -> browse(info.buttonUrl()));
        }
        Updates.State s = Updates.State.load();
        if (!info.version.equals(s.notified)) {
            if (trayIcon != null) {
                try {
                    trayIcon.displayMessage(Strings.get(lang, "tray.updTitle"),
                            Strings.fmt(Strings.get(lang, "tray.updMsg"), info.version), TrayIcon.MessageType.INFO);
                } catch (Throwable ignored) { }
            }
            s.notified = info.version;
            s.save();
        }
    }

    private static void browse(String url) {
        try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) { }
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
            icon.addActionListener(e -> openWindow());   // also a click on the "new version" notification
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
            trayMenu = menu;
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
