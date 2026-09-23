package io.github.kal429.kfupmsorter;

import java.util.HashMap;
import java.util.Map;

/**
 * Interface text. English is the default; Arabic is Modern Standard Arabic.
 * Same wording as the Windows PowerShell edition (src/Strings.ps1).
 * Placeholders {0} {1} ... are filled by fmt().
 */
public final class Strings {
    private Strings() {}

    private static final Map<String, Map<String, String>> TABLE = new HashMap<>();

    private static void en() {
        Map<String, String> m = new HashMap<>();
        m.put("about.catalog", "Course catalog");
        m.put("about.disclaimer", "Independent student project. Not affiliated with or endorsed by King Fahd University of Petroleum and Minerals.");
        m.put("about.open", "Open source");
        m.put("about.privacy", "Privacy");
        m.put("about.source", "Source: the official university bulletin, bulletin.kfupm.edu.sa (public pages, no login).");
        m.put("about.version", "Version {0}");
        m.put("app.subtitle", "Sort your downloads by course, automatically");
        m.put("ask.enable", "Settings saved.\n\nTurn on automatic sorting now? It checks the folder quietly every minute.");
        m.put("auto.idle", "On, but not running right now. Press \"Turn on automatic sorting\" to start it again.");
        m.put("auto.off", "Off  -  files are sorted only when you press \"Sort now\"");
        m.put("auto.on", "On  -  checks the folder every minute");
        m.put("browse.desc", "Choose the folder to keep sorted");
        m.put("btn.add", "+  Add checked");
        m.put("btn.browse", "Browse...");
        m.put("btn.cancel", "Cancel");
        m.put("btn.clear", "Clear all");
        m.put("btn.close", "Close");
        m.put("btn.disable", "Turn off automatic sorting");
        m.put("btn.enable", "Turn on automatic sorting");
        m.put("btn.no", "No");
        m.put("btn.ok", "OK");
        m.put("btn.openFolder", "Open folder");
        m.put("btn.preview", "Preview (no moves)");
        m.put("btn.refresh", "Refresh");
        m.put("btn.remove", "Remove selected");
        m.put("btn.save", "Save");
        m.put("btn.sortNow", "Sort now");
        m.put("btn.update", "Update catalog from KFUPM");
        m.put("btn.yes", "Yes");
        m.put("catalog.bundled", "copy shipped with the app");
        m.put("catalog.fmt", "{0} courses from {1} departments  -  {2}");
        m.put("catalog.updated", "updated {0}");
        m.put("chk.byType", "Sort them by file type");
        m.put("chk.other", "Put any other file type in an \"Other\" folder");
        m.put("chk.termFolder", "Put course folders inside a term folder  (e.g. 261\\COE 301)");
        m.put("credits.fmt", "{0} courses  |  {1} credit hours");
        m.put("credits.none", "No courses selected yet");
        m.put("cust.add", "+  Add filter");
        m.put("cust.anyTag", "anywhere in the name");
        m.put("cust.count", "{0} custom filters");
        m.put("cust.empty", "No custom filters yet.");
        m.put("cust.example", "Example:  folder  Internship,  keywords  internship, coop, training");
        m.put("cust.folder", "Folder name");
        m.put("cust.head", "Your own filters");
        m.put("cust.help1", "Any file whose name contains one of the keywords is moved to that folder.");
        m.put("cust.help2", "Custom filters are checked first, before courses and file types.");
        m.put("cust.keywords", "Keywords, separated by commas");
        m.put("cust.list", "Your filters");
        m.put("cust.whole", "Whole word only  (\"lab\" matches \"Lab 3 Report.pdf\" but not \"Syllabus.pdf\")");
        m.put("cust.wordTag", "whole word");
        m.put("dept.all", "*  All departments");
        m.put("edition", "Desktop edition for Windows and macOS (Java)");
        m.put("err.auto", "Automatic sorting could not be turned on: ");
        m.put("err.save", "Could not save: ");
        m.put("err.update", "Could not update the catalog: ");
        m.put("fmt.shown", "{0} courses shown");
        m.put("foot.first", "First time? Pick your courses, then press Save.");
        m.put("foot.imported", "Imported your choices from the PowerShell edition. Review them and press Save.");
        m.put("foot.saved", "Settings saved  -  {0}");
        m.put("foot.sorted", "Sorted  -  {0}");
        m.put("gb.safe", "Safety");
        m.put("lang.label", "Language");
        m.put("last.none", "Has not run yet.");
        m.put("lbl.auto", "Automatic sorting");
        m.put("lbl.dept", "Department");
        m.put("lbl.mine", "My courses this term");
        m.put("lbl.recent", "Recently moved files");
        m.put("lbl.search", "Search by course code or title");
        m.put("lbl.settingsAt", "Settings are stored in:");
        m.put("lbl.term", "Term");
        m.put("lbl.unmatched", "Files that match no course");
        m.put("lbl.watch", "Folder to keep sorted");
        m.put("log.empty", "No files moved yet.");
        m.put("mac.access", "macOS may ask to let KFUPM Sorter use your Downloads folder. Choose Allow.");
        m.put("preview.head", "Preview  -  nothing was moved:");
        m.put("privacy.1", "Everything runs on your computer. No file or file name is ever sent anywhere.");
        m.put("privacy.2", "The only network access is the catalog update button, which reads public bulletin pages.");
        m.put("ps.both", "The Windows PowerShell edition of KFUPM Sorter is also sorting this folder. Turn one of them off.");
        m.put("safe.1", "Never deletes anything. It only moves files.");
        m.put("safe.2", "Never overwrites. A duplicate becomes \"name (1)\".");
        m.put("safe.3", "Unfinished downloads (.crdownload, .part) are skipped.");
        m.put("safe.4", "Files younger than 20 seconds, or open in another");
        m.put("safe.5", "program, are left alone until they are ready.");
        m.put("safe.6", "Only files are moved, never folders.");
        m.put("safe.7", "Anything that matches no rule stays where it is.");
        m.put("safe.8", "Every move is logged in the Status tab.");
        m.put("tab.about", "About");
        m.put("tab.courses", "My Courses");
        m.put("tab.custom", "Custom Filters");
        m.put("tab.sort", "Sorting");
        m.put("tab.status", "Status");
        m.put("tray.open", "Open KFUPM Sorter");
        m.put("tray.quit", "Stop until next sign-in");
        m.put("tray.sort", "Sort now");
        m.put("type.3D Printing", "3D Printing");
        m.put("type.Archives", "Archives");
        m.put("type.Audio", "Audio");
        m.put("type.Code", "Code");
        m.put("type.Documents", "Documents");
        m.put("type.Images", "Images");
        m.put("type.Installers", "Installers");
        m.put("type.Videos", "Videos");
        m.put("type.eBooks", "eBooks");
        m.put("upd.connecting", "Connecting to bulletin.kfupm.edu.sa ...");
        m.put("upd.done", "Done: {0} courses.");
        m.put("upd.failed", "Update failed.");
        m.put("upd.progress", "{0} / {1}    {2}    ({3} courses)");
        m.put("warn.custFolder", "Enter a folder name. It cannot contain  \\ / : * ? \" < > |");
        m.put("warn.custKeys", "Enter at least one keyword.");
        m.put("warn.folder", "The folder to sort does not exist.");
        m.put("warn.nothing", "Pick at least one course, add a custom filter, or turn on sorting by file type.");
        m.put("warn.term", "Enter the term number (e.g. 261), or turn off the term folder option.");
        TABLE.put("en", m);
    }

