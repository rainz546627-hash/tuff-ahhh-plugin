# InfamySMP

Paper plugin targeting Java 21. All gameplay code is in one Java file: `src/main/java/dev/infamy/smp/InfamyPlugin.java`.

## Features
- Infamy is clamped from -10 to +10. PvP kills award the killer +1 and subtract 1 from the player killed. Non-player-caused deaths do not change Infamy. A killer at +10 gets a physical +1 Infamy Shard drop; a player at -10 can still gain Infamy from kills.
- Infamy titles appear in chat, tab, nametags, and `/infamy gui`. Score milestones are announced when title bands change.
- Threshold effects: +5 Strength I and Speed I, +8 Resistance I, +10 Haste I. Effects update on join, respawn, and score changes.
- Persistent score storage; secure PDC-tagged Infamy Shards can be withdrawn, traded, dropped, and deposited by right-click.
- Four unbreakable custom items: Infamy Blade (critical-hit damage boost), Infamy Cleaver (Efficiency VII and connected-tree felling), Infamy Pickaxe (Fortune V and Efficiency VII), and Infamy Maul (right-click upward boost/dash, five-second cooldown).
- No custom armor.

## Commands
- `/infamy` or `/infamy <player>` — show a score.
- `/infamy gui` — rankings GUI.
- `/infamy leaderboard` — leaderboard in chat.
- `/infamy withdraw <amount>` — turn positive score into a shard.
- `/infamy weapons` — give yourself all four custom weapons; operators/admins only.
- `/infamy weapons <online-player>` — give weapons to an online player; operators/admins only.
- `/infamy set <player> <-10..10>` and `/infamy reset <player>` — operator/admin score commands.

`infamy.admin` defaults to operators. `infamy.use` defaults to everyone.

## Build and install
1. Create a GitHub repository and upload the contents of this project at the repository top level (`pom.xml`, `.github`, and `src`).
2. Commit. The **Build Paper plugin** action builds the plugin. You can also start it from the Actions tab.
3. Download the `InfamySMP-Paper-1.21.11` artifact from the completed run and extract `InfamySMP.jar`.
4. Upload the JAR into your server's `plugins` folder and restart the server.

Use a compatible Paper server and Java 21. Verify the GitHub Actions run succeeds before installing the JAR.
