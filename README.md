# Locus Map - add-on Wearables

Add-on for [Locus Map](http://www.locusmap.eu) (Android) application, focused on using most important features if Locus Map on Android Wear devices.

Add-on consists of these parts.

- `device` module - middleman who communicate with Locus Map (over Locus API) on one side and add-on running on watches (over messages) on second side
- `wear` module - separate application running on watches
- and `common` where code shared/used by both device and wear is used.

## Change log
See [CHANGELOG.md](CHANGELOG.md)

## Important information

Available at: [Wear for Locus Map at Google Play](https://play.google.com/store/apps/details?id=com.asamm.locus.addon.wear)

Supports both Android Wear 1.x and 2.0. For Android Wear 2.0 the add-on must be installed separately on both the mobile phone and the watch.

Requires Google Play Services and Locus Map to work. Paired mobile phone required, standalone function not supported on the watch.

## Setup project in Android Studio

- clone current repository in created directory by
  - `git clone https://github.com/asamm/locus-addon-wearables.git`
- open gradle project with Android Studio
- setup global gradle properties, mainly debug keystore - please refer to [Locus API, using global parameters.](https://github.com/asamm/locus-api/wiki/Adding-Locus-API-to-project#using-global-parameters)
- run device and wear modules
