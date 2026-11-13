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

## Inhalte (Stand 13.11.2026)

- 6 Einheiten, 18 Lektionen, 98 Fragen
  1. Fachsprache: Wortbausteine *(Grundlagen)*
  2. Herz & Kreislauf *(Grundlagen)*
  3. EKG & Monitor
  4. Rhythmusstörungen am Monitor (mit gezeichneten EKG-Streifen)
  5. Telemetrie-Alltag (Alarme, Elektrolyte, nach Herzkatheter)
  6. Notfälle (Brustschmerz, Reanimation)
- 17 Nachschlage-Artikel, 48 Fachbegriffe mit Wortbausteinen, 15 Quellen
- Fragetypen: Einfachauswahl, Richtig/Falsch, Zuordnen, Wortbausteine; optional mit EKG-Streifen

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
