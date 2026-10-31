# Marketplace Post

Text for the post in the Add-on Marketplace of the openHAB community.

## Requirements

| Requirement | Status |
|---|---|
| Public repository, so that the JAR link works | Open: the repository is still private |
| Direct link to the binary that ends with `.jar` and has no URL parameters | The release file below |
| Category: Marketplace → Bindings | To choose when posting |
| Tag for the add-on type | `binding` |
| Tag for the maturity | `alpha` for 0.0.x, `beta` for 0.x, `stable` from 1.0 |
| Tag `published`, otherwise the entry is hidden in the add-on store | To set when the post is ready |
| Runs on the stated openHAB versions | openHAB 5.0 and later, enforced by the import range in `pom.xml` |
| `addon.xml`, thing XML, EPL-2.0 license, translations | In the repository |
| Static code analysis | Runs in the release workflow |

Check the posting template of the category before posting, as the community can change it.

## Post

Title: `Photoframe Binding: digital photo frame and tablet screensaver`

```markdown
![Slideshow](https://raw.githubusercontent.com/pierrecinema/openhab-binding-photoframe/main/docs/images/slideshow.png)

Turns openHAB into a digital photo frame. Pictures from a folder run as a full-screen slideshow in any browser and as tablet screensaver. Nothing extra is needed on the server.

**Requires openHAB 5.0 or later.**

- One folder = one photo frame: the binding creates the folder, you put pictures in
- Local folder or shared Dropbox folder
- Transitions, Ken Burns effect, blurred background for pictures that do not fit the screen
- Clock, date and item values on the pictures
- Night mode
- All screens show the same picture, with play, pause, next and previous from openHAB
- A tap opens openHAB on top of the slideshow, or the openHAB app
- Dashboard widget with live preview

Tested on Android with Fully Kiosk Browser, in Android browsers, and on an iPad with iOS 15.

## Changelog

### Version 0.0.12
- Choose the Main UI page or sitemap opened by a tap from a list

### Version 0.0.11
- Close button disappears after 6 seconds
- Previous picture is correct at the start of a new round

### Version 0.0.10
- Close button moved to the bottom left

### Version 0.0.9
- Wrong Docker address in 192.168.x.x networks fixed
- App addresses open from a slideshow embedded in a page
- Dashboard widget and complete guides

## Resources

Documentation: https://github.com/pierrecinema/openhab-binding-photoframe
Widget: https://github.com/pierrecinema/openhab-binding-photoframe/blob/main/widgets/photoframe_card.yaml
Source: https://github.com/pierrecinema/openhab-binding-photoframe

https://github.com/pierrecinema/openhab-binding-photoframe/releases/download/v0.0.12/org.openhab.binding.photoframe-0.0.12.jar
```

Update the version in the changelog and in the last line for each new release.
