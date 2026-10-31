# Photoframe Binding – Anleitung

## Dein Bilderrahmen in der openHAB-App: Android, iOS oder Browser, ganz ohne Kiosk-App

Du bleibst in deiner App. Die Diashow läuft im Vollbild genau dort, wo du openHAB nutzt: in der Android-App, in der iOS-App und in jedem Browser.
Ein Tipp bringt dich auf die openHAB-Seite, die du gewählt hast, und ohne Berührung kommt die Diashow nach der eingestellten Zeit von selbst zurück.

**Bald verfügbar:** Nextcloud, Dropbox mit Login und Google Drive.

Das Binding macht aus openHAB einen digitalen Bilderrahmen.
Alles läuft im Binding selbst: keine Skripte, kein ImageMagick, keine Cron-Jobs, auf dem Server muss nichts zusätzlich installiert werden.

Benötigt **openHAB 5.0 oder neuer**. Auf openHAB 4.x startet das Binding nicht.

| Diashow | Thing-Einstellungen | Dashboard-Widget |
|---|---|---|
| ![Diashow](images/slideshow.png) | ![Thing-Einstellungen](images/thing-configuration.png) | ![Dashboard-Widget](images/widget-photoframe-card.png) |

Die vollständige Referenz aller Einstellungen steht auf Englisch in der [README](../README.md).

## 1. Installieren

