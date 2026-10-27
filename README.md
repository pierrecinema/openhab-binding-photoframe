# Photoframe Binding

Turns openHAB into a digital photo frame: pictures from a folder are shown as a full-screen slideshow with transitions, usable in any browser and as tablet screensaver (Fully Kiosk Browser, WallPanel).

- **Nothing to install on the server** – no scripts, no ImageMagick, no cron jobs
- **One folder = one photo frame**: the binding creates the folder, you put pictures in
- Transitions (fade, slide, wipe, zoom), Ken Burns effect, blurred background for pictures in the wrong format
- Pictures are scaled down and turned correctly (EXIF orientation)
- Overlays: clock, date, item values (e.g. temperatures), caption from the file name
- Night mode: black, dimmed, or black with a dimmed clock
- All screens show the same picture; control from openHAB (play, pause, next, previous)

Requires **openHAB 5.0 or later**.

> **Status:** test version 0.0.4 – local folder only. Google Drive and Dropbox follow in the next test versions.
> Specification (German): [docs/SPEZIFIKATION.md](docs/SPEZIFIKATION.md)

## Quick Start

1. Download the JAR of the [latest release](https://github.com/pierrecinema/openhab-binding-photoframe/releases) and copy it into the `addons` folder (Docker: `/openhab/addons`, openHABian: `/usr/share/openhab/addons`).
1. Add a **Photoframe** thing (Settings → Things → + → Photoframe Binding) with source **Local folder**.
1. Put pictures (JPEG, PNG, GIF, BMP) into the folder shown in the thing status, by default `userdata/photoframe` (Docker: `/openhab/userdata/photoframe`). For a second photo frame, enter another folder in its settings.
1. Open the address from the **Slideshow Address** channel (`pageUrl`) on a tablet or PC. Tap to switch to full screen.

If openHAB runs in Docker without host network, the detected address may be the container address.
Then set **openHAB Address** (advanced setting `baseUrl`), for example `http://192.168.1.10:8080`.

### As Tablet Screensaver

Two ways with [Fully Kiosk Browser](https://www.fully-kiosk.com) (Android). Menu names can differ slightly between versions.

**A – free version: slideshow as start page, back to it when idle**

1. Fully Kiosk → Settings → **Web Content Settings → Start URL**: the slideshow address.
1. In the Photoframe thing set **Tap Address** (advanced) to your openHAB page, e.g. `http://192.168.1.10:8080/page/overview`.
1. Fully Kiosk → Settings → **Web Auto Reload → Auto Reload on Idle**: e.g. `120` seconds.

Result: the tablet shows the slideshow; a tap opens openHAB; after 2 minutes without touch it returns to the slideshow.

**B – Fully Kiosk PLUS: real screensaver**

1. Fully Kiosk → Settings → **Screensaver → Screensaver Timer**: e.g. `120` seconds.
1. **Screensaver Playlist** → add an item of type *Web page / URL* with the slideshow address.

Result: openHAB stays the start page; after 2 minutes without touch the slideshow starts, a tap returns to openHAB.

At night the tablet screen can be switched off with Fully Kiosk ("Screen off" times) or darkened with the night mode of the binding.

## License

[Eclipse Public License 2.0](LICENSE)
