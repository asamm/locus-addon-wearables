# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2017-12-22
### Fixed
- App returns to the last opened activity (for ambient-enabled screens) after wake up from the sleep mode. Originally returned to watch face.

### Added
- Last translations

## [0.1.4] - 2017-12-19
### Added
#### Wear
- Track recording activity with basic statistics. Ambient enabled.
- Map activity with navigation panel and zoom buttons. Ambient enabled.
- Profile selection activity for track recording. Ambient not supported.
- Fail activity which is shown after global application fail. Mainly on handshaking(version, periodic updates) or general communication problems.
- Navigation drawer pull-down "main" menu.
### Device
- Device application. Almost stateless, just reacting to communication and/or Locus periodic updates using receivers. 
  Couple of timers to send periodic data and to detect long inactivity to release app resources.
### Common
  - Common module for shared code between device and the wear modules.