1. Lade `org.openhab.binding.photoframe-<version>.jar` aus dem [neuesten Release](https://github.com/pierrecinema/openhab-binding-photoframe/releases).
1. Kopiere die Datei in den `addons`-Ordner von openHAB:

   | Installation            | Ordner                                                    |
   |-------------------------|-----------------------------------------------------------|
   | Docker (offizielles Image) | `/openhab/addons` im Container (meist ein Volume auf dem Host) |
   | openHABian / apt / rpm  | `/usr/share/openhab/addons`                               |
   | Manuell (zip)           | `<openhab>/addons`                                        |

1. Nach 10 bis 30 Sekunden ist das Binding geladen. Ein Neustart ist nicht nötig.

**Update:** Alte JAR aus dem `addons`-Ordner löschen, neue hineinkopieren. Things und Items bleiben erhalten.

## 2. Thing anlegen

1. Gehe zu **Einstellungen → Things → + → Photoframe Binding**.
1. Wähle **Photoframe** und als Quelle **Lokaler Ordner**.
1. Klicke auf **Erstellen**.

Das Binding legt den Ordner `userdata/photoframe` selbst an (Docker: `/openhab/userdata/photoframe`).
Für einen zweiten Rahmen legst du ein zweites Thing mit einem anderen Ordner an.

## 3. Bilder hineinlegen

Kopiere JPEG-, PNG-, GIF- oder BMP-Dateien in den Ordner. Unterordner werden nicht gelesen.
Neue Bilder erscheinen nach spätestens 5 Minuten, oder sofort über den Channel **Jetzt einlesen**.
Deine Originale werden nie verändert.

### Bilder aus Dropbox

1. Erstelle in Dropbox einen Ordner für den Rahmen und lege Bilder hinein.
1. Klicke beim Ordner auf **Teilen → Link kopieren**.
1. Wähle im Thing die Quelle **Dropbox-Freigabelink** und füge den Link bei **Freigabelink** ein.

Der Ordner wird alle 60 Minuten gelesen. Wer den Link kennt, kann die Bilder sehen.

## 4. Items anlegen

1. Öffne das Thing und wechsle zum Reiter **Channels**.
1. Klicke auf **Add Equipment to Model** und wähle die gewünschten Channels aus.

Das sind die Channels:

| Channel | Typ | Bedeutung |
|---|---|---|
| Steuerung (`control`) | Player | Abspielen, Pause, Weiter, Zurück |
| Aktuelles Bild (`currentImage`) | String | Dateiname des gezeigten Bildes |
| Bilder (`imageCount`) | Number | Anzahl Bilder im Ordner |
| Diashow-Adresse (`pageUrl`) | String | Adresse der Diashow, die brauchst du für jeden Bildschirm |
| Nachtmodus aktiv (`night`) | Switch | Zeigt den Nachtmodus an, ein Befehl erzwingt ihn bis zur nächsten Start- oder Endzeit |
| Zuletzt eingelesen (`lastRefresh`) | DateTime | Zeitpunkt des letzten Einlesens |
| Jetzt einlesen (`refresh`) | Switch | ON liest den Ordner sofort neu ein |

Oder als Textdateien:

```java
// things/photoframe.things
Thing photoframe:frame:wohnzimmer "Photoframe Wohnzimmer" [ sourceType="LOCAL", interval=30, transition="FADE", kenBurns=true, fitMode="BLUR", clock="TIME_DATE", nightMode="CLOCK", nightStart="22:30", nightEnd="06:30" ]
```

```java
// items/photoframe.items
Player   Photoframe_Steuerung  "Photoframe"                 { channel="photoframe:frame:wohnzimmer:control" }
String   Photoframe_Bild       "Aktuelles Bild [%s]"        { channel="photoframe:frame:wohnzimmer:currentImage" }
Number   Photoframe_Anzahl     "Bilder [%d]"                { channel="photoframe:frame:wohnzimmer:imageCount" }
String   Photoframe_Adresse    "Diashow-Adresse [%s]"       { channel="photoframe:frame:wohnzimmer:pageUrl" }
Switch   Photoframe_Nacht      "Nachtmodus"                 { channel="photoframe:frame:wohnzimmer:night" }
Switch   Photoframe_Einlesen   "Jetzt einlesen"             { channel="photoframe:frame:wohnzimmer:refresh" }
```

## 5. Diashow öffnen

Öffne die Adresse aus dem Item **Diashow-Adresse** im Browser. Sie sieht so aus:

```text
http://192.168.1.10:8080/photoframe/<id>/?token=<token>
```

Ein Tipp schaltet auf Vollbild, solange keine Tipp-Adresse eingestellt ist.

Zeigt die Adresse eine falsche IP, trage in den erweiterten Einstellungen **openHAB-Adresse** deine Adresse ein, zum Beispiel `http://192.168.1.10:8080`.

## 6. Einstellungen

Im Thing unter **Diashow**, **Einblendungen** und **Nachtmodus** stellst du alles ein:

| Einstellung | Möglichkeiten |
|---|---|
| Anzeigedauer | 5 bis 3600 Sekunden |
| Reihenfolge | Zufällig (jedes Bild einmal, bevor sich eines wiederholt) oder nach Dateiname |
| Übergang | Überblenden, Schieben, Wischen, Zoomen, Zufällig, Keiner |
| Ken-Burns-Effekt | Langsames Zoomen und Schwenken während der Anzeige |
| Anpassung | Unscharfer Hintergrund, schwarze Ränder, Bildschirm füllen, Automatisch |
| Uhr | Keine, Zeit, Zeit und Datum |
| Einblend-Items | Bis zu 4 Items, zum Beispiel Temperaturen |
| Bildunterschrift | Keine oder Dateiname |
| Nachtmodus | Aus, schwarz, gedimmte Bilder, schwarz mit gedimmter Uhr, mit Start- und Endzeit |

Im Nachtmodus weckt ein Tipp die Diashow für 30 Sekunden.

## 7. Dashboard-Widget

Das Widget zeigt eine Live-Vorschau, das aktuelle Bild, die Anzahl Bilder und den Nachtmodus. Es hat Tasten für Zurück, Abspielen/Pause, Weiter und Vollbild.

![Dashboard-Widget](images/widget-photoframe-card.png)

1. Öffne in der Main UI **Entwicklerwerkzeuge → Widgets → +**.
1. Ersetze den Inhalt durch [widgets/photoframe_card.yaml](../widgets/photoframe_card.yaml) und speichere.
1. Füge auf einer Seite das Widget **photoframe_card** hinzu (unter „Personal widgets“) und wähle deine Items aus.

```yaml
component: widget:photoframe_card
config:
  lang: de
  urlItem: Photoframe_Adresse
  controlItem: Photoframe_Steuerung
  countItem: Photoframe_Anzahl
  currentImageItem: Photoframe_Bild
  nightItem: Photoframe_Nacht
```

## 8. Vollbild-Seite in der Main UI

Die Diashow kann als eigene Main-UI-Seite ohne Kopfzeile laufen.

1. Gehe zu **Einstellungen → Seiten → + → Layout-Seite** und speichere die leere Seite.
1. Öffne den Reiter **Code** der Seite und ersetze den Inhalt durch das YAML aus dem Abschnitt [Full-Screen Page in Main UI](../README.md#full-screen-page-in-main-ui) der README.
1. Trage dort `<id>` und `<token>` aus deiner Diashow-Adresse ein.

Mit `&tap=%2F` öffnet ein Tipp openHAB über der Diashow. Eine fest eingeblendete Seitenleiste liegt auf grossen Bildschirmen über dem linken Bildrand.

## 9. Tablet als Bilderrahmen

### Android mit Fully Kiosk Browser

**Gratisversion:**

1. Fully → Einstellungen → **Web Content Settings → Start URL**: die Diashow-Adresse.
1. Im Thing die **Tipp-Adresse** (erweitert) auf deine openHAB-Seite setzen.
1. Fully → **Web Auto Reload → Auto Reload on Idle**: zum Beispiel `120` Sekunden.

**Fully PLUS, echter Bildschirmschoner:**

1. Fully → **Screensaver → Screensaver Timer**: zum Beispiel `120` Sekunden.
1. **Screensaver Playlist** → Eintrag vom Typ *Web page / URL* mit der Diashow-Adresse.

**Bildschirm anlassen:** Fully → Device Management → **Keep Screen On**. Am Tablet: Akkuoptimierung für Fully aus, langer Bildschirm-Timeout, Android-Bildschirmschoner aus, Tablet am Ladegerät.

Der Bildschirmschoner der openHAB-Android-App zeigt nur Item-Text, keine Webseite. Für die Diashow eignet er sich nicht.

### Tipp öffnet die openHAB-App

Trage in der **Tipp-Adresse** (erweitert) die Adresse für dein Gerät ein:

| Gerät | Tipp-Adresse |
|---|---|
| Android, openHAB-App (getestet mit Fully Kiosk) | `intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component=org.openhab.habdroid/.ui.MainActivity;end` |
| iPad / iPhone, openHAB-App | `openhab://` |
| openHAB im Browser | `http://192.168.1.10:8080/` |

### Tipp öffnet openHAB über der Diashow

Wähle im Thing bei **Seite beim Tippen** eine Main-UI-Seite oder eine Sitemap aus der Liste, eine Adresse brauchst du dafür nicht. Sitemaps öffnen sich in der Basic UI. Diese Seite öffnet sich beim Tippen im Vollbild über der Diashow. Dasselbe gilt für eine Tipp-Adresse, die auf eine Seite desselben openHAB zeigt.
Ohne Berührung kehrt die Diashow nach **Zurück zur Diashow** (Standard 120 Sekunden) von selbst zurück. Das ✕ unten links geht sofort zurück. Es verschwindet nach 6 Sekunden, damit es openHAB nicht verdeckt. Steht **Zurück zur Diashow** auf 0, bleibt es stehen.

Jeder Bildschirm kann eine eigene Tipp-Adresse haben: Hänge `&tap=` mit der URL-kodierten Adresse an, optional auch `&return=60`. `&tap=` hat Vorrang vor den Einstellungen im Thing. Soll **Seite beim Tippen** gelten, darf `&tap=` also nicht in der Adresse stehen, auch nicht in der Vollbild-Seite aus Abschnitt 8.

### Diashow nach einer Zeit ohne Eingabe

Die Main UI und die openHAB-Apps erkennen keine Leerlaufzeit, eine Seite kann die Diashow also nicht von selbst starten.
Dreh es deshalb um und mach die Diashow zur Startseite. Ein Tipp öffnet openHAB darüber, und ohne Berührung ist die Diashow nach **Zurück zur Diashow** wieder da.

1. Lege die Vollbild-Seite aus Abschnitt 8 an, mit `&tap=%2F` in der Adresse.
1. Stelle diese Seite auf dem Gerät als Startseite ein, in der Main UI oder in der openHAB-App.

Die Zeit stellst du mit **Zurück zur Diashow** (erweitert) ein oder mit `&return=60` in der Adresse der Seite.

### iPad, ohne Kiosk-App

1. iPad → Einstellungen → Anzeige & Helligkeit → **Automatische Sperre: Nie**, iPad am Ladegerät.
1. Öffne in Safari die Diashow-Adresse mit `&tap=`, zum Beispiel `…/?token=<token>&tap=http%3A%2F%2F192.168.1.10%3A8080%2F`.
1. **Teilen → Zum Home-Bildschirm**, dann das neue Symbol öffnen: Die Diashow läuft im Vollbild.

Getestet mit iOS 15.

## 10. Probleme lösen

| Problem | Ursache und Lösung |
|---|---|
| Adresse zeigt eine falsche IP | openHAB meldet eventuell eine Docker-Adresse. Trage **openHAB-Adresse** ein oder setze die primäre Adresse unter **Einstellungen → Netzwerk**. |
| _Forbidden_ oder leere Seite | Der Token fehlt. Kopiere die ganze Adresse mit `?token=…`. |
| Schwarzer Bildschirm in der openHAB-Android-App | Deren Bildschirmschoner zeigt nur Text. Nimm Fully Kiosk oder einen Browser. |
| Neue Bilder erscheinen nicht | Warte auf das nächste Einlesen (lokal 5, Dropbox 60 Minuten) oder nutze **Jetzt einlesen**. |
| Dropbox: _„kein Freigabelink“_ | Nutze beim Ordner **Teilen → Link kopieren**. Links auf einzelne Dateien gehen nicht. |
| Dropbox: _„grösser als 2 GB“_ | Nimm weniger oder kleinere Bilder. |
| Tablet-Bildschirm geht aus | **Keep Screen On** in Fully aktivieren oder langen Timeout einstellen. |
| _„Dieser Inhalt ist blockiert“_ nach einem Tipp in einer Main-UI-Seite | App-Adressen gehen dort nicht. Aktualisiere auf 0.0.9 oder hänge `&tap=%2F` an die Adresse der Seite an. |

Für Details aktiviere in der Karaf-Konsole das Debug-Log:

```shell
log:set DEBUG org.openhab.binding.photoframe
```

## 11. Sicherheit

- Die Diashow-Adresse enthält einen Token, weil sich Tablets in einer Kiosk-App nicht bei openHAB anmelden können. Wer die ganze Adresse kennt, sieht die Bilder.
- Die Rule-Action `newToken()` erzeugt einen neuen Token. Adressen mit dem alten Token funktionieren dann sofort nicht mehr.
- Mach die Adresse nicht ohne weiteren Schutz aus dem Internet erreichbar.

## 12. Entfernen

1. Lösche das Thing.
1. Lösche die JAR aus dem `addons`-Ordner.
1. Lösche bei Bedarf `userdata/photoframe` und `userdata/cache/photoframe`.
