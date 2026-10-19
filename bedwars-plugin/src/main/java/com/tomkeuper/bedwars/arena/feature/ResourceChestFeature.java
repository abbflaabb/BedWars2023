package com.tomkeuper.bedwars.arena.feature;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerItemDepositEvent;
import com.tomkeuper.bedwars.api.hologram.IHologramManager;
import com.tomkeuper.bedwars.api.hologram.containers.IHologram;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.arena.Arena;
import com.tomkeuper.bedwars.configuration.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.stream.Collectors;

public class ResourceChestFeature implements Listener {

    private static ResourceChestFeature instance;
    private Set<Material> blockedItems;
    private final Map<IArena, Map<Location, IHologram>> chestHolograms = new HashMap<>();
    private final Map<IArena, org.bukkit.scheduler.BukkitTask> pendingSpawns = new HashMap<>();
    private final Map<IArena, List<IHologram>> arenaHolograms = new HashMap<>();
    private final IHologramManager hologramManager;
    private static final Map<String, String> OLD_MATERIAL_NAMES = new HashMap<>();

    private ResourceChestFeature() {
        this.blockedItems = readBlockedItems(BedWars.config.getYml());

        this.hologramManager = BedWars.getAPI().getHologramsUtil();
        Bukkit.getPluginManager().registerEvents(this, BedWars.plugin);
        try {
            Sounds.addDefSound("ChestOpen",
                    BedWars.getForCurrentVersion("CHEST_OPEN", "BLOCK_CHEST_OPEN", "BLOCK_CHEST_OPEN"));
        } catch (Exception e) {
            BedWars.plugin.getLogger().info("Using default sound configuration for ChestOpen");
        }
    }

    public static void init() {
        if (instance == null) {
            instance = new ResourceChestFeature();
        }
    }

    private static Set<Material> readBlockedItems(YamlConfiguration config) {
        Set<Material> items = new HashSet<>();
        for (String name : config.getStringList("click-in-chest-to-deposit-feature.blocked-items")) {
            String key = name.toUpperCase(Locale.ROOT);
            Material material = Material.getMaterial(key);
            if (material == null) material = Material.getMaterial(OLD_MATERIAL_NAMES.getOrDefault(key, key));
            if (material == null) {
                for (Map.Entry<String, String> alias : OLD_MATERIAL_NAMES.entrySet()) {
                    if (alias.getValue().equals(key)) material = Material.getMaterial(alias.getKey());
                }
            }
            if (material == null) throw new IllegalArgumentException("Unknown blocked item: " + name);
            items.add(material);
        }
        return items;
    }

