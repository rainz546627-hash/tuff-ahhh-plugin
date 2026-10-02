# InfamySMP

A Paper plugin targeting Minecraft 1.21.11 and Java 21. All plugin behavior is in one Java file: `src/main/java/dev/infamy/smp/InfamyPlugin.java`. The Maven file, Paper descriptor, config, and GitHub workflow are needed to build and install it.

## Easy controls (no client mod required)

Paper server plugins cannot add new keyboard bindings to a player's client. This plugin uses familiar built-in mouse actions and checks the custom item in the player's **main hand** before activating an ability:

- **Infamy Blade:** attack with the left mouse button while holding the custom sword; its hit gets the automatic critical damage boost.
- **Infamy Cleaver:** break a log with the left mouse button while holding the custom axe; connected tree trunks are felled.
- **Infamy Pickaxe:** mine normally with the left mouse button while holding the custom pickaxe; Fortune V and Efficiency VII are applied by the item.
- **Infamy Maul:** right-click air or a block while holding the custom mace to boost upward and dash forward. Five-second cooldown.

Ordinary weapons never activate custom abilities. Each ability checks the custom item's persistent ID on the held main-hand weapon.

## Infamy features

- Score from -10 to +10; PvP kill gives the killer +1 and the player killed -1. Ordinary deaths do not change scores. At +10 a killer drops a +1 shard instead; a -10 player can still gain Infamy.
- Titles: Legend, Notorious, Feared, Dangerous, Blooded, Unknown, Fallen, Outcast, Hunted, Despised, and Shamed, shown in chat, tab, nametags, and `/infamy gui`.
- Effects: at +5 Strength I and Speed I; at +8 Resistance I; at +10 Haste I. Refreshed on join, respawn, and score updates.
- Persistent Infamy scores, milestone announcements, leaderboard, admin set/reset, withdrawals, and PDC-identified shards that can be deposited by right-click.
- Four unbreakable weapons: sword, axe, pickaxe, and mace. No custom armor.

## Commands

- `/infamy` or `/infamy <player>` — view score and title.
- `/infamy gui` — rankings GUI.
- `/infamy leaderboard` — leaderboard in chat.
- `/infamy withdraw <amount>` — withdraw positive Infamy as a shard.
- `/infamy weapons` — give yourself all four weapons; operator/admin only.
- `/infamy weapons <online-player>` — give weapons to a player; operator/admin only.
- `/infamy set <player> <-10..10>` and `/infamy reset <player>` — admin score commands.

`infamy.admin` defaults to operators. `infamy.use` defaults to everyone.

## GitHub build and install

1. Extract this ZIP. Upload its **contents** to the top level of your GitHub repository. If you upload the containing folder too, the workflow searches for `pom.xml` inside it.
2. Commit to `main` or `master`, or manually start **Build Paper plugin** in the Actions tab.
3. Download the `InfamySMP-Paper` artifact from a successful run and extract `InfamySMP.jar`.
4. Put the JAR in your Paper server's `plugins` folder and restart the server.

The Actions workflow installs Java 21, locates `pom.xml` anywhere in the repository, runs Maven, and uploads the built JAR. The server needs Paper compatible with Minecraft 1.21.11 and Java 21.
