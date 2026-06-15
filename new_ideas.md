# Termina Launcher — idee per il futuro

Raccolta di possibili sviluppi, emersi durante lo sviluppo. Non ancora implementati.

## Terminale / shell
- **Cronologia comandi navigabile** con le frecce ↑/↓ (la lista `history` nel
  `TerminalViewModel` è già popolata, manca solo l'aggancio nella UI del campo input).
- **`echo` e redirezione** semplice (`echo testo > file.txt`, `>>` per append).
- **`grep <pattern> <file>`**: cercare *dentro* i file, non solo per nome.
- **Pipe basilari** (`cat file | grep x`) — più ambizioso.
- **Autocompletamento** dei path con Tab (oltre ai comandi `/` già suggeriti).

## Ricerca file
- **Profondità/ordinamento estesi**: `find` con filtri per tipo (`-d` solo cartelle,
  `-f` solo file) o per estensione.
- **Aggiornamento incrementale dell'indice** invece del rebuild completo (watch su
  cartelle modificate) — più complesso ma più efficiente delle aggiunte profonde.

## Alias / configurazione
- Alias con **argomenti posizionali** (es. `$1`) o funzioni, oltre alla sostituzione
  testuale attuale.

## Esplorazione file
- **`du` con profondità** (`du -d <n>`) per il breakdown annidato.
- **Azioni extra** sui risultati: rinomina, elimina, copia-percorso (oltre a
  apri/vai/condividi già presenti).

## Sistema / launcher
- **Widget orologio ASCII** in cima al terminale.
- **`top`**: app che consumano più batteria/memoria.
- **Notifiche** in stile riga di terminale.
- **Gesture**: swipe per aprire app preferite.

## Idee già realizzate (per memoria)
- ✅ Comandi unix file system (ls, cd, cat, nano, rm, cp, mv, du, tree…)
- ✅ `.terminarc` con alias + reload automatico + `source`/`/reload`
- ✅ Espansione alias multi-pass con guardia anti-loop
- ✅ Ricerca file `find` con indice in cache, auto-refresh e cooldown del prewarm
- ✅ Apertura file col tap (FileProvider) e menu azioni col tap lungo
- ✅ `/time`, temi, i18n IT/EN
