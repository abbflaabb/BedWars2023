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

package com.tomkeuper.bedwars.api.events.achievements;

import com.tomkeuper.bedwars.api.achievements.IAchievement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player makes progress on an achievement.
 */
public class PlayerAchievementProgressEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final IAchievement achievement;
    private final int newProgress;
    private final int oldProgress;
    private final boolean isCompleted;

    /**
     * Create a new PlayerAchievementProgressEvent.
     *
     * @param player The player who made progress
     * @param achievement The achievement progress was made on
     * @param newProgress The new progress amount
     * @param oldProgress The old progress amount
     * @param isCompleted Whether the achievement was completed by this progress
     */
    public PlayerAchievementProgressEvent(@NotNull Player player, @NotNull IAchievement achievement, 
                                        int newProgress, int oldProgress, boolean isCompleted) {
        this.player = player;
        this.achievement = achievement;
        this.newProgress = newProgress;
        this.oldProgress = oldProgress;
        this.isCompleted = isCompleted;
    }

    /**
     * Get the player who made progress.
     *
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the achievement progress was made on.
     *
     * @return The achievement
     */
    @NotNull
    public IAchievement getAchievement() {
        return achievement;
    }

    /**
     * Get the new progress amount.
     *
     * @return The new progress amount
     */
    public int getNewProgress() {
        return newProgress;
    }

    /**
     * Get the old progress amount.
     *
     * @return The old progress amount
     */
    public int getOldProgress() {
        return oldProgress;
    }

    /**
     * Check if the achievement was completed by this progress.
     *
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Get the list of event handlers for the event.
     *
     * @return The list of event handlers
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Get the list of event handlers for the event.
     *
     * @return The list of event handlers
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}