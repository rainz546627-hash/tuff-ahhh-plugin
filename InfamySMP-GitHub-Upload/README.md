# InfamySMP Paper Plugin

InfamySMP is a server-side Paper plugin for Minecraft 1.21.11. It adds PvP reputation, titles, Infamy rewards, tradeable Infamy Shards, admin controls, and a custom equipment set. Players do not install anything on their own computers.

## Requirements

- Paper server on Minecraft 1.21.11 (compatible 1.21.x versions may work)
- Java 21 on the server
- No client mods are required

This project includes GitHub Actions to compile the plugin using Java 21, so you do not need Java or Maven installed on your Windows computer to build it.

## Put these files in GitHub

1. Download this ZIP and choose **Extract All**.
2. Open your GitHub repository in a web browser.
3. Choose **Add file → Upload files**.
4. Upload the extracted project contents: `pom.xml`, `README.md`, `.gitignore`, the `.github` folder, and the `src` folder. Keep the folders in their shown arrangement. The `.github` folder may be hidden in File Explorer; turn on **View → Hidden items** if needed.
5. Commit the upload to the `main` branch. GitHub Actions will build the plugin.
6. In the repository, open **Actions**, open the newest successful **Build Paper plugin** run, and download the `InfamySMP-Paper-1.21.11` artifact.
7. Extract `InfamySMP.jar` from that downloaded ZIP.

## Install on the server

In your host's file manager, upload `InfamySMP.jar` into the server's `plugins` folder. Make sure the server software is Paper 1.21.11 and start the server. Use a full stop/start after installing; do not use `/reload`. The plugin creates `plugins/InfamySMP/config.yml` on first startup.

## Infamy rules

- Every player starts at 0. Scores are saved by UUID and clamped between -10 and +10.
- When Minecraft credits a player kill, the killer gains 1 Infamy and the victim loses 1. Environmental deaths do not change Infamy.
- A killer already at +10 stays at +10 and drops a genuine +1 Infamy Shard at the victim's death location for each credited kill. A killer at -10 can still gain Infamy from kills, rising to -9.
- Crossing into a different title band triggers a server announcement (enabled by default).
- Infamy Shards are special tagged Nether Stars. Right-click to deposit their value, up to +10. If the score cap leaves a remainder, it stays on the shard. Shards can be stored, traded, and dropped like items.
- `/infamy withdraw <amount>` exchanges positive Infamy for a shard. Withdrawal cannot make a score negative.

## Titles

| Score | Title |
| ---: | --- |
| +10 | ☠ Legend |
| +8 to +9 | ☠ Notorious |
| +6 to +7 | ⚔ Feared |
| +4 to +5 | ⚔ Dangerous |
| +2 to +3 | ⚔ Blooded |
| -1 to +1 | • Unknown |
| -2 to -3 | ☠ Fallen |
| -4 to -5 | ☠ Outcast |
| -6 to -7 | ☠ Hunted |
| -8 to -9 | ☠ Despised |
| -10 | ☠ Shamed |

Titles appear in chat, the player list, and overhead nametags where the server scoreboard allows it. Another plugin that manages the main scoreboard may affect overhead titles.

## Rewards

- +5 through +7: Strength I and Speed I
- +8 through +9: Strength I, Speed I, and Resistance I
- +10: Strength I, Speed I, Resistance I, and Haste I

Effects refresh on join, respawn, and Infamy changes. When refreshing, the plugin removes and reapplies these effect types, which can replace same-type effects from other sources.

## Custom Infamy equipment

Operators can give a complete set to an online player with `/infamy gear <player>`:

- **Infamy Maul** — mace; Density IV, Wind Burst II, Unbreaking III, Mending
- **Infamy Blade** — netherite sword; Sharpness V, Fire Aspect II, Looting III, Unbreaking III, Mending
- **Infamy Cleaver** — netherite axe; Sharpness V, Efficiency V, Unbreaking III, Mending
- **Infamy Crown** — netherite helmet; Protection IV, Respiration III, Aqua Affinity, Unbreaking III, Mending
- **Infamy Chestplate** — netherite chestplate; Protection IV, Thorns II, Unbreaking III, Mending
- **Infamy Greaves** — netherite leggings; Protection IV, Swift Sneak III, Unbreaking III, Mending
- **Infamy Warboots** — netherite boots; Protection IV, Feather Falling IV, Depth Strider III, Soul Speed III, Unbreaking III, Mending

If the recipient's inventory is full, leftover equipment drops at their feet. Gear is made from normal Minecraft equipment materials and can be equipped, stored, traded, or lost normally.

## Commands and permissions

- `/infamy` — view your Infamy and title
- `/infamy <player>` — look up a known player's score
- `/infamy leaderboard` — show the top players (10 by default)
- `/infamy withdraw <amount>` — withdraw positive Infamy as a shard
- `/infamy set <player> <score>` — set a known player's score from -10 to +10
- `/infamy reset <player>` — set a known player's score to 0
- `/infamy gear <online-player>` — give the full custom equipment set

`infamy.admin` defaults to server operators and is required for set, reset, and gear. `infamy.use` defaults to everyone and is required for withdrawal. Score viewing and leaderboard are available to everyone.

## Configuration

After the first start, adjust `plugins/InfamySMP/config.yml` to change `leaderboard-size`, `milestone-announcements`, or the three reward thresholds. Restart the server after editing.

## Build locally (optional)

With Java 21 and Maven installed, run `mvn clean package` in this project. The output is `target/InfamySMP.jar`.
