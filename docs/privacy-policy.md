# Privacy Policy — Termina Launcher

_Last updated: 19 June 2026_

Termina Launcher ("the app") is an open-source, terminal-style home-screen
launcher for Android. The app is designed to run entirely on your device.

**The app does not collect, store on remote servers, transmit, or share any
personal or user data.** The app does not contain advertising or analytics, and
it does not have the `INTERNET` permission, so it is technically unable to send
any information off your device.

## Data the app accesses (locally, on your device)

To provide its features, the app reads the following data **only on your device,
only while the app is running, and never sends it anywhere**:

- **Contacts** (`READ_CONTACTS`): to let you search and call your contacts from
  the `/contacts` screen. Contact data is read on demand and displayed locally;
  it is never copied, uploaded, or shared.
- **App usage statistics** (`PACKAGE_USAGE_STATS`): to show your screen time for
  today via the `/usage` command. These statistics are read from the Android
  system and shown locally; they are never transmitted.
- **List of installed apps**: to display and launch your apps. This is read from
  the system and used only to show the app list.
- **Network state** (`ACCESS_NETWORK_STATE`): to display the local IP address of
  the active network via the `/ip` command. No network traffic is generated.
- **Files** (full-file-access edition distributed on F-Droid only): the optional
  `sudo` mode lets the built-in terminal browse your files. Files are read or
  written locally at your explicit request and are never transmitted or shared.
  The Google Play edition does not include this permission and is confined to the
  app's own folder.

All of the above stays on your device. The app keeps a small amount of local
configuration (theme, settings, command history, a file index) in its private
app storage; this never leaves the device and is removed when you uninstall the
app.

## Permissions summary

| Permission | Why | Data leaves device? |
|---|---|---|
| `READ_CONTACTS` | search & call contacts | No |
| `PACKAGE_USAGE_STATS` | screen-time (`/usage`) | No |
| `ACCESS_NETWORK_STATE` | show local IP (`/ip`) | No |
| `REQUEST_DELETE_PACKAGES` | uninstall apps you choose | No |
| `MANAGE_EXTERNAL_STORAGE` (F-Droid edition only) | terminal file access (`sudo`) | No |

## Children's privacy

The app does not knowingly collect any data from anyone, including children.

## Changes to this policy

If this policy changes, the updated version will be published at this same
address with a new "Last updated" date.

## Contact

For any question about this policy, open an issue at
<https://github.com/chemickypes/termina-launcher/issues> or contact the developer
at the email shown on the Google Play store listing.

---

# Informativa sulla privacy — Termina Launcher (Italiano)

_Ultimo aggiornamento: 19 giugno 2026_

Termina Launcher ("l'app") è un launcher open source per la schermata home di
Android, in stile terminale, progettato per funzionare interamente sul tuo
dispositivo.

**L'app non raccoglie, non memorizza su server remoti, non trasmette e non
condivide alcun dato personale o dell'utente.** Non contiene pubblicità né
strumenti di analisi e non dispone del permesso `INTERNET`, quindi è
tecnicamente impossibilitata a inviare informazioni fuori dal dispositivo.

## Dati a cui l'app accede (localmente, sul dispositivo)

Per offrire le sue funzioni l'app legge i seguenti dati **solo sul dispositivo,
solo mentre è in esecuzione, senza inviarli da nessuna parte**:

- **Contatti** (`READ_CONTACTS`): per cercare e chiamare i contatti dalla
  schermata `/contacts`. Letti su richiesta e mostrati localmente, mai copiati,
  caricati o condivisi.
- **Statistiche di utilizzo** (`PACKAGE_USAGE_STATS`): per mostrare il tempo di
  utilizzo di oggi con il comando `/usage`. Lette dal sistema e mostrate
  localmente, mai trasmesse.
- **Elenco delle app installate**: per mostrare e avviare le app.
- **Stato della rete** (`ACCESS_NETWORK_STATE`): per mostrare l'indirizzo IP
  locale con `/ip`. Non viene generato traffico di rete.
- **File** (solo edizione con accesso completo distribuita su F-Droid): la
  modalità opzionale `sudo` consente al terminale di sfogliare i file, letti o
  scritti localmente solo su tua esplicita richiesta. L'edizione Google Play non
  include questo permesso ed è confinata alla cartella dell'app.

Tutti questi dati restano sul dispositivo. L'app conserva una piccola
configurazione locale (tema, impostazioni, cronologia comandi, indice dei file)
nella propria area privata; non lascia mai il dispositivo e viene rimossa
disinstallando l'app.

## Privacy dei minori

L'app non raccoglie consapevolmente alcun dato da nessuno, minori inclusi.

## Modifiche

Eventuali modifiche saranno pubblicate a questo stesso indirizzo con una nuova
data di "Ultimo aggiornamento".

## Contatti

Per domande apri una issue su
<https://github.com/chemickypes/termina-launcher/issues>.