    private static void ar() {
        Map<String, String> m = new HashMap<>();
        m.put("about.catalog", "دليل المقررات");
        m.put("about.disclaimer", "مشروع طلابي مستقل، غير تابع لجامعة الملك فهد للبترول والمعادن ولا معتمد منها.");
        m.put("about.open", "مفتوح المصدر");
        m.put("about.privacy", "الخصوصية");
        m.put("about.source", "المصدر: النشرة الرسمية للجامعة bulletin.kfupm.edu.sa (صفحات عامة دون تسجيل دخول).");
        m.put("about.version", "الإصدار {0}");
        m.put("app.subtitle", "رتّب تنزيلاتك حسب مقرراتك تلقائيًا");
        m.put("ask.enable", "حُفظت الإعدادات.\n\nهل تريد تفعيل الترتيب التلقائي الآن؟ سيفحص المجلد بهدوء كل دقيقة.");
        m.put("auto.idle", "مفعّل، لكنه لا يعمل الآن. اضغط \"فعّل الترتيب التلقائي\" لتشغيله من جديد.");
        m.put("auto.off", "متوقف  -  لا يُرتَّب إلا عند الضغط على \"رتّب الآن\"");
        m.put("auto.on", "مفعّل  -  يفحص المجلد كل دقيقة");
        m.put("browse.desc", "اختر المجلد المراد ترتيبه تلقائيًا");
        m.put("btn.add", "+  أضف المحدد");
        m.put("btn.browse", "استعراض...");
        m.put("btn.cancel", "إلغاء");
        m.put("btn.clear", "امسح الكل");
        m.put("btn.close", "إغلاق");
        m.put("btn.disable", "أوقف الترتيب التلقائي");
        m.put("btn.enable", "فعّل الترتيب التلقائي");
        m.put("btn.no", "لا");
        m.put("btn.ok", "حسنًا");
        m.put("btn.openFolder", "افتح المجلد");
        m.put("btn.preview", "معاينة دون نقل");
        m.put("btn.refresh", "تحديث");
        m.put("btn.remove", "احذف المحدد");
        m.put("btn.save", "حفظ");
        m.put("btn.sortNow", "رتّب الآن");
        m.put("btn.update", "حدّث الدليل من الجامعة");
        m.put("btn.yes", "نعم");
        m.put("catalog.bundled", "نسخة مرفقة بالبرنامج");
        m.put("catalog.fmt", "{0} مقررًا من {1} قسمًا  -  {2}");
        m.put("catalog.updated", "آخر تحديث {0}");
        m.put("chk.byType", "رتّبها حسب نوع الملف");
        m.put("chk.other", "اجمع أنواع الملفات الأخرى في مجلد \"Other\"");
        m.put("chk.termFolder", "ضع مجلدات المقررات داخل مجلد الفصل الدراسي  (مثال: 261\\COE 301)");
        m.put("credits.fmt", "المقررات: {0}  |  الساعات المعتمدة: {1}");
        m.put("credits.none", "لم تختر أي مقرر بعد");
        m.put("cust.add", "+  أضف الفلتر");
        m.put("cust.anyTag", "في أي موضع من الاسم");
        m.put("cust.count", "الفلاتر المخصصة: {0}");
        m.put("cust.empty", "لا توجد فلاتر مخصصة بعد");
        m.put("cust.example", "مثال: المجلد  Internship  والكلمات  internship, coop, training");
        m.put("cust.folder", "اسم المجلد");
        m.put("cust.head", "فلاترك الخاصة");
        m.put("cust.help1", "يُنقل إلى المجلد المحدد كل ملف يحتوي اسمه على إحدى الكلمات المفتاحية.");
        m.put("cust.help2", "تُطبَّق الفلاتر المخصصة أولًا، قبل المقررات وأنواع الملفات.");
        m.put("cust.keywords", "الكلمات المفتاحية، مفصولة بفواصل");
        m.put("cust.list", "فلاترك");
        m.put("cust.whole", "كلمة كاملة فقط  (\"lab\" تطابق \"Lab 3 Report.pdf\" ولا تطابق \"Syllabus.pdf\")");
        m.put("cust.wordTag", "كلمة كاملة");
        m.put("dept.all", "*  جميع الأقسام");
        m.put("edition", "نسخة سطح المكتب لنظامي Windows وmacOS (Java)");
        m.put("err.auto", "تعذّر تفعيل الترتيب التلقائي: ");
        m.put("err.save", "تعذّر الحفظ: ");
        m.put("err.update", "تعذّر تحديث الدليل: ");
        m.put("fmt.shown", "عدد المقررات المعروضة: {0}");
        m.put("foot.first", "أول استخدام؟ اختر مقرراتك ثم اضغط حفظ.");
        m.put("foot.imported", "استُورِدت اختياراتك من نسخة PowerShell. راجعها ثم اضغط حفظ.");
        m.put("foot.saved", "حُفظت الإعدادات  -  {0}");
        m.put("foot.sorted", "اكتمل الترتيب  -  {0}");
        m.put("gb.safe", "ضمانات الأمان");
        m.put("lang.label", "اللغة");
        m.put("last.none", "لم يعمل بعد.");
        m.put("lbl.auto", "الترتيب التلقائي");
        m.put("lbl.dept", "القسم");
        m.put("lbl.mine", "مقرراتي لهذا الفصل");
        m.put("lbl.recent", "آخر الملفات المنقولة");
        m.put("lbl.search", "ابحث برمز المقرر أو اسمه");
        m.put("lbl.settingsAt", "تُحفظ الإعدادات في:");
        m.put("lbl.term", "الفصل");
        m.put("lbl.unmatched", "الملفات التي لا تطابق أي مقرر");
        m.put("lbl.watch", "المجلد المراد ترتيبه");
        m.put("log.empty", "لم يُنقل أي ملف بعد");
        m.put("mac.access", "قد يطلب macOS السماح لـ KFUPM Sorter باستخدام مجلد التنزيلات، فاختر السماح.");
        m.put("preview.head", "معاينة  -  لم يُنقل أي ملف:");
        m.put("privacy.1", "يعمل البرنامج بالكامل على جهازك، ولا يرسل أي ملف أو اسم ملف إلى أي جهة.");
        m.put("privacy.2", "الاتصال الوحيد بالإنترنت هو زر تحديث الدليل، ويقرأ صفحات النشرة العامة فقط.");
        m.put("ps.both", "نسخة PowerShell من KFUPM Sorter ترتّب هذا المجلد أيضًا، فأوقف إحداهما.");
        m.put("safe.1", "لا يحذف أي ملف إطلاقًا، وإنما ينقل فقط.");
        m.put("safe.2", "لا يستبدل ملفًا موجودًا، بل يضيف (1) إلى الاسم المكرر.");
        m.put("safe.3", "يتجاهل التنزيلات غير المكتملة (.crdownload و .part).");
        m.put("safe.4", "يترك الملف الذي مضى على إنشائه أقل من 20 ثانية");
        m.put("safe.5", "أو المفتوح في برنامج آخر حتى يكتمل.");
        m.put("safe.6", "ينقل الملفات فقط، ولا ينقل المجلدات.");
        m.put("safe.7", "يبقى في مكانه كل ما لا يطابق أي قاعدة.");
        m.put("safe.8", "تُسجَّل كل عملية نقل في تبويب الحالة.");
        m.put("tab.about", "حول البرنامج");
        m.put("tab.courses", "مقرراتي");
        m.put("tab.custom", "فلاتر مخصصة");
        m.put("tab.sort", "الفرز");
        m.put("tab.status", "الحالة");
        m.put("tray.open", "افتح KFUPM Sorter");
        m.put("tray.quit", "أوقفه حتى تسجيل الدخول القادم");
        m.put("tray.sort", "رتّب الآن");
        m.put("type.3D Printing", "طباعة ثلاثية الأبعاد");
        m.put("type.Archives", "ملفات مضغوطة");
        m.put("type.Audio", "ملفات صوتية");
        m.put("type.Code", "ملفات برمجية");
        m.put("type.Documents", "مستندات");
        m.put("type.Images", "صور");
        m.put("type.Installers", "برامج تثبيت");
        m.put("type.Videos", "مقاطع فيديو");
        m.put("type.eBooks", "كتب إلكترونية");
        m.put("upd.connecting", "جارٍ الاتصال بـ bulletin.kfupm.edu.sa ...");
        m.put("upd.done", "تم: {0} مقررًا.");
        m.put("upd.failed", "تعذّر التحديث.");
        m.put("upd.progress", "{0} / {1}    {2}    ({3} مقررًا)");
        m.put("warn.custFolder", "أدخل اسم المجلد، على ألا يحتوي على  \\ / : * ? \" < > |");
        m.put("warn.custKeys", "أدخل كلمة مفتاحية واحدة على الأقل.");
        m.put("warn.folder", "المجلد المحدد غير موجود.");
        m.put("warn.nothing", "اختر مقررًا واحدًا على الأقل، أو أضف فلترًا مخصصًا، أو فعّل الفرز حسب نوع الملف.");
        m.put("warn.term", "أدخل رقم الفصل الدراسي (مثال: 261)، أو ألغِ خيار مجلد الفصل.");
        TABLE.put("ar", m);
    }

    static { en(); ar(); }

    public static String get(String lang, String key) {
        Map<String, String> m = TABLE.get(lang);
        if (m != null && m.containsKey(key)) return m.get(key);
        String en = TABLE.get("en").get(key);
        return en != null ? en : key;
    }

    public static String fmt(String text, Object... values) {
        for (int i = 0; i < values.length; i++) text = text.replace("{" + i + "}", String.valueOf(values[i]));
        return text;
    }

    public static boolean has(String key) { return TABLE.get("en").containsKey(key); }
}
