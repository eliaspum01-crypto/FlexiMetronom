# FlexiMetronom – Windows-Version

Windows-Desktop-Prototyp für Windows 10/11 mit dunklem Studio-Look.

## Funktionen
- Taktart-Zähler 1–64 und Nenner 1, 2, 4, 8, 16, 32
- Tempo 20–300 BPM
- Zähleinheit 1/4, 1/8, 1/16, 1/32
- Eigene Akzente, z. B. `1,4,7`
- Gruppierungseingabe, z. B. `3+3+4`
- Klangprofile Click, Woodblock und Bell
- Presets speichern und laden

## Starten
1. Python 3 für Windows installieren: https://www.python.org/downloads/windows/
2. Bei der Installation „Add Python to PATH“ aktivieren.
3. Projekt entpacken und `start_windows.bat` doppelklicken.

Es sind keine zusätzlichen Python-Pakete für den Start erforderlich.

## Eine Windows-EXE bauen
Im Projektordner eine Eingabeaufforderung öffnen und ausführen:

```bat
python -m pip install pyinstaller
python -m PyInstaller --noconfirm --onefile --windowed --name FlexiMetronom main.py
```

Die EXE wird unter `dist\FlexiMetronom.exe` erstellt.

## Hinweis
Dies ist ein Prototyp. Windows-Thread-Timing kann je nach Systemlast schwanken. Gruppierungen werden derzeit als Orientierung gespeichert/eingegeben; die Taktdauer folgt Zähler und Zähleinheit.


## Windows-EXE über GitHub Actions bauen

Die Workflow-Datei liegt unter `.github/workflows/build-windows.yml`. Lade den Projektinhalt mit dieser Ordnerstruktur in dein GitHub-Repository. Öffne danach **Actions**, wähle **Build FlexiMetronom Windows EXE** und starte bei Bedarf **Run workflow**. Nach erfolgreichem Lauf findest du unter dem Lauf das Artefakt `FlexiMetronom-Windows-EXE` zum Herunterladen. Die EXE wird auf GitHub gebaut; sie ist nicht bereits in diesem ZIP enthalten.
