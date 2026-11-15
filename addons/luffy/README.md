# Luffy

My Raspberry Pi and smart bulb on the TV: one screen with the Pi's vital signs (CPU temperature,
memory, disk, load, uptime) on the left and the bulb's controls on the right (power, dimmer, brighter,
warm, cool, white, four colours). It is a module card, so it also shows as a dock tile.

It talks to a small bridge that runs on the Pi (the `home-twin` project, `bridge/`), over the home
network only. The bridge address and its token are personal, so they are not in this repo: put them in
`addons/luffy/local.properties` (git-ignored) before building.

```
luffy.url=http://<pi ip>:8788
luffy.token=<token from ~/.config/home-twin/token on the Pi>
```

```bash
cd addons/luffy && ./gradlew assembleRelease
```

The token ends up inside the APK you build, so keep that APK to yourself.

## Permissions

- `INTERNET`: to reach the bridge on the Pi. Cleartext HTTP is allowed only to the Pi's address and
  `luffy.local` (`res/xml/network_security_config.xml`); every other host stays HTTPS-only.
- No background service, no telemetry. It polls the bridge every 5 seconds only while its screen is open.

Tested on: TCL G07 (Google TV, Android 11).
