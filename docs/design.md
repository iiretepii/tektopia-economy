# Adding an economy to TekTopia: findings and design

Notes from analysing `tektopia-1.1.0.jar` (Forge 1.12.2, sha256 `331b47a7…a807f1`). The jar came from a public GitHub mirror (`psorus/mods`) because CurseForge was unreachable during the analysis, and was decompiled with Vineflower. Check the hash against the official CurseForge file.

## 1. What TekTopia already has

TekTopia already has a basic economy, but it only touches the player. Villagers never hold or spend money.

| Piece | Class | What it does |
|---|---|---|
| Export market | `economy.ItemEconomy`, `economy.ItemValue` | The wandering Merchant buys "villager items" (green-tagged goods made only from villager inputs) for emeralds. Each item has a `baseValue`, an `appearanceWeight` and a `requiredProfession`. Each sale is pushed onto a rolling `salesHistory` (30–50 entries, larger in bigger villages). Items that were sold get marked down by 10–30%. The value they lose is spread as a mark-up over all the other items. An item stops appearing after 8 sales in the current history. A trade only appears if the village has the profession that makes it. |
| Price list | `caps.VillageData.initEconomy()` | About 40 items are hardcoded, for example 64 logs = 4 emeralds, 4 diamonds = 12, diamond chestplate = 20, 8 books = 8. There is no config file. |
| Sinks | `items.ItemProfessionToken.getCost`, `ItemStructureToken.getCost` | The Tradesman (profession tokens) and Architect (structure tokens) sell for emeralds. Every 5 tokens sold raises all prices by 20% (`totalProfessionSales`), up to +1000%. |
| Merchant | `entities.EntityMerchant`, `MerchantScheduler` | Visits daily. The trade list is `getMerchantList(village, stallLevel)`, with 8 + 2×stall level slots. |
| Per-village save data | `caps.IVillageData` / `VillageData` | A Forge capability on the Town Hall's item frame. It holds the UUID, sales history and token counter. You get it with `village.getTownData()`. |

Other parts an economy can use:
- **Villagers** (`entities.EntityVillagerTek`) have `get/setHunger`, `get/setHappy`, `modifyHappy(int)`, `getSkill(ProfessionType)`, `getProfessionType()`, `getInventory()`, `getVillage()`, `isWorkTime()` and `isSleepingTime()`. The `tasks` field is public (`field_70714_bg`), so another mod can add AI tasks to it.
- **Village** (`Village`) has `getResidentCount()`, `getStructures(type)`, `getStorageCount(predicate)`, `purchaseFromMerchant(...)` (called on each player sale), `onStorageChange(...)`, and `addJob(TickJob)` for repeating server jobs.
- **Daily loop:** villagers deliver output to Storage (`EntityAIDeliverToStorage2.dropOffItems`), pick up food (`EntityAIRetrieveFromStorage2`), eat (`EntityAIEatFood`, food → hunger/happy table), and visit the Tavern at night (`EntityAITavernVisit`). Unhappy villagers work slowly, which gives money a lever to pull.
- **Hooks are limited:** TekTopia fires no Forge events of its own and has no API package or config. You can hook in through Forge events on its entities, Forge capabilities, or Mixins into its classes.

## 2. Recommended "basic economy" (v1)

The goal is a closed loop: **Treasury → wages → villager wallets → rent / food / tavern → Treasury**. Merchant exports bring emeralds in from outside, and the player can deposit or withdraw.

1. **Town Treasury.** Store one emerald balance per village in a `WorldSavedData`, keyed by `village.getTownData().getUUID()`. This avoids touching TekTopia's own save data.
2. **Villager wallet.** Attach a capability to every `EntityVillagerTek` with `AttachCapabilitiesEvent<Entity>`. It stores `coins` and `lastPaidDay`, and saves with the entity automatically.
3. **Payroll (no Mixin needed).** At dawn, a `WorldTickEvent` handler loops over villagers and pays `wage = base[profession] × (1 + skill/100)` from the Treasury. If the Treasury can't cover it, the villager gets `modifyHappy(-10)`, so unpaid workers slow down. Children, Nitwits and Nomads are not paid.
4. **Living costs.** In the same handler, take daily rent and food money from each wallet back into the Treasury. If a villager can't pay, apply a happiness penalty. A villager with savings above a threshold gets a small happiness bonus.
5. **Player interaction.** Add `/tekeco` commands (`balance`, `deposit <n>`, `withdraw <n>`, `report`). Shift-right-clicking a villager shows their wallet in chat (`PlayerInteractEvent.EntityInteract`). This avoids editing TekTopia's GUI.
6. **Config.** Wages, rent and food costs per profession go in a Forge `@Config` file so they can be tuned without code changes.

This version uses only public Forge events and TekTopia's public methods. That makes it the least fragile option, and it still works when TekTopia is updated.

## 3. v2 options (need Mixins)

Forge 1.12 doesn't ship Mixin, so these need the **MixinBooter** mod.

| Feature | Mixin target |
|---|---|
| Pay per item delivered instead of per day | `entities.ai.EntityAIDeliverToStorage2#dropOffItems` (or `storage.ItemDesireSet#deliverItems`) |
| Villagers pay for each meal (price by food value) | `entities.ai.EntityAIEatFood#startEat` / `func_75251_c` |
| Tavern costs coin (Bard gets a cut) | `entities.ai.EntityAITavernVisit` |
| A tax on Merchant sales goes to the Treasury | `Village#purchaseFromMerchant` (TAIL) |
| Pay for tokens from the Treasury, not the player's inventory | `EntityTradesman#func_70933_a`, `EntityArchitect` |
| Make Merchant base prices configurable | `caps.VillageData#initEconomy` (redirect `ItemEconomy#addItem`) |

## 4. Practical notes

- **License.** TekTopia is closed source. I couldn't confirm its exact license because CurseForge is blocked here. Ship this as a separate add-on that depends on `tektopia`, and don't redistribute a modified jar.
- **Build.** The jar is compiled against SRG names. In a ForgeGradle 2.3 / 1.12.2 workspace, put it in `libs/` and add it as `deobfCompile`, so that `func_*` names map to MCP names.
- **Dependencies.** Add `required-after:tektopia` to your `@Mod(dependencies=…)`. TekTopia needs Forge 14.23.5.2768+. CraftStudio is bundled inside it.

## 5. Implementation

Version 0.1.0 of this repository implements the v1 design. See `README.md` for how it works, and `src/main/java/tekeco/` for the code.
