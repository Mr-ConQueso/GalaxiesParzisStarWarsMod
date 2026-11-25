# Galaxies: Parzi’s Star Wars Mod for NeoForge

This fork is building a native NeoForge port for Minecraft 1.21.1. The target combines the current PSWG development bundle and playable content from the Fabric 1.20.1 release. The port is in progress. See [the parity matrix](PORT_PARITY.md) for implemented and pending features.

## Installation

Use a fresh world with Minecraft 1.21.1 and Java 21. Put the PSWG JAR and Curios API in the `mods` folder on each client and the dedicated server. The tested combination is NeoForge 21.1.93 and Curios API 9.5.1+1.21.1. [Curios API download](https://modrinth.com/mod/curios/version/9.5.1%2B1.21.1)

Equip jetpacks and backpacks in the Back slot in the Curios inventory. By default, G opens Curios and B toggles blaster patrol posture.

## Development

Use Java 21 and the included Gradle wrapper.

| Command | Purpose |
| --- | --- |
| `./gradlew build` | Build the development JAR in `build/libs` |
| `./gradlew runData` | Regenerate the 1.21.1 item models, block tags, and loot tables |
| `./gradlew runGameTestServer` | Check behavior on the dedicated GameTest server |
| `./gradlew runClient` | Start the NeoForge development client |
| `./gradlew runServer` | Start an isolated dedicated server under `run/server` |

## Credits

Galaxies was created by the [Parzivail Modding Team](https://github.com/Parzivail-Modding-Team/GalaxiesParzisStarWarsMod). Some ideas and concepts are copyright or trademarks of Lucasfilm Ltd., LucasArts, or Disney. Code is licensed under [LGPL-3.0](LICENSE). Unless otherwise stated, non-code assets are licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
