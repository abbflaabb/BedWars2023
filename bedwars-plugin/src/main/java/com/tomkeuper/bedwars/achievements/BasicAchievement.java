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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class BasicAchievement implements IAchievement {

    private final String id;
    private final String name;
    private final String description;
    private final AchievementCategory category;
    private final int requiredAmount;
    private final AchievementReward reward;

    public BasicAchievement(@NotNull String id, @NotNull String name, @NotNull String description,
                           @NotNull AchievementCategory category, int requiredAmount, @NotNull AchievementReward reward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.requiredAmount = requiredAmount;
        this.reward = reward;
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
        // This would be implemented to check database
        // For now, return false
        return false;
    }

    @Override
    public void complete(@NotNull Player player) {
        // This would be implemented to mark as completed in database
        // For now, just log
        System.out.println("Achievement " + id + " completed by " + player.getName());
    }

    @Override
    public int getProgress(@NotNull Player player) {
        // This would be implemented to get progress from database
        // For now, return 0
        return 0;
    }

    @Override
    public void setProgress(@NotNull Player player, int progress) {
        // This would be implemented to save progress to database
        // For now, just log
        System.out.println("Achievement " + id + " progress set to " + progress + " for " + player.getName());
    }

    @Override
    public @NotNull String getFormattedProgress(@NotNull Player player) {
        int progress = getProgress(player);
        return progress + "/" + requiredAmount;
    }
}