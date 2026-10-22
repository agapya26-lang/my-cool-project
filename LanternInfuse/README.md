# LanternInfuse
Custom Infuse-style effects for the **LanternLight+** team. Server by PixelForge.gg.
Built for Paper 26.1.2 (Java 25).

## Build
**Easiest (no install):** push this folder to a GitHub repo. The included Actions workflow
(`.github/workflows/build.yml`) builds the jar. Download it from the Actions tab > Artifacts.

**Locally:** install JDK 25 + Maven, then run `mvn package`. The jar lands in `target/`.

## Install
1. Remove the old `Infuse` plugin (its `/infuse`, `/trust` etc. commands would clash).
2. Drop `LanternInfuse-1.0.0.jar` in `plugins/` and restart.
3. Edit `plugins/LanternInfuse/config.yml` and `recipes.yml`, then `/infuse reload`.

## How it works
- 8 primary + 4 support effects. You hold one of each.
- Right-click an effect item to equip it. `/ldrain` and `/rdrain` turn it back into an item.
- Each effect has a passive and a **spark** ability. Press **F** (primary) or **Shift+F** (support),
  or use `/lspark` and `/rspark`. `/controls` switches between the two styles.
- Cooldowns/durations come from config.yml (cooldown / augmented-cooldown / duration, in seconds).
- The first craft of each effect is **Augmented** (half cooldown, stronger spark), then 3 normal crafts, then it can't be crafted again.
- Crafting is announced server-wide (with coordinates, configurable).
- `/trust <player>`: your abilities never hit trusted players, and Regen spark heals them.
- On death, equipped effects drop as items (config: DROP / KEEP / LOSE) and cooldowns reset.
- `/infuses [effect]` shows what an effect does and its recipe.

## Admin
`/infuse give <player> <effect> [aug]`, `/infuse clear <player>`, `/infuse resetcd <player>`,
`/infuse resetcrafts <effect|all>`, `/infuse reload` (permission `lanterninfuse.admin`).
