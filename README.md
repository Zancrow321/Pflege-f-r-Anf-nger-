# Pflege für Anfänger

Android-App zum Lernen für die **kardiologische Telemetrie-Station**: kurze Lektionen im Duolingo-Stil
(Lernpfad, XP, Tagesserie, Herzen, Wiederholung von Fehlern, Einstiegstest) und ein durchsuchbares
**Nachschlagewerk** mit Artikeln und Fachbegriffen, die in **Wortbausteine** zerlegt sind (z.B. *Hypo | Thermie*).

> **Hinweis:** Die Inhalte stützen sich auf Leitlinien und Fachliteratur (siehe `content/sources.json`),
> sind aber **nicht fachlich geprüft**. Die App ersetzt keine Einarbeitung, keine Hausstandards und
> keine ärztliche Anordnung. Das steht auch so in der App.

## Aufbau

| Ordner | Inhalt |
|---|---|
| `content/` | Alle Lerninhalte als JSON: `units.json` (Einheiten → Lektionen → Fragen), `articles.json`, `terms.json` (Wortbausteine), `sources.json`, `placement.json` (Einstiegstest) |
| `core/` | Reine Kotlin-Logik ohne Android: Datenmodell, Lernrunde, XP/Serie/Herzen, Suche, EKG-Streifen-Generator, Tests |
| `app/` | Android-App mit Jetpack Compose (Material 3). Liest `content/` als Assets, speichert den Fortschritt lokal (DataStore) |

Offline zuerst: Es gibt kein Login und keinen Server. Das Datenmodell ist so geschnitten, dass Inhalte
und Fortschritt später nach Supabase wandern können.

## Wiki-Aufbau

Artikel haben eine Art (`kind`): Wissen, Diagnose, Prozedur, Therapie oder Medikament. Diagnosen, Prozeduren,
Therapien und Medikamente folgen einer festen Gliederung (siehe `ArticleTemplate` in `core/.../model/Content.kt`),
z.B. Diagnose: Kurz erklärt · Ursachen und Risikofaktoren · Symptome · Diagnostik · Therapie · Pflege und Beobachtung · Warnzeichen.
Artikel verweisen über `relatedArticleIds` aufeinander („Siehe auch“), Lektionen über `articleIds` auf Artikel
(im Artikel als „Dazu lernen“ sichtbar). Die Tests prüfen Gliederung und Verweise.

## Inhalte (Stand 13.11.2026)

- 19 Einheiten, 80 Lektionen, 625 Fragen (davon 90 Fallfragen, 50 mit Schaubild); 78 Nachschlage-Artikel, 80 Fachbegriffe mit Wortbausteinen
- Grundlagen: Fachsprache (Wortbausteine), **Lage- und Richtungsbegriffe** (dexter/sinister, kranial/kaudal,
  ventral/dorsal, medial/lateral, proximal/distal …), Herz & Kreislauf
- Station: EKG & Monitor, Rhythmusstörungen, Telemetrie-Alltag, Notfälle, Krankheitsbilder 1–8, Medikamente, Prozeduren,
  **Fallbeispiele** (Patient mit aktuellen Beschwerden: Diagnose erkennen oder richtige Maßnahme wählen)
- Fragetypen: Einfachauswahl, Richtig/Falsch, Zuordnen, Wortbausteine, Fallfrage (`case`: Patient, Situation, Befunde, ggf. Monitorbild); optional mit EKG-Streifen oder Schaubild

## Schaubilder

Alle Grafiken sind selbst gezeichnete Vektorschemata (keine fremden Bilder). Sie entstehen im Code
(`core/.../figure/Figures.kt`) und werden in der App mit Compose-Canvas gezeichnet, in hellem und dunklem Design:

| Schaubild (`figure`) | Inhalt | hervorhebbare Teile (`highlight`) |
|---|---|---|
| `heart_anatomy` | Vorhöfe, Kammern, Klappen, große Gefäße, Blutfluss | `rechter_vorhof`, `mitralklappe`, `aorta` … |
| `conduction` | Sinusknoten bis Purkinje-Fasern mit Eigenfrequenzen | `sinusknoten`, `av_knoten`, `his`, `tawara`, `purkinje` |
| `coronaries` | RCA, Hauptstamm, RIVA/LAD, RCX mit Versorgungsgebieten | `rca`, `hauptstamm`, `lad`, `rcx` |
| `ecg_waves` | P, QRS, T, PQ-, ST- und QT-Strecke auf EKG-Papier | `p`, `qrs`, `t`, `pq`, `st`, `qt` |
| `electrodes` | 5-Kanal-Elektroden nach IEC (Ampel-Regel) | `rot`, `gelb`, `gruen`, `schwarz`, `weiss` |
| `body_front`, `body_side`, `limb` | Lage- und Richtungsbegriffe am Körper | `dexter`, `sinister`, `kranial`, `ventral`, `proximal` … |
| `positioning` | Flachlage, Oberkörperhochlage, Herzbettlage | `flach`, `oberkoerper`, `herzbett` |

In den JSON-Inhalten: `"illustration": {"figure": "conduction", "highlight": "av_knoten"}` an Lernkarte, Frage oder
Begriff, bzw. `"illustrations": [...]` am Artikel. Mit `"labels": false` werden die Beschriftungen ausgeblendet
und der gesuchte Teil mit „?“ markiert – das Bild wird zur Frage. Auf Lernkarten und im Wiki lassen sich die Teile
per Chip antippen und hervorheben. `FigureTest` prüft alle Varianten und schreibt Vorschaubilder (SVG) nach
`core/build/figures/`.

Inhalte bearbeiten: JSON in `content/` ändern und `./gradlew -PcoreOnly :core:test` laufen lassen.
Die Tests prüfen u.a., dass jede Lektion, jeder Artikel und jeder Begriff gültige Quellen hat,
dass alle Verweise auflösbar sind und dass die Wortbausteine zusammen den Begriff ergeben.

## Bauen

Voraussetzung: Android Studio (oder JDK 17 + Android SDK 35).

```bash
./gradlew :core:test          # Logik- und Inhaltstests
./gradlew :app:assembleDebug  # APK unter app/build/outputs/apk/debug/
```

Ohne Android-SDK lässt sich nur der Kern bauen: `./gradlew -PcoreOnly :core:test`.

Auf GitHub baut der Workflow `.github/workflows/android.yml` bei jedem Push die Debug-APK und legt sie als
Artefakt ab (Actions → letzter Lauf → *pflege-fuer-anfaenger-debug*).
