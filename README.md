# FlexiMetronom – Musiker-Edition (Android-Prototyp)

Ein Kotlin-/Jetpack-Compose-Prototyp mit professionellem Musiker-Layout für ein flexibel einstellbares Metronom.

## Enthalten
- Frei einstellbarer Zähler (1–64) und Nenner (1, 2, 4, 8, 16, 32)
- Tempo 20–300 BPM
- Wählbare Schlag-Einheit (Viertel, Achtel, Sechzehntel, Zweiunddreißigstel)
- Akzent-Schlagnummern
- Rhythmusgruppen wie `3+3+3+4`
- Drei Klangoptionen
- Einstellungen werden lokal gespeichert
- Foreground-Service-Prototyp für Wiedergabe bei ausgeschaltetem Bildschirm
- Benachrichtigung mit Stop-Aktion
- GitHub Actions-Workflow zum automatischen Erzeugen einer Debug-APK

## APK ohne Android Studio erzeugen (über GitHub Actions)
Diese Umgebung enthält kein Android-SDK und keine Gradle-Installation, daher konnte hier keine APK gebaut oder getestet werden. Du kannst den vorbereiteten Workflow verwenden:

1. Melde dich bei GitHub an und erstelle ein neues, leeres Repository.
2. Lade den Inhalt dieses Projektordners in das Repository hoch (nicht nur die ZIP-Datei selbst).
3. Öffne im Repository den Reiter **Actions** und aktiviere Actions, falls GitHub danach fragt.
4. Starte **Build FlexiMetronom APK** über **Run workflow** oder warte, bis ein Push-Build fertig ist.
5. Öffne den abgeschlossenen Workflow-Lauf und lade unter **Artifacts** `FlexiMetronom-debug-apk` herunter.
6. Entpacke das Artifact. Darin liegt `app-debug.apk`; übertrage sie auf dein Android-Handy und öffne sie zum Installieren. Falls Android nachfragt, erlaube die Installation aus dieser Quelle.

## Lokal mit Android Studio bauen
1. Android Studio öffnen und den entpackten Ordner `FlexiMetronom` wählen.
2. Gradle-Synchronisierung abwarten.
3. Über `Build > Build APK(s)` eine APK erzeugen.

## Wichtige Hinweise
Dies ist Version 0.1.0 und noch kein bühnentauglich getestetes Metronom. Die Tonausgabe nutzt in diesem ersten Prototyp Androids `ToneGenerator` und eine Coroutine-Schleife. Hintergrunddienst und Stopp-Aktion sind ein Prototyp. Vor produktivem Einsatz müssen Audio-Timing, Android-13+-Benachrichtigungsberechtigung, Geräteverhalten und Unterbrechungsfälle getestet und verbessert werden. Für präzises Timing ist eine dedizierte Audio-API/ein Audio-Thread erforderlich.
