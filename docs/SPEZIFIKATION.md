# Photo Frame Binding für openHAB – Spezifikation

| | |
|---|---|
| Status | Entwurf v0.1 – zur Prüfung |
| Datum | 2026-10-27 |
| Ziel-Version | openHAB 5.0 und neuer (läuft nicht auf 4.x) |
| Add-on-ID | `photoframe` |
| Paket | `org.openhab.binding.photoframe` |
| Lizenz | EPL-2.0 |
| Repo | `pierrecinema/openhab-binding-photoframe` (privat, für Beta-Tester öffentlich) |

---

## 1. Ziel

Ein Binding, das openHAB zu einem **digitalen Bilderrahmen** macht:

- Bilder kommen aus **einem Ordner** – lokal, in Google Drive oder in Dropbox.
- Das Binding liefert eine **eigene Diashow-Seite** (Vollbild, Übergänge, Ken-Burns-Effekt), die auf jedem Tablet läuft und sich als **Bildschirmschoner** in Kiosk-Apps (Fully Kiosk Browser, WallPanel) eintragen lässt.
- Die Diashow ist aus openHAB **steuerbar** (Pause, weiter, zurück) und es gibt ein **Dashboard-Widget**.

**Grundsatz: Alles im Binding.** Auf dem openHAB-Server muss nichts zusätzlich installiert werden – keine Skripte, kein ImageMagick, keine Cron-Jobs, keine Web-Server. Bildverarbeitung (Java ImageIO), Diashow-Seite (HTML/JS im JAR), Cache und Ordner-Anlage erledigt das Binding selbst. Auf dem Tablet reicht ein Browser; eine Kiosk-App ist nur nötig, wenn die Diashow automatisch als Bildschirmschoner starten soll.

**Grundsatz: Ein Ordner = ein Bilderrahmen.** Der Nutzer legt einen Ordner an, legt Bilder hinein, verbindet ihn – fertig. Neue Bilder erscheinen automatisch, gelöschte verschwinden.

### Nicht-Ziele in 0.1
- Keine Unterordner, kein Aufnahmedatum, keine Sortierung nach Datum.
- Kein Hochladen/Löschen von Bildern über das Binding.
- Kein Google-Login (nur Freigabelink + API-Key, siehe §4.2). Google Photos wird nicht unterstützt (API seit 2025 auf app-eigene Inhalte beschränkt).
- Keine Videos.
- **Kamera-Quelle (später, nicht 0.1):** Schnappschuss/Live-Bild von Kamera-Bindings als Teil der Diashow und als Einblendung bei Ereignissen (z. B. Türklingel → Kamera für 60 s). Die Architektur (§3) lässt Platz dafür.

---

