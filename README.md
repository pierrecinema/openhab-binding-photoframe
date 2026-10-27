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

> **Status:** test version 0.0.2 – local folder only. Google Drive and Dropbox follow in the next test versions.
> Specification (German): [docs/SPEZIFIKATION.md](docs/SPEZIFIKATION.md)

## Quick Start

1. Download the JAR of the [latest release](https://github.com/pierrecinema/openhab-binding-photoframe/releases) and copy it into the `addons` folder (Docker: `/openhab/addons`, openHABian: `/usr/share/openhab/addons`).
1. Add a **Photoframe** thing (Settings → Things → + → Photoframe Binding) with source **Local folder**.
1. Put pictures (JPEG, PNG, GIF, BMP) into the folder shown in the thing status, by default `userdata/photoframe/<thing id>`.
1. Open the address from the **Slideshow Address** channel (`pageUrl`) on a tablet or PC. Tap to switch to full screen.

If openHAB runs in Docker without host network, the detected address may be the container address.
Then set **openHAB Address** (advanced setting `baseUrl`), for example `http://192.168.1.10:8080`.

### As Tablet Screensaver

- **Fully Kiosk Browser:** Settings → Screensaver → set the timer and enter the slideshow address as **Screensaver Wallpaper URL / Playlist**. Touching the screen returns to openHAB. To switch the screen off at night, use "Screen off" times of Fully Kiosk or the night mode of the binding.
- **WallPanel:** Settings → Screen saver → enable and set the slideshow address as web page.

## License

[Eclipse Public License 2.0](LICENSE)
