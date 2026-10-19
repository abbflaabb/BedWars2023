/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.achievements;

import com.tomkeuper.bedwars.api.achievements.*;
import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.achievements.config.AchievementConfig;
import com.tomkeuper.bedwars.api.database.IDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AchievementManager implements IAchievementManager {

    private static final Gson GSON = new Gson();
    // Interface types prevent old server-provided Gson versions from reflecting
    // on JDK-internal collection implementations (for example EmptySet).
    private static final Type PROGRESS_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final Type COMPLETED_TYPE = new TypeToken<Map<String, Long>>() {}.getType();
    private static final Type NOTIFIED_TYPE = new TypeToken<Set<String>>() {}.getType();

    private final Plugin plugin;
    private final Map<String, IAchievement> achievements = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> playerProgress = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> completedAchievements = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> notifiedAchievements = new ConcurrentHashMap<>();
    private final Map<String, AchievementCategory> categories = new ConcurrentHashMap<>();

    private boolean enabled = true;
    private boolean notificationsEnabled = true;
    private boolean soundsEnabled = true;
    private boolean particlesEnabled = true;
    private boolean commandsEnabled = true;
    private boolean statisticsEnabled = true;
    private int saveInterval = 5;
    private String saveFormat = "JSON";
    private String saveLocation = "data/achievements/";

    public AchievementManager() {
        this(BedWars.plugin);
    }

    public AchievementManager(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        // Initialize achievement config
        new AchievementConfig(plugin, "achievements", plugin.getDataFolder().getPath(), this);
    }

    @Override
    public @NotNull List<IAchievement> getAllAchievements() {
        return new ArrayList<>(achievements.values());
    }

    @Override
    public @Nullable IAchievement getAchievement(@NotNull String id) {
        return achievements.get(id);
    }

    @Override
    public @NotNull List<IAchievement> getAchievementsByCategory(@NotNull AchievementCategory category) {
        return achievements.values().stream()
                .filter(achievement -> achievement.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAchievementCompleted(@NotNull Player player, @NotNull String achievementId) {
        return completedAchievements.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .containsKey(achievementId);
    }

    @Override
    public int getAchievementProgress(@NotNull Player player, @NotNull String achievementId) {
        return playerProgress.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .getOrDefault(achievementId, 0);
    }

    @Override
    public void setAchievementProgress(@NotNull Player player, @NotNull String achievementId, int progress) {
        playerProgress.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(achievementId, progress);
    }

    @Override
    public void completeAchievement(@NotNull Player player, @NotNull String achievementId) {
        completedAchievements.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(achievementId, System.currentTimeMillis());

        // Mark as notified
        notifiedAchievements.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(achievementId);

        // Give rewards
        IAchievement achievement = getAchievement(achievementId);
        if (achievement != null && commandsEnabled) {
            AchievementReward reward = achievement.getReward();
            if (reward != null) {
                // Execute commands
                String command = reward.getCommand().replace("%player%", player.getName());
                if (!command.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }

        // Send notification
        if (notificationsEnabled) {
            sendAchievementNotification(player, achievement);
        }
    }

    @Override
    public @NotNull Map<String, Long> getCompletedAchievements(@NotNull Player player) {
        return completedAchievements.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>());
    }

    @Override
    public @NotNull AchievementStats getAchievementStats(@NotNull Player player) {
        Map<String, Long> completed = getCompletedAchievements(player);
        Map<AchievementCategory, Integer> categoryStats = new HashMap<>();

        // Initialize all categories with 0
        for (AchievementCategory category : AchievementCategory.values()) {
            categoryStats.put(category, 0);
        }

        // Count completed achievements by category
        for (Map.Entry<String, Long> entry : completed.entrySet()) {
            IAchievement achievement = getAchievement(entry.getKey());
            if (achievement != null) {
                AchievementCategory category = achievement.getCategory();
                categoryStats.put(category, categoryStats.getOrDefault(category, 0) + 1);
            }
        }

        long firstTimestamp = completed.values().stream()
                .min(Long::compare)
                .orElse(System.currentTimeMillis());
        long lastTimestamp = completed.values().stream()
                .max(Long::compare)
                .orElse(System.currentTimeMillis());

        return new AchievementStats(getAllAchievements().size(), completed.size(), categoryStats, firstTimestamp, lastTimestamp);
    }

    @Override
    public boolean shouldNotifyAchievement(@NotNull Player player, @NotNull String achievementId) {
        return notificationsEnabled && !isAchievementCompleted(player, achievementId) &&
                !notifiedAchievements.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                        .contains(achievementId);
    }

    @Override
    public void markAchievementNotified(@NotNull Player player, @NotNull String achievementId) {
        notifiedAchievements.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(achievementId);
    }

    @Override
    public @NotNull Map<UUID, Map<String, Integer>> getAllAchievementProgress() {
        return new ConcurrentHashMap<>(playerProgress);
    }

    @Override
    public void saveAllAchievementData() {
        if (!enabled) return;

        try {
            // Create save directory if it doesn't exist
            Path saveDir = Paths.get(plugin.getDataFolder().getPath(), saveLocation);
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
            }

            // Save player data based on format
            if (saveFormat.equalsIgnoreCase("JSON")) {
                saveJsonData();
            } else if (saveFormat.equalsIgnoreCase("YAML")) {
                saveYamlData();
            } else if (saveFormat.equalsIgnoreCase("SQLITE")) {
                saveSqliteData();
            } else if (saveFormat.equalsIgnoreCase("MYSQL")) {
                saveMySqlData();
            } else if (saveFormat.equalsIgnoreCase("H2")) {
                saveH2Data();
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save achievement data: " + e.getMessage());
        }
    }

    @Override
    public void loadAchievementData(@NotNull Player player) {
        if (!enabled) return;

        try {
            // Load player data based on format
            if (saveFormat.equalsIgnoreCase("JSON")) {
                loadJsonData(player);
            } else if (saveFormat.equalsIgnoreCase("YAML")) {
                loadYamlData(player);
            } else if (saveFormat.equalsIgnoreCase("SQLITE")) {
                loadSqliteData(player);
            } else if (saveFormat.equalsIgnoreCase("MYSQL")) {
                loadMySqlData(player);
            } else if (saveFormat.equalsIgnoreCase("H2")) {
                loadH2Data(player);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load achievement data for " + player.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void saveAchievementData(@NotNull Player player) {
        if (!enabled) return;

        try {
            // Save player data based on format
            if (saveFormat.equalsIgnoreCase("JSON")) {
                saveJsonData(player);
            } else if (saveFormat.equalsIgnoreCase("YAML")) {
                saveYamlData(player);
            } else if (saveFormat.equalsIgnoreCase("SQLITE")) {
                saveSqliteData(player);
            } else if (saveFormat.equalsIgnoreCase("MYSQL")) {
                saveMySqlData(player);
            } else if (saveFormat.equalsIgnoreCase("H2")) {
                saveH2Data(player);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save achievement data for " + player.getName() + ": " + e.getMessage());
        }
    }

    // Helper methods
    public void createCategory(String id, String displayName, String color, String icon, String description) {
        try {
            categories.put(id, AchievementCategory.valueOf(id.toUpperCase()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown achievement category: " + id);
        }
    }

    public void createAchievement(String id, String categoryId, String name, String description,
                                  int requiredAmount, String rewardType, String rewardValue,
                                  String message, String sound, String particles) {
        AchievementCategory category;
        try {
            category = AchievementCategory.valueOf(categoryId.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown achievement category for achievement " + id + ": " + categoryId);
            return;
        }
        AchievementReward reward = new AchievementReward(0, 0, message, rewardValue);

        // Create achievement implementation
        IAchievement achievement = new BasicAchievement(id, name, description, category,
                requiredAmount, reward, this);
        achievements.put(id, achievement);
    }

    @Override
    public void initializeAchievements() {
        // Load achievements from config
        // This will be called from AchievementConfig
    }

    // Getters
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    @Override
    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    @Override
    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    @Override
    public boolean isCommandsEnabled() {
        return commandsEnabled;
    }

    @Override
    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    @Override
    public int getSaveInterval() {
        return saveInterval;
    }

    @Override
    public String getSaveFormat() {
        return saveFormat;
    }

    @Override
    public String getSaveLocation() {
        return saveLocation;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public void setSoundsEnabled(boolean soundsEnabled) {
        this.soundsEnabled = soundsEnabled;
    }

    public void setParticlesEnabled(boolean particlesEnabled) {
        this.particlesEnabled = particlesEnabled;
    }

    public void setCommandsEnabled(boolean commandsEnabled) {
        this.commandsEnabled = commandsEnabled;
    }

    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

    public void setSaveInterval(int saveInterval) {
        this.saveInterval = saveInterval;
    }

    public void setSaveFormat(String saveFormat) {
        this.saveFormat = saveFormat;
    }

    public void setSaveLocation(String saveLocation) {
        this.saveLocation = saveLocation;
    }

    private void sendAchievementNotification(@NotNull Player player, @Nullable IAchievement achievement) {
        if (achievement == null) return;

        // Send title and subtitle (translate &-codes coming from the config)
        BedWars.nms.sendTitle(player, "§aAchievement Unlocked!",
                ChatColor.translateAlternateColorCodes('&', achievement.getName()), 10, 60, 20);

        // Play sound
        if (soundsEnabled) {
            player.playSound(player.getLocation(), "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);
        }

        // Send particles
        if (particlesEnabled) {
            for (int i = 0; i < 10; i++) {
                BedWars.nms.playVillagerEffect(player, player.getLocation());
            }
        }

        // Send chat message
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§6§l[ACHIEVEMENT] §r" + achievement.getName()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "§7" + achievement.getDescription()));
    }

    // ---------------------------------------------------------------------
    // Persistence helpers
    // ---------------------------------------------------------------------

    private File getSaveDirectory() {
        File dir = new File(plugin.getDataFolder(), saveLocation);
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create achievement data directory: " + dir.getPath());
        }
        return dir;
    }

    private File getPlayerFile(UUID uuid, String extension) {
        return new File(getSaveDirectory(), uuid.toString() + "." + extension);
    }

    private Set<UUID> collectPlayerIds() {
        Set<UUID> uuids = new HashSet<>();
        uuids.addAll(playerProgress.keySet());
        uuids.addAll(completedAchievements.keySet());
        uuids.addAll(notifiedAchievements.keySet());
        return uuids;
    }

    private String getProgressJson(UUID uuid) {
        return GSON.toJson(playerProgress.getOrDefault(uuid, Collections.emptyMap()), PROGRESS_TYPE);
    }

    private String getCompletedJson(UUID uuid) {
        return GSON.toJson(completedAchievements.getOrDefault(uuid, Collections.emptyMap()), COMPLETED_TYPE);
    }

    private String getNotifiedJson(UUID uuid) {
        return GSON.toJson(notifiedAchievements.getOrDefault(uuid, Collections.emptySet()), NOTIFIED_TYPE);
    }

    private void applyProgress(UUID uuid, @Nullable String json) {
        if (json == null || json.trim().isEmpty()) return;
        Map<String, Integer> data = GSON.fromJson(json, PROGRESS_TYPE);
        if (data != null) {
            playerProgress.put(uuid, new ConcurrentHashMap<>(data));
        }
    }

    private void applyCompleted(UUID uuid, @Nullable String json) {
        if (json == null || json.trim().isEmpty()) return;
        Map<String, Long> data = GSON.fromJson(json, COMPLETED_TYPE);
        if (data != null) {
            completedAchievements.put(uuid, new ConcurrentHashMap<>(data));
        }
    }

    private void applyNotified(UUID uuid, @Nullable String json) {
        if (json == null || json.trim().isEmpty()) return;
        Set<String> data = GSON.fromJson(json, NOTIFIED_TYPE);
        if (data != null) {
            Set<String> set = ConcurrentHashMap.newKeySet();
            set.addAll(data);
            notifiedAchievements.put(uuid, set);
        }
    }

    @Nullable
    private static String elementToString(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        return element.toString();
    }

    // ---------------------------------------------------------------------
    // JSON persistence (one file per player)
    // ---------------------------------------------------------------------

    private void saveJsonData() throws IOException {
        for (UUID uuid : collectPlayerIds()) {
            saveJsonData(uuid);
        }
    }

    private void saveJsonData(@NotNull Player player) throws IOException {
        saveJsonData(player.getUniqueId());
    }

    private void saveJsonData(UUID uuid) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("uuid", uuid.toString());
        root.add("progress", GSON.toJsonTree(playerProgress.getOrDefault(uuid, Collections.emptyMap()), PROGRESS_TYPE));
        root.add("completed", GSON.toJsonTree(completedAchievements.getOrDefault(uuid, Collections.emptyMap()), COMPLETED_TYPE));
        root.add("notified", GSON.toJsonTree(notifiedAchievements.getOrDefault(uuid, Collections.emptySet()), NOTIFIED_TYPE));
        try (Writer writer = new FileWriter(getPlayerFile(uuid, "json"))) {
            GSON.toJson(root, writer);
        }
    }

    private void loadJsonData(@NotNull Player player) throws IOException {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid, "json");
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return;
            applyProgress(uuid, elementToString(root.get("progress")));
            applyCompleted(uuid, elementToString(root.get("completed")));
            applyNotified(uuid, elementToString(root.get("notified")));
        }
    }

    // ---------------------------------------------------------------------
    // YAML persistence (one file per player)
    // ---------------------------------------------------------------------

    private void saveYamlData() throws IOException {
        for (UUID uuid : collectPlayerIds()) {
            saveYamlData(uuid);
        }
    }

    private void saveYamlData(@NotNull Player player) throws IOException {
        saveYamlData(player.getUniqueId());
    }

    private void saveYamlData(UUID uuid) throws IOException {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry : playerProgress.getOrDefault(uuid, Collections.emptyMap()).entrySet()) {
            yml.set("progress." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Long> entry : completedAchievements.getOrDefault(uuid, Collections.emptyMap()).entrySet()) {
            yml.set("completed." + entry.getKey(), entry.getValue());
        }
        yml.set("notified", new ArrayList<>(notifiedAchievements.getOrDefault(uuid, Collections.emptySet())));
        yml.save(getPlayerFile(uuid, "yml"));
    }

    private void loadYamlData(@NotNull Player player) throws IOException {
        UUID uuid = player.getUniqueId();
        File file = getPlayerFile(uuid, "yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection progressSection = yml.getConfigurationSection("progress");
        if (progressSection != null) {
            Map<String, Integer> progress = new ConcurrentHashMap<>();
            for (String key : progressSection.getKeys(false)) {
                progress.put(key, progressSection.getInt(key));
            }
            playerProgress.put(uuid, progress);
        }

        ConfigurationSection completedSection = yml.getConfigurationSection("completed");
        if (completedSection != null) {
            Map<String, Long> completed = new ConcurrentHashMap<>();
            for (String key : completedSection.getKeys(false)) {
                completed.put(key, completedSection.getLong(key));
            }
            completedAchievements.put(uuid, completed);
        }

        List<String> notified = yml.getStringList("notified");
        if (!notified.isEmpty()) {
            Set<String> set = ConcurrentHashMap.newKeySet();
            set.addAll(notified);
            notifiedAchievements.put(uuid, set);
        }
    }

    // ---------------------------------------------------------------------
    // SQL persistence (uses the active remote database: MySQL / SQLite / H2)
    // ---------------------------------------------------------------------

    private void saveSqliteData() {
        saveToDatabase(collectPlayerIds());
    }

    private void saveMySqlData() {
        saveToDatabase(collectPlayerIds());
    }

    private void saveH2Data() {
        saveToDatabase(collectPlayerIds());
    }

    private void saveSqliteData(@NotNull Player player) {
        saveToDatabase(Collections.singleton(player.getUniqueId()));
    }

    private void saveMySqlData(@NotNull Player player) {
        saveToDatabase(Collections.singleton(player.getUniqueId()));
    }

    private void saveH2Data(@NotNull Player player) {
        saveToDatabase(Collections.singleton(player.getUniqueId()));
    }

    private void saveToDatabase(@NotNull Collection<UUID> uuids) {
        IDatabase database = BedWars.getRemoteDatabase();
        if (database == null) {
            plugin.getLogger().warning("No remote database available to save achievement data.");
            return;
        }
        for (UUID uuid : uuids) {
            database.saveAchievementData(uuid, getProgressJson(uuid), getCompletedJson(uuid), getNotifiedJson(uuid));
        }
    }

    private void loadSqliteData(@NotNull Player player) {
        loadFromDatabase(player.getUniqueId());
    }

    private void loadMySqlData(@NotNull Player player) {
        loadFromDatabase(player.getUniqueId());
    }

    private void loadH2Data(@NotNull Player player) {
        loadFromDatabase(player.getUniqueId());
    }

    private void loadFromDatabase(UUID uuid) {
        IDatabase database = BedWars.getRemoteDatabase();
        if (database == null) return;
        String[] data = database.fetchAchievementData(uuid);
        if (data == null) return;
        applyProgress(uuid, data.length > 0 ? data[0] : null);
        applyCompleted(uuid, data.length > 1 ? data[1] : null);
        applyNotified(uuid, data.length > 2 ? data[2] : null);
    }

    // Achievement implementation class
    private static class AchievementImpl implements IAchievement {
        private final String id;
        private final String name;
        private final String description;
        private final AchievementCategory category;
        private final int requiredAmount;
        private final AchievementReward reward;
        private final AchievementManager manager;

        public AchievementImpl(String id, String name, String description, AchievementCategory category,
                               int requiredAmount, AchievementReward reward, AchievementManager manager) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.category = category;
            this.requiredAmount = requiredAmount;
            this.reward = reward;
            this.manager = manager;
        }

        @Override
        public @NotNull String getId() {
            return id;
        }

        @Override
        public @NotNull String getName() {
            return name;
        }

        @Override
        public @NotNull String getDescription() {
            return description;
        }

        @Override
        public @NotNull AchievementCategory getCategory() {
            return category;
        }

        @Override
        public int getRequiredAmount() {
            return requiredAmount;
        }

        @Override
        public @NotNull AchievementReward getReward() {
            return reward;
        }

        @Override
        public boolean isCompleted(@NotNull Player player) {
            return manager.isAchievementCompleted(player, id);
        }

        @Override
        public void complete(@NotNull Player player) {
            manager.completeAchievement(player, id);
        }

        @Override
        public int getProgress(@NotNull Player player) {
            return manager.getAchievementProgress(player, id);
        }

        @Override
        public void setProgress(@NotNull Player player, int progress) {
            manager.setAchievementProgress(player, id, progress);
        }

        @Override
        public @NotNull String getFormattedProgress(@NotNull Player player) {
            int progress = getProgress(player);
            return progress + "/" + requiredAmount;
        }
    }
}
