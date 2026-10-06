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

/**
 * Represents the reward for completing an achievement.
 */
public class AchievementReward {

    private final int coins;
    private final int experience;
    private final String message;

    /**
     * Create a new achievement reward.
     *
     * @param coins The amount of coins to reward
     * @param experience The amount of experience to reward
     * @param message The message to show when the achievement is completed
     */
    public AchievementReward(int coins, int experience, @NotNull String message) {
        this.coins = coins;
        this.experience = experience;
        this.message = message;
    }

    /**
     * Get the amount of coins to reward.
     *
     * @return The amount of coins
     */
    public int getCoins() {
        return coins;
    }

    /**
     * Get the amount of experience to reward.
     *
     * @return The amount of experience
     */
    public int getExperience() {
        return experience;
    }

    /**
     * Get the message to show when the achievement is completed.
     *
     * @return The completion message
     */
    @NotNull
    public String getMessage() {
        return message;
    }

    /**
     * Check if the reward has any content.
     *
     * @return true if the reward has content, false otherwise
     */
    public boolean hasReward() {
        return coins > 0 || experience > 0 || !message.isEmpty();
    }
}