# Changelog

All notable changes to ZenPhone are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[semantic versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-07-30

The home screen stops being a fixed drawing and becomes something that can be arranged. Which
tiles it shows, in what order, and which indicators sit along the top are now decisions made
once by whoever sets the phone up, rather than decisions taken for them in a layout file.

### Added

- **Choose which tiles the home screen shows.** Settings › Home screen › Edit home screen ›
  Choose tiles. Seventeen to pick from, twelve at most on screen — beyond four rows the tiles
  shrink faster than the eye forgives.
- **Rearrange tiles by dragging them**, and remove one by dragging it onto the red bar that
  appears while a tile is held. The bar stays away when only one tile is left, since an empty
  home screen would be read as never having been set up and quietly filled back in.
- **Choose what appears in the bar along the top**, from Wi-Fi, mobile signal, torch, sound,
  brightness, notifications, an emergency shortcut, and a light-or-dark switch. Five places, in
  an order set by dragging them in a strip that shows what the bar will look like.
- **The next alarm under the clock**, with the weekday in front of the time unless the alarm is
  later today. Asked of Android rather than of this launcher, so an alarm set in any clock app
  on the phone appears here too. Nothing is shown when no alarm is set.
- **Medication, apps, alarms, photos, videos, internet, maps and settings as home tiles.** They
  worked only from the second page before, and could not be put on the home screen at all.

### Changed

- **The page to the left of the home screen is now the settings menu.** It used to hold eight
  fixed buttons, seven of which are now tiles that can be put on the home screen or left off
  it. The settings were the eighth, and they could not become a tile alone: a tile can be
  removed, and removing the way into the settings removes the way to put it back.
- **The top bar reports the Wi-Fi instead of the battery**, which was already beside the clock
  and saying the same thing twice. Three states, not two — a Wi-Fi switched on but connected to
  nothing looks exactly like a working one on any indicator that reports only the switch.
  Tapping opens Android's own internet panel, since no app may switch Wi-Fi on or off from
  Android 10 onwards.
- **The detailed battery reading moved to the battery beside the clock**, which is where the
  battery still is.
- **Internet and maps ask Android which app to use**, in the dialog the person meets everywhere
  else on the phone, and it remembers the answer. The launcher used to offer a list of its own
  that forgot every time.
- **The clock reads better when the phone is held sideways.** The condensed light typeface was
  dropped in favour of the one used in portrait — narrow shapes and thin strokes are the first
  things to fail for an ageing eye — and the battery and next alarm now appear there at all.
- **The fourth-row setting is gone.** It had stopped doing anything, and choosing tiles
  replaces it with something that can say which three.

### Fixed

- **A deleted alarm went on ringing.** Deleting removed its row and left the schedule standing,
  so the alarm sounded at the appointed hour with nothing left on screen to explain it and no
  way to stop it.
- **The emergency number could be dialled by one accidental touch.** It now always asks first,
  whatever the "confirm before calling" setting says. Ringing a daughter by mistake costs an
  apology; this costs an ambulance sent somewhere it is not needed.
- **Text messages never appeared in the notification list.** Every group summary was discarded
  as a repeat of the notifications beneath it, which is only true when there are any — and
  messaging apps routinely post a summary over a group of one.
- **The settings tile was nearly invisible on the light theme**, being the one tile icon taking
  its colour from the theme rather than the fixed one the other sixteen carry.
- **A tile's label sat too near the bottom edge**, the icon's own padding counting as space
  above it with nothing answering underneath.

## [1.0.0] - 2026-07

First release of ZenPhone, continuing from
[BaldPhone](https://github.com/UriahShaulMandel/BaldPhone) by Uriah Shaul Mandel and
[BaldPhone Neo](https://github.com/DamianKuzmiak/BaldPhoneNeo) by Damian Kuzmiak.

[1.1.0]: https://github.com/zenolabs/ZenPhone/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/zenolabs/ZenPhone/releases/tag/v1.0.0
