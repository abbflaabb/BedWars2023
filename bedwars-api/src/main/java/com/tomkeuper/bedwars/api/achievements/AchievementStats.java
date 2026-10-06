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

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents achievement statistics for a player.
 */
public class AchievementStats {

    private final int totalAchievements;
    private final int completedAchievements;
    private final Map<AchievementCategory, Integer> categoryStats;
    private final long firstAchievementTimestamp;
    private final long lastAchievementTimestamp;

    /**
     * Create new achievement statistics.
     *
     * @param totalAchievements Total number of achievements
     * @param completedAchievements Number of completed achievements
     * @param categoryStats Statistics by category
     * @param firstAchievementTimestamp Timestamp of first achievement completion
     * @param lastAchievementTimestamp Timestamp of last achievement completion
     */
    public AchievementStats(int totalAchievements, int completedAchievements, 
                           @NotNull Map<AchievementCategory, Integer> categoryStats,
                           long firstAchievementTimestamp, long lastAchievementTimestamp) {
        this.totalAchievements = totalAchievements;
        this.completedAchievements = completedAchievements;
        this.categoryStats = categoryStats;
        this.firstAchievementTimestamp = firstAchievementTimestamp;
        this.lastAchievementTimestamp = lastAchievementTimestamp;
    }

    /**
     * Get the total number of achievements.
     *
     * @return Total achievements
     */
    public int getTotalAchievements() {
        return totalAchievements;
    }

    /**
     * Get the number of completed achievements.
     *
     * @return Completed achievements
     */
    public int getCompletedAchievements() {
        return completedAchievements;
    }

    /**
     * Get the completion percentage.
     *
     * @return Completion percentage (0-100)
     */
    public int getCompletionPercentage() {
        if (totalAchievements == 0) return 0;
        return (int) ((completedAchievements * 100.0) / totalAchievements);
    }

    /**
     * Get statistics by category.
     *
     * @return Category statistics
     */
    @NotNull
    public Map<AchievementCategory, Integer> getCategoryStats() {
        return categoryStats;
    }

    /**
     * Get the timestamp of the first achievement completion.
     *
     * @return First achievement timestamp
     */
    public long getFirstAchievementTimestamp() {
        return firstAchievementTimestamp;
    }

    /**
     * Get the timestamp of the last achievement completion.
     *
     * @return Last achievement timestamp
     */
    public long getLastAchievementTimestamp() {
        return lastAchievementTimestamp;
    }

    /**
     * Check if the player has completed any achievements.
     *
     * @return true if has completed achievements, false otherwise
     */
    public boolean hasCompletedAchievements() {
        return completedAchievements > 0;
    }

    /**
     * Get the number of remaining achievements.
     *
     * @return Remaining achievements
     */
    public int getRemainingAchievements() {
        return totalAchievements - completedAchievements;
    }
}