# InfamySMP Paper Plugin

This is a Paper server plugin for Minecraft 1.21.11 and Java 21. The gameplay code is in one Java file: `src/main/java/dev/infamy/smp/InfamyPlugin.java`. The Maven file, plugin descriptor, configuration, and GitHub workflow are required build files.

## Build on GitHub

1. Create a new empty GitHub repository.
2. Extract this project ZIP. Upload its contents directly to the repository's top level, including `.github/workflows/build.yml`.
3. Commit the files to `main`.
4. Open **Actions**, wait for **Build Paper plugin** to succeed, and download the `InfamySMP-Paper-1.21.11` artifact.
5. Extract `InfamySMP.jar` from that artifact ZIP.

## Install

Upload `InfamySMP.jar` to your Paper server's `plugins` folder and restart the server. No client-side mod is needed.

## Infamy system

- Players start at 0. Infamy stays within -10 to +10 and persists across restarts.
- Credited PvP kills give the killer +1 and the player killed -1. Other deaths do not affect scores.
- At +10, each further credited kill drops a real tagged +1 Infamy Shard at the death location instead of adding to the capped score. A player at -10 can still earn Infamy by killing another player.
- Titles: +10 Legend; +8..9 Notorious; +6..7 Feared; +4..5 Dangerous; +2..3 Blooded; -1..+1 Unknown; -2..-3 Fallen; -4..-5 Outcast; -6..-7 Hunted; -8..-9 Despised; -10 Shamed.
- Titles appear in chat, tab, and overhead nametags where scoreboard compatibility permits. Crossing title bands announces the change.
- At +5: Strength I and Speed I. At +8: also Resistance I. At +10: also Haste I. Effects recalculate on join, respawn, and Infamy changes.
- Withdraw positive points using `/infamy withdraw <amount>`. Shards can be traded/stored and right-clicked to deposit up to +10. Shards use persistent item data.

## Equipment commands

These equipment commands require `infamy.admin`, which defaults to operators. An operator can omit the player name to give items to themselves; from console or to target someone else, use an online player's name.

- `/infamy weapons` — Infamy Blade (netherite sword), Infamy Cleaver (netherite axe), Infamy Pickaxe (netherite pickaxe), and Infamy Maul (mace).
- `/infamy armor` — Infamy Crown, Infamy Chestplate, Infamy Greaves, and Infamy Warboots (full enchanted netherite set).
- `/infamy gear` — all weapons and armor together.

## Other commands

- `/infamy` — view your score/title; `/infamy <player>` — look up a known player's score.
- `/infamy leaderboard` — top scores.
- `/infamy set <player> <-10..10>` and `/infamy reset <player>` — operator/admin score controls.

`infamy.use` defaults to everyone and permits withdrawals. `infamy.admin` defaults to operators and permits score administration and equipment grants.
