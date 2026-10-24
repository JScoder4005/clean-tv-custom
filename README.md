# Clean TV Custom

My personal Android TV / Google TV setup: a debloated home screen with a clean, Apple-TV-style
launcher, customized on top with a few things I wanted that weren't there by default.

TV: TCL G07 (Google TV, Android 11).

## What this does

- **Debloats the TV over Wi-Fi.** Disables manufacturer telemetry, ad rows, and preloaded junk with
  `pm disable-user` — nothing is uninstalled, everything is reversible with a restore script.
- **Replaces the stock home screen** with a minimal launcher: a dock of favourite apps, a clean app
  grid, no sponsored content, no "Discover" nags.
- **Themeable live**, no rebuild needed for most changes — push a wallpaper, logos, colors, or layout
  tweaks and the launcher picks them up on restart.

## What I added on top

- **Bluetooth pill** — top bar shows Bluetooth state and connected device name/count; tap opens the
  TV's accessories screen, not a dead settings link.
- **Per-app logo override** — drop a `<package>.png`/`.webp` into a theme's `logos/` folder and that
  app's tile uses it instead of its own banner, no rebuild. Transparent art floats on the wallpaper
  without a background box.
- **Taglines** — an optional short line under a tile's name, set per app in a theme. Off by default,
  costs nothing when unused.
- **Weather chip** — current conditions, today's high/low, and a 9-icon condition set (clear/cloudy/
  rain/snow/fog/thunder, day and night variants) next to the clock, from Open-Meteo (no account or
  key), set by coordinates in the theme config.
- **Second city clock** — another city's local time and weather in the header, for anywhere else you
  care about, config-driven (label, timezone, coordinates).
- **Thought of the day** — a short line under the date, picked deterministically per calendar day
  from a bundled quote pool, same for everyone on that day.
- **Spotify (or any media session) now playing** — track name, album art and play/pause/next/previous
  in the header, driven by Android's MediaSession API (no notification is ever read).
- **Icon-only top bar** — Wi-Fi / Bluetooth / Inputs / Settings collapse to small round icon buttons
  instead of full labeled pills, for a cleaner header.
- **Serif display clock** (Cormorant Garamond) in place of the default sans clock face.

## Layout

```
launcher/     the Android launcher app (Java, Gradle)
tools/        Python scripts to debloat, theme, and manage the TV over ADB-over-TCP, no adb binary needed
themes/       theme folders (config.json + optional wallpaper/logos) pushed live to the TV
addons/       standalone module-card apps (weather, stats, etc.) that show up as dock tiles
examples/     per-TV-model debloat package lists from the community
docs/         device notes and troubleshooting
```

## Running it

Needs a computer on the same Wi-Fi as the TV, Python 3, and (for building the launcher) a JDK 17 +
Android SDK.

```bash
# one-time setup
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt

# debloat (see examples/ for a list matching your TV model)
TV_HOST=<tv ip> tools/disable.sh examples/<your-tv>/debloat.txt

# build and install the launcher
TV_HOST=<tv ip> launcher/build-and-install.sh

# push a theme (wallpaper, logos, taglines, weather coordinates, layout)
TV_HOST=<tv ip> tools/theme.sh themes/<name>
```

## Screenshots

_(adding photos)_

## Credit

Built on top of [CleanYourTV](https://cleanyour.tv) (MIT licensed) — the debloat tooling, the base
launcher, and the per-TV-model device lists in `examples/` are its work. This repo is my personal
customized setup, not the upstream project.
