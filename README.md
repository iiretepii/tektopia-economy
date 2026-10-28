# TekTopia Economy

A Forge 1.12.2 add-on for [TekTopia](https://www.curseforge.com/minecraft/mc-mods/tektopia) that gives each village a treasury and each villager a wallet. Every morning the treasury pays villagers a wage, and they pay rent and food back to it. Villagers who go unpaid, or can't cover their living costs, lose happiness, and TekTopia already makes unhappy villagers work slowly.

New emeralds come in two ways: the Town Hall's Tradesman sells surplus village goods to TekTopia's wandering Merchant for the treasury, and you can deposit emeralds yourself.

## How it works

| Piece | What it does |
|---|---|
| Treasury | One emerald balance per village (needs a Town Hall). Saved with the world. |
| Wallet | Each TekTopia villager carries emeralds. Saved with the villager, and carried over when TekTopia replaces the villager (profession change, child growing up). |
| Payday | Once per Minecraft day at `paydayTime`, each loaded villager on the payroll is paid `wage × (1 + skillBonus × skill/100)` from the treasury, then pays `rent + foodCost` back. |
| Happiness | Unpaid: `-unpaidPenalty`. Can't pay living costs: `-brokePenalty`. Savings at or above `comfortSavings`: `+comfortBonus`. |
| Estates | When a villager dies, their savings go to the treasury (`estateToTreasury`). |
| Tavern tabs | Villagers on the payroll pay `tavernTab` (1 emerald) for an evening at the Tavern. A Bard playing there gets `bardShare` (25%) and the treasury the rest. Villagers who can't pay are turned away. See below. |
| Tradesman sales | Once a day, when the Merchant shows up, the Tradesman sells items on the village's sell list from Storage to the Merchant and puts the emeralds in the treasury. See below. |

Children, nitwits, nomads and TekTopia's vendors (Merchant, Tradesman, Architect) aren't on the payroll. A villager in an unloaded chunk is settled when it loads again. Missed days are not back-paid, and changing the clock with `/time` never pays anyone twice. Paydays follow the world clock, so they stop if `doDaylightCycle` is off.

Default daily wages, in emeralds: blacksmith and enchanter 5; chef, cleric, druid, guard, miner and teacher 4; bard, butcher, farmer, lumberjack and rancher 3; captain 6.

### Tradesman sales

When the wandering Merchant arrives, the Tradesman (who lives in the Town Hall) goes through the Merchant's current offers once, the same offers you'd see:

- It only sells items on the village's **sell list**. By default that's `minecraft:diamond` and `minecraft:redstone` (`tradesmanItems`).
- It only sells villager-made (green) items from Storage, as the Merchant requires.
- It always leaves a **reserve** in Storage (`tradesmanReserves`): 4 diamonds (the Blacksmith uses them), 0 redstone, and `defaultReserve` (64) of anything else.
- Each offer can be used up to 5 times, the same limit you have. Each sale lowers the Merchant's prices like your own sales do, and uses up offers you could otherwise take.
- All the emeralds go to the treasury. `/tekeco report` shows the last sale.

Items are written as `modid:name` (any variant) or `modid:name:meta` (one variant), for example `minecraft:dye:4` for lapis or `minecraft:log:2` for birch logs. `*` means anything the Merchant buys, and an empty list means nothing. Set `tradesmanSells = false` to turn Tradesman sales off.

### Tavern tabs

In TekTopia, off-duty villagers spend evenings in the Tavern, where they cheer with a beer and gain happiness, more so when a Bard is performing. The beer itself is only an animation, so this mod charges for the evening rather than per beer:

- A villager on the payroll who is inside a Tavern outside work hours pays `tavernTab` (default 1 emerald) once per day. Children, nitwits, nomads, vendors and the Bard don't pay.
- If a Bard is in the same Tavern, it gets `bardShare` (default 25%) of the tab and the treasury keeps the rest. Shares smaller than an emerald build up and are paid out in whole emeralds.
- A villager who can't pay finishes the evening but stops going to the Tavern until they have the tab again. This uses TekTopia's own "visit tavern" setting in the villager's screen; the mod only switches it back on for villagers it switched off. With `tavernTurnsAwayBroke = false` they lose `tavernBrokePenalty` (5) happiness instead.
- `tavernTab = 0` makes the Tavern free.

## Playing

Stand inside a village and use:

- `/tekeco balance`: show the treasury balance.
- `/tekeco deposit <amount|all>`: move emeralds from your inventory into the treasury.
- `/tekeco withdraw <amount>`: take emeralds out (operators only if `withdrawRequiresOp` is on).
- `/tekeco report`: treasury balance, total villager savings, the last payday's wages, upkeep and shortfalls, the Tradesman's last sale, and the last Tavern night. Income shows in green and problems in red; hover over a line for an explanation. Click `[Deposit]`, `[Sell list]` or `[Refresh]` at the bottom as shortcuts.
- `/tekeco sell`: show what this village's Tradesman sells.
- `/tekeco sell add <item>` / `/tekeco sell remove <item>`: change this village's sell list (operators only if `sellListRequiresOp` is on). The first change copies the config list into the village, which keeps its own list from then on.
- `/tekeco sell reset`: go back to the config list.
- `/tekeco morale`: why villagers are unhappy. See below.

Sneak and right-click a villager with an empty hand to see their wallet, and whether they've been turned away from the Tavern.

### Sign boards

Place a sign anywhere in a village and write one of these on its first line: `[Treasury]`, `[Payday]`, `[Tradesman]`, `[Tavern]`, `[Morale]`, `[Hunger]` or `[Skills]` (any capitalization). Within a few seconds the mod fills in the other three lines and keeps them up to date. A row of them on a wall makes a status board:

```
 [Treasury]        [Payday]          [Tradesman]       [Tavern]
 120 emeralds      Yesterday         Today             Last night
 In 46 Out 41      Paid 14 of 16     Sold 3 trades     Tabs 6 Bard +1
 Savings 85        Broke: 0          +26 emeralds      Turned away: 1
```

- **Treasury:** the balance; emeralds in today (rent and food, the treasury's Tavern share, Tradesman sales) and out (wages); and villager savings, counting villagers in loaded chunks.
- **Payday:** when the last payday was; villagers paid out of those on the payroll; villagers who couldn't afford rent and food.
- **Tradesman:** when the Tradesman last sold, the trades made and the emeralds earned.
- **Tavern:** the last Tavern night, tabs paid with the Bard's cut, and villagers turned away. With more than 99 tabs in a night, the Bard's cut is left off to fit; it's still in `/tekeco report`.

The village boards show how villagers are doing:

```
 [Morale]          [Hunger]          [Skills]          [Skills]
 Happy avg 72      Fed avg 64        All jobs          Miner
 Sulking: 3/16     Hungry: 2/16      Skill avg 41      Avg 41 Top 88
 Lowest: 12        Starving: 0       Intel avg 34      Count: 4
```

- **Morale:** average happiness; villagers sulking (below 20, when TekTopia makes them walk slowly and barely work) out of all; the lowest happiness.
- **Hunger:** average hunger (100 is full); villagers hungry (below 30, when they go looking for food) out of all; villagers starving (at 0).
- **Skills:** leave line 2 blank (it becomes `All jobs`) for the average skill in each villager's own profession and the average intelligence. Or write a profession on line 2, such as `Miner` or `Lumberjack`, for that profession's average and best skill and how many villagers have it.

These count the village's residents in loaded chunks (workers, children and nitwits), not vendors or visiting nomads; Skills counts only villagers with a profession. Averages are dark green from 60, black from 30 and dark red below 30. These boards work before the village has a Town Hall.

Every line fits a 1.12 sign (90 pixels) at its widest value. Numbers over 9,999 show as `12k` or `1.2M`; the balance and savings show in full up to 99,999, and the sulking and hungry counts switch to `1k` above 999. Signs use dark colors so they read on wood: a dark blue title, dark green for income, dark red for problems. Anyone can set up a board sign. A sign outside a village says so, and so does one in a village without a Town Hall. Set `signBoards = false` to turn this off. Widths assume the default font; a resource pack or Minecraft's "Force Unicode Font" option can change them.

### Morale report

`/tekeco morale` shows the average happiness and how it changed since the last payday, how many villagers are sulking, every likely cause of unhappiness (biggest first), and the five unhappiest villagers with what's wrong for each. Hover over a cause for what it costs and how to fix it, and over a villager for where they are.

TekTopia doesn't record why happiness changes, so causes are worked out from its rules:

| Cause | TekTopia's effect |
|---|---|
| Villager deaths since yesterday | -25 to -40 for every villager with a bed (twice that for a child) |
| Injuries today | -8 each (Guards and Clerics don't mind) |
| Nitwits | -1 every minute awake, and only a third of any boost |
| No bed | Miss the +10 to 30 from waking up rested |
| Hungry or starving | Eating is one of the main boosts |
| Workers without their tool | -5 each time they craft one (hoe, axe, pickaxe or weapon) |
| Leashed | Slowly lose happiness |
| Unpaid, or couldn't afford rent and food, at the last payday | `unpaidPenalty`, `brokePenalty` |
| Turned away from the Tavern | Miss Tavern evenings |
| Children at school | Sometimes sulk, -10 |
| Crowded buildings or low ceilings | Up to -5 each time a villager enters |
| No Tavern, no Bard, no Library | Fewer ways to get happier |

Causes are sorted by a rough estimate: villagers affected times the usual penalty. Everyday work, which slowly lowers everyone's happiness, isn't listed, and neither is being scared by monsters; neither can be pinned on anything. Only villagers in loaded chunks are counted. Deaths, injuries and the trend need a Town Hall; the rest works without one.

Players standing in a village see a short notice above the hotbar at payday (in red if anyone went unpaid) and when the Tradesman sells to the Merchant. Set `actionBarNotices = false` to turn these off.

All numbers are in `config/tekeco.cfg`, and can also be changed in-game from the Mods menu.

## Installing

The built jar is in [`dist/`](dist/). Put `tektopia-economy-1.12.2-<version>.jar` in your `mods` folder next to TekTopia 1.1.0. It needs Forge 14.23.5.2768 or later, the same as TekTopia. Install it on the server and on every client.

## Building

TekTopia is closed source and isn't on any Maven repository, so you supply the jar yourself: download `tektopia-1.1.0.jar` from CurseForge and put it in `libs/`. It is git-ignored; don't commit it. The mod jar ends up in `build/libs/` with either method below.

**With ForgeGradle (normal, not yet tried).** Install JDK 8 (ForgeGradle 2.3 doesn't run on newer Java) and run `./gradlew build`. `./gradlew runClient` starts a dev client with TekTopia copied into `run/mods`.

**Without ForgeGradle.** `tools/offline-build/build.sh` builds the same jar on machines that can't reach Forge's or Mojang's servers. It needs JDK 11 or newer, `git` and `curl`, and downloads only from Maven Central and GitHub. It compiles against small stand-ins for the Minecraft and Forge classes the mod uses (`tools/offline-build/stubs/`), then renames Minecraft members to their runtime (SRG) names using the official MCP 1.12 and `stable_39` mappings, as ForgeGradle's reobfuscation does. It stops if any Minecraft reference has no mapping. Pass a folder of other Forge 1.12.2 mod jars as the first argument to also check every Minecraft, Forge and library reference against what those mods use. A vanilla Minecraft member that none of them uses still passes if the official mappings declare it; Forge and library references must appear in a mod. If you add code that uses another Minecraft or Forge class or method, add it to the stand-ins first.

## Status

The 0.7.1 jar was built with `tools/offline-build/build.sh`, with these checks:

- It compiles against the real TekTopia 1.1.0 jar.
- Every Minecraft reference maps to a runtime name in the official mappings, with a matching signature.
- 167 of its 171 Minecraft, Forge and library references appear, with the same signatures, in real 1.12.2 mods (Twilight Forest, Millénaire, Mowzie's Mobs, Quark, TekTopia and others). The other 4 are vanilla Minecraft members no sampled mod uses (two click-event actions, `Style.setBold` and `World.loadedTileEntityList`), which the official mappings declare with the same signatures.

In 0.1.0 the chat messages showed raw translation keys (such as `commands.tekeco.balance`) instead of text, because the jar had no `pack.mcmeta`, so Minecraft didn't load `en_us.lang`. 0.1.1 adds it. Gameplay itself (payday timing, wage amounts, the wallet carry-over when villagers change profession, Tradesman sales, Tavern tabs, sign boards, the morale report) still needs testing in a world.

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
| `src/main/java/tekeco/export/` | Tradesman sales and item matching |
| `src/main/java/tekeco/tavern/` | Tavern tabs |
| `src/main/java/tekeco/text/` | Styled chat text and action-bar notices |
| `src/main/java/tekeco/board/` | Sign boards, sign text sizing and village statistics |
| `src/main/java/tekeco/morale/` | The `/tekeco morale` report |
| `docs/design.md` | Notes on TekTopia's internals and the economy design |
| `dist/` | Built mod jars, ready to install |
| `tools/offline-build/` | Build script for machines without access to Forge's servers |
