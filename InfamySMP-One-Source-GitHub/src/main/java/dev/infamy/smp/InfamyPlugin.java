package dev.infamy.smp;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InfamyPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static final int MIN_INFAMY = -10;
    private static final int MAX_INFAMY = 10;
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private NamespacedKey shardMarkerKey;
    private NamespacedKey shardValueKey;
    private NamespacedKey gearMarkerKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        shardMarkerKey = new NamespacedKey(this, "infamy_shard");
        shardValueKey = new NamespacedKey(this, "shard_value");
        gearMarkerKey = new NamespacedKey(this, "infamy_gear");
        loadScores();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("infamy") != null) getCommand("infamy").setExecutor(this);
        for (Player player : Bukkit.getOnlinePlayers()) refreshPlayer(player);
    }

    @Override
    public void onDisable() {
        saveScores();
        for (Player player : Bukkit.getOnlinePlayers()) removeNametagTeam(player);
    }

    private void loadScores() {
        ConfigurationSection section = getConfig().getConfigurationSection("players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                scores.put(uuid, clamp(section.getInt(key + ".score", 0)));
                names.put(uuid, section.getString(key + ".name", uuid.toString()));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Skipping invalid player UUID in saved data: " + key);
            }
        }
    }

    private void saveScores() {
        getConfig().set("players", null);
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            String path = "players." + entry.getKey();
            getConfig().set(path + ".score", entry.getValue());
            getConfig().set(path + ".name", names.getOrDefault(entry.getKey(), entry.getKey().toString()));
        }
        saveConfig();
    }

    private int clamp(int value) { return Math.max(MIN_INFAMY, Math.min(MAX_INFAMY, value)); }
    private int score(UUID uuid) { return scores.getOrDefault(uuid, 0); }
    private String format(int value) { return value > 0 ? "+" + value : Integer.toString(value); }

    private void setScore(UUID uuid, String name, int value) {
        int old = score(uuid);
        int updated = clamp(value);
        scores.put(uuid, updated);
        names.put(uuid, name);
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) refreshPlayer(online);
        if (getConfig().getBoolean("milestone-announcements", true) && !title(old).equals(title(updated))) {
            Bukkit.broadcast(Component.text(name + " is now " + title(updated) + " (" + format(updated) + " Infamy).", NamedTextColor.GOLD));
        }
        saveScores();
    }

    private record PlayerRef(UUID uuid, String name) { }

    private PlayerRef findKnownPlayer(String search) {
        Player online = Bukkit.getPlayerExact(search);
        if (online != null) return new PlayerRef(online.getUniqueId(), online.getName());
        for (Map.Entry<UUID, String> entry : names.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(search)) return new PlayerRef(entry.getKey(), entry.getValue());
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(search);
        if (cached != null && cached.getName() != null) return new PlayerRef(cached.getUniqueId(), cached.getName());
        return null;
    }

    private String title(int value) {
        if (value == 10) return "☠ Legend";
        if (value >= 8) return "☠ Notorious";
        if (value >= 6) return "⚔ Feared";
        if (value >= 4) return "⚔ Dangerous";
        if (value >= 2) return "⚔ Blooded";
        if (value >= -1) return "• Unknown";
        if (value >= -3) return "☠ Fallen";
        if (value >= -5) return "☠ Outcast";
        if (value >= -7) return "☠ Hunted";
        if (value >= -9) return "☠ Despised";
        return "☠ Shamed";
    }

    private void refreshPlayer(Player player) {
        int value = score(player.getUniqueId());
        String display = title(value) + " " + player.getName();
        player.playerListName(Component.text(display, NamedTextColor.WHITE));
        player.displayName(Component.text(display, NamedTextColor.WHITE));

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "inf" + player.getUniqueId().toString().replace("-", "").substring(0, 13);
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        for (String entry : new ArrayList<>(team.getEntries())) {
            if (!entry.equals(player.getName())) team.removeEntry(entry);
        }
        team.prefix(Component.text(title(value) + " ", NamedTextColor.GOLD));
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());

        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.HASTE);
        if (value >= getConfig().getInt("effects.feared-at", 5)) {
            applyPermanent(player, PotionEffectType.STRENGTH);
            applyPermanent(player, PotionEffectType.SPEED);
        }
        if (value >= getConfig().getInt("effects.notorious-at", 8)) applyPermanent(player, PotionEffectType.RESISTANCE);
        if (value >= getConfig().getInt("effects.legend-at", 10)) applyPermanent(player, PotionEffectType.HASTE);
    }

    private void applyPermanent(Player player, PotionEffectType type) {
        player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, 0, true, false, true));
    }

    private void removeNametagTeam(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "inf" + player.getUniqueId().toString().replace("-", "").substring(0, 13);
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) team.unregister();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        scores.putIfAbsent(player.getUniqueId(), 0);
        names.put(player.getUniqueId(), player.getName());
        refreshPlayer(player);
        saveScores();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { removeNametagTeam(event.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(this, () -> refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        int killerScore = score(killer.getUniqueId());
        if (killerScore >= MAX_INFAMY) {
            event.getDrops().add(createShard(1));
        } else {
            setScore(killer.getUniqueId(), killer.getName(), killerScore + 1);
        }
        setScore(victim.getUniqueId(), victim.getName(), score(victim.getUniqueId()) - 1);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) ->
                sourceDisplayName.append(Component.text(": ", NamedTextColor.WHITE)).append(message));
    }

    private ItemStack createShard(int value) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("🩸 Infamy Shard", NamedTextColor.RED, TextDecoration.BOLD));
        meta.lore(List.of(Component.text("Value: " + value, NamedTextColor.GRAY),
                Component.text("Right-click to deposit", NamedTextColor.DARK_GRAY)));
        meta.getPersistentDataContainer().set(shardMarkerKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(shardValueKey, PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return item;
    }

    private int shardValue(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        var data = item.getItemMeta().getPersistentDataContainer();
        if (!data.has(shardMarkerKey, PersistentDataType.BYTE)) return 0;
        Integer value = data.get(shardValueKey, PersistentDataType.INTEGER);
        return value == null ? 0 : Math.max(0, value);
    }

    @EventHandler
    public void onShardUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        int shard = shardValue(item);
        if (shard <= 0) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        int deposited = Math.min(shard, MAX_INFAMY - score(player.getUniqueId()));
        if (deposited <= 0) {
            player.sendMessage(Component.text("Your Infamy is already at +10; the shard was not changed.", NamedTextColor.RED));
            return;
        }
        int remaining = shard - deposited;
        if (remaining == 0) item.setAmount(item.getAmount() - 1);
        else item.setItemMeta(createShard(remaining).getItemMeta());
        setScore(player.getUniqueId(), player.getName(), score(player.getUniqueId()) + deposited);
        player.sendMessage(Component.text("Deposited " + deposited + " Infamy. Current score: " + format(score(player.getUniqueId())) + ".", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
    }

    private ItemStack customItem(Material material, String name, String description, Enchantment[] enchants, int[] levels) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.DARK_RED, TextDecoration.BOLD));
        meta.lore(List.of(Component.text(description, NamedTextColor.GRAY),
                Component.text("Infamy SMP custom equipment", NamedTextColor.DARK_GRAY)));
        meta.getPersistentDataContainer().set(gearMarkerKey, PersistentDataType.BYTE, (byte) 1);
        for (int i = 0; i < enchants.length; i++) meta.addEnchant(enchants[i], levels[i], true);
        item.setItemMeta(meta);
        return item;
    }

    private List<ItemStack> weapons() {
        return List.of(
                customItem(Material.MACE, "☠ Infamy Maul", "A crushing weapon carried by Legends.",
                        new Enchantment[]{Enchantment.DENSITY, Enchantment.WIND_BURST, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{4, 2, 3, 1}),
                customItem(Material.NETHERITE_SWORD, "⚔ Infamy Blade", "A blade forged in the name of Infamy.",
                        new Enchantment[]{Enchantment.SHARPNESS, Enchantment.FIRE_ASPECT, Enchantment.LOOTING, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{5, 2, 3, 3, 1}),
                customItem(Material.NETHERITE_AXE, "⚔ Infamy Cleaver", "A heavy weapon for feared fighters.",
                        new Enchantment[]{Enchantment.SHARPNESS, Enchantment.EFFICIENCY, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{5, 5, 3, 1}),
                customItem(Material.NETHERITE_PICKAXE, "⛏ Infamy Pickaxe", "A mining tool worthy of Infamy.",
                        new Enchantment[]{Enchantment.EFFICIENCY, Enchantment.FORTUNE, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{5, 3, 3, 1})
        );
    }

    private List<ItemStack> armor() {
        return List.of(
                customItem(Material.NETHERITE_HELMET, "☠ Infamy Crown", "Part of the Infamy armor set.",
                        new Enchantment[]{Enchantment.PROTECTION, Enchantment.RESPIRATION, Enchantment.AQUA_AFFINITY, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{4, 3, 1, 3, 1}),
                customItem(Material.NETHERITE_CHESTPLATE, "☠ Infamy Chestplate", "Part of the Infamy armor set.",
                        new Enchantment[]{Enchantment.PROTECTION, Enchantment.THORNS, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{4, 2, 3, 1}),
                customItem(Material.NETHERITE_LEGGINGS, "☠ Infamy Greaves", "Part of the Infamy armor set.",
                        new Enchantment[]{Enchantment.PROTECTION, Enchantment.SWIFT_SNEAK, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{4, 3, 3, 1}),
                customItem(Material.NETHERITE_BOOTS, "☠ Infamy Warboots", "Part of the Infamy armor set.",
                        new Enchantment[]{Enchantment.PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.DEPTH_STRIDER, Enchantment.SOUL_SPEED, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{4, 4, 3, 3, 3, 1})
        );
    }

    private void giveItems(CommandSender sender, String targetName, List<ItemStack> items, String label) {
        if (!sender.hasPermission("infamy.admin")) {
            sender.sendMessage(Component.text("You need the infamy.admin permission to give custom equipment.", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("That player must be online to receive the equipment.", NamedTextColor.RED));
            return;
        }
        Map<Integer, ItemStack> leftovers = target.getInventory().addItem(items.toArray(ItemStack[]::new));
        leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        target.sendMessage(Component.text("You received the Infamy " + label + "!", NamedTextColor.DARK_RED, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Gave Infamy " + label + " to " + target.getName() + ".", NamedTextColor.GREEN));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Use /infamy <player> or /infamy leaderboard from console.");
                return true;
            }
            showScore(sender, player.getUniqueId(), player.getName());
            return true;
        }
        if (args[0].equalsIgnoreCase("weapons")) {
            if (args.length > 2 || (args.length == 2 && !sender.hasPermission("infamy.admin"))) {
                sender.sendMessage(ChatColor.RED + "Usage: /infamy weapons [online-player]");
                return true;
            }
            String target = args.length == 2 ? args[1] : sender instanceof Player p ? p.getName() : null;
            if (target == null) { sender.sendMessage(ChatColor.RED + "From console, specify an online player."); return true; }
            giveItems(sender, target, weapons(), "weapons");
            return true;
        }
        if (args[0].equalsIgnoreCase("armor")) {
            if (args.length > 2 || (args.length == 2 && !sender.hasPermission("infamy.admin"))) {
                sender.sendMessage(ChatColor.RED + "Usage: /infamy armor [online-player]");
                return true;
            }
            String target = args.length == 2 ? args[1] : sender instanceof Player p ? p.getName() : null;
            if (target == null) { sender.sendMessage(ChatColor.RED + "From console, specify an online player."); return true; }
            giveItems(sender, target, armor(), "armor set");
            return true;
        }
        if (args[0].equalsIgnoreCase("gear")) {
            if (args.length > 2 || (args.length == 2 && !sender.hasPermission("infamy.admin"))) {
                sender.sendMessage(ChatColor.RED + "Usage: /infamy gear [online-player]");
                return true;
            }
            String target = args.length == 2 ? args[1] : sender instanceof Player p ? p.getName() : null;
            if (target == null) { sender.sendMessage(ChatColor.RED + "From console, specify an online player."); return true; }
            List<ItemStack> all = new ArrayList<>(weapons());
            all.addAll(armor());
            giveItems(sender, target, all, "weapons and armor");
            return true;
        }
        if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset")) {
            manageInfamy(sender, args);
            return true;
        }
        if (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("top")) {
            showLeaderboard(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("withdraw")) {
            withdraw(sender, args);
            return true;
        }
        PlayerRef target = findKnownPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "No known player named " + args[0] + ".");
            return true;
        }
        showScore(sender, target.uuid(), target.name());
        return true;
    }

    private void manageInfamy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("infamy.admin")) {
            sender.sendMessage(Component.text("You need the infamy.admin permission to change player Infamy.", NamedTextColor.RED));
            return;
        }
        boolean reset = args[0].equalsIgnoreCase("reset");
        if ((reset && args.length != 2) || (!reset && args.length != 3)) {
            sender.sendMessage(Component.text(reset ? "Usage: /infamy reset <player>" : "Usage: /infamy set <player> <-10..10>", NamedTextColor.RED));
            return;
        }
        PlayerRef target = findKnownPlayer(args[1]);
        if (target == null) { sender.sendMessage(Component.text("No known player named " + args[1] + ".", NamedTextColor.RED)); return; }
        int value = 0;
        if (!reset) {
            try { value = Integer.parseInt(args[2]); }
            catch (NumberFormatException ex) { sender.sendMessage(Component.text("Infamy must be a whole number from -10 to 10.", NamedTextColor.RED)); return; }
            if (value < MIN_INFAMY || value > MAX_INFAMY) {
                sender.sendMessage(Component.text("Infamy must be between -10 and 10.", NamedTextColor.RED));
                return;
            }
        }
        setScore(target.uuid(), target.name(), value);
        sender.sendMessage(Component.text("Set " + target.name() + "'s Infamy to " + format(value) + ".", NamedTextColor.GREEN));
    }

    private void showScore(CommandSender sender, UUID uuid, String name) {
        int value = score(uuid);
        sender.sendMessage(Component.text(name + " — " + title(value) + " — " + format(value) + " Infamy", NamedTextColor.GOLD));
    }

    private void showLeaderboard(CommandSender sender) {
        int limit = Math.max(1, Math.min(50, getConfig().getInt("leaderboard-size", 10)));
        sender.sendMessage(Component.text("—— Infamy Leaderboard ——", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<Map.Entry<UUID, Integer>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> names.getOrDefault(e.getKey(), ""), String.CASE_INSENSITIVE_ORDER))
                .limit(limit).toList();
        if (sorted.isEmpty()) sender.sendMessage(Component.text("No player scores recorded yet.", NamedTextColor.GRAY));
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<UUID, Integer> entry = sorted.get(i);
            sender.sendMessage(Component.text((i + 1) + ". " + names.getOrDefault(entry.getKey(), "Unknown")
                    + " — " + format(entry.getValue()) + " — " + title(entry.getValue()), NamedTextColor.WHITE));
        }
    }

    private void withdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(ChatColor.RED + "Only players can withdraw Infamy."); return; }
        if (!player.hasPermission("infamy.use")) { player.sendMessage(ChatColor.RED + "You don't have permission to withdraw Infamy."); return; }
        if (args.length != 2) { player.sendMessage(ChatColor.RED + "Usage: /infamy withdraw <amount>"); return; }
        final int amount;
        try { amount = Integer.parseInt(args[1]); }
        catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Amount must be a whole number."); return; }
        int available = score(player.getUniqueId());
        if (amount <= 0) { player.sendMessage(ChatColor.RED + "Amount must be at least 1."); return; }
        if (amount > available) { player.sendMessage(ChatColor.RED + "You can only withdraw up to your current positive Infamy (" + available + ")."); return; }
        if (player.getInventory().firstEmpty() == -1) { player.sendMessage(ChatColor.RED + "Make room in your inventory first."); return; }
        player.getInventory().addItem(createShard(amount));
        setScore(player.getUniqueId(), player.getName(), available - amount);
        player.sendMessage(Component.text("Withdrew " + amount + " Infamy as a shard. Current score: " + format(score(player.getUniqueId())) + ".", NamedTextColor.GREEN));
    }
}
