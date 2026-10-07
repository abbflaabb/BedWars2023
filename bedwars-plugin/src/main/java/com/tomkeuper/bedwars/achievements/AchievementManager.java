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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AchievementManager implements IAchievementManager {

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
        this(BedWars.getInstance());
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
                String command = reward.getMessage().replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
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
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load achievement data for " + player.getName() + ": " + e.getMessage());
        }
    }

    // Helper methods
    public void createCategory(String id, String displayName, String color, String icon, String description) {
        categories.put(id, AchievementCategory.valueOf(id.toUpperCase()));
    }

    public void createAchievement(String id, String categoryId, String name, String description, 
                                 int requiredAmount, String rewardType, String rewardValue,
                                 String message, String sound, String particles) {
        AchievementCategory category = AchievementCategory.valueOf(categoryId.toUpperCase());
        AchievementReward reward = new AchievementReward(0, 0, message);
        
        // Create achievement implementation
        IAchievement achievement = new AchievementImpl(id, name, description, category, 
                requiredAmount, reward, this);
        achievements.put(id, achievement);
    }

    public void initializeAchievements() {
        // Load achievements from config
        // This will be called from AchievementConfig
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
        
        // Send title and subtitle
        BedWars.nms.sendTitle(player, "§aAchievement Unlocked!", achievement.getName(), 10, 60, 20);
        
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
        player.sendMessage("§6§l[ACHIEVEMENT] §r" + achievement.getName());
        player.sendMessage("§7" + achievement.getDescription());
    }

    private void saveJsonData() throws IOException {
        // Implementation for JSON saving
    }

    private void saveYamlData() throws IOException {
        // Implementation for YAML saving
    }

    private void saveSqliteData() throws IOException {
        // Implementation for SQLite saving
    }

    private void saveMySqlData() throws IOException {
        // Implementation for MySQL saving
    }

    private void loadJsonData(@NotNull Player player) throws IOException {
        // Implementation for JSON loading
    }

    private void loadYamlData(@NotNull Player player) throws IOException {
        // Implementation for YAML loading
    }

    private void loadSqliteData(@NotNull Player player) throws IOException {
        // Implementation for SQLite loading
    }

    private void loadMySqlData(@NotNull Player player) throws IOException {
        // Implementation for MySQL loading
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
