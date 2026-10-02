package dev.infamy.smp;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InfamyPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static final int MIN = -10, MAX = 10, TREE_LIMIT = 64, TREE_RADIUS = 7;
    private static final long MACE_COOLDOWN = 5000L;
    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private NamespacedKey shardKey, shardValueKey, weaponKey;

    @Override public void onEnable() {
        saveDefaultConfig();
        shardKey = new NamespacedKey(this, "infamy_shard");
        shardValueKey = new NamespacedKey(this, "shard_value");
        weaponKey = new NamespacedKey(this, "infamy_weapon");
        loadScores();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("infamy") != null) getCommand("infamy").setExecutor(this);
        for (Player p : Bukkit.getOnlinePlayers()) refresh(p);
    }
    @Override public void onDisable() { saveScores(); for (Player p : Bukkit.getOnlinePlayers()) removeTeam(p); }

    private int clamp(int n) { return Math.max(MIN, Math.min(MAX, n)); }
    private int score(UUID id) { return scores.getOrDefault(id, 0); }
    private String signed(int n) { return n > 0 ? "+" + n : String.valueOf(n); }
    private void loadScores() {
        ConfigurationSection ps = getConfig().getConfigurationSection("players");
        if (ps == null) return;
        for (String id : ps.getKeys(false)) try {
            UUID uuid = UUID.fromString(id); scores.put(uuid, clamp(ps.getInt(id + ".score")));
            names.put(uuid, ps.getString(id + ".name", uuid));
        } catch (IllegalArgumentException e) { getLogger().warning("Ignoring invalid saved player UUID: " + id); }
    }
    private void saveScores() {
        getConfig().set("players", null);
        for (var e : scores.entrySet()) {
            String path = "players." + e.getKey(); getConfig().set(path + ".score", e.getValue());
            getConfig().set(path + ".name", names.getOrDefault(e.getKey(), e.getKey().toString()));
        }
        saveConfig();
    }
    private String title(int n) {
        if (n == 10) return "☠ Legend"; if (n >= 8) return "☠ Notorious"; if (n >= 6) return "⚔ Feared";
        if (n >= 4) return "⚔ Dangerous"; if (n >= 2) return "⚔ Blooded"; if (n >= -1) return "• Unknown";
        if (n >= -3) return "☠ Fallen"; if (n >= -5) return "☠ Outcast"; if (n >= -7) return "☠ Hunted";
        if (n >= -9) return "☠ Despised"; return "☠ Shamed";
    }
    private void setScore(UUID id, String name, int requested) {
        int old = score(id), value = clamp(requested); scores.put(id, value); names.put(id, name);
        Player p = Bukkit.getPlayer(id); if (p != null) refresh(p);
        if (getConfig().getBoolean("milestone-announcements", true) && !title(old).equals(title(value)))
            Bukkit.broadcast(Component.text(name + " is now " + title(value) + " (" + signed(value) + " Infamy).", NamedTextColor.GOLD));
        saveScores();
    }
    private void refresh(Player p) {
        int n = score(p.getUniqueId()); String label = title(n) + " " + p.getName();
        p.playerListName(Component.text(label, NamedTextColor.WHITE)); p.displayName(Component.text(label, NamedTextColor.WHITE));
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String key = "inf" + p.getUniqueId().toString().replace("-", "").substring(0, 13);
        Team team = board.getTeam(key); if (team == null) team = board.registerNewTeam(key);
        for (String entry : new ArrayList<>(team.getEntries())) if (!entry.equals(p.getName())) team.removeEntry(entry);
        team.prefix(Component.text(title(n) + " ", NamedTextColor.GOLD)); if (!team.hasEntry(p.getName())) team.addEntry(p.getName());
        p.removePotionEffect(PotionEffectType.STRENGTH); p.removePotionEffect(PotionEffectType.SPEED);
        p.removePotionEffect(PotionEffectType.RESISTANCE); p.removePotionEffect(PotionEffectType.HASTE);
        if (n >= getConfig().getInt("effects.feared-at", 5)) { permanent(p, PotionEffectType.STRENGTH); permanent(p, PotionEffectType.SPEED); }
        if (n >= getConfig().getInt("effects.notorious-at", 8)) permanent(p, PotionEffectType.RESISTANCE);
        if (n >= getConfig().getInt("effects.legend-at", 10)) permanent(p, PotionEffectType.HASTE);
    }
    private void permanent(Player p, PotionEffectType effect) { p.addPotionEffect(new PotionEffect(effect, Integer.MAX_VALUE, 0, true, false, true)); }
    private void removeTeam(Player p) {
        Scoreboard b = Bukkit.getScoreboardManager().getMainScoreboard();
        String key = "inf" + p.getUniqueId().toString().replace("-", "").substring(0, 13); Team t = b.getTeam(key);
        if (t != null) { t.removeEntry(p.getName()); if (t.getEntries().isEmpty()) t.unregister(); }
    }
    @EventHandler public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer(); scores.putIfAbsent(p.getUniqueId(), 0); names.put(p.getUniqueId(), p.getName()); refresh(p); saveScores();
    }
    @EventHandler public void onQuit(PlayerQuitEvent e) { removeTeam(e.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e) { Bukkit.getScheduler().runTask(this, () -> refresh(e.getPlayer())); }
    @EventHandler public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity(), killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        if (score(killer.getUniqueId()) >= MAX) e.getDrops().add(shard(1));
        else setScore(killer.getUniqueId(), killer.getName(), score(killer.getUniqueId()) + 1);
        setScore(victim.getUniqueId(), victim.getName(), score(victim.getUniqueId()) - 1);
    }
    @EventHandler public void onChat(AsyncChatEvent e) {
        String t = title(score(e.getPlayer().getUniqueId()));
        e.renderer((source, display, message, viewer) -> Component.text(t + " " + source.getName(), NamedTextColor.GOLD)
                .append(Component.text(": ", NamedTextColor.WHITE)).append(message));
    }

    private ItemStack shard(int value) {
        ItemStack item = new ItemStack(Material.NETHER_STAR); ItemMeta m = item.getItemMeta();
        m.displayName(Component.text("🩸 Infamy Shard", NamedTextColor.RED, TextDecoration.BOLD));
        m.lore(List.of(Component.text("Value: " + value, NamedTextColor.GRAY), Component.text("Right-click to deposit", NamedTextColor.DARK_GRAY)));
        m.getPersistentDataContainer().set(shardKey, PersistentDataType.BYTE, (byte) 1);
        m.getPersistentDataContainer().set(shardValueKey, PersistentDataType.INTEGER, value); item.setItemMeta(m); return item;
    }
    private int shardValue(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0; var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(shardKey, PersistentDataType.BYTE)) return 0;
        Integer v = pdc.get(shardValueKey, PersistentDataType.INTEGER); return v == null ? 0 : Math.max(0, v);
    }
    @EventHandler public void onShardUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem(); int value = shardValue(item); if (value <= 0) return;
        e.setCancelled(true); Player p = e.getPlayer(); int add = Math.min(value, MAX - score(p.getUniqueId()));
        if (add <= 0) { p.sendMessage(Component.text("Your Infamy is already +10; shard unchanged.", NamedTextColor.RED)); return; }
        int left = value - add; if (left == 0) item.setAmount(item.getAmount() - 1); else item.setItemMeta(shard(left).getItemMeta());
        setScore(p.getUniqueId(), p.getName(), score(p.getUniqueId()) + add);
        p.sendMessage(Component.text("Deposited " + add + " Infamy. Current score: " + signed(score(p.getUniqueId())) + ".", NamedTextColor.GREEN));
    }

    private ItemStack weapon(Material type, String id, String name, String lore, Enchantment[] enchants, int[] levels) {
        ItemStack item = new ItemStack(type); ItemMeta m = item.getItemMeta();
        m.displayName(Component.text(name, NamedTextColor.DARK_RED, TextDecoration.BOLD));
        m.lore(List.of(Component.text(lore, NamedTextColor.GRAY), Component.text("Infamy SMP custom weapon", NamedTextColor.DARK_GRAY)));
        m.setUnbreakable(true); m.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, id);
        for (int i = 0; i < enchants.length; i++) m.addEnchant(enchants[i], levels[i], true);
        item.setItemMeta(m); return item;
    }
    private List<ItemStack> weapons() {
        return List.of(
            weapon(Material.NETHERITE_SWORD, "blade", "⚔ Infamy Blade", "Always strikes as a critical hit.",
                    new Enchantment[]{Enchantment.SHARPNESS, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{5,3,1}),
            weapon(Material.NETHERITE_AXE, "cleaver", "⚔ Infamy Cleaver", "Efficiency VII; fells connected tree trunks.",
                    new Enchantment[]{Enchantment.EFFICIENCY, Enchantment.SHARPNESS, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{7,5,3,1}),
            weapon(Material.NETHERITE_PICKAXE, "pickaxe", "⛏ Infamy Pickaxe", "Fortune V and Efficiency VII.",
                    new Enchantment[]{Enchantment.FORTUNE, Enchantment.EFFICIENCY, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{5,7,3,1}),
            weapon(Material.MACE, "maul", "☠ Infamy Maul", "Right-click to boost upward and dash forward.",
                    new Enchantment[]{Enchantment.DENSITY, Enchantment.WIND_BURST, Enchantment.UNBREAKING, Enchantment.MENDING}, new int[]{4,2,3,1}));
    }
    private String weaponId(ItemStack i) {
        if (i == null || !i.hasItemMeta()) return null;
        return i.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH) public void onSwordHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !"blade".equals(weaponId(p.getInventory().getItemInMainHand()))) return;
        e.setDamage(e.getDamage() * 1.5); Entity target = e.getEntity();
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, .3, .4, .3, .1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, .7f, 1.15f);
    }
    @EventHandler(ignoreCancelled = true) public void onTreeChop(BlockBreakEvent e) {
        Player p = e.getPlayer(); if (p.getGameMode() == GameMode.CREATIVE) return;
        ItemStack tool = p.getInventory().getItemInMainHand(); if (!"cleaver".equals(weaponId(tool)) || !isWood(e.getBlock().getType())) return;
        Block origin = e.getBlock(); World world = origin.getWorld(); ArrayDeque<Block> q = new ArrayDeque<>();
        Set<String> seen = new HashSet<>(); q.add(origin); seen.add(key(origin)); int count = 0;
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!q.isEmpty() && count < TREE_LIMIT) {
            Block cur = q.removeFirst();
            for (int[] d : dirs) {
                Block b = world.getBlockAt(cur.getX()+d[0], cur.getY()+d[1], cur.getZ()+d[2]);
                if (!seen.add(key(b)) || !isWood(b.getType())) continue;
                if (Math.abs(b.getX()-origin.getX()) > TREE_RADIUS || Math.abs(b.getY()-origin.getY()) > TREE_RADIUS || Math.abs(b.getZ()-origin.getZ()) > TREE_RADIUS) continue;
                q.addLast(b); b.breakNaturally(tool); count++; if (count >= TREE_LIMIT) break;
            }
        }
        if (count > 0) p.sendActionBar(Component.text("Infamy Cleaver felled " + (count+1) + " trunk blocks", NamedTextColor.GOLD));
    }
    private boolean isWood(Material m) { String n=m.name(); return n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE"); }
    private String key(Block b) { return b.getX()+":"+b.getY()+":"+b.getZ(); }
    @EventHandler public void onMace(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || (e.getAction()!=Action.RIGHT_CLICK_AIR && e.getAction()!=Action.RIGHT_CLICK_BLOCK)
                || !"maul".equals(weaponId(e.getItem()))) return;
        Player p=e.getPlayer(); long now=System.currentTimeMillis(), ready=cooldowns.getOrDefault(p.getUniqueId(),0L);
        if (now < ready) { p.sendActionBar(Component.text("Mace burst ready in " + Math.max(1,(ready-now+999)/1000) + "s", NamedTextColor.RED)); return; }
        Vector forward=p.getLocation().getDirection().setY(0).normalize(); if (forward.lengthSquared()==0) forward=new Vector(0,0,1);
        p.setVelocity(forward.multiply(1.65).setY(.95)); cooldowns.put(p.getUniqueId(),now+MACE_COOLDOWN);
        p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),16,.35,.2,.35,.08);
        p.playSound(p.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,.9f,1.25f);
        p.sendActionBar(Component.text("Infamy Maul burst!",NamedTextColor.GOLD));
    }

    private Player knownOnline(CommandSender sender, String name) {
        Player p=Bukkit.getPlayerExact(name); if (p==null) sender.sendMessage(ChatColor.RED+"That player must be online."); return p;
    }
    private void giveWeapons(CommandSender sender, String target) {
        if (!sender.hasPermission("infamy.admin")) { sender.sendMessage(ChatColor.RED+"You need infamy.admin to give custom weapons."); return; }
        Player p=knownOnline(sender,target); if (p==null)return;
        Map<Integer,ItemStack> overflow=p.getInventory().addItem(weapons().toArray(ItemStack[]::new));
        overflow.values().forEach(i->p.getWorld().dropItemNaturally(p.getLocation(),i));
        p.sendMessage(Component.text("You received the Infamy sword, axe, pickaxe, and mace.",NamedTextColor.DARK_RED));
        sender.sendMessage(ChatColor.GREEN+"Gave the weapons to "+p.getName()+".");
    }
    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length==0) { if(s instanceof Player p) showScore(s,p.getUniqueId(),p.getName()); else s.sendMessage("Use /infamy <player> or /infamy leaderboard."); return true; }
        if(a[0].equalsIgnoreCase("weapons")) {
            if(a.length>2 || (a.length==2&&!s.hasPermission("infamy.admin"))) { s.sendMessage(ChatColor.RED+"Usage: /infamy weapons [online-player]"); return true; }
            String target=a.length==2?a[1]:s instanceof Player p?p.getName():null;
            if(target==null){s.sendMessage("From console, specify an online player.");return true;} giveWeapons(s,target);return true;
        }
        if(a[0].equalsIgnoreCase("set")||a[0].equalsIgnoreCase("reset")){manage(s,a);return true;}
        if(a[0].equalsIgnoreCase("gui")){if(s instanceof Player p)openGui(p);else s.sendMessage("The GUI is only available in-game.");return true;}
        if(a[0].equalsIgnoreCase("leaderboard")||a[0].equalsIgnoreCase("top")){leaderboard(s);return true;}
        if(a[0].equalsIgnoreCase("withdraw")){withdraw(s,a);return true;}
        PlayerRef ref=findPlayer(a[0]); if(ref==null){s.sendMessage(ChatColor.RED+"No known player named "+a[0]+".");return true;}
        showScore(s,ref.id,ref.name);return true;
    }
    private record PlayerRef(UUID id,String name){}
    private PlayerRef findPlayer(String search) {
        Player p=Bukkit.getPlayerExact(search); if(p!=null)return new PlayerRef(p.getUniqueId(),p.getName());
        for(var e:names.entrySet())if(e.getValue().equalsIgnoreCase(search))return new PlayerRef(e.getKey(),e.getValue());
        OfflinePlayer cached=Bukkit.getOfflinePlayerIfCached(search); return cached!=null&&cached.getName()!=null?new PlayerRef(cached.getUniqueId(),cached.getName()):null;
    }
    private void manage(CommandSender s,String[] a) {
        if(!s.hasPermission("infamy.admin")){s.sendMessage(ChatColor.RED+"You need infamy.admin.");return;}
        boolean reset=a[0].equalsIgnoreCase("reset"); if((reset&&a.length!=2)||(!reset&&a.length!=3)){s.sendMessage(reset?"Usage: /infamy reset <player>":"Usage: /infamy set <player> <-10..10>");return;}
        PlayerRef ref=findPlayer(a[1]);if(ref==null){s.sendMessage(ChatColor.RED+"Unknown player.");return;} int value=0;
        if(!reset)try{value=Integer.parseInt(a[2]);}catch(NumberFormatException ex){s.sendMessage(ChatColor.RED+"Score must be a whole number.");return;}
        if(value<MIN||value>MAX){s.sendMessage(ChatColor.RED+"Score must be between -10 and +10.");return;}
        setScore(ref.id,ref.name,value);s.sendMessage(ChatColor.GREEN+"Set "+ref.name+" to "+signed(value)+" Infamy.");
    }
    private void showScore(CommandSender s,UUID id,String name){int n=score(id);s.sendMessage(Component.text(name+" — "+title(n)+" — "+signed(n)+" Infamy",NamedTextColor.GOLD));}
    private List<Map.Entry<UUID,Integer>> ranked(){return scores.entrySet().stream().sorted(Map.Entry.<UUID,Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(e->names.getOrDefault(e.getKey(),""),String.CASE_INSENSITIVE_ORDER)).toList();}
    private void leaderboard(CommandSender s){int limit=Math.max(1,Math.min(50,getConfig().getInt("leaderboard-size",10)));s.sendMessage(Component.text("—— Infamy Leaderboard ——",NamedTextColor.GOLD,TextDecoration.BOLD));List<Map.Entry<UUID,Integer>> list=ranked().stream().limit(limit).toList();if(list.isEmpty())s.sendMessage("No scores recorded yet.");for(int i=0;i<list.size();i++){var e=list.get(i);s.sendMessage((i+1)+". "+names.getOrDefault(e.getKey(),"Unknown")+" — "+signed(e.getValue())+" — "+title(e.getValue()));}}
    private void openGui(Player p){Inventory inv=Bukkit.createInventory(null,27,Component.text("Infamy Rankings",NamedTextColor.DARK_RED,TextDecoration.BOLD));ItemStack profile=new ItemStack(Material.NETHER_STAR);ItemMeta pm=profile.getItemMeta();int n=score(p.getUniqueId());pm.displayName(Component.text(p.getName(),NamedTextColor.GOLD,TextDecoration.BOLD));pm.lore(List.of(Component.text(title(n),NamedTextColor.YELLOW),Component.text("Infamy: "+signed(n),NamedTextColor.WHITE)));profile.setItemMeta(pm);inv.setItem(4,profile);Material[] mats={Material.DIAMOND,Material.EMERALD,Material.IRON_INGOT,Material.GOLD_INGOT,Material.COPPER_INGOT,Material.COAL,Material.REDSTONE,Material.LAPIS_LAZULI,Material.AMETHYST_SHARD,Material.QUARTZ};List<Map.Entry<UUID,Integer>> list=ranked().stream().limit(10).toList();for(int i=0;i<list.size();i++){var e=list.get(i);ItemStack icon=new ItemStack(mats[i]);ItemMeta m=icon.getItemMeta();m.displayName(Component.text("#"+(i+1)+" "+names.getOrDefault(e.getKey(),"Unknown"),NamedTextColor.GOLD));m.lore(List.of(Component.text(title(e.getValue()),NamedTextColor.YELLOW),Component.text("Infamy: "+signed(e.getValue()),NamedTextColor.WHITE)));icon.setItemMeta(m);inv.setItem(9+i,icon);}p.openInventory(inv);}
    @EventHandler public void onGuiClick(InventoryClickEvent e){if(e.getView().title().equals(Component.text("Infamy Rankings",NamedTextColor.DARK_RED,TextDecoration.BOLD)))e.setCancelled(true);}
    private void withdraw(CommandSender s,String[] a){if(!(s instanceof Player p)){s.sendMessage("Only players can withdraw.");return;}if(!p.hasPermission("infamy.use")){p.sendMessage(ChatColor.RED+"No permission.");return;}if(a.length!=2){p.sendMessage("Usage: /infamy withdraw <amount>");return;}int amount;try{amount=Integer.parseInt(a[1]);}catch(NumberFormatException ex){p.sendMessage("Amount must be a whole number.");return;}int available=score(p.getUniqueId());if(amount<=0||amount>available){p.sendMessage(ChatColor.RED+"Withdraw 1 through your positive Infamy total ("+available+").");return;}if(p.getInventory().firstEmpty()==-1){p.sendMessage(ChatColor.RED+"Make room in your inventory first.");return;}p.getInventory().addItem(shard(amount));setScore(p.getUniqueId(),p.getName(),available-amount);p.sendMessage(ChatColor.GREEN+"Withdrew "+amount+" Infamy as a shard.");}
}
