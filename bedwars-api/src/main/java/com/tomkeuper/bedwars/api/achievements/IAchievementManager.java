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

package com.tomkeuper.bedwars.api.achievements;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages achievements for BedWars.
 */
public interface IAchievementManager {

    /**
     * Get all registered achievements.
     *
     * @return List of all achievements
     */
    @NotNull List<IAchievement> getAllAchievements();

    /**
     * Get an achievement by its ID.
     *
     * @param id The achievement ID
     * @return The achievement, or null if not found
     */
    @Nullable IAchievement getAchievement(@NotNull String id);

    /**
     * Get achievements by category.
     *
     * @param category The category to filter by
     * @return List of achievements in the category
     */
    @NotNull List<IAchievement> getAchievementsByCategory(@NotNull AchievementCategory category);

    /**
     * Check if an achievement is completed by a player.
     *
     * @param player The player to check
     * @param achievementId The achievement ID
     * @return true if completed, false otherwise
     */
    boolean isAchievementCompleted(@NotNull Player player, @NotNull String achievementId);

    /**
     * Get the progress of an achievement for a player.
     *
     * @param player The player to get progress for
     * @param achievementId The achievement ID
     * @return The progress amount
     */
    int getAchievementProgress(@NotNull Player player, @NotNull String achievementId);

    /**
     * Set the progress of an achievement for a player.
     *
     * @param player The player to set progress for
     * @param achievementId The achievement ID
     * @param progress The progress amount
     */
    void setAchievementProgress(@NotNull Player player, @NotNull String achievementId, int progress);

    /**
     * Complete an achievement for a player.
     *
     * @param player The player to complete the achievement for
     * @param achievementId The achievement ID
     */
    void completeAchievement(@NotNull Player player, @NotNull String achievementId);

    /**
     * Get completed achievements for a player.
     *
     * @param player The player to get completed achievements for
     * @return Map of achievement ID to completion timestamp
     */
    @NotNull Map<String, Long> getCompletedAchievements(@NotNull Player player);

    /**
     * Get achievement statistics for a player.
     *
     * @param player The player to get stats for
     * @return Achievement statistics
     */
    @NotNull AchievementStats getAchievementStats(@NotNull Player player);

    /**
     * Check if a player should be notified about an achievement.
     *
     * @param player The player to check
     * @param achievementId The achievement ID
     * @return true if should notify, false otherwise
     */
    boolean shouldNotifyAchievement(@NotNull Player player, @NotNull String achievementId);

    /**
     * Mark an achievement as notified for a player.
     *
     * @param player The player to mark as notified
     * @param achievementId The achievement ID
     */
    void markAchievementNotified(@NotNull Player player, @NotNull String achievementId);

    /**
     * Get achievement progress for all players.
     *
     * @return Map of player UUID to their achievement progress
     */
    @NotNull Map<UUID, Map<String, Integer>> getAllAchievementProgress();

    /**
     * Save achievement data for all players.
     */
    void saveAllAchievementData();

    /**
     * Load achievement data for a player.
     *
     * @param player The player to load data for
     */
    void loadAchievementData(@NotNull Player player);
}