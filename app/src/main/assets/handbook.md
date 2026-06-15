# Termina Launcher — User Manual

Termina Launcher is a text-terminal-style Android launcher. Everything is done by
typing at the `termina:~$` prompt, or by tapping entries in the TUI screens.
This manual lists **every possible action**.

The interface follows the phone language (English or Italian).

---

## 1. How the prompt works

At the terminal you can type three kinds of input:

1. **Launcher commands** that start with `/` (e.g. `/battery`, `/apps`).
2. **Unix-style commands** without `/` (e.g. `ls`, `cd`, `find`).
3. **Free text**: anything else is interpreted as the *name of an app to
   launch* (e.g. typing `chrome` opens Chrome).

Tip: while you type a command that starts with `/`, suggestions for the available
commands appear below the prompt — tap them to complete.

Type `/help` at any time for the quick list.

---

## 2. Launcher commands (`/`)

| Command | What it does |
|---|---|
| `/help` | Show the guide with every command |
| `/launch <name>` | Launch an app or a section (see below) |
| `/apps` | Open the TUI list of installed apps |
| `/contacts` | Open the TUI address book |
| `/settings` | Open the launcher settings (TUI) |
| `/battery` | Battery status: level, charging, temperature, voltage |
| `/info` | Device info: model, Android, CPU, screen… |
| `/ip` | IP addresses of the network interfaces |
| `/ram` | Memory usage |
| `/storage` | Storage space |
| `/date` | Full date and time + time zone |
| `/time` | Current time only |
| `/uptime` | How long the device has been on |
| `/lang` | System language + opens the language settings |
| `/search <text>` | Search the **web** for the text |
| `/call <number\|name>` | Dial a number (also by looking up a contact by name) |
| `/torch on\|off` | Turn the flashlight on/off (no argument: toggle) |
| `/uninstall <name>` | Start uninstalling an app |
| `/theme <color>` | Change theme: `green`, `amber`, `cyan`, `white` |
| `/reload` | Reload aliases from `.terminarc` |
| `/clear` | Clear the screen (reprints the banner) |
| `/wifi` | Open the Wi-Fi settings |
| `/bt` or `/bluetooth` | Open the Bluetooth settings |

### `/launch` — special destinations
Besides an app name, `/launch` accepts:
`settings`, `wifi`, `bluetooth`, `lingua`/`language`, `contatti`/`contacts`.
Examples: `/launch wifi`, `/launch settings`, `/launch whatsapp`.

---

## 3. File system commands (unix-style)

Typed without `/`. The prompt shows the current folder (e.g. `termina:~/Download$`).

| Command | What it does |
|---|---|
| `ls [path]` | List files (folders end with `/`) |
| `ll [path]` | Detailed list (permissions, size, date) |
| `la [path]` | Like `ll` but includes hidden files |
| `cd <dir>` | Enter a folder (`cd ~` back home, `cd ..` up) |
| `pwd` | Show the current absolute path |
| `cat <file>` | Show the contents of a text file |
| `head <file> [n]` | First n lines (default 10) |
| `tail <file> [n]` | Last n lines (default 10) |
| `mkdir <dir>` | Create a folder |
| `touch <file>` | Create an empty file (or update its date) |
| `rm <file>` | Delete a file |
| `rm -r <dir>` | Delete a folder and its contents |
| `cp [-r] <src> <dst>` | Copy a file (or folder with `-r`) |
| `mv <src> <dst>` | Move or rename |
| `nano <file>` | Open the text editor (see section 5) |
| `find <name> [--reindex]` | Search files by name (see section 6) |
| `du [-n] [path]` | Disk usage per entry, sorted by size (`-n` = by name) |
| `tree [-L n] [path]` | Folder tree (`-L n` limits the depth, default 2) |
| `sudo` | Unlock access to **all** files (see section 4) |
| `source` | Reload aliases from `.terminarc` (like `/reload`) |
| `alias` | List the defined aliases |
| `alias <name>=<command>` | Define an alias for the session |
| `unalias <name>` | Remove an alias |

Paths accept `~` (home), `..` (parent folder) and absolute paths `/...`.

---

## 4. File access and `sudo`

By default the launcher only sees its own private folder.
The **`sudo`** command opens the system settings to grant access to
*all files*: once granted, the home (`~`) becomes the entire device
storage (`/storage/emulated/0`) and access is active when you return to the app.

---

## 5. Text editor (`nano`)

`nano <file>` opens a full-screen editor, inspired by nano:
- the top bar shows the file (with `*` if there are unsaved changes);
- **`[^O save]`** saves the file;
- **`[^X exit]`** saves (if modified) and exits;
- the *back* key exits without forcing a save.

You can use it to create/edit any text file, including `.terminarc`.

---

## 6. File search (`find`)

`find <name>` opens a TUI screen and searches files/folders by name.

- On the first search an **index** is built (in the background) which is
  saved and reused: later searches are instant.
- The index **updates itself** when it detects changes, and is
  pre-warmed at app startup (with a rate limit).
- Force a rebuild: `find --reindex` (or `find -r`).
- In the `find:` field you can refine the search in real time.
- Results are sorted by relevance (exact name → prefix → contents).

### Actions on results
- **Single tap**: if it is a file it opens with the system app; if it is a folder
  it enters it (`cd`) and returns to the terminal.
- **Long tap**: opens a menu with:
  - **open** — open the file / enter the folder;
  - **go to folder** — `cd` to the location and return to the terminal;
  - **share** — share the file with another app (files only);
  - **cancel**.

---

## 7. TUI screens

Besides the terminal, there are full-screen screens navigated by touch. The
*back* key (or `ESC` in the footer) returns to the terminal.

- **Apps** (`/apps`): app list with a filter; tap to launch.
- **Contacts** (`/contacts`): address book with a filter; tap a contact to call.
  On first open it asks for permission to read contacts.
- **Settings** (`/settings`): theme, font size, and shortcuts to the
  system settings (Wi-Fi, Bluetooth, language, display, default launcher).
- **Editor** (`nano`): see section 5.
- **Search** (`find`): see section 6.
- **Handbook** (`/handbook`): this manual, with a **preview** tab and a
  **source** tab (the raw markdown).

---

## 8. Aliases and `.terminarc`

As in bash/zsh, you can define **aliases** for commands.

- Config file: `~/.terminarc` (created on first launch with examples).
  Edit it with `nano ~/.terminarc` — the exact path is shown in `/help`.
- Syntax: `alias name=command` (lines with `#` are comments).
- Aliases are **reloaded automatically** when you save the file, or with
  `source` / `/reload`.
- Aliases can call other aliases (chained expansion, with loop protection).
- You can also define them on the fly for the session: `alias g=chrome`.

Examples:
```
alias g=chrome
alias ll=ls -l
alias home=cd ~
alias bt=/bluetooth
alias web=/search
```

---

## 9. Customization

- **Themes**: `green` (default), `amber`, `cyan`, `white`. Change with
  `/theme <color>` or from Settings. The choice is remembered.
- **Text size**: small / normal / large, from Settings.
- **Language**: automatically follows the phone language (English/Italian).
- **Default launcher**: from Settings you can set Termina as the phone Home
  screen.

---

## 10. Handy shortcuts

- `chrome` (free text) → launch the app by name.
- `/launch wifi` → Wi-Fi settings on the fly.
- `find <name>` → find a file anywhere and open or share it.
- `nano notes.txt` → create a text note on the fly.
- `du` → discover what takes up space; `tree` → look at the folder structure.
