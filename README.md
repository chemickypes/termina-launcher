# Termina Launcher

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

An Android home-screen launcher styled as a **terminal**. Type commands instead of
tapping icons: launch apps by name, browse a TUI app/contact list, run unix-like file
commands, search files, edit a `.terminarc` config, and more.

Built with **Jetpack Compose + Material 3**, Kotlin, `minSdk 24`.

## Features

- **Terminal home screen** — a prompt (`termina:~$`) with command history and a
  scrollable output buffer.
- **Launcher commands** (`/`-prefixed): `/launch`, `/apps`, `/contacts`, `/settings`,
  `/battery`, `/info`, `/ip`, `/ram`, `/storage`, `/date`, `/time`, `/uptime`, `/lang`,
  `/search` (web), `/call`, `/torch`, `/uninstall`, `/theme`, `/clear`, `/reload`,
  `/config`, `/handbook`.
- **Launch apps by name** — any free text starts the matching app (e.g. `chrome`).
- **Unix-like file system** — `ls`, `cd`, `pwd`, `cat`, `head`, `tail`, `mkdir`,
  `touch`, `rm`, `cp`, `mv`, `nano`, `du`, `tree`, `find`, plus `sudo` to unlock full
  storage access (`MANAGE_EXTERNAL_STORAGE`).
- **File search** — `find <name>` builds a background index with a live-filtered TUI.
- **`.terminarc` config** — bash-style aliases (`alias g=chrome`). Edit it any time with
  `/config` (also reachable from Settings).
- **Themes & font scale** — green / amber / cyan / white palettes, persisted.
- **Internationalization** — English (base) and Italian, selected automatically.
- **Async loading** — apps/contacts/searches load off the main thread with a
  terminal-style spinner, so the UI never freezes.
- **Recovery gesture** — hold a finger still in the center of the screen for ~5s to fully
  restart the app (a safety net if the UI/keyboard ever gets stuck).

## Build & run

```bash
./gradlew :app:assembleDebug
# install on a connected device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Set Termina as your home app from `/settings` → *default launcher* (or Android's launcher
settings).

## Project layout

```
app/src/main/java/com/example/terminalauncher/
├─ MainActivity.kt              # Compose host + recovery gesture wiring
├─ terminal/                    # TerminalViewModel, command models
├─ system/                      # apps, contacts, file system, file index, device info
├─ ui/                          # terminal, apps, contacts, settings, editor, search… screens
└─ theme/                       # palettes, typography
```

The in-app user manual lives in `app/src/main/assets/handbook.md` (read it with
`/handbook`).

## License

Copyright (C) 2026 Angelo Moroni

This program is free software: you can redistribute it and/or modify it under the terms
of the **GNU General Public License v3.0** as published by the Free Software Foundation.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the [LICENSE](LICENSE) file for the full text.
