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

> **Status:** test version 0.0.6 – local folder and Dropbox shared link. Dropbox login and Google Drive follow.
> Specification (German): [docs/SPEZIFIKATION.md](docs/SPEZIFIKATION.md)

## Quick Start

1. Download the JAR of the [latest release](https://github.com/pierrecinema/openhab-binding-photoframe/releases) and copy it into the `addons` folder (Docker: `/openhab/addons`, openHABian: `/usr/share/openhab/addons`).
1. Add a **Photoframe** thing (Settings → Things → + → Photoframe Binding) with source **Local folder**.
1. Put pictures (JPEG, PNG, GIF, BMP) into the folder shown in the thing status, by default `userdata/photoframe` (Docker: `/openhab/userdata/photoframe`). For a second photo frame, enter another folder in its settings.
1. Open the address from the **Slideshow Address** channel (`pageUrl`) on a tablet or PC. Tap to switch to full screen.

### Pictures from Dropbox

1. In Dropbox, create a folder for the photo frame and put pictures into it.
1. **Share** the folder → **Copy link** (a link like `https://www.dropbox.com/scl/fo/...`).
1. In the Photoframe thing select source **Dropbox shared folder link** and paste the link into **Shared Link**.

The folder is read every 60 minutes (advanced setting *Refresh Interval*); the *Refresh Now* channel reads it immediately.
Anyone who knows a shared link can see the pictures – only use it for pictures that may be seen this way.

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

**Keep the screen on:** Fully → Device Management → **Keep Screen On**. On the tablet: battery optimisation off for Fully, a long screen timeout, Android screen saver off, tablet on the charger.

### Tap Opens the openHAB App

The **Tap Address** accepts any address the device can open, including app addresses:

| Device | Tap Address |
|---|---|
| Android, openHAB app (tested with Fully Kiosk) | `intent:#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component=org.openhab.habdroid/.ui.MainActivity;end` |
| iPad / iPhone, openHAB app | `openhab://` |
| openHAB in the browser | `http://192.168.1.10:8080/` |

The Android address opens the app like its icon does, so the app shows its configured start page.

### Different Tap Address per Screen

One Photoframe thing can serve several screens. Append `&tap=` with the URL-encoded address to the slideshow address of a screen; it overrides the Tap Address of the thing for this screen only:

```text
http://192.168.1.10:8080/photoframe/<id>/?token=<token>&tap=openhab%3A%2F%2F
```

### Old iPad (Kiosker)

1. iPad → Settings → Display & Brightness → **Auto-Lock: Never**, iPad on the charger.
1. Kiosker → settings → **Home URL**: the slideshow address with `&tap=openhab%3A%2F%2F` appended.
1. If available, let Kiosker return to the home URL after inactivity.

A tap opens the openHAB app; going back to Kiosker (app switcher or home screen) shows the slideshow again.

## License

[Eclipse Public License 2.0](LICENSE)
