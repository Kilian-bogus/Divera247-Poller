# Divera247 Poller

## Build

Unter Windows kannst du die JAR-Datei mit dieser Datei erstellen:

```bash
build.bat
```

Danach liegt die Datei `divera247-poller.jar` im selben Ordner.

## Starten

Lege zuerst eine Datei mit deinen Access Keys an, zum Beispiel:

```text
C:\Pfad\Zur\access-keys.txt
```

Starte das Programm dann so:

```bash
java -jar divera247-poller.jar -a PFAD/ZUR/access-keys.txt
```

Beispiel unter Windows:

```bash
java -jar divera247-poller.jar -a C:\Pfad\Zur\access-keys.txt
```

Wichtig: Ersetze `PFAD/ZUR/access-keys.txt` durch den echten Pfad zu deiner Datei.

Bei jedem erfolgreich gefundenen Einsatz wird ein Popup-Fenster angezeigt.
Die Alarmtöne werden direkt im Java-Code erzeugt; MP3-Dateien werden nicht benötigt.

## Config

Beim Start wird automatisch eine `config.properties` erstellt, falls sie fehlt.

Beispiele:

```properties
language=de
theme=dark
```

`language` kann `de` oder `en` sein.  
`theme` kann `dark` oder `light` sein.
