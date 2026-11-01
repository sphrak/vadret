[![github-ci](https://github.com/vadret/android/workflows/V%C3%A4dret%20Android%20App/badge.svg)](https://github.com/vadret/android/actions)
[![github-releases](https://img.shields.io/github/v/release/sphrak/vadret)](https://github.com/vadret/android/releases/)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/vadret/android/blob/master/LICENSE)

<img src="https://raw.githubusercontent.com/vadret/android/dev/assets/logo.png" alt="Vädret" width="64">

# Vädret
A simple weather app that uses the Swedish weather service [SMHI](https://opendata.smhi.se/) to fetch weather data, and [Krisinformation](https://www.krisinformation.se/en) for emergency information from Swedish authorities. MVI written in Kotlin Multiplatform supporting Android and iOS targets. The data is licensed under [Creative commons Erkännande 4.0 SE](https://www.smhi.se/data/om-smhis-data/villkor-for-anvandning).

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/fi.kroon.vadret/)
[<img src="https://raw.githubusercontent.com/vadret/android/master/assets/get-github.png"
      alt="Get it on Github"
      height="80">](https://github.com/vadret/android/releases)

<p>
  <img src="assets/screenshots/weather-dark.webp" width="24%" alt="Weather in dark mode">
  <img src="assets/screenshots/warnings-dark.webp" width="24%" alt="Warnings in dark mode">
  <img src="assets/screenshots/radar-precipitation-dark.webp" width="24%" alt="Radar with precipitation in dark mode">
  <img src="assets/screenshots/radar-wind-dark.webp" width="24%" alt="Radar with wind in dark mode">
</p>

<p>
  <img src="assets/screenshots/weather-light.webp" width="24%" alt="Weather in light mode">
  <img src="assets/screenshots/warnings-light.webp" width="24%" alt="Warnings in light mode">
  <img src="assets/screenshots/radar-precipitation-light.webp" width="24%" alt="Radar with precipitation in light mode">
  <img src="assets/screenshots/radar-wind-light.webp" width="24%" alt="Radar with wind in light mode">
</p>

## Changelog
Changelog available [here](https://github.com/vadret/android/blob/master/androidApp/src/main/res/raw/changelog.md).

## Architecture

This project builds on concepts from [Clean Architecture](https://8thlight.com/blog/uncle-bob/2012/08/13/the-clean-architecture.html).
The presentation layer is heavily inspired by Jake Wharton's talk on _[Managing State with RxJava](https://www.youtube.com/watch?v=0IKHxjkgop4)_.
The project also uses a modified `Either` type originally written by [Fernando Cejas](https://github.com/android10/Android-CleanArchitecture-Kotlin/blob/master/app/src/main/kotlin/com/fernandocejas/sample/core/functional/Either.kt) with some additional extensions added.

## SMHI Open Data API Docs

* [Meteorological Forecast](https://opendata.smhi.se/metfcst/snow1gv1/introduction)
* [Meteorological Analysis](https://opendata.smhi.se/metanalys/mesan2gv3/introduction)
* [Warnings](https://opendata.smhi.se/warnings/introduction)
* [Radar](https://opendata.smhi.se/radar/introduction)

## Building
This project uses [ktlint](https://github.com/pinterest/ktlint) for linting.

#### Running

```sh
./gradlew :androidApp:installDevRelease && adb shell am start -n se.vadretapp.vadret/se.vadretapp.vadret.main.MainActivity
```

The radar uses the shared maplibre-compose map by default. To build Android with the older MapLibre Android map instead, for comparison:

```sh
./gradlew :androidApp:installDevRelease -Pvadret.radarMap=mapView
```

#### Running on iOS

Requires a Mac with Xcode 26. Open `iosApp/iosApp.xcodeproj` and run the `iosApp` scheme, or build from the command line:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator build
```

To run on a device with your own Apple team, change `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.

Push alerts arrive as an alert that `iosApp/NotificationService` opens before iOS shows it. The extension reaches the key and the saved place names through the app group `group.<bundle id>`, so both App IDs, `<bundle id>` and `<bundle id>.NotificationService`, need App Groups with that group.

To upload a build to TestFlight, run the `Vädret iOS TestFlight` workflow under Actions on GitHub.

#### Testing

```sh
./gradlew test allTests
```

#### Linting
```sh
./gradlew ktlint
./gradlew ktlintFormat
```

#### Releasing

```sh
./release.sh <version>
```

## Contributor Guidelines
If you would like to contribute code to the project fork the project and find an issue/feature you would like to work on. Ideally check with a maintainer so you dont work on something that might be in the workings already.

Your pull request will be failed by the build server if it does not have passing unittests and lintchecks. A build can also be failed if you decrease the testing coverage.

When submitting a pull request make sure you squash
the commit(s) for that PR -- Do this so we can keep a clean
git history.

* [More information here](https://github.com/vadret/android/blob/master/CONTRIBUTING.md)

## Localization
Translations live in `core/src/main/res/values-<language>/strings.xml`. To add a language or fix a
translation, open a pull request and add your name to the `TRANSLATORS` file.

## License

	Copyright 2018 Niclas Kron

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
