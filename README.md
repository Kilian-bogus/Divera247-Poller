# Divera247 Poller
![Logo](img/divera247_long.png)

Desktop notification tool for [Divera 24/7](https://app.divera247.com/login.html) alarms — monitor one or more organizations via the Divera Web API and receive instant notifications for new incidents.

Current version: 1.0.0

Support: If you encounter issues, include your console output, configuration file, and Java version when reporting bugs.

---

# Deutsch

## API-Schlüssel erstellen

Der Poller benötigt mindestens einen [Divera 24/7](https://app.divera247.com/login.html) Web-API-Schlüssel, um Einsätze abrufen zu können.

### API-Schlüssel anlegen

1. Melde dich bei [Divera 24/7](https://app.divera247.com/login.html) an.
2. Wähle die gewünschte Organisation aus.
3. Öffne **Verwaltung**.
4. Navigiere zu **Schnittstellen**.
5. Wechsle auf den Reiter **API**.
6. Erstelle unter **Alarmübertragung per Web-API/Schnittstelle** einen neuen API-Schlüssel.
7. Kopiere den erzeugten Schlüssel.
8. Speichere den Schlüssel in deiner Access-Key-Datei.

Beispiel:

```text
1234567890abcdef1234567890abcdef
```

Mehrere Schlüssel können zeilenweise eingetragen werden:

```text
API_KEY_1
API_KEY_2
API_KEY_3
```

## Build

Unter Windows kann die JAR-Datei mit folgendem Skript erstellt werden:

```bash
build.bat
```

Ausgabe:

```text
divera247-poller.jar
```

## Installation & Start

Erstelle zunächst eine Datei für deine API-Schlüssel:

```text
C:\Pfad\Zur\access-keys.txt
```

Starte den Poller anschließend mit:

```bash
java -jar divera247-poller.jar -a PFAD/ZUR/access-keys.txt
```

Beispiel:

```bash
java -jar divera247-poller.jar -a C:\Pfad\Zur\access-keys.txt
```

Wichtig: Ersetze den Pfad durch den tatsächlichen Speicherort deiner Datei.

## Konfiguration

Beim ersten Start wird automatisch eine `config.properties` erzeugt.

Beispiel:

```properties
language=de
theme=dark
```

### Sprache

| Wert | Beschreibung |
| ---- | ------------ |
| de   | Deutsch      |
| en   | Englisch     |

### Design

| Wert  | Beschreibung   |
| ----- | -------------- |
| dark  | Dunkles Design |
| light | Helles Design  |

## Benachrichtigungen

Bei jedem neu erkannten Einsatz wird automatisch ein Desktop-Popup angezeigt.

Alarmtöne werden direkt durch das Programm erzeugt. Es sind keine zusätzlichen Audiodateien erforderlich.

---

# English

## Creating an API Key

The poller requires at least one [Divera 24/7](https://app.divera247.com/login.html) Web API key to retrieve incident data.

### Generate an API Key

1. Log in to [Divera 24/7](https://app.divera247.com/login.html).
2. Select the desired organization.
3. Open **Administration**.
4. Navigate to **Interfaces**.
5. Switch to the **API** tab.
6. Create a new API key under **Alarm Transmission via Web API/Interface**.
7. Copy the generated key.
8. Save the key inside your access key file.

Example:

```text
1234567890abcdef1234567890abcdef
```

Multiple keys can be added, one per line:

```text
API_KEY_1
API_KEY_2
API_KEY_3
```

## Build

On Windows, build the JAR file using:

```bash
build.bat
```

Output:

```text
divera247-poller.jar
```

## Installation & Startup

First, create a file containing your API keys:

```text
C:\Path\To\access-keys.txt
```

Then start the poller:

```bash
java -jar divera247-poller.jar -a PATH/TO/access-keys.txt
```

Example:

```bash
java -jar divera247-poller.jar -a C:\Path\To\access-keys.txt
```

Make sure to replace the example path with the actual location of your file.

## Configuration

A default `config.properties` file is generated automatically on first startup.

Example:

```properties
language=en
theme=dark
```

### Language

| Value | Description |
| ----- | ----------- |
| de    | German      |
| en    | English     |

### Theme

| Value | Description |
| ----- | ----------- |
| dark  | Dark theme  |
| light | Light theme |

## Notifications

Whenever a new incident is detected, the application displays a desktop notification.

Alert sounds are generated directly by the application. No external audio files are required.
