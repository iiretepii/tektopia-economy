# TekTopia Economy

A Forge 1.12.2 add-on for [TekTopia](https://www.curseforge.com/minecraft/mc-mods/tektopia) that gives each village a treasury and each villager a wallet. Every morning the treasury pays villagers a wage, and they pay rent and food back to it. Villagers who go unpaid, or can't cover their living costs, lose happiness, and TekTopia already makes unhappy villagers work slowly.

New emeralds come in from you: sell villager goods to TekTopia's wandering Merchant as usual, then deposit emeralds into the treasury.

## How it works

| Piece | What it does |
|---|---|
| Treasury | One emerald balance per village (needs a Town Hall). Saved with the world. |
| Wallet | Each TekTopia villager carries emeralds. Saved with the villager, and carried over when TekTopia replaces the villager (profession change, child growing up). |
| Payday | Once per Minecraft day at `paydayTime`, each loaded villager on the payroll is paid `wage × (1 + skillBonus × skill/100)` from the treasury, then pays `rent + foodCost` back. |
| Happiness | Unpaid: `-unpaidPenalty`. Can't pay living costs: `-brokePenalty`. Savings at or above `comfortSavings`: `+comfortBonus`. |
| Estates | When a villager dies, their savings go to the treasury (`estateToTreasury`). |

Children, nitwits, nomads and TekTopia's vendors (Merchant, Tradesman, Architect) aren't on the payroll. A villager in an unloaded chunk is settled when it loads again. Missed days are not back-paid, and changing the clock with `/time` never pays anyone twice. Paydays follow the world clock, so they stop if `doDaylightCycle` is off.

Default daily wages, in emeralds: blacksmith and enchanter 5; chef, cleric, druid, guard, miner and teacher 4; bard, butcher, farmer, lumberjack and rancher 3; captain 6.

## Playing

Stand inside a village and use:

- `/tekeco balance`: show the treasury balance.
- `/tekeco deposit <amount|all>`: move emeralds from your inventory into the treasury.
- `/tekeco withdraw <amount>`: take emeralds out (operators only if `withdrawRequiresOp` is on).
- `/tekeco report`: treasury balance, total villager savings, and the last payday's wages, upkeep and shortfalls.

Sneak and right-click a villager with an empty hand to see their wallet.

All numbers are in `config/tekeco.cfg`, and can also be changed in-game from the Mods menu.

## Installing

The built jar is in [`dist/`](dist/). Put `tektopia-economy-1.12.2-<version>.jar` in your `mods` folder next to TekTopia 1.1.0. It needs Forge 14.23.5.2768 or later, the same as TekTopia. Install it on the server and on every client.

## Building

TekTopia is closed source and isn't on any Maven repository, so you supply the jar yourself: download `tektopia-1.1.0.jar` from CurseForge and put it in `libs/`. It is git-ignored; don't commit it. The mod jar ends up in `build/libs/` with either method below.

**With ForgeGradle (normal, not yet tried).** Install JDK 8 (ForgeGradle 2.3 doesn't run on newer Java) and run `./gradlew build`. `./gradlew runClient` starts a dev client with TekTopia copied into `run/mods`.

**Without ForgeGradle.** `tools/offline-build/build.sh` builds the same jar on machines that can't reach Forge's or Mojang's servers. It needs JDK 11 or newer, `git` and `curl`, and downloads only from Maven Central and GitHub. It compiles against small stand-ins for the Minecraft and Forge classes the mod uses (`tools/offline-build/stubs/`), then renames Minecraft members to their runtime (SRG) names using the official MCP 1.12 and `stable_39` mappings, as ForgeGradle's reobfuscation does. It stops if any Minecraft reference has no mapping. Pass a folder of other Forge 1.12.2 mod jars as the first argument to also check every Minecraft, Forge and library reference against what those mods use. If you add code that uses another Minecraft or Forge class or method, add it to the stand-ins first.

## Status

The 0.1.1 jar was built with `tools/offline-build/build.sh`, with these checks:

- It compiles against the real TekTopia 1.1.0 jar.
- Every Minecraft reference maps to a runtime name in the official mappings, with a matching signature.
- All 104 Minecraft, Forge and library references it makes appear, with the same signatures, in real 1.12.2 mods (Twilight Forest, Millénaire, Mowzie's Mobs, Quark, TekTopia and others).

In 0.1.0 the chat messages showed raw translation keys (such as `commands.tekeco.balance`) instead of text, because the jar had no `pack.mcmeta`, so Minecraft didn't load `en_us.lang`. 0.1.1 adds it. Gameplay itself (payday timing, wage amounts, the wallet carry-over when villagers change profession) still needs testing in a world.

## Code map

| Path | Contents |
|---|---|
| `src/main/java/tekeco/TekEconomy.java` | Mod entry point |
| `src/main/java/tekeco/EcoConfig.java` | Config file |
| `src/main/java/tekeco/wallet/` | Villager wallet capability |
| `src/main/java/tekeco/treasury/` | Per-village treasury saved data |
| `src/main/java/tekeco/payroll/` | Daily payday logic and village lookups |
| `src/main/java/tekeco/event/EconomyEvents.java` | Attaching wallets, payday ticks, wallet carry-over, estates, wallet inspection |
| `src/main/java/tekeco/command/CommandTekEco.java` | `/tekeco` command |
| `docs/design.md` | Notes on TekTopia's internals and the economy design |
| `dist/` | Built mod jars, ready to install |
| `tools/offline-build/` | Build script for machines without access to Forge's servers |
