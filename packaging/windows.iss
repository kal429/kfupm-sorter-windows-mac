; KFUPM Sorter Desktop (Java edition) - Windows installer.
; Wraps the folder made by jpackage (build\image\KFUPM Sorter Desktop) into one setup file.
; Built automatically by .github/workflows/build.yml; see README.md to build it yourself.

#define AppName      "KFUPM Sorter Desktop"
#define AppExe       "KFUPM Sorter Desktop.exe"
#define AppVersion   "1.0.0"
#define AppPublisher "KFUPM Sorter (student project)"
#define AppURL       "https://github.com/kal429/kfupm-sorter-windows-mac"

[Setup]
AppId={{B6E1B0B2-5C47-4E11-9C1B-7A7C2D4A9E31}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}/issues
AppUpdatesURL={#AppURL}/releases/latest
; per-user install: no administrator rights needed
PrivilegesRequired=lowest
DefaultDirName={localappdata}\Programs\{#AppName}
DisableProgramGroupPage=yes
DisableDirPage=yes
OutputDir=..\build\installer
OutputBaseFilename=KFUPM-Sorter-Windows-Setup
SetupIconFile=app.ico
UninstallDisplayIcon={app}\{#AppExe}
UninstallDisplayName={#AppName}
WizardStyle=modern
WizardImageFile=wizard-large.bmp
WizardSmallImageFile=wizard-small.bmp
Compression=lzma2/max
SolidCompression=yes
CloseApplications=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Shortcuts:"; Flags: unchecked

[Files]
Source: "..\build\image\{#AppName}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{userprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"; Comment: "Pick your courses and keep Downloads sorted"
Name: "{userdesktop}\{#AppName}";  Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "Open {#AppName} now"; Flags: postinstall nowait skipifsilent

[UninstallRun]
; stop the background sorter so its files can be removed
Filename: "{sys}\taskkill.exe"; Parameters: "/F /IM ""{#AppExe}"""; Flags: runhidden; RunOnceId: "StopSorter"

[UninstallDelete]
; the sign-in launcher, settings and log. Your sorted files are never touched.
Type: files; Name: "{userstartup}\{#AppName}.vbs"
Type: filesandordirs; Name: "{userappdata}\{#AppName}"