    /** Reload this feature without changing other managers' cached configuration. */
    public static void reloadConfiguration() throws IOException, InvalidConfigurationException {
        YamlConfiguration fresh = new YamlConfiguration();
        fresh.load(new File(BedWars.plugin.getDataFolder(), "config.yml"));
        fresh.setDefaults(BedWars.config.getYml().getDefaults());
        Set<Material> blocked = readBlockedItems(fresh);
        for (String key : Arrays.asList("x-offset", "y-offset", "z-offset", "spacing")) {
            Object value = fresh.get("resource-chest.hologram." + key);
            if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue())
                    || (key.equals("spacing") && ((Number) value).doubleValue() <= 0)) {
                throw new IllegalArgumentException("resource-chest.hologram." + key + " must be a finite number"
                        + (key.equals("spacing") ? " greater than zero" : ""));
            }
        }
        for (String key : Arrays.asList("click-in-chest-to-deposit-feature.enable",
                "click-in-chest-to-deposit-feature.blocked-items", "resource-chest.hologram.enabled",
                "resource-chest.hologram.x-offset", "resource-chest.hologram.y-offset",
                "resource-chest.hologram.z-offset", "resource-chest.hologram.spacing",
                "resource-chest.hologram.title", "resource-chest.hologram.subtitle")) {
            BedWars.config.getYml().set(key, fresh.get(key));
        }
        init();
        instance.blockedItems = blocked;
        for (IArena arena : new ArrayList<>(instance.pendingSpawns.keySet())) instance.removeArenaHolograms(arena);
        for (IArena arena : new ArrayList<>(instance.arenaHolograms.keySet())) instance.removeArenaHolograms(arena);
        for (IArena arena : Arena.getArenas()) {
            if (arena.getStatus() == GameState.playing && instance.hologramsEnabled()) {
                instance.loadAllChunks(arena);
                instance.createAllHolograms(arena);
            }
        }
    }

    private boolean hologramsEnabled() {
        return BedWars.config.getBoolean("click-in-chest-to-deposit-feature.enable")
                && BedWars.config.getBoolean("resource-chest.hologram.enabled");
    }

    @EventHandler
    public void onArenaStateChange(GameStateChangeEvent e) {
        IArena arena = e.getArena();
        removeArenaHolograms(arena);
        if (e.getNewState() == GameState.playing) {
            if (hologramsEnabled()) {
                loadAllChunks(arena);
                pendingSpawns.put(arena, Bukkit.getScheduler().runTaskLater(BedWars.plugin, () -> {
                    pendingSpawns.remove(arena);
                    createAllHolograms(arena);
                }, 20L));
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeftClickChest(PlayerInteractEvent e) {
        IArena arena = Arena.getArenaByPlayer(e.getPlayer());
        if (arena == null || arena.getStatus() != GameState.playing
                || !BedWars.config.getBoolean("click-in-chest-to-deposit-feature.enable")) return;

        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;

        boolean normalChest = clickedBlock.getType() == Material.CHEST;
        boolean enderChest = clickedBlock.getType() == Material.ENDER_CHEST;
        if (!normalChest && !enderChest) return;

        Player player = e.getPlayer();
        ITeam playerTeam = arena.getTeam(player);
        if (playerTeam == null || arena.isSpectator(player) || arena.isReSpawning(player)) return;

        if (normalChest && !canUseThisChest(arena, player, clickedBlock.getLocation())) {
            String message = Language.getMsg(player, Messages.INTERACT_CHEST_CANT_OPEN_TEAM_ELIMINATED);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            e.setCancelled(true);
            return;
        }

        if (BedWars.config.getBoolean("resource-chest.hologram.enabled")) {
            makeHologramIfNeeded(arena, clickedBlock.getLocation());
        }

        ItemStack itemInHand = BedWars.nms.getItemInHand(player);
        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        itemInHand = itemInHand.clone();
        e.setCancelled(true);

        if (blockedItems.contains(itemInHand.getType())
                || BedWars.nms.isTool(itemInHand)
                || "DEFAULT_ITEM".equalsIgnoreCase(BedWars.nms.getCustomData(itemInHand))) {

            String message = Language.getMsg(player, Messages.RESOURCE_CHEST_BLOCKED_ITEM)
                    .replace("{item}", itemInHand.getType().name().toLowerCase());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        Inventory chestInventory = normalChest
                ? ((Chest) clickedBlock.getState()).getInventory()
                : player.getEnderChest();

        int howManyDeposited = tryDepositItems(player, itemInHand, chestInventory);

        if (howManyDeposited > 0) {
            if (itemInHand.getType().name().contains("SWORD")) {
                playerTeam.defaultSword(player, true);
            }

            String chestType = enderChest ? "ender chest" : "team chest";
            String message = Language.getMsg(player, Messages.RESOURCE_CHEST_DEPOSITED)
                    .replace("{amount}", String.valueOf(howManyDeposited))
                    .replace("{item}", makeNamePretty(itemInHand.getType()))
                    .replace("{chest}", chestType);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));

            Sounds.playSound("ChestOpen", player);

            ItemStack deposited = itemInHand.clone();
            deposited.setAmount(howManyDeposited);
            tellOtherPlugins(player, arena, deposited, chestInventory, enderChest);
        }
    }

    // Use the island owner, never whichever team happened to deposit first.
    public static boolean canUseThisChest(IArena arena, Player player, Location chestLoc) {
        ITeam owner = null;
        double radius = Math.max(0, arena.getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS));
        double nearest = radius * radius;
        for (ITeam team : arena.getTeams()) {
            Location spawn = team.getSpawn();
            if (spawn == null || !Objects.equals(spawn.getWorld(), chestLoc.getWorld())) continue;
            double distance = spawn.distanceSquared(chestLoc);
            if (distance <= nearest) {
                nearest = distance;
                owner = team;
            }
        }
        return owner == null || owner.isMember(player)
                || (owner.getMembers().isEmpty() && owner.isBedDestroyed());
    }

    private int tryDepositItems(Player player, ItemStack hand, Inventory chest) {
        ItemStack itemsToStore = hand.clone();
        int triedToAdd = itemsToStore.getAmount();
        Map<Integer, ItemStack> leftoverItems = chest.addItem(itemsToStore);
        int couldntFit = leftoverItems.values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();
        int actuallyAdded = triedToAdd - couldntFit;

        if (actuallyAdded <= 0) {
            String message = Language.getMsg(player, Messages.RESOURCE_CHEST_FULL);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return 0;
        }

        // Only change the held slot. addItem may mutate its argument, and leftovers
        // are already in the player's hand: adding them again duplicates items.
        ItemStack remaining = hand.clone();
        remaining.setAmount(couldntFit);
        player.getInventory().setItem(player.getInventory().getHeldItemSlot(),
                couldntFit == 0 ? null : remaining);
        player.updateInventory();

        return actuallyAdded;
    }

    private String makeNamePretty(Material material) {
        String rawName = material.name().replace('_', ' ').toLowerCase();
        return Arrays.stream(rawName.split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private void tellOtherPlugins(Player player, IArena arena, ItemStack item,
                                  Inventory inv, boolean isEnder) {
        Bukkit.getPluginManager().callEvent(
                new PlayerItemDepositEvent(player, arena, item, inv, isEnder));
    }

    private void loadAllChunks(IArena arena) {
        for (ITeam team : arena.getTeams()) {
            if (team.getSpawn() != null) {
                team.getSpawn().getChunk().load(true);
            }
            if (team.getShop() != null) {
                team.getShop().getChunk().load(true);
            }
            if (team.getTeamUpgrades() != null) {
                team.getTeamUpgrades().getChunk().load(true);
            }
        }

        if (arena.getSpectatorLocation() != null) {
            arena.getSpectatorLocation().getChunk().load(true);
        }
    }

    private void createAllHolograms(IArena arena) {
        if (arena.getStatus() != GameState.playing || !hologramsEnabled()) return;
        for (Chunk chunk : arena.getWorld().getLoadedChunks()) {
            for (BlockState block : chunk.getTileEntities()) {
                if (block.getType() == Material.CHEST || block.getType() == Material.ENDER_CHEST) {
                    makeHologramForChest(arena, block.getLocation());
                }
            }
        }
    }

    private void makeHologramForChest(IArena arena, Location chestLoc) {
        if (!hologramsEnabled()) return;
        Map<Location, IHologram> byChest = chestHolograms.computeIfAbsent(arena, key -> new HashMap<>());
        if (byChest.containsKey(chestLoc)) return;
        double xOffset = BedWars.config.getYml().getDouble("resource-chest.hologram.x-offset", 0.5);
        double yOffset = BedWars.config.getYml().getDouble("resource-chest.hologram.y-offset", 0.04);
        double zOffset = BedWars.config.getYml().getDouble("resource-chest.hologram.z-offset", 0.5);
        double spacing = BedWars.config.getYml().getDouble("resource-chest.hologram.spacing", 0.25);

        String title = BedWars.config.getYml().getString("resource-chest.hologram.title", "&e&l⚡ STORAGE CHEST");
        String subtitle = BedWars.config.getYml().getString("resource-chest.hologram.subtitle", "&7Left-click to deposit");

        // Full-size armor stand names render 1.975 + 0.5 blocks above their feet.
        // The configured offset places the lowest line above the chest block.
        Location holoPosition = chestLoc.clone().add(xOffset, 1.0 + yOffset - 2.475, zOffset);

        IHologram hologram = hologramManager.createHologram(
                arena.getPlayers(),
                holoPosition,
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle)
        );

        hologram.setGap(spacing);

        arenaHolograms.computeIfAbsent(arena, key -> new ArrayList<>()).add(hologram);
        byChest.put(chestLoc.clone(), hologram);
    }

    private void makeHologramIfNeeded(IArena arena, Location chestLoc) {
        makeHologramForChest(arena, chestLoc);
    }

    public List<IHologram> getArenaHolograms(IArena arena) {
        return arenaHolograms.getOrDefault(arena, new ArrayList<>());
    }

    public void removeArenaHolograms(IArena arena) {
        org.bukkit.scheduler.BukkitTask pending = pendingSpawns.remove(arena);
        if (pending != null) pending.cancel();
        chestHolograms.remove(arena);
        List<IHologram> holosInArena = arenaHolograms.get(arena);
        if (holosInArena != null) {
            holosInArena.forEach(IHologram::remove);
            arenaHolograms.remove(arena);
        }
    }

    public List<IHologram> getHologramsNearLocation(IArena arena, Location spot, double howClose) {
        List<IHologram> nearby = new ArrayList<>();
        List<IHologram> allHolos = arenaHolograms.get(arena);

        if (allHolos != null) {
            for (IHologram holo : allHolos) {
                if (holo.getLocation().distance(spot) <= howClose) {
                    nearby.add(holo);
                }
            }
        }

        return nearby;
    }

    public void updateChestHologram(IArena arena, Location chestLoc,
                                    String topLine, String bottomLine) {
        IHologram holo = chestHolograms.getOrDefault(arena, Collections.emptyMap()).get(chestLoc);
        if (holo != null && holo.getLines().size() >= 2) {
            holo.setLine(0, ChatColor.translateAlternateColorCodes('&', bottomLine), true);
            holo.setLine(1, ChatColor.translateAlternateColorCodes('&', topLine), true);
        }
    }

    public static ResourceChestFeature getInstance() {
        return instance;
    }

    static {
        OLD_MATERIAL_NAMES.put("WOODEN_SWORD", "WOOD_SWORD");
        OLD_MATERIAL_NAMES.put("WOODEN_PICKAXE", "WOOD_PICKAXE");
        OLD_MATERIAL_NAMES.put("WOODEN_AXE", "WOOD_AXE");
        OLD_MATERIAL_NAMES.put("WOODEN_SHOVEL", "WOOD_SPADE");
        OLD_MATERIAL_NAMES.put("WOODEN_HOE", "WOOD_HOE");
    }
}
