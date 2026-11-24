# Galaxies: Parzi’s Star Wars Mod for NeoForge

This fork is building a native NeoForge port for Minecraft 1.21.1. The target combines the current PSWG development bundle and playable content from the Fabric 1.20.1 release. The port is in progress. See [the parity matrix](PORT_PARITY.md) for implemented and pending features.

## Development

Use Java 21 and the included Gradle wrapper.

| Command | Purpose |
| --- | --- |
| `./gradlew build` | Build the development JAR in `build/libs` |
| `./gradlew runData` | Regenerate the 1.21.1 item models, block tags, and loot tables |
| `./gradlew runGameTestServer` | Check behavior on the dedicated GameTest server |
| `./gradlew runClient` | Start the NeoForge development client |
| `./gradlew runServer` | Start an isolated dedicated server under `run/server` |

Use a fresh world for this port. The [parity matrix](PORT_PARITY.md) records the pinned source revisions and verification status.

## Credits

Galaxies was created by the [Parzivail Modding Team](https://github.com/Parzivail-Modding-Team/GalaxiesParzisStarWarsMod). Some ideas and concepts are copyright or trademarks of Lucasfilm Ltd., LucasArts, or Disney. Code is licensed under [LGPL-3.0](LICENSE). Unless otherwise stated, non-code assets are licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
