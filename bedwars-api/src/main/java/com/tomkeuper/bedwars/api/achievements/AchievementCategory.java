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

/**
 * Represents the categories for achievements.
 */
public enum AchievementCategory {
    
    /**
     * General achievements (first play, games played, etc.)
     */
    GENERAL("General"),
    
    /**
     * Combat achievements (kills, final kills, etc.)
     */
    COMBAT("Combat"),
    
    /**
     * Bed-related achievements (bed breaks, bed defenses, etc.)
     */
    BED("Bed"),
    
    /**
     * Victory achievements (wins, win streaks, etc.)
     */
    VICTORY("Victory"),
    
    /**
     * Special achievements (rare achievements, milestones, etc.)
     */
    SPECIAL("Special"),
    
    /**
     * Challenge achievements (daily/weekly challenges)
     */
    CHALLENGE("Challenge");

    private final String displayName;

    AchievementCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the display name of the category.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
}