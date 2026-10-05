package io.github.kal429.kfupmsorter.ui;

import io.github.kal429.kfupmsorter.AppDirs;
import io.github.kal429.kfupmsorter.Autostart;
import io.github.kal429.kfupmsorter.Catalog;
import io.github.kal429.kfupmsorter.Engine;
import io.github.kal429.kfupmsorter.Main;
import io.github.kal429.kfupmsorter.Prefs;
import io.github.kal429.kfupmsorter.Settings;
import io.github.kal429.kfupmsorter.SortLog;
import io.github.kal429.kfupmsorter.Strings;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** The main window: My Courses, Custom Filters, Sorting, Status, About. */
public final class MainWindow extends JFrame {

    static final String REPO_URL = "https://github.com/kal429/kfupm-sorter-windows-mac";
    static final String SITE_URL = "https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest";

    private final Prefs prefs = Prefs.load();
    private Settings settings;
    private String lang;

    // every translated piece of text registers here, so a language switch can redo them all
    private final List<Runnable> retext = new ArrayList<>();
    // components whose content is English (course codes, paths, log) and stay left-to-right
    private final List<JComponent> keepLtr = new ArrayList<>();

    private String footerKey = "";
    private Object[] footerArgs = new Object[0];

    // header
    private final JLabel credits = new JLabel();
    private final JComboBox<String> langBox = new JComboBox<>(new String[]{"English", "العربية"});
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel footer = Theme.muted("");

    // courses
    private final JTextField termField = new JTextField(5);
    private final JCheckBox termFolderBox = new JCheckBox();
    private final DefaultListModel<String> deptModel = new DefaultListModel<>();
    private final JList<String> deptList = new JList<>(deptModel);
    private final JTextField searchField = new JTextField();
    private final DefaultListModel<Catalog.Course> courseModel = new DefaultListModel<>();
    private final JList<Catalog.Course> courseList = new JList<>(courseModel);
    private final Set<String> checked = new HashSet<>();
    private final JLabel shownLabel = Theme.muted("");
    private final DefaultListModel<Catalog.Course> mineModel = new DefaultListModel<>();
    private final JList<Catalog.Course> mineList = new JList<>(mineModel);

    // custom filters
    private final JTextField filterFolder = new JTextField();
    private final JTextField filterKeys = new JTextField();
    private final JCheckBox wholeWordBox = new JCheckBox();
    private final DefaultListModel<String> filterModel = new DefaultListModel<>();
    private final JList<String> filterList = new JList<>(filterModel);

    // sorting
    private final JTextField watchField = new JTextField();
    private final JCheckBox byTypeBox = new JCheckBox();
    private final DefaultListModel<Settings.TypeDef> typeModel = new DefaultListModel<>();
    private final JList<Settings.TypeDef> typeList = new JList<>(typeModel);
    private final JTextField typeName = new JTextField();
    private final JTextField typeExts = new JTextField();
    private final List<JComponent> typeEditors = new ArrayList<>();
    private final JCheckBox otherBox = new JCheckBox();
    private final JCheckBox keepMovesBox = new JCheckBox();

    // status
    private final JLabel autoLabel = new JLabel();
    private final JLabel lastRunLabel = Theme.muted("");
    private final JLabel bothLabel = new JLabel();
    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JList<String> logList = new JList<>(logModel);
    private Theme.KButton enableBtn, disableBtn;

    // about
    private final JLabel versionLabel = Theme.muted("");
    private final JLabel catalogLabel = new JLabel();
    private final JLabel updLabel = Theme.muted("");
    private final JProgressBar progress = new JProgressBar();
    private Theme.KButton updateBtn, saveBtn;

    private boolean busy = false;   // true while lists are being refilled
    // filter and course folders as last saved, to spot new ones
    private final Set<String> savedNameFolders = new HashSet<>();

    public MainWindow() {
        super("KFUPM Sorter");
        lang = prefs.language;
        loadSettings();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImages(List.of(icon(16), icon(32), icon(64), icon(256)));
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        tabs.addTab("", buildCoursesTab());
        tabs.addTab("", buildFiltersTab());
        tabs.addTab("", buildSortingTab());
        tabs.addTab("", buildStatusTab());
        tabs.addTab("", buildAboutTab());
        retext.add(() -> {
            String[] keys = {"tab.courses", "tab.custom", "tab.sort", "tab.status", "tab.about"};
            for (int i = 0; i < keys.length; i++) tabs.setTitleAt(i, t(keys[i]));
        });
        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 3) refreshStatus(); });
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        Theme.pad(center, 10, 10, 0);
        center.add(tabs, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        fillDepartments();
        refreshCourses();
        refreshMine();
        refreshFilters();
        applySettingsToForm();
        applyLanguage(false);
        refreshStatus();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { System.exit(0); }
        });
        setMinimumSize(new Dimension(900, 640));
        setSize(980, 700);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------ helpers

    private String t(String key) {
        String s = Strings.get(lang, key);
        if (AppDirs.OS_TYPE != AppDirs.OS.WINDOWS) s = s.replace("261\\COE", "261/COE");
        // keep a folder path such as 261\COE 301 in one left-to-right piece inside Arabic text
        if ("ar".equals(lang)) s = s.replaceAll("(261[\\\\/]COE 301)", "\u202A$1\u202C");
        return s;
    }

    private JLabel label(String key) {
        JLabel l = new JLabel();
        l.setHorizontalAlignment(SwingConstants.LEADING);
        retext.add(() -> l.setText(t(key)));
        return l;
    }

    private JLabel heading(String key) {
        JLabel l = Theme.heading("");
        retext.add(() -> l.setText(t(key)));
        return l;
    }

    private JLabel mutedLabel(String key) {
        JLabel l = Theme.muted("");
        retext.add(() -> l.setText(t(key)));
        return l;
    }

    /** A small grey label that wraps onto more lines instead of being cut off. */
    private JLabel wrappedMuted(String key, int width) {
        JLabel l = Theme.muted("");
        retext.add(() -> l.setText("<html><div style='width:" + width + "px'>"
                + t(key).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div></html>"));
        return l;
    }

    private Theme.KButton button(String key, boolean primary, Runnable action) {
        Theme.KButton b = new Theme.KButton("", primary);
        retext.add(() -> b.setText(t(key)));
        b.addActionListener(e -> action.run());
        return b;
    }

    private JCheckBox check(JCheckBox box, String key) {
        box.setOpaque(false);
        retext.add(() -> box.setText(t(key)));
        return box;
    }

    private static Image icon(int size) {
        ImageIcon ic = new ImageIcon(MainWindow.class.getResource("/kfupmsorter/icon.png"));
        return ic.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

    private static JPanel row(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        p.setOpaque(false);
        for (Component c : parts) p.add(c);
        return p;
    }

    private JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createLineBorder(Theme.GRAY));
        if (keepLtr.contains(c)) keepLtr.add(s);   // an English list keeps its scrollbars left-to-right too
        return s;
    }

    private static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Color.WHITE);
        Theme.pad(p, 14, 16, 14);
        return p;
    }

    private void setFooter(String key, Object... args) {
        footerKey = key;
        footerArgs = args;
        footer.setText(key.isEmpty() ? " " : Strings.fmt(t(key), args));
    }

    private void info(String text) { JOptionPane.showMessageDialog(this, text, "KFUPM Sorter", JOptionPane.INFORMATION_MESSAGE); }
    private void warn(String text) { JOptionPane.showMessageDialog(this, text, "KFUPM Sorter", JOptionPane.WARNING_MESSAGE); }
    private boolean ask(String text) {
        return JOptionPane.showConfirmDialog(this, text, "KFUPM Sorter", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ------------------------------------------------------------ header and footer

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Theme.GOLD);
                g.fillRect(0, getHeight() - 4, getWidth(), 4);   // gold rule under the green bar
            }
        };
        header.setBackground(Theme.GREEN);
        header.setBorder(BorderFactory.createEmptyBorder(10, 18, 14, 18));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("KFUPM Sorter");
        title.setFont(Theme.size(Theme.base().getSize2D() + 9f, Font.BOLD));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel();
        sub.setForeground(Theme.PALE);
        retext.add(() -> sub.setText(t("app.subtitle")));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(title);
        titles.add(sub);
        header.add(titles, BorderLayout.LINE_START);

        credits.setForeground(Theme.GOLD);
        credits.setFont(Theme.size(Theme.base().getSize2D() + 2f, Font.BOLD));
        langBox.setSelectedIndex("ar".equals(lang) ? 1 : 0);
        langBox.addActionListener(e -> {
            String want = langBox.getSelectedIndex() == 1 ? "ar" : "en";
            if (!want.equals(lang)) { lang = want; prefs.language = want; prefs.save(); applyLanguage(true); }
        });
        keepLtr.add(langBox);
        JPanel right = row(credits, Box.createHorizontalStrut(12), langBox);
        right.setLayout(new FlowLayout(FlowLayout.TRAILING, 8, 8));
        header.add(right, BorderLayout.LINE_END);
        return header;
    }

    private JComponent buildFooter() {
        JPanel f = new JPanel(new BorderLayout());
        f.setOpaque(false);
        Theme.pad(f, 8, 18, 12);
        footer.setText(" ");
        f.add(footer, BorderLayout.CENTER);
        saveBtn = button("btn.save", true, this::save);
        Theme.KButton close = button("btn.close", false, this::dispose);
        f.add(row(saveBtn, close), BorderLayout.LINE_END);
        return f;
    }

    // ------------------------------------------------------------ tab 1: courses

    private JComponent buildCoursesTab() {
        JPanel p = page();

        JPanel top = row(label("lbl.term"), termField, Box.createHorizontalStrut(12), check(termFolderBox, "chk.termFolder"));
        keepLtr.add(termField);
        p.add(top, BorderLayout.NORTH);

        // department list | search + course checklist
        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setOpaque(false);
        left.add(heading("lbl.dept"), BorderLayout.NORTH);
        deptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deptList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && !busy) refreshCourses(); });
        keepLtr.add(deptList);
        JScrollPane deptScroll = scroll(deptList);
        deptScroll.setPreferredSize(new Dimension(290, 250));
        left.add(deptScroll, BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        JPanel searchBox = new JPanel(new BorderLayout(0, 4));
        searchBox.setOpaque(false);
        searchBox.add(heading("lbl.search"), BorderLayout.NORTH);
        searchBox.add(searchField, BorderLayout.CENTER);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshCourses(); }
            public void removeUpdate(DocumentEvent e) { refreshCourses(); }
            public void changedUpdate(DocumentEvent e) { refreshCourses(); }
        });
        mid.add(searchBox, BorderLayout.NORTH);

        courseList.setCellRenderer(new CheckRenderer());
        courseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        courseList.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int i = courseList.locationToIndex(e.getPoint());
                if (i < 0 || !courseList.getCellBounds(i, i).contains(e.getPoint())) return;
                String code = courseModel.get(i).code;
                if (!checked.remove(code)) checked.add(code);
                courseList.repaint();
            }
        });
        keepLtr.add(courseList);
        mid.add(scroll(courseList), BorderLayout.CENTER);
        Theme.KButton add = button("btn.add", true, this::addChecked);
        mid.add(row(add, shownLabel), BorderLayout.SOUTH);

        JPanel lists = new JPanel(new BorderLayout(12, 0));
        lists.setOpaque(false);
        lists.add(left, BorderLayout.LINE_START);
        lists.add(mid, BorderLayout.CENTER);
        p.add(lists, BorderLayout.CENTER);

        // my courses
        JPanel mine = new JPanel(new BorderLayout(10, 6));
        mine.setOpaque(false);
        mine.add(heading("lbl.mine"), BorderLayout.NORTH);
        mineList.setVisibleRowCount(4);
        keepLtr.add(mineList);
        JScrollPane mineScroll = scroll(mineList);
        mineScroll.setPreferredSize(new Dimension(400, 104));
        mine.add(mineScroll, BorderLayout.CENTER);
        JPanel side = new JPanel(new GridLayout(2, 1, 0, 6));
        side.setOpaque(false);
        side.add(button("btn.remove", false, this::removeSelected));
        side.add(button("btn.clear", false, () -> { settings.courses.clear(); refreshMine(); }));
        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.setOpaque(false);
        sideWrap.add(side, BorderLayout.NORTH);
        mine.add(sideWrap, BorderLayout.LINE_END);
        p.add(mine, BorderLayout.SOUTH);
        return p;
    }

    /** Course list rows with a checkbox in front. */
    private final class CheckRenderer extends JCheckBox implements javax.swing.ListCellRenderer<Catalog.Course> {
        @Override public Component getListCellRendererComponent(JList<? extends Catalog.Course> list, Catalog.Course c,
                                                                int index, boolean sel, boolean focus) {
            setText(c.toString());
            setSelected(checked.contains(c.code));
            setOpaque(true);
            setBackground(index % 2 == 0 ? Color.WHITE : new Color(250, 251, 252));
            setForeground(Theme.DARK);
            setFont(list.getFont());
            setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            return this;
        }
    }

    private void fillDepartments() {
        busy = true;
        int keep = deptList.getSelectedIndex();
        deptModel.clear();
        deptModel.addElement(t("dept.all"));
        Catalog cat = Catalog.get();
        for (String s : cat.subjects()) {
            String n = cat.subjectNames.getOrDefault(s, "");
            deptModel.addElement(s + "  -  " + n);
        }
        deptList.setSelectedIndex(keep >= 0 && keep < deptModel.size() ? keep : 0);
        busy = false;
    }

    private void refreshCourses() {
        String dept = "";
        int di = deptList.getSelectedIndex();
        if (di > 0) dept = deptModel.get(di).split("\\s")[0];
        String q = searchField.getText().trim();
        String qq = q.replaceAll("\\s+", "").toLowerCase();
        List<Catalog.Course> shown = new ArrayList<>();
        for (Catalog.Course c : Catalog.get().courses) {
            if (!dept.isEmpty() && !c.subject.equals(dept)) continue;
            if (!q.isEmpty() && !c.code.replaceAll("\\s+", "").toLowerCase().contains(qq)
                    && !c.title.toLowerCase().contains(q.toLowerCase())) continue;
            shown.add(c);
            if (shown.size() >= 600) break;
        }
        courseModel.clear();
        courseModel.addAll(shown);
        shownLabel.setText(Strings.fmt(t("fmt.shown"), shown.size()));
    }

    private Catalog.Course courseOrStub(String code) {
        Catalog.Course c = Catalog.get().find(code);
        return c != null ? c : new Catalog.Course(code, "", 0, code.split(" ")[0]);
    }

    private void refreshMine() {
        mineModel.clear();
        int cr = 0;
        for (String code : settings.courses) {
            Catalog.Course c = courseOrStub(code);     // courses no longer in the catalog still show
            mineModel.addElement(c);
            cr += c.credits;
        }
        credits.setText(settings.courses.isEmpty() ? t("credits.none")
                : Strings.fmt(t("credits.fmt"), settings.courses.size(), cr));
    }

    private void addChecked() {
        for (Catalog.Course c : Catalog.get().courses) {
            if (checked.contains(c.code) && !settings.courses.contains(c.code)) settings.courses.add(c.code);
        }
        checked.clear();
        courseList.repaint();
        refreshMine();
    }

    private void removeSelected() {
        for (Catalog.Course c : mineList.getSelectedValuesList()) settings.courses.remove(c.code);
        refreshMine();
    }

    // ------------------------------------------------------------ tab 2: custom filters

    private JComponent buildFiltersTab() {
        JPanel p = page();
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.gridwidth = 3; g.anchor = GridBagConstraints.LINE_START;
        g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(0, 0, 6, 0);
        top.add(heading("cust.head"), g);
        g.gridy++; top.add(label("cust.help1"), g);
        g.gridy++; top.add(label("cust.help2"), g);
        g.gridy++; g.insets = new Insets(0, 0, 14, 0); top.add(mutedLabel("cust.example"), g);

        g.gridy++; g.gridwidth = 1; g.insets = new Insets(0, 0, 4, 10);
        g.weightx = 0.35; top.add(label("cust.folder"), g);
        g.gridx = 1; g.weightx = 0.65; top.add(label("cust.keywords"), g);
        g.gridy++; g.gridx = 0; g.weightx = 0.35; top.add(filterFolder, g);
        g.gridx = 1; g.weightx = 0.65; top.add(filterKeys, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        top.add(button("cust.add", true, this::addFilter), g);
        g.gridy++; g.gridx = 0; g.gridwidth = 3; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(6, 0, 0, 0);
        wholeWordBox.setSelected(true);
        top.add(check(wholeWordBox, "cust.whole"), g);
        filterKeys.addActionListener(e -> addFilter());
        p.add(top, BorderLayout.NORTH);

        JPanel list = new JPanel(new BorderLayout(10, 6));
        list.setOpaque(false);
        list.add(heading("cust.list"), BorderLayout.NORTH);
        filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filterList.addListSelectionListener(e -> {
            int i = filterList.getSelectedIndex();
            if (e.getValueIsAdjusting() || i < 0 || i >= settings.filters.size()) return;
            Settings.Filter f = settings.filters.get(i);          // pick a filter to edit it
            filterFolder.setText(f.folder);
            filterKeys.setText(String.join(", ", f.keywords));
            wholeWordBox.setSelected(f.wholeWord);
        });
        keepLtr.add(filterList);
        list.add(scroll(filterList), BorderLayout.CENTER);
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        JPanel sideBtns = new JPanel(new GridLayout(0, 1, 0, 6));
        sideBtns.setOpaque(false);
        sideBtns.add(button("btn.remove", false, () -> {
            int i = filterList.getSelectedIndex();
            if (i >= 0 && i < settings.filters.size()) { settings.filters.remove(i); refreshFilters(); }
        }));
        sideBtns.add(button("cust.apply", false, this::applyToSorted));
        side.add(sideBtns, BorderLayout.NORTH);
        list.add(side, BorderLayout.LINE_END);
        p.add(list, BorderLayout.CENTER);
        return p;
    }

    private void refreshFilters() {
        filterModel.clear();
        for (Settings.Filter f : settings.filters) {
            filterModel.addElement(f.folder + "   <-   " + String.join(", ", f.keywords) + "     ("
                    + (f.wholeWord ? t("cust.wordTag") : t("cust.anyTag")) + ")");
        }
        if (settings.filters.isEmpty()) filterModel.addElement(t("cust.empty"));
    }

    private boolean addFilter() {
        String folder = filterFolder.getText().trim().replaceAll("^\\.+|\\.+$", "");
        if (folder.isEmpty() || folder.matches(".*[\\\\/:*?\"<>|].*")) { warn(t("warn.custFolder")); return false; }
        List<String> keys = Settings.splitKeywords(filterKeys.getText());
        if (keys.isEmpty()) { warn(t("warn.custKeys")); return false; }
        Settings.Filter nf = new Settings.Filter(folder, keys, wholeWordBox.isSelected());
        int at = -1;
        for (int i = 0; i < settings.filters.size(); i++) if (settings.filters.get(i).folder.equalsIgnoreCase(folder)) at = i;
        if (at >= 0) settings.filters.set(at, nf); else settings.filters.add(nf);   // same folder = edit
        filterFolder.setText("");
        filterKeys.setText("");
        wholeWordBox.setSelected(true);
        refreshFilters();
        return true;
    }

    // ------------------------------------------------------------ tab 3: sorting

    private JComponent buildSortingTab() {
        JPanel p = page();
        JPanel top = new JPanel(new BorderLayout(8, 6));
        top.setOpaque(false);
        top.add(heading("lbl.watch"), BorderLayout.NORTH);
        keepLtr.add(watchField);
        top.add(watchField, BorderLayout.CENTER);
        top.add(button("btn.browse", false, this::browse), BorderLayout.LINE_END);
        p.add(top, BorderLayout.NORTH);

        JPanel types = new JPanel();
        types.setOpaque(false);
        types.setLayout(new BoxLayout(types, BoxLayout.Y_AXIS));
        Consumer<JComponent> addLeft = c -> { c.setAlignmentX(Component.LEFT_ALIGNMENT); types.add(c); };
        addLeft.accept(heading("lbl.unmatched"));
        types.add(Box.createVerticalStrut(4));
        check(byTypeBox, "chk.byType");
        byTypeBox.addActionListener(e -> updateTypeEnabled());
        addLeft.accept(byTypeBox);
        types.add(Box.createVerticalStrut(4));

        // the editable list of types: tick to use, click a row to edit it
        typeList.setCellRenderer(new TypeRenderer());
        typeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        typeList.setVisibleRowCount(8);
        typeList.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int i = typeList.locationToIndex(e.getPoint());
                if (i < 0 || !typeList.isEnabled() || !typeList.getCellBounds(i, i).contains(e.getPoint())) return;
                Settings.TypeDef td = typeModel.get(i);
                if (e.getX() < 26) { td.on = !td.on; typeList.repaint(); }      // the tick box
                typeName.setText(td.name);
                typeExts.setText(extText(td.extensions));
            }
        });
        keepLtr.add(typeList);
        JScrollPane typeScroll = scroll(typeList);
        typeScroll.setPreferredSize(new Dimension(440, 176));
        typeScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 176));
        addLeft.accept(typeScroll);
        types.add(Box.createVerticalStrut(6));

        JPanel editor = new JPanel(new GridBagLayout());
        editor.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.LINE_START; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 2, 8); g.weightx = 0.35;
        JLabel ln = label("types.folder");
        editor.add(ln, g);
        g.gridx = 1; g.weightx = 0.65; g.insets = new Insets(0, 0, 2, 0);
        JLabel le = label("types.exts");
        editor.add(le, g);
        g.gridy = 1; g.gridx = 0; g.weightx = 0.35; g.insets = new Insets(0, 0, 2, 8); editor.add(typeName, g);
        g.gridx = 1; g.weightx = 0.65; g.insets = new Insets(0, 0, 2, 0); editor.add(typeExts, g);
        keepLtr.add(typeExts);
        typeExts.addActionListener(e -> addType());
        editor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        addLeft.accept(editor);
        types.add(Box.createVerticalStrut(6));
        Theme.KButton addType = button("types.add", true, this::addType);
        Theme.KButton removeType = button("types.remove", false, this::removeType);
        Theme.KButton resetTypes = button("types.reset", false, this::resetTypes);
        JPanel typeBtns = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        typeBtns.setOpaque(false);
        typeBtns.add(addType);
        typeBtns.add(Box.createHorizontalStrut(8));
        typeBtns.add(removeType);
        typeBtns.add(Box.createHorizontalStrut(8));
        typeBtns.add(resetTypes);
        addLeft.accept(typeBtns);
        types.add(Box.createVerticalStrut(4));
        JLabel help = wrappedMuted("types.help", 330);
        addLeft.accept(help);
        typeEditors.addAll(List.of(typeList, typeName, typeExts, addType, removeType, resetTypes, ln, le, help));
        types.add(Box.createVerticalStrut(6));
        check(otherBox, "chk.other");
        addLeft.accept(otherBox);

        // files the user moves back himself (right column, under Safety)
        JPanel keep = new JPanel();
        keep.setOpaque(false);
        keep.setLayout(new BoxLayout(keep, BoxLayout.Y_AXIS));
        Consumer<JComponent> addKeep = c -> { c.setAlignmentX(Component.LEFT_ALIGNMENT); keep.add(c); };
        keep.add(Box.createVerticalStrut(14));
        addKeep.accept(heading("lbl.keep"));
        keep.add(Box.createVerticalStrut(4));
        check(keepMovesBox, "chk.keepMoves");
        addKeep.accept(keepMovesBox);
        addKeep.accept(wrappedMuted("keep.help", 290));
        keep.add(Box.createVerticalStrut(6));
        JPanel forgetRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        forgetRow.setOpaque(false);
        forgetRow.add(button("btn.forget", false, this::forgetMoves));
        addKeep.accept(forgetRow);

        JPanel safe = new JPanel(new GridLayout(0, 1, 0, 4));
        safe.setBackground(Theme.BG);
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Theme.GRAY), "");
        tb.setTitleColor(Theme.FOREST);
        retext.add(() -> tb.setTitle(t("gb.safe")));
        safe.setBorder(BorderFactory.createCompoundBorder(tb, BorderFactory.createEmptyBorder(6, 10, 8, 10)));
        for (int i = 1; i <= 8; i++) {
            JLabel l = label("safe." + i);
            l.setForeground(Theme.DARK);
            safe.add(l);
        }
        safe.add(mutedLabel("lbl.settingsAt"));
        JLabel where = new JLabel(AppDirs.dataDir().toString());
        where.setForeground(Theme.GREEN);
        keepLtr.add(where);
        safe.add(where);
        if (AppDirs.OS_TYPE == AppDirs.OS.MAC) safe.add(mutedLabel("mac.access"));

        JPanel body = new JPanel(new GridLayout(1, 2, 20, 0));
        body.setOpaque(false);
        JPanel typesWrap = new JPanel(new BorderLayout());
        typesWrap.setOpaque(false);
        typesWrap.add(types, BorderLayout.NORTH);
        JPanel safeWrap = new JPanel(new BorderLayout());
        safeWrap.setOpaque(false);
        JPanel rightCol = new JPanel(new BorderLayout());
        rightCol.setOpaque(false);
        rightCol.add(safe, BorderLayout.NORTH);
        rightCol.add(keep, BorderLayout.CENTER);
        safeWrap.add(rightCol, BorderLayout.NORTH);
        body.add(typesWrap);
        body.add(safeWrap);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private void updateTypeEnabled() {
        for (JComponent c : typeEditors) c.setEnabled(byTypeBox.isSelected());
        otherBox.setEnabled(byTypeBox.isSelected());
        typeList.repaint();
    }

    /** A type row: tick box, folder name, extensions. */
    private final class TypeRenderer extends JCheckBox implements javax.swing.ListCellRenderer<Settings.TypeDef> {
        @Override public Component getListCellRendererComponent(JList<? extends Settings.TypeDef> list, Settings.TypeDef td,
                                                                int index, boolean sel, boolean focus) {
            boolean builtIn = Strings.has("type." + td.name);
            String name = builtIn && "ar".equals(lang) ? td.name + "  (" + t("type." + td.name) + ")\u200E" : td.name;
            String ex = td.extensions.isEmpty() ? t("types.none") : extText(td.extensions.size() > 9 ? td.extensions.subList(0, 9) : td.extensions)
                    + (td.extensions.size() > 9 ? "  ..." : "");
            setText("\u200E" + name + "      " + ex.replace(",", ""));
            setSelected(td.on);
            setEnabled(list.isEnabled());
            setOpaque(true);
            setBackground(sel ? Theme.PALE : (index % 2 == 0 ? Color.WHITE : new Color(250, 251, 252)));
            setForeground(Theme.DARK);
            setFont(list.getFont());
            setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            return this;
        }
    }

    private static String extText(List<String> exts) {
        StringBuilder b = new StringBuilder();
        for (String e : exts) b.append(b.length() > 0 ? ", " : "").append('.').append(e);
        return b.toString();
    }

    private void refreshTypes() {
        int keep = typeList.getSelectedIndex();
        typeModel.clear();
        for (Settings.TypeDef td : settings.types) typeModel.addElement(td);
        if (keep >= 0 && keep < typeModel.size()) typeList.setSelectedIndex(keep);
    }

    /** Adds a new type, or updates the one with the same folder name. An extension belongs to one type only. */
    private boolean addType() {
        String name = typeName.getText().trim().replaceAll("^\\.+|\\.+$", "");
        if (name.isEmpty() || name.matches(".*[\\\\/:*?\"<>|].*")) { warn(t("warn.custFolder")); return false; }
        List<String> exts = Settings.parseExtensions(typeExts.getText());
        if (exts == null) { warn(t("warn.typeBad")); return false; }
        if (exts.isEmpty()) { warn(t("warn.typeExts")); return false; }
        Settings.TypeDef target = null;
        for (Settings.TypeDef td : settings.types) if (td.name.equalsIgnoreCase(name)) target = td;
        if (target == null) { target = new Settings.TypeDef(name, exts, true); settings.types.add(target); }
        else { target.name = name; target.extensions = new ArrayList<>(exts); target.on = true; }
        List<String> taken = new ArrayList<>();
        for (Settings.TypeDef td : settings.types) {
            if (td == target) continue;
            for (String e : exts) if (td.extensions.remove(e)) taken.add("." + e);
        }
        typeName.setText("");
        typeExts.setText("");
        refreshTypes();
        typeList.setSelectedIndex(settings.types.indexOf(target));
        typeList.ensureIndexIsVisible(settings.types.indexOf(target));
        if (taken.isEmpty()) setFooter("foot.typeSaved", name);
        else setFooter("foot.typeMoved", String.join(" ", taken), name);
        return true;
    }

    private void removeType() {
        int i = typeList.getSelectedIndex();
        if (i < 0 || i >= settings.types.size()) return;
        settings.types.remove(i);
        typeName.setText("");
        typeExts.setText("");
        refreshTypes();
    }

    private void resetTypes() {
        if (!ask(t("ask.resetTypes"))) return;
        settings.types = Settings.defaultTypes();
        typeName.setText("");
        typeExts.setText("");
        refreshTypes();
    }

    private void forgetMoves() {
        int n = io.github.kal429.kfupmsorter.MovedMemory.forget();
        setFooter("foot.forgot", n);
    }

    private void browse() {
        JFileChooser fc = new JFileChooser(watchField.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle(t("browse.desc"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) watchField.setText(fc.getSelectedFile().getPath());
    }

    // ------------------------------------------------------------ tab 4: status

    private JComponent buildStatusTab() {
        JPanel p = page();
        JPanel top = new JPanel(new BorderLayout(12, 4));
        top.setOpaque(false);
        JPanel texts = new JPanel(new GridLayout(0, 1, 0, 4));
        texts.setOpaque(false);
        texts.add(heading("lbl.auto"));
        autoLabel.setFont(Theme.size(Theme.base().getSize2D() + 2f, Font.BOLD));
        texts.add(autoLabel);
        keepLtr.add(lastRunLabel);
        texts.add(lastRunLabel);
        bothLabel.setForeground(Theme.STONE);
        texts.add(bothLabel);
        top.add(texts, BorderLayout.CENTER);
        JPanel btns = new JPanel(new GridLayout(2, 1, 0, 6));
        btns.setOpaque(false);
        enableBtn = button("btn.enable", true, this::enableAuto);
        disableBtn = button("btn.disable", false, this::disableAuto);
        btns.add(enableBtn);
        btns.add(disableBtn);
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(btns, BorderLayout.NORTH);
        top.add(btnWrap, BorderLayout.LINE_END);
        p.add(top, BorderLayout.NORTH);

        JPanel logBox = new JPanel(new BorderLayout(0, 6));
        logBox.setOpaque(false);
        logBox.add(heading("lbl.recent"), BorderLayout.NORTH);
        logList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Theme.base().getSize()));
        keepLtr.add(logList);
        logBox.add(scroll(logList), BorderLayout.CENTER);
        p.add(logBox, BorderLayout.CENTER);

        p.add(row(button("btn.sortNow", true, () -> runSort(false)),
                  button("btn.preview", false, () -> runSort(true)),
                  button("btn.openFolder", false, this::openFolder),
                  button("btn.refresh", false, this::refreshStatus)), BorderLayout.SOUTH);
        return p;
    }

    private void refreshStatus() {
        boolean on = prefs.auto && Autostart.isEnabled();
        boolean running = false;
        String last = t("last.none");
        try {
            Path hb = AppDirs.lastRunFile();
            if (Files.exists(hb)) {
                last = String.join("   |   ", Files.readAllLines(hb)).replace("﻿", "");
                running = System.currentTimeMillis() - Files.getLastModifiedTime(hb).toMillis() < 3 * 60_000L;
            }
        } catch (Exception ignored) { }
        if (on && running) { autoLabel.setText(t("auto.on")); autoLabel.setForeground(Theme.GREEN); }
        else if (on) { autoLabel.setText(t("auto.idle")); autoLabel.setForeground(Theme.STONE); }
        else { autoLabel.setText(t("auto.off")); autoLabel.setForeground(Theme.BAD); }
        lastRunLabel.setText(last);
        if (enableBtn != null) {
            enableBtn.setEnabled(!(on && running));
            disableBtn.setEnabled(on);
        }
        // the Windows PowerShell edition sorting the same folder?
        boolean both = false;
        try {
            Path ps = AppDirs.powershellEditionHeartbeat();
            both = ps != null && Files.exists(ps)
                    && System.currentTimeMillis() - Files.getLastModifiedTime(ps).toMillis() < 3 * 60_000L;
        } catch (Exception ignored) { }
        bothLabel.setText(both ? t("ps.both") : " ");

        logModel.clear();
        List<String> moves = SortLog.recentMoves(200);
        if (moves.isEmpty()) logModel.addElement(t("log.empty")); else logModel.addAll(moves);
    }

    private void enableAuto() {
        try {
            if (!Files.exists(AppDirs.rulesFile()) && !save(false)) return;
            prefs.auto = true;
            prefs.save();
            Autostart.enable();
            Autostart.startNow();
        } catch (Exception e) {
            warn(t("err.auto") + e.getMessage());
        }
        new Timer(2500, ev -> { ((Timer) ev.getSource()).stop(); refreshStatus(); }).start();
        refreshStatus();
    }

    private void disableAuto() {
        prefs.auto = false;       // the background sorter notices within a minute and stops
        prefs.save();
        try { Autostart.disable(); } catch (Exception ignored) { }
        refreshStatus();
    }

    private void runSort(boolean dry) {
        if (!save(false)) return;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Engine.Result, Void>() {
            @Override protected Engine.Result doInBackground() { return Engine.runSaved(dry); }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    Engine.Result r = get();
                    if (dry) {
                        logModel.clear();
                        logModel.addElement(t("preview.head"));
                        for (Engine.Move m : r.moves) logModel.addElement(m.file + "  ->  " + m.folder);
                    } else {
                        refreshStatus();
                        setFooter("foot.sorted", now());
                    }
                    if (!r.errors.isEmpty()) warn(String.join("\n", r.errors));
                } catch (Exception e) {
                    warn(e.getMessage());
                }
            }
        }.execute();
    }

    private void openFolder() {
        try { Desktop.getDesktop().open(new File(watchField.getText().trim())); } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------ tab 5: about

    private JComponent buildAboutTab() {
        JPanel p = page();
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        Consumer<JComponent> add = c -> { c.setAlignmentX(Component.LEFT_ALIGNMENT); col.add(c); };

        JLabel title = new JLabel("KFUPM Sorter");
        title.setFont(Theme.size(Theme.base().getSize2D() + 9f, Font.BOLD));
        title.setForeground(Theme.GREEN);
        add.accept(title);
        retext.add(() -> versionLabel.setText(Strings.fmt(t("about.version"), Main.VERSION) + "   ·   " + t("edition")));
        add.accept(versionLabel);
        col.add(Box.createVerticalStrut(18));

        add.accept(heading("about.catalog"));
        col.add(Box.createVerticalStrut(4));
        add.accept(catalogLabel);
        add.accept(mutedLabel("about.source"));
        col.add(Box.createVerticalStrut(8));
        updateBtn = button("btn.update", true, this::updateCatalog);
        add.accept(row(updateBtn, updLabel));
        progress.setVisible(false);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        add.accept(progress);
        col.add(Box.createVerticalStrut(18));

        add.accept(heading("about.privacy"));
        col.add(Box.createVerticalStrut(4));
        add.accept(label("privacy.1"));
        add.accept(label("privacy.2"));
        col.add(Box.createVerticalStrut(18));

        add.accept(heading("about.open"));
        col.add(Box.createVerticalStrut(4));
        add.accept(link(REPO_URL));
        add.accept(link(SITE_URL));
        col.add(Box.createVerticalStrut(18));
        add.accept(mutedLabel("about.disclaimer"));

        p.add(col, BorderLayout.NORTH);
        return p;
    }

    private JLabel link(String url) {
        JLabel l = new JLabel("<html><u>" + url + "</u></html>");
        l.setForeground(Theme.GREEN);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ignored) { }
            }
        });
        keepLtr.add(l);
        return l;
    }

    private void refreshCatalogLabel() {
        Catalog cat = Catalog.get();
        String when = cat.updated.isEmpty() ? t("catalog.bundled") : Strings.fmt(t("catalog.updated"), cat.updated);
        catalogLabel.setText(Strings.fmt(t("catalog.fmt"), cat.courses.size(), cat.subjects().size(), when));
    }

    private void updateCatalog() {
        updateBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        progress.setVisible(true);
        progress.setIndeterminate(true);
        updLabel.setText(t("upd.connecting"));
        new SwingWorker<Integer, String>() {
            @Override protected Integer doInBackground() throws Exception { return Catalog.update(this::publish); }
            @Override protected void process(List<String> chunks) {
                String[] parts = chunks.get(chunks.size() - 1).split("\\s+");   // "i/total CODE found"
                String[] it = parts[0].split("/");
                progress.setIndeterminate(false);
                progress.setMaximum(Integer.parseInt(it[1]));
                progress.setValue(Integer.parseInt(it[0]));
                updLabel.setText(Strings.fmt(t("upd.progress"), it[0], it[1], parts[1], parts[2]));
            }
            @Override protected void done() {
                progress.setVisible(false);
                updateBtn.setEnabled(true);
                saveBtn.setEnabled(true);
                try {
                    int n = get();
                    updLabel.setText(Strings.fmt(t("upd.done"), n));
                    fillDepartments();
                    refreshCourses();
                    refreshMine();
                    refreshCatalogLabel();
                } catch (Exception e) {
                    updLabel.setText(t("upd.failed"));
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    warn(t("err.update") + c.getMessage());
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------ settings

    private void loadSettings() {
        if (Files.exists(AppDirs.rulesFile())) {
            settings = Settings.load();
        } else {
            Settings imported = prefs.imported ? null : Settings.importPowershellEdition();
            prefs.imported = true;
            prefs.save();
            if (imported != null) { settings = imported; footerKey = "foot.imported"; }
            else { settings = new Settings(); footerKey = "foot.first"; }
        }
        savedNameFolders.addAll(nameFolders());
    }

    /** Folders of the custom filters and courses. */
    private Set<String> nameFolders() {
        Set<String> out = new HashSet<>();
        for (Settings.Filter f : settings.filters) out.add(f.folder);
        for (String c : settings.courses) out.add(settings.courseFolder(c));
        return out;
    }

    /**
     * Files sorted by type before a filter or course existed (e.g. in Documents) are offered
     * a move to that filter's or course's folder. only = some folders, or null for all.
     */
    private void offerResorts(Set<String> only, boolean tellIfNone) {
        Engine e = new Engine(settings.toRules());
        List<Engine.Resort> list = e.findResorts(only);
        if (list.isEmpty()) { if (tellIfNone) info(t("resort.none")); return; }
        StringBuilder b = new StringBuilder(Strings.fmt(t("ask.resort"), list.size())).append("\n\n");
        for (int i = 0; i < Math.min(8, list.size()); i++) {
            Engine.Resort r = list.get(i);
            b.append("\u200E").append(r.from).append(" / ").append(r.file).append("   ->   ").append(r.to).append("\n");
        }
        if (list.size() > 8) b.append(Strings.fmt(t("resort.more"), list.size() - 8));
        if (!ask(b.toString())) return;
        int n = e.applyResorts(list);
        setFooter("foot.resorted", n);
        refreshStatus();
    }

    private void applyToSorted() {
        if (!save(false)) return;
        offerResorts(null, true);
    }

    private void applySettingsToForm() {
        termField.setText(settings.term);
        termFolderBox.setSelected(settings.termFolder);
        watchField.setText(settings.watchFolder);
        byTypeBox.setSelected(settings.sortByType);
        refreshTypes();
        otherBox.setSelected(settings.otherFolder);
        keepMovesBox.setSelected(settings.keepUserMoves);
        updateTypeEnabled();
    }

    private void readForm() {
        settings.term = termField.getText().trim();
        settings.termFolder = termFolderBox.isSelected();
        settings.watchFolder = watchField.getText().trim();
        settings.sortByType = byTypeBox.isSelected();
        settings.otherFolder = otherBox.isSelected();
        settings.keepUserMoves = keepMovesBox.isSelected();
        settings.courses = new ArrayList<>(new LinkedHashSet<>(settings.courses));
    }

    private void save() {
        if (!save(true)) return;
    }

    /** Validates and writes rules.json. When interactive, offers to turn on automatic sorting. */
    private boolean save(boolean interactive) {
        // a filter typed but not yet added is almost certainly meant to be saved too
        if (!filterFolder.getText().trim().isEmpty() && !filterKeys.getText().trim().isEmpty() && !addFilter()) return false;
        if (!typeName.getText().trim().isEmpty() && !typeExts.getText().trim().isEmpty() && byTypeBox.isSelected()) {
            Settings.TypeDef same = null;
            for (Settings.TypeDef td : settings.types) if (td.name.equalsIgnoreCase(typeName.getText().trim())) same = td;
            // only when it is a change, not just a row picked for viewing
            if ((same == null || !extText(same.extensions).equals(extText(Settings.parseExtensions(typeExts.getText()) == null
                    ? List.of() : Settings.parseExtensions(typeExts.getText())))) && !addType()) return false;
        }
        readForm();
        if (!Files.isDirectory(Paths.get(settings.watchFolder))) { warn(t("warn.folder")); return false; }
        if (settings.courses.isEmpty() && settings.filters.isEmpty() && !settings.sortByType) { warn(t("warn.nothing")); return false; }
        if (settings.termFolder && settings.term.isEmpty()) { warn(t("warn.term")); return false; }
        try {
            settings.save();
        } catch (Exception e) {
            warn(t("err.save") + e.getMessage());
            return false;
        }
        setFooter("foot.saved", now());
        Set<String> added = nameFolders();
        added.removeAll(savedNameFolders);
        savedNameFolders.clear();
        savedNameFolders.addAll(nameFolders());
        if (interactive && !added.isEmpty()) offerResorts(added, false);
        if (interactive && !(prefs.auto && Autostart.isEnabled())) {
            if (ask(t("ask.enable"))) enableAuto();
        }
        return true;
    }

    private static String now() { return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); }

    // ------------------------------------------------------------ language

    private void applyLanguage(boolean switching) {
        UIManager.put("OptionPane.yesButtonText", t("btn.yes"));
        UIManager.put("OptionPane.noButtonText", t("btn.no"));
        UIManager.put("OptionPane.okButtonText", t("btn.ok"));
        UIManager.put("OptionPane.cancelButtonText", t("btn.cancel"));
        for (Runnable r : retext) r.run();
        typeList.repaint();
        if (switching) {
            int keep = deptList.getSelectedIndex();
            busy = true;
            if (deptModel.size() > 0) deptModel.set(0, t("dept.all"));
            deptList.setSelectedIndex(keep);
            busy = false;
        }
        shownLabel.setText(Strings.fmt(t("fmt.shown"), courseModel.size()));
        refreshMine();
        refreshFilters();
        refreshCatalogLabel();
        refreshStatus();
        if (!footerKey.isEmpty()) setFooter(footerKey, footerArgs);

        // Arabic reads right to left: Swing mirrors every layout for us
        ComponentOrientation o = "ar".equals(lang) ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
        getContentPane().applyComponentOrientation(o);
        for (JComponent c : keepLtr) c.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        langBox.setSelectedIndex("ar".equals(lang) ? 1 : 0);
        getContentPane().revalidate();
        getContentPane().repaint();
    }
}
