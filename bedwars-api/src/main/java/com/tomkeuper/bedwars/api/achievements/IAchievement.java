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
import org.jetbrains.annotations.Nullable;

/**
 * Represents an achievement in BedWars.
 */
public interface IAchievement {

    /**
     * Get the unique identifier of the achievement.
     *
     * @return The unique identifier
     */
    @NotNull String getId();

    /**
     * Get the display name of the achievement.
     *
     * @return The display name
     */
    @NotNull String getName();

    /**
     * Get the description of the achievement.
     *
     * @return The description
     */
    @NotNull String getDescription();

    /**
     * Get the category of the achievement.
     *
     * @return The category
     */
    @NotNull AchievementCategory getCategory();

    /**
     * Get the required amount to complete the achievement.
     *
     * @return The required amount
     */
    int getRequiredAmount();

    /**
     * Get the reward for completing the achievement.
     *
     * @return The reward
     */
    @NotNull AchievementReward getReward();

    /**
     * Check if the achievement is completed by a player.
     *
     * @param player The player to check
     * @return true if completed, false otherwise
     */
    boolean isCompleted(@NotNull Player player);

    /**
     * Complete the achievement for a player.
     *
     * @param player The player to complete the achievement for
     */
    void complete(@NotNull Player player);

    /**
     * Get the progress of the achievement for a player.
     *
     * @param player The player to get progress for
     * @return The progress amount
     */
    int getProgress(@NotNull Player player);

    /**
     * Set the progress of the achievement for a player.
     *
     * @param player The player to set progress for
     * @param progress The progress amount
     */
    void setProgress(@NotNull Player player, int progress);

    /**
     * Get the formatted progress string.
     *
     * @param player The player to get formatted progress for
     * @return The formatted progress string
     */
    @NotNull String getFormattedProgress(@NotNull Player player);
}