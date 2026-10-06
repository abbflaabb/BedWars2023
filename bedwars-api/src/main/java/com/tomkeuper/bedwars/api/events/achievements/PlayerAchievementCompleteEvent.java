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
 * Called when a player completes an achievement.
 */
public class PlayerAchievementCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final IAchievement achievement;
    private final long completionTime;

    /**
     * Create a new PlayerAchievementCompleteEvent.
     *
     * @param player The player who completed the achievement
     * @param achievement The achievement that was completed
     * @param completionTime The timestamp when the achievement was completed
     */
    public PlayerAchievementCompleteEvent(@NotNull Player player, @NotNull IAchievement achievement, long completionTime) {
        this.player = player;
        this.achievement = achievement;
        this.completionTime = completionTime;
    }

    /**
     * Get the player who completed the achievement.
     *
     * @return The player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the achievement that was completed.
     *
     * @return The achievement
     */
    @NotNull
    public IAchievement getAchievement() {
        return achievement;
    }

    /**
     * Get the timestamp when the achievement was completed.
     *
     * @return Completion timestamp
     */
    public long getCompletionTime() {
        return completionTime;
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