# Termina Launcher — Manuale d'uso

Termina Launcher è un launcher Android in stile terminale di testo. Tutto si fa
digitando nel prompt `termina:~$`, oppure toccando le voci nelle schermate TUI.
Questo manuale elenca **ogni azione possibile**.

L'interfaccia segue la lingua del telefono (Italiano o Inglese).

---

## 1. Come funziona il prompt

Nel terminale puoi digitare tre tipi di input:

1. **Comandi launcher** che iniziano con `/` (es. `/battery`, `/apps`).
2. **Comandi in stile unix** senza `/` (es. `ls`, `cd`, `find`).
3. **Testo libero**: qualsiasi altra cosa viene interpretata come *nome di un'app
   da avviare* (es. scrivere `chrome` apre Chrome).

Suggerimento: mentre digiti un comando che inizia con `/`, sotto al prompt compaiono
i suggerimenti dei comandi disponibili — toccali per completarli.

Digita `/help` in qualsiasi momento per la lista rapida.

---

## 2. Comandi launcher (`/`)

| Comando | Cosa fa |
|---|---|
| `/help` | Mostra la guida con tutti i comandi |
| `/launch <nome>` | Avvia un'app o una sezione (vedi sotto) |
| `/apps` | Apre l'elenco TUI delle app installate |
| `/contacts` | Apre la rubrica TUI |
| `/settings` | Apre le impostazioni del launcher (TUI) |
| `/battery` | Stato batteria: livello, ricarica, temperatura, voltaggio |
| `/info` | Info dispositivo: modello, Android, CPU, schermo… |
| `/ip` | Indirizzi IP delle interfacce di rete |
| `/ram` | Uso della memoria |
| `/storage` | Spazio di archiviazione |
| `/date` | Data e ora complete + fuso orario |
| `/time` | Solo l'ora corrente |
| `/uptime` | Da quanto tempo è acceso il dispositivo |
| `/lang` | Lingua di sistema + apre le impostazioni lingua |
| `/search <testo>` | Ricerca **sul web** del testo |
| `/call <numero\|nome>` | Compone un numero (anche cercando un contatto per nome) |
| `/torch on\|off` | Accende/spegne la torcia (senza argomento: alterna) |
| `/uninstall <nome>` | Avvia la disinstallazione di un'app |
| `/theme <colore>` | Cambia tema: `green`, `amber`, `cyan`, `white` |
| `/reload` | Ricarica gli alias da `.terminarc` |
| `/clear` | Pulisce lo schermo (ristampa il banner) |
| `/wifi` | Apre le impostazioni Wi-Fi |
| `/bt` o `/bluetooth` | Apre le impostazioni Bluetooth |

### `/launch` — destinazioni speciali
Oltre al nome di un'app, `/launch` accetta:
`settings`, `wifi`, `bluetooth`, `lingua`/`language`, `contatti`/`contacts`.
Esempi: `/launch wifi`, `/launch settings`, `/launch whatsapp`.

---

## 3. Comandi file system (stile unix)

Si digitano senza `/`. Il prompt mostra la cartella corrente (es. `termina:~/Download$`).

| Comando | Cosa fa |
|---|---|
| `ls [path]` | Elenca i file (cartelle con `/` finale) |
| `ll [path]` | Elenco dettagliato (permessi, dimensione, data) |
| `la [path]` | Come `ll` ma include i file nascosti |
| `cd <dir>` | Entra in una cartella (`cd ~` torna alla home, `cd ..` sale) |
| `pwd` | Mostra il percorso assoluto corrente |
| `cat <file>` | Mostra il contenuto di un file di testo |
| `head <file> [n]` | Prime n righe (default 10) |
| `tail <file> [n]` | Ultime n righe (default 10) |
| `mkdir <dir>` | Crea una cartella |
| `touch <file>` | Crea un file vuoto (o ne aggiorna la data) |
| `rm <file>` | Cancella un file |
| `rm -r <dir>` | Cancella una cartella e il suo contenuto |
| `cp [-r] <orig> <dest>` | Copia file (o cartella con `-r`) |
| `mv <orig> <dest>` | Sposta o rinomina |
| `nano <file>` | Apre l'editor di testo (vedi sezione 5) |
| `find <nome> [--reindex]` | Cerca file per nome (vedi sezione 6) |
| `du [-n] [path]` | Uso disco per voce, ordinato per dimensione (`-n` = per nome) |
| `tree [-L n] [path]` | Albero delle cartelle (`-L n` limita la profondità, default 2) |
| `sudo` | Sblocca l'accesso a **tutti** i file (vedi sezione 4) |
| `source` | Ricarica gli alias da `.terminarc` (come `/reload`) |
| `alias` | Elenca gli alias definiti |
| `alias <nome>=<comando>` | Definisce un alias per la sessione |
| `unalias <nome>` | Rimuove un alias |

I percorsi accettano `~` (home), `..` (cartella superiore) e percorsi assoluti `/...`.

---

## 4. Accesso ai file e `sudo`

Per impostazione predefinita il launcher vede solo la propria cartella privata.
Il comando **`sudo`** apre le impostazioni di sistema per concedere l'accesso a
*tutti i file*: una volta concesso, la home (`~`) diventa l'intera memoria del
dispositivo (`/storage/emulated/0`) e tornando nell'app l'accesso è attivo.

---

## 5. Editor di testo (`nano`)

`nano <file>` apre un editor a schermo intero, ispirato a nano:
- la barra in alto mostra il file (con `*` se ci sono modifiche non salvate);
- **`[^O salva]`** salva il file;
- **`[^X esci]`** salva (se modificato) ed esce;
- il tasto *indietro* esce senza forzare il salvataggio.

Puoi usarlo per creare/modificare qualsiasi file di testo, incluso `.terminarc`.

---

## 6. Ricerca file (`find`)

`find <nome>` apre una schermata TUI e cerca i file/cartelle per nome.

- Alla prima ricerca viene costruito un **indice** (in background) che viene
  salvato e riusato: le ricerche successive sono istantanee.
- L'indice si **aggiorna da solo** quando rileva cambiamenti, e viene
  pre-riscaldato all'avvio dell'app (con un limite di frequenza).
- Forzare la ricostruzione: `find --reindex` (o `find -r`).
- Nel campo `find:` puoi affinare la ricerca in tempo reale.
- I risultati sono ordinati per pertinenza (nome esatto → prefisso → contenuto).

### Azioni sui risultati
- **Tap singolo**: se è un file lo apre con l'app di sistema; se è una cartella
  ci entra (`cd`) e torna al terminale.
- **Tap lungo**: apre un menu con:
  - **apri** — apre il file / entra nella cartella;
  - **vai alla cartella** — `cd` nella posizione e torna al terminale;
  - **condividi** — condivide il file con un'altra app (solo file);
  - **annulla**.

---

## 7. Schermate TUI

Oltre al terminale, ci sono schermate a tutto schermo navigabili col tocco. Il
tasto *indietro* (o `ESC` nel footer) torna al terminale.

- **App** (`/apps`): elenco app con filtro; tocca per avviare.
- **Contatti** (`/contacts`): rubrica con filtro; tocca un contatto per chiamarlo.
  Alla prima apertura chiede il permesso di leggere i contatti.
- **Impostazioni** (`/settings`): tema, dimensione font, e scorciatoie alle
  impostazioni di sistema (Wi-Fi, Bluetooth, lingua, display, launcher predefinito).
- **Editor** (`nano`): vedi sezione 5.
- **Ricerca** (`find`): vedi sezione 6.

---

## 8. Alias e `.terminarc`

Come in bash/zsh, puoi definire **alias** per i comandi.

- File di configurazione: `~/.terminarc` (creato al primo avvio con esempi).
  Modificalo con `nano ~/.terminarc` — il percorso esatto è mostrato in `/help`.
- Sintassi: `alias nome=comando` (le righe con `#` sono commenti).
- Gli alias vengono **ricaricati automaticamente** quando salvi il file, oppure
  con `source` / `/reload`.
- Gli alias possono richiamare altri alias (espansione a catena, con protezione
  anti-loop).
- Puoi anche definirli al volo per la sessione: `alias g=chrome`.

Esempi:
```
alias g=chrome
alias ll=ls -l
alias home=cd ~
alias bt=/bluetooth
alias web=/search
```

---

## 9. Personalizzazione

- **Temi**: `green` (default), `amber`, `cyan`, `white`. Cambia con
  `/theme <colore>` o da Impostazioni. La scelta viene ricordata.
- **Dimensione testo**: piccola / normale / grande, da Impostazioni.
- **Lingua**: segue automaticamente la lingua del telefono (Italiano/Inglese).
- **Launcher predefinito**: da Impostazioni puoi impostare Termina come schermata
  Home del telefono.

---

## 10. Scorciatoie utili

- `chrome` (testo libero) → avvia l'app per nome.
- `/launch wifi` → impostazioni Wi-Fi al volo.
- `find <nome>` → trova un file ovunque e aprilo o condividilo.
- `nano appunti.txt` → crea al volo una nota di testo.
- `du` → scopri cosa occupa spazio; `tree` → guarda la struttura delle cartelle.