### Was es heute schon gibt
Im Community-Thread [Photoframe – MainUI Widget](https://community.openhab.org/t/photoframe-mainui-widget/147072) (2023) läuft ein Bilderrahmen so: Ein Cron-Skript kopiert Bilder vom NAS in den `static`-Ordner von openHAB, eine Rule schreibt die Bild-Adressen mit Semikolon getrennt in ein String-Item, und ein Widget mit Swiper blättert sie durch. Gemeldete Probleme dort: fummelige Einrichtung (Skript + Rule + Item-Format), keine Bildunterschriften, Uhr/Temperaturen über den Bildern gewünscht, kein Bildschirmschoner.
**Dieses Binding ersetzt die ganze Kette** (kein Skript, keine Rule, kein Item-Format) und greift die Wünsche als Einblendungen auf (§6).

## 2. Anforderungen für offizielle Aufnahme & Marketplace

Gleich wie beim Google Drive Binding:

| Bereich | Umsetzung |
|---|---|
| Struktur | Maven-Modul wie in openhab-addons, Parent `org.openhab.addons.reactor.bundles` **5.0.x** |
| Kompatibilität | Manifest importiert `org.openhab.core.*` im Bereich `[5.0,6)` |
| Metadaten | `addon.xml`, Thing-Typen, i18n Englisch + Deutsch |
| Code | EPL-2.0-Header, `@NonNullByDefault`, Spotless, SAT ohne Fehler |
| Abhängigkeiten | **keine Fremdbibliotheken**: Jetty-HttpClient, Gson, `OAuthFactory`, Java-ImageIO aus dem JDK |
| Tests | JUnit für Link-Parsing, Sortierung, EXIF-Ausrichtung, API-Antworten |
| Releases | GitHub-Release bei jedem Versions-Tag (Workflow wie beim Google Drive Binding). Als Dateien sichtbar im Release: **JAR**, **Widget-YAML(s)**, **Anleitungen** (README.md und deutsche Anleitung); GitHub hängt den Quellcode (zip/tar.gz) automatisch an |
| Commits | DCO-Sign-off, anonyme GitHub-Adresse |

---

## 3. Architektur

```
photoframe:dropbox  (Bridge, optional) – Dropbox-Login, Token-Erneuerung
 └── photoframe:frame (Thing)          – ein Bilderrahmen mit einer Quelle

photoframe:frame    (Thing, ohne Bridge) – für lokalen Ordner, Google-Drive-Link, Dropbox-Link
```

| Teil | Aufgabe |
|---|---|
| `ImageSource` (Schnittstelle) | liefert die Bildliste und lädt ein Bild – je Quelle eine Klasse: `LocalFolderSource`, `GoogleDriveLinkSource`, `DropboxLinkSource`, `DropboxAccountSource` (später `CameraSource`) |
| `ImageCache` | verkleinert Bilder einmal auf die Zielgröße, dreht sie nach EXIF, speichert sie in `userdata/cache/photoframe/<thing>/` |
| `Slideshow` | Server-seitiger Ablauf: aktuelles Bild, Reihenfolge, Takt, Pause – **alle Tablets zeigen dasselbe Bild** |
| `PhotoFrameServlet` | liefert Diashow-Seite, Bilder und einen kleinen Status-Endpunkt |
| Diashow-Seite (HTML/JS/CSS im JAR) | zeigt Bilder im Vollbild mit Übergängen, fragt jede Sekunde den Status ab |

**Warum der Ablauf auf dem Server läuft:** Dann zeigen alle Tablets das gleiche Bild, die Channels (aktuelles Bild, Pause) stimmen immer, und „Weiter“ aus einer Rule wirkt sofort überall.

---

## 4. Quellen

### 4.1 Lokaler Ordner
- **Das Binding legt den Ordner selbst an:** Bleibt der Pfad leer (Standard), erstellt das Binding beim Anlegen des Things den Ordner `userdata/photoframe` (Docker: `/openhab/userdata/photoframe`) – **ohne Unterordner**. Für einen zweiten Rahmen trägt man einen eigenen Pfad ein, z. B. `/openhab/userdata/photoframe-urlaub`. Der Pfad steht in den Thing-Properties (`localFolder`) und in der Statusmeldung, solange der Ordner leer ist: *„Lege Bilder in /openhab/userdata/photoframe“*.
- Alternativ ein eigener Pfad, z. B. eine Netzwerkfreigabe, die im Container eingebunden ist. Existiert er nicht, wird er angelegt (sofern openHAB dort schreiben darf).
- Beim Löschen des Things bleibt der Ordner mit den Bildern erhalten (keine Bilder ungefragt löschen).
- Ordner wird beim Neu-Einlesen (Standard alle 5 min) gelesen; Änderungen erscheinen ohne Neustart.

### 4.2 Google Drive – Freigabelink + API-Key
**Getestet am 2026-10-27:** Ein Freigabelink allein reicht nicht – Google liefert den Ordnerinhalt ohne API-Key nicht aus (die frühere Einbettungsansicht verlangt inzwischen eine Anmeldung). Mit API-Key klappt es über die offizielle Schnittstelle.

- Konfiguration: Freigabelink (`https://drive.google.com/drive/folders/<ID>?usp=sharing`, auch `/u/0/folders/<ID>`, `resourcekey=` und die reine ID) und **API-Key**.
- Der Ordner muss auf **„Jeder mit dem Link“** stehen.
- Liste: `files.list` mit `'<ID>' in parents and mimeType contains 'image/' and trashed = false`.
- Bild: `files/<id>?alt=media&key=…`, danach Verkleinerung im Cache. Geänderte Bilder werden über `md5Checksum` erkannt.
- API-Key: im selben Google-Cloud-Projekt wie beim Google Drive Binding („Anmeldedaten → API-Schlüssel“), auf die Google Drive API beschränkt. Anleitung wie beim Google Drive Binding, Englisch und Deutsch.

### 4.3 Dropbox – Freigabelink (ohne Konto)
**Getestet am 2026-10-27:** Mit `dl=1` am Freigabelink liefert Dropbox den ganzen Ordner als ZIP (Test: 4 Bilder, 1,6 MB) – ohne Konto.

- Konfiguration: Freigabelink `https://www.dropbox.com/scl/fo/…?rlkey=…`.
- Das Binding lädt das ZIP beim Neu-Einlesen (Standard alle 60 min), entpackt nur Bilddateien in den Cache und verkleinert sie.
- Grenzen: Gedacht für Bilderrahmen-Ordner mit einigen hundert Bildern. Bei sehr großen Ordnern ist der Login (§4.4) besser. Das ZIP wird gestreamt, nicht im Speicher gehalten.

### 4.4 Dropbox – mit Login (sicherer, nicht öffentlich)
Bridge `photoframe:dropbox`.

**Einrichtung (einmal, ca. 3 min):**
1. Auf `https://www.dropbox.com/developers/apps` eine App anlegen: **Scoped access**, Zugriff **„App folder“**, Name z. B. `openHAB Photo Frame`.
2. Unter *Permissions* `files.metadata.read` und `files.content.read` aktivieren.
3. Den **App key** in die Bridge eintragen (kein Secret nötig).

**Login (ohne Weiterleitung, PKCE):**
1. Die Bridge zeigt im Status einen Link: *„Öffne https://www.dropbox.com/oauth2/authorize?… und füge den angezeigten Code ein“*.
2. Der Nutzer erlaubt den Zugriff; Dropbox zeigt einen **Code**.
3. Code in die Bridge-Einstellung `authCode` einfügen → Binding tauscht ihn gegen ein dauerhaftes Token, das openHAB verschlüsselt speichert und erneuert. Das Feld wird danach geleert.

**„App folder“ ist bewusst gewählt:** Dropbox legt dafür den Ordner `Apps/openHAB Photo Frame/` an, und das Binding sieht **nur diesen Ordner** – nichts sonst in deiner Dropbox. Jeder Bilderrahmen nutzt einen Unterordner darin (Thing-Einstellung `folder`, z. B. `familie`), oder den App-Ordner selbst.

- Liste: `files/list_folder`, Änderungen über den Cursor (`list_folder/continue`) – es werden nur neue/geänderte Bilder geladen.
- Bild: `files/get_thumbnail_v2` in passender Größe (Dropbox verkleinert und dreht selbst), sonst `files/download`.

---

## 5. Bilder: Formate, Größe, Ausrichtung

| Thema | Umsetzung |
|---|---|
| Formate 0.1 | JPEG, PNG, GIF (erstes Bild), BMP. **HEIC/WebP** werden übersprungen (vom JDK nicht unterstützt) und im Log einmal genannt. Bei Dropbox mit Login liefert Dropbox HEIC als JPEG-Vorschau – dort funktioniert es. |
| Größe | Verkleinerung auf `maxSize` (Standard 1920 px längste Kante), JPEG-Qualität 85 %. Spart Speicher und lädt schnell aufs Tablet. |
| Ausrichtung | EXIF-Ausrichtung wird ausgelesen und angewendet, damit Handyfotos nicht quer liegen (eigener kleiner EXIF-Leser, keine Fremdbibliothek). |
| Cache | `userdata/cache/photoframe/<thing>/`, je Bild einmal berechnet. Größenlimit (Standard 500 MB), älteste Einträge fliegen zuerst. Entfernte Bilder werden aus dem Cache gelöscht. |

### Falsches Seitenverhältnis (z. B. Hochformat auf Quer-Tablet)
| Modus | Wirkung |
|---|---|
| `BLUR` (Standard) | Ganzes Bild sichtbar, Ränder mit **unscharfer, vergrößerter Version desselben Bildes** gefüllt |
| `FIT` | Ganzes Bild, schwarze Ränder |
| `FILL` | Bildschirm voll, Ränder abgeschnitten |
| `AUTO` | `FILL`, wenn das Seitenverhältnis nah dran ist (< 15 % Abweichung), sonst `BLUR` |

---

## 6. Diashow-Seite

- Adresse: `http://<openhab>:<port>/photoframe/<thingId>/?token=<token>` – steht fertig im Channel `pageUrl`.
- **Zugriffsschutz:** openHAB-Servlets sind im Heimnetz ohne Anmeldung erreichbar. Damit nicht jeder im Netz die Bilder sieht, erzeugt das Binding je Rahmen einen zufälligen **Token** (Thing-Property, neu erzeugbar über Aktion). Ohne gültigen Token: HTTP 403.
- Technik: einzelne HTML-Seite mit CSS/JS, ohne externe Bibliotheken oder Internetzugriff. Fragt jede Sekunde `…/state` ab (aktuelles und nächstes Bild, Pause, Einstellungen), lädt das nächste Bild vorab.
- **Vollbild** per Tippen (Fullscreen-API), **Bildschirm bleibt an** (Wake-Lock-API), Mauszeiger wird ausgeblendet.
- Optional: Tippen öffnet eine Adresse (z. B. openHAB-Startseite) – nützlich, wenn die Seite ohne Kiosk-App läuft.
- Funktioniert in **Fully Kiosk Browser** (Bildschirmschoner-URL) und **WallPanel**; Anleitung für beide in der Doku.

### Einblendungen (aus Wünschen in der Community)
| Option | Wirkung |
|---|---|
| `clock` | Uhrzeit und Datum unten im Bild (`NONE` / `TIME` / `TIME_DATE`) |
| `overlayItems` | bis zu 4 Items, deren Wert unten im Bild steht, z. B. Außen- und Innentemperatur (`Aussen_Temp, Wohnzimmer_Temp`). Label und formatierter Wert kommen aus openHAB. |
| `caption` | Bildunterschrift: `NONE` (Standard) oder `FILENAME` (Dateiname ohne Endung und ohne führende Nummer, z. B. `012_Strand Mallorca.jpg` → „Strand Mallorca“) |

Die Einblendungen liegen auf einem halbtransparenten Verlauf, damit sie auf hellen und dunklen Bildern lesbar sind.

### Nachtmodus
Damit das Tablet nachts nicht das Wohnzimmer erhellt.

| Option | Werte |
|---|---|
| `nightMode` | `OFF` (Standard), `BLACK` (schwarzer Bildschirm), `DIM` (Bilder stark abgedunkelt), `CLOCK` (schwarz mit gedimmter Uhr) |
| `nightStart` / `nightEnd` | Uhrzeit, z. B. `22:00` / `06:30` (über Mitternacht möglich) |
| Channel `night` | `Switch`: zeigt an, ob gerade Nacht ist; ein Befehl `ON`/`OFF` erzwingt den Nachtmodus bis zur nächsten Zeitgrenze – z. B. per Rule „Licht im Wohnzimmer aus → Nacht“ |

Im Nachtmodus wechseln keine Bilder und es wird nichts geladen. Tippen auf den Bildschirm zeigt für 30 s wieder Bilder. Hinweis: Den Bildschirm wirklich abschalten kann nur die Kiosk-App (Fully Kiosk: „Screen off“ zu Zeiten) – das steht in der Anleitung.

### Übergänge
| Option | Werte |
|---|---|
| Übergang | `FADE` (Standard), `SLIDE`, `WIPE`, `ZOOM`, `RANDOM`, `NONE` |
| Übergangsdauer | 0,5–5 s (Standard 1,5 s) |
| Ken-Burns | an/aus (Standard an): langsames Zoomen/Schwenken während der Anzeige |

---

## 7. Thing `photoframe:frame`

### 7.1 Konfiguration
| Parameter | Typ | Standard | Beschreibung |
|---|---|---|---|
| `sourceType` | Auswahl `LOCAL` / `GOOGLE_DRIVE` / `DROPBOX_LINK` / `DROPBOX` | – | Quelle (`DROPBOX` braucht die Dropbox-Bridge) |
| `localFolder` | Text | leer | Pfad für `LOCAL`; leer = Binding legt `userdata/photoframe` an (ohne Unterordner) |
| `link` | Text | – | Freigabelink für `GOOGLE_DRIVE` / `DROPBOX_LINK` |
| `apiKey` | Text (password) | – | Google-API-Key für `GOOGLE_DRIVE` |
| `folder` | Text | leer | Unterordner im Dropbox-App-Ordner für `DROPBOX` (leer = App-Ordner selbst) |
| `interval` | Zahl (s) | 30 | Anzeigedauer je Bild (5–3600) |
| `order` | Auswahl `RANDOM` / `NAME` | `RANDOM` | Reihenfolge; `RANDOM` zeigt alle einmal, bevor sich etwas wiederholt |
| `transition` | Auswahl (§6) | `FADE` | Übergang |
| `transitionSeconds` | Dezimal | 1.5 | Übergangsdauer |
| `kenBurns` | Schalter | an | Ken-Burns-Effekt |
| `fitMode` | Auswahl (§5) | `BLUR` | Umgang mit falschem Seitenverhältnis |
| `clock` | Auswahl `NONE` / `TIME` / `TIME_DATE` | `NONE` | Uhrzeit/Datum einblenden |
| `overlayItems` | Text | leer | bis zu 4 Item-Namen, kommagetrennt, deren Werte eingeblendet werden |
| `caption` | Auswahl `NONE` / `FILENAME` | `NONE` | Bildunterschrift |
| `nightMode` | Auswahl `OFF` / `BLACK` / `DIM` / `CLOCK` | `OFF` | Nachtmodus (§6) |
| `nightStart` | Text `HH:mm` | `22:00` | Beginn Nachtmodus |
| `nightEnd` | Text `HH:mm` | `06:30` | Ende Nachtmodus |
| `maxSize` | Zahl (px) | 1920 | längste Bildkante (erweitert) |
| `refreshMinutes` | Zahl (min) | 5 lokal / 15 Google / 60 Dropbox-Link / 15 Dropbox | Ordner neu einlesen (erweitert) |
| `tapPage` | Auswahl | leer | Main-UI-Seite oder Sitemap, die sich beim Tippen über der Diashow öffnet; Vorrang vor `tapUrl` |
| `tapUrl` | Text | leer | Adresse, die beim Tippen geöffnet wird (erweitert) |
| `cacheSizeMb` | Zahl | 500 | Cache-Grenze (erweitert) |

### 7.2 Channels
| Channel | Item-Typ | Beschreibung |
|---|---|---|
| `control` | `Player` | `PLAY` / `PAUSE` / `NEXT` / `PREVIOUS` |
| `currentImage` | `String` | Dateiname des aktuellen Bildes |
| `imageCount` | `Number` | Anzahl Bilder im Ordner |
| `pageUrl` | `String` | Adresse der Diashow-Seite inkl. Token |
| `lastRefresh` | `DateTime` | letztes erfolgreiches Einlesen des Ordners |
| `refresh` | `Switch` | `ON` liest den Ordner sofort neu ein |
| `night` | `Switch` | Nachtmodus aktiv; Befehl erzwingt ihn bis zur nächsten Zeitgrenze |

### 7.3 Aktionen
- `next()`, `previous()`, `pause()`, `play()`
- `refresh()` – Ordner neu einlesen
- `newToken()` – neuen Zugriffstoken erzeugen (alte Adressen funktionieren dann nicht mehr)

### 7.4 Status
- `ONLINE`, sobald die Quelle gelesen wurde und mindestens ein Bild vorhanden ist.
- `OFFLINE / CONFIGURATION_ERROR` bei ungültigem Pfad/Link/Key, mit verständlicher Meldung (z. B. „Ordner ist nicht für ‚Jeder mit dem Link‘ freigegeben“).
- `OFFLINE / COMMUNICATION_ERROR` bei Netzwerkproblemen; bereits zwischengespeicherte Bilder werden **weiter angezeigt**.
- Ordner leer → `ONLINE`, Diashow zeigt einen Hinweis „Keine Bilder im Ordner“.

---

## 8. Bridge `photoframe:dropbox`
| Parameter | Typ | Beschreibung |
|---|---|---|
| `appKey` | Text | App key der eigenen Dropbox-App |
| `authCode` | Text | Einmal-Code von Dropbox; wird nach erfolgreichem Login geleert |

Status `OFFLINE / CONFIGURATION_PENDING` mit Login-Link, bis der Code eingetragen ist. Aktion `logout()`.

---

## 9. Widget
- **Bilderrahmen-Karte** für das Dashboard: aktuelles Bild als Vorschau, Name, Anzahl, Tasten Zurück / Pause / Weiter und **„Vollbild“** (öffnet die Diashow-Seite).
- Englisch/Deutsch umschaltbar, Items frei wählbar – wie das Backup-Widget.

---

## 10. Tests
- Link-Parser: alle Google- und Dropbox-Linkformen, ungültige Links
- Reihenfolge: `RANDOM` ohne Wiederholung bis alle gezeigt wurden, `NAME` natürlich sortiert (`2.jpg` vor `10.jpg`)
- EXIF-Ausrichtung: Testbilder mit Orientierung 1/3/6/8
- Verkleinerung und `fitMode`-Berechnung
- Dropbox-ZIP: nur Bilder werden entpackt, Pfade wie `../` werden abgewiesen (Sicherheit)
- Antworten von Google- und Dropbox-API (JSON-Beispiele), Token-Prüfung des Servlets
- Manuell: Docker openHAB 5.2.1, Tablet mit Fully Kiosk als Bildschirmschoner, deine zwei Testordner

---

## 11. Meilensteine

| # | Inhalt |
|---|---|
| M1 | Projektgerüst, Build + SAT grün, Release-Workflow |
| M2 | Lokaler Ordner, Cache mit Verkleinerung/EXIF, Server-Ablauf, Diashow-Seite mit Übergängen, Channels, Aktionen |
| M3 | Google-Drive-Link mit API-Key |
| M4 | Dropbox-Link (ZIP) |
| M5 | Dropbox-Bridge mit Login |
| M6 | Widget, README (EN) + Anleitung (DE), Screenshots → **Release 0.1.0** |

Geschätzter Aufwand: etwa 4–5 Arbeitstage.

**Test-Versionen nach jedem Meilenstein**, jede mit GitHub-Release (JAR, Widget, Anleitungen):

| Version | nach | Neu zum Testen |
|---|---|---|
| 0.0.1 | M2 | lokaler Ordner, Diashow im Vollbild, Übergänge, Nachtmodus, Steuerung |
| 0.0.2 | – | Name „Photoframe“, nur funktionierende Optionen sichtbar (Dropbox-Bridge ausgeblendet) |
| 0.0.3 | – | Standardordner `userdata/photoframe` ohne Unterordner |
| 0.0.4 | – | Korrekturen aus dem ersten Test: Schwarzbild bei „Schieben“, Docker-Adresse, Item-Auswahl für Einblendungen |
| 0.0.5 | M4 | Dropbox-Link (vorgezogen, weil einfacher als Google) |
| 0.0.6 | – | Tipp-Adresse nimmt App-Adressen an, `&tap=` pro Bildschirm, ältere Tablet-Browser, Fehleranzeige |
| 0.0.7 | – | iPad: Uhr, Datum und Einblendungen weiß statt schwarz (iOS-Datumserkennung abgeschaltet) |
| 0.0.8 | – | Tippen öffnet openHAB als Vollbild-Ebene über der Diashow, automatische Rückkehr nach Leerlauf (`returnSeconds`, `&return=`) |
| 0.0.9 | M6 (Teil) | Widget, vollständige Anleitungen, Vollbild-Seite, Korrekturen an Adresse und App-Adressen |
| 0.0.10 | – | Schließen-Taste unten links, Anleitung für die Rückkehr nach Leerlauf |
| 0.0.11 | – | Schließen-Taste verschwindet nach 6 Sekunden, Zurück am Rundenanfang korrigiert |
| 0.0.12 | – | Seite beim Tippen (`tapPage`), Liste blieb auf openHAB 5.2 leer |
| 0.0.13 | – | Liste der Main-UI-Seiten und Sitemaps funktioniert auf allen 5.x, neue Einleitung |
| 0.0.14 | – | Einblendungen zeigen den Text aus den Zustandsoptionen eines Items statt des Rohwerts |
| offen | M5 | Dropbox mit Login (Bridge erscheint erst hier) |
| offen | M3 | Google Drive (Link + API-Key) |
| **0.1.0** | M6 | erste Version für Beta-Tester |

M1 (nur Gerüst) bekommt keine Test-Version, weil es darin noch nichts auszuprobieren gibt.

---

## 12. Entscheidungen

| # | Thema | Stand |
|---|---|---|
| 1 | Optionen | Standardwerte ok, **Nachtmodus** ergänzt (§6); weitere Optionen kann Pierrecinema noch nachreichen |
| 2 | Zugriffstoken | **ja** |
| 3 | Test-Versionen | **nach jedem Meilenstein**, 0.0.x bis 0.1.0 (§11) |
| 4 | Name | **„Photoframe Binding“**, Thing „Photoframe“ – in allen Sprachen |
| 7 | Sichtbare Optionen | Nur funktionierende Quellen/Things werden angeboten; Dropbox-Bridge erst mit dem Dropbox-Login |
| 5 | Release-Dateien | JAR + Widget-YAML + Anleitungen sichtbar im Release |
| 6 | Lokaler Ordner | Binding legt `userdata/photoframe` selbst an, **ohne Unterordner**; weitere Rahmen bekommen einen eigenen Pfad |
