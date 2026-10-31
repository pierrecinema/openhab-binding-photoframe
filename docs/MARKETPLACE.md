# Marketplace Post

Checklist for the post in the Add-on Marketplace of the openHAB community.
The text of the post is in [COMMUNITY-POST.md](COMMUNITY-POST.md).

Title: `Photoframe Binding: photo frame inside the openHAB app, no kiosk app needed`

## Requirements

| Requirement | Status |
|---|---|
| Public repository, so that the JAR link works | Done |
| Direct link to the binary that ends with `.jar` and has no URL parameters | Last link group in the post |
| Category: Marketplace → Bindings | To choose when posting |
| Tag for the add-on type | `binding` |
| Tag for the maturity | `alpha` for 0.0.x, `beta` for 0.x, `stable` from 1.0 |
| Tag `published`, otherwise the entry is hidden in the add-on store | To set when the post is ready |
| Runs on the stated openHAB versions | openHAB 5.0 and later, enforced by the import range in `pom.xml` |
| `addon.xml`, thing XML, EPL-2.0 license, translations | In the repository |
| Static code analysis | Runs in the release workflow |

Check the posting template of the category before posting, as the community can change it.

For each new release update the version in the features heading, the changelog and the JAR links of the post.
