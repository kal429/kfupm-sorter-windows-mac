<p align="center"><img src="docs/icon-256.png" width="110" alt="KFUPM Sorter icon"></p>

<h1 align="center">KFUPM Sorter for Windows and Mac</h1>

<p align="center">Keeps your Downloads folder sorted by KFUPM course, automatically.<br>
<b>One app for macOS and Windows</b> &middot; written in Java, with Java built in &middot; English and Arabic</p>

<p align="center">
  <a href="https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest/download/KFUPM-Sorter-macOS-AppleSilicon.dmg"><b>Mac (M1 or newer)</b></a> &nbsp;&middot;&nbsp;
  <a href="https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest/download/KFUPM-Sorter-macOS-Intel.dmg"><b>Mac (Intel)</b></a> &nbsp;&middot;&nbsp;
  <a href="https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest/download/KFUPM-Sorter-Windows-Setup.exe"><b>Windows</b></a> &nbsp;&middot;&nbsp;
  <a href="#بالعربية">العربية</a>
</p>

> **Two versions of KFUPM Sorter.** This one runs on **both Windows and Mac**. The other one, [kfupm-sorter-windows](https://github.com/kal429/kfupm-sorter-windows), is a lighter **Windows-only** app written in PowerShell, and it also has the iPhone / iPad version. Both sort the same way and use the same settings file, so pick whichever you like, but use only one of them on a Windows PC.

---

## What it does

You pick your courses for the term from the official KFUPM course list. From then on, every file you download goes to its course folder on its own, and anything that is not course material is sorted by type.

```
Downloads/
├── 261/
│   ├── COE 301/          <- Lecture_T261_COE_301_Ch3.pdf
│   ├── EE 236/           <- 261-EE236-L2-Resistive Circuits.pdf
│   └── ENGL 214/         <- ENGL214_Progress_Report.docx
├── Internship/           <- Internship_Offer_Letter.pdf          (custom filter)
├── Documents/            <- anything else that is a pdf, docx, pptx...
├── Videos/
└── Archives/
```

- **2,123 courses from 58 departments** from the official bulletin (bulletin.kfupm.edu.sa), with a button to refresh the list every term.
- **Custom filters**: your own folder with a few keywords, for an internship, a club or a side project. Custom filters are checked before courses.
- **Smart matching**: `COE 301` catches `COE301_Lab.pdf` and `Coe-301 HW.docx`, but not `COE 3011`.
- **Automatic sorting**: a small background sorter checks your Downloads folder every minute and starts when you sign in. It has an icon in the system tray (Windows) or menu bar (Mac) with *Open*, *Sort now* and *Stop*. No administrator rights needed.
- **English or Arabic** interface (right-to-left in Arabic), KFUPM green and gold.

## Safety

- It **never deletes** anything and **never overwrites**. A duplicate name becomes `name (1).pdf`.
- It waits for downloads to finish: unfinished downloads and files younger than 20 seconds are left alone.
- It only moves files, never folders. Every move is logged in the **Status** tab.
- It sends nothing anywhere. The only network access is the optional "update catalog" button, which reads the public bulletin pages.

## Install

| System | File |
|---|---|
| Mac with Apple silicon (M1 or newer) | [`KFUPM-Sorter-macOS-AppleSilicon.dmg`](https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest/download/KFUPM-Sorter-macOS-AppleSilicon.dmg) |
| Mac with Intel | [`KFUPM-Sorter-macOS-Intel.dmg`](https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest/download/KFUPM-Sorter-macOS-Intel.dmg) |
| Windows 10 / 11 | [`KFUPM-Sorter-Windows-Setup.exe`](https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest/download/KFUPM-Sorter-Windows-Setup.exe) |

**Mac:** open the .dmg and drag *KFUPM Sorter Desktop* into Applications. The app is not notarized by Apple yet, so the first time right-click it and choose **Open** (on macOS 15: *System Settings › Privacy & Security › Open Anyway*). When macOS asks to let it use your Downloads folder, choose **Allow**. Not sure which Mac you have? Apple menu › *About This Mac*: "Chip: Apple M…" means Apple silicon.

**Windows:** run the installer. It installs for your user only. If Windows shows "Windows protected your PC", click **More info → Run anyway** (the installer is not code-signed yet). If you used the PowerShell edition before, your courses and filters are imported on first start; turn its automatic sorting off so two sorters don't work on the same folder.

Requirements: macOS 12 or later, or Windows 10 / 11. Nothing else to install.

## How the code is organised

| File | What it does |
|---|---|
| `Main.java` | Starts the window, or the background sorter with `--background` |
| `Engine.java` | The sorting rules: custom filters, then courses, then file types. Safe moves only |
| `Settings.java` | Your choices, saved as `rules.json` (same format as the PowerShell edition) |
| `Catalog.java` | The bundled course list, and the update from bulletin.kfupm.edu.sa |
| `Background.java` | Sorts every minute; tray / menu-bar icon |
| `Autostart.java` | Starts the background sorter at sign-in (Startup folder / LaunchAgent), no admin rights |
| `Strings.java` | English and Arabic text |
| `ui/MainWindow.java`, `ui/Theme.java` | The window (Swing) and the KFUPM colors |
| `Json.java` | A tiny JSON reader and writer, so there are no libraries at all |
| `src/test/.../SelfTest.java` | 31 checks of the rules and the engine |
| `packaging/` | Icons and the Inno Setup script for the Windows installer |

Settings live in `%APPDATA%\KFUPM Sorter Desktop` on Windows and `~/Library/Application Support/KFUPM Sorter Desktop` on Mac.

## Build it yourself

You only need a JDK, version 21 or newer ([Temurin](https://adoptium.net) is free).

```
./build.sh          # compile, run the self-test, make build/jar/kfupm-sorter-desktop.jar
./build.sh run      # ... and open the window
./build.sh package  # ... and make the Mac .dmg (on a Mac) with jpackage
```

On Windows, run it from Git Bash, or let GitHub build everything: every push runs [`build.yml`](.github/workflows/build.yml), and pushing a tag such as `v1.0.1` runs [`release.yml`](.github/workflows/release.yml), which attaches the installers to a new release.

---

<div dir="rtl">

## بالعربية

**KFUPM Sorter لنظامي Windows وMac**: يرتّب مجلد التنزيلات حسب مقررات الجامعة تلقائيًا. هذا التطبيق مكتوب بلغة Java ليعمل البرنامج الواحد على **Windows وmacOS**، وJava مرفقة بداخله فلا يلزم تثبيت أي شيء آخر.

> توجد نسختان من KFUPM Sorter: هذه النسخة تعمل على **Windows وMac معًا**، والنسخة الأخرى [kfupm-sorter-windows](https://github.com/kal429/kfupm-sorter-windows) تعمل على **Windows فقط** (مكتوبة بـ PowerShell) وفيها نسخة iPhone وiPad. استعمل واحدة منهما فقط على جهاز Windows.

- اختر مقرراتك من الدليل الرسمي للجامعة (2,123 مقررًا من 58 قسمًا)، وأضف فلاترك المخصصة، ويُرتَّب ما سوى ذلك حسب نوع الملف.
- **ترتيب تلقائي**: يفحص مجلد التنزيلات كل دقيقة ويبدأ عند تسجيل الدخول، مع أيقونة في شريط المهام (Windows) أو شريط القوائم (Mac).
- واجهة بالعربية والإنجليزية.
- لا يحذف ولا يستبدل أي ملف، ولا يرسل أي بيانات.

**التنزيل:** من [أحدث إصدار](https://github.com/kal429/kfupm-sorter-windows-mac/releases/latest):
`KFUPM-Sorter-macOS-AppleSilicon.dmg` لأجهزة Mac بمعالج M1 أو أحدث، و`KFUPM-Sorter-macOS-Intel.dmg` لأجهزة Mac بمعالج Intel، و`KFUPM-Sorter-Windows-Setup.exe` لنظام Windows.

**على Mac:** افتح ملف .dmg واسحب *KFUPM Sorter Desktop* إلى مجلد التطبيقات. ولأن التطبيق غير موثَّق من Apple بعد، افتحه أول مرة بالنقر بالزر الأيمن ثم **فتح** (وفي macOS 15: الإعدادات ‹ الخصوصية والأمان ‹ فتح على أي حال)، واسمح له باستخدام مجلد التنزيلات عندما يطلب ذلك.

**على Windows:** شغّل ملف التثبيت، ولا يحتاج صلاحيات المسؤول. وإن ظهرت رسالة «Windows protected your PC» فاضغط **More info** ثم **Run anyway**.

</div>

---

<sub>An independent student project. It is not affiliated with or endorsed by King Fahd University of Petroleum and Minerals. Course data comes from the university's public bulletin. MIT License.</sub>
