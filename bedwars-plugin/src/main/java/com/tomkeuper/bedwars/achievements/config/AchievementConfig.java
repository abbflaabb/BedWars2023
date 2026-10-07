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

package com.tomkeuper.bedwars.achievements.config;

import com.tomkeuper.bedwars.achievements.AchievementManager;
import com.tomkeuper.bedwars.api.configuration.ConfigManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;


public class AchievementConfig extends ConfigManager {

    private final AchievementManager achievementManager;

    public AchievementConfig(Plugin plugin, String name, String path) {
        this(plugin, name, path, com.tomkeuper.bedwars.BedWars.getAchievementManager());
    }

    public AchievementConfig(Plugin plugin, String name, String path, AchievementManager achievementManager) {
        super(plugin, name, path);
        this.achievementManager = java.util.Objects.requireNonNull(achievementManager, "achievementManager");
        loadAchievements();
    }

    public void onLoad() {
        // Load achievements from config
        loadAchievements();
    }

    public void onStartup() {
        // Initialize achievement system
        achievementManager.initializeAchievements();
    }

    public void onShutdown() {
        // Save achievement data
        achievementManager.saveAllAchievementData();
    }

    private void loadAchievements() {
        YamlConfiguration config = getYml();
        
        // Load categories
        if (config.contains("categories")) {
            for (String categoryId : config.getConfigurationSection("categories").getKeys(false)) {
                String path = "categories." + categoryId;
                String displayName = config.getString(path + ".display_name", "&f" + categoryId);
                String color = config.getString(path + ".color", "GRAY");
                String icon = config.getString(path + ".icon", "PAPER");
                String description = config.getString(path + ".description", "");
                
                // Create category
                achievementManager.createCategory(categoryId, displayName, color, icon, description);
            }
        }
        
        // Load achievements
        if (config.contains("achievements")) {
            for (String achievementId : config.getConfigurationSection("achievements").getKeys(false)) {
                String path = "achievements." + achievementId;
                String categoryId = config.getString(path + ".category", "GENERAL");
                String name = config.getString(path + ".name", "&f" + achievementId);
                String description = config.getString(path + ".description", "");
                int requiredAmount = config.getInt(path + ".required_amount", 1);
                
                // Load reward
                String rewardType = config.getString(path + ".reward.type", "COMMAND");
                String rewardValue = config.getString(path + ".reward.value", "");
                
                // Load settings
                String message = config.getString(path + ".message", "&aAchievement completed!");
                String sound = config.getString(path + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
                String particles = config.getString(path + ".particles", "VILLAGER_HAPPY");
                
                // Create achievement
                achievementManager.createAchievement(
                    achievementId,
                    categoryId,
                    name,
                    description,
                    requiredAmount,
                    rewardType,
                    rewardValue,
                    message,
                    sound,
                    particles
                );
            }
        }
        
        // Load settings
        if (config.contains("settings")) {
            String path = "settings.";
            achievementManager.setEnabled(config.getBoolean(path + "enabled", true));
            achievementManager.setNotificationsEnabled(config.getBoolean(path + "notifications", true));
            achievementManager.setSoundsEnabled(config.getBoolean(path + "sounds", true));
            achievementManager.setParticlesEnabled(config.getBoolean(path + "particles", true));
            achievementManager.setCommandsEnabled(config.getBoolean(path + "commands", true));
            achievementManager.setStatisticsEnabled(config.getBoolean(path + "statistics", true));
            achievementManager.setSaveInterval(config.getInt(path + "save_interval", 5));
            achievementManager.setSaveFormat(config.getString(path + "save_format", "JSON"));
            achievementManager.setSaveLocation(config.getString(path + "save_location", "data/achievements/"));
        }
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    // Config paths for achievement system
    public static class AchievementConfigPath {
        public static final String ENABLED = "settings.enabled";
        public static final String NOTIFICATIONS = "settings.notifications";
        public static final String SOUNDS = "settings.sounds";
        public static final String PARTICLES = "settings.particles";
        public static final String COMMANDS = "settings.commands";
        public static final String STATISTICS = "settings.statistics";
        public static final String SAVE_INTERVAL = "settings.save_interval";
        public static final String SAVE_FORMAT = "settings.save_format";
        public static final String SAVE_LOCATION = "settings.save_location";
        
        public static final String CATEGORIES = "categories";
        public static final String ACHIEVEMENTS = "achievements";
    }
}
