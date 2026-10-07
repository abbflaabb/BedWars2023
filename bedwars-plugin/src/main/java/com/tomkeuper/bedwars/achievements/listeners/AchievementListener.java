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

package com.tomkeuper.bedwars.achievements.listeners;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.achievements.IAchievementManager;
import com.tomkeuper.bedwars.api.achievements.IAchievement;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.events.player.PlayerBedBreakEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerJoinArenaEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerLeaveArenaEvent;
import com.tomkeuper.bedwars.api.events.gameplay.GameEndEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public class AchievementListener implements Listener {

    private final Plugin plugin;
    private final IAchievementManager achievementManager;

    public AchievementListener() {
        this.plugin = BedWars.getInstance();
        this.achievementManager = BedWars.getAchievementManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load achievement data for player
        achievementManager.loadAchievementData(player);
        
        // Check for first game achievement
        checkFirstGameAchievement(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save achievement data for player
        achievementManager.saveAllAchievementData();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerArenaJoin(PlayerJoinArenaEvent event) {
        Player player = event.getPlayer();
        IArena arena = event.getArena();
        
        // Increment games played counter
        incrementAchievementProgress(player, "games_played");
        
        // Check for first game achievement
        checkFirstGameAchievement(player);
        
        // Check for games played achievements
        checkGamesPlayedAchievements(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerArenaLeave(PlayerLeaveArenaEvent event) {
        Player player = event.getPlayer();
        IArena arena = event.getArena();
        
        // Save achievement data for player
        achievementManager.saveAllAchievementData();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(GameEndEvent event) {
        List<UUID> winners = event.getWinners();
        IArena arena = event.getArena();
        
        // Increment wins counter for all winners
        for (UUID winnerUUID : winners) {
            Player winner = arena.getPlayer(winnerUUID);
            if (winner != null) {
                // Increment wins counter
                incrementAchievementProgress(winner, "wins");
                
                // Check for win achievements
                checkWinAchievements(winner);
                
                // Check for perfect game achievement
                checkPerfectGameAchievement(winner);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKill(PlayerKillEvent event) {
        Player killer = event.getKiller();
        Player victim = event.getVictim();
        IArena arena = event.getArena();
        
        if (killer != null && killer != victim) {
            // Increment kill counter for killer
            incrementAchievementProgress(killer, "kills");
            
            // Check for first kill achievement
            checkFirstKillAchievement(killer);
            
            // Check for kill achievements
            checkKillAchievements(killer);
            
            // Check if it's a final kill
            if (event.getCause().isFinalKill()) {
                // Increment final kill counter for killer
                incrementAchievementProgress(killer, "final_kills");
                
                // Check for final kill achievements
                checkFinalKillAchievements(killer);
            }
        }
        
        // Increment death counter for victim
        incrementAchievementProgress(victim, "deaths");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedBreak(PlayerBedBreakEvent event) {
        Player player = event.getPlayer();
        IArena arena = event.getArena();
        
        // Increment bed break counter
        incrementAchievementProgress(player, "beds_broken");
        
        // Check for bed break achievements
        checkBedBreakAchievements(player);
    }

    // Helper methods for achievement checking
    private void checkFirstGameAchievement(Player player) {
        if (getAchievementProgress(player, "games_played") == 1) {
            completeAchievementIfEligible(player, "first_game");
        }
    }

    private void checkFirstKillAchievement(Player player) {
        if (getAchievementProgress(player, "kills") == 1) {
            completeAchievementIfEligible(player, "first_kill");
        }
    }

    private void checkGamesPlayedAchievements(Player player) {
        int gamesPlayed = getAchievementProgress(player, "games_played");
        
        if (gamesPlayed >= 10) {
            completeAchievementIfEligible(player, "games_played_10");
        }
        
        if (gamesPlayed >= 50) {
            completeAchievementIfEligible(player, "games_played_50");
        }
        
        if (gamesPlayed >= 100) {
            completeAchievementIfEligible(player, "games_played_100");
        }
    }

    private void checkKillAchievements(Player player) {
        int kills = getAchievementProgress(player, "kills");
        
        if (kills >= 10) {
            completeAchievementIfEligible(player, "kills_10");
        }
        
        if (kills >= 50) {
            completeAchievementIfEligible(player, "kills_50");
        }
    }

    private void checkFinalKillAchievements(Player player) {
        int finalKills = getAchievementProgress(player, "final_kills");
        
        if (finalKills >= 5) {
            completeAchievementIfEligible(player, "final_kills_5");
        }
        
        if (finalKills >= 20) {
            completeAchievementIfEligible(player, "final_kills_20");
        }
    }

    private void checkBedBreakAchievements(Player player) {
        int bedsBroken = getAchievementProgress(player, "beds_broken");
        
        if (bedsBroken >= 1) {
            completeAchievementIfEligible(player, "first_bed_break");
        }
        
        if (bedsBroken >= 10) {
            completeAchievementIfEligible(player, "beds_broken_10");
        }
        
        if (bedsBroken >= 50) {
            completeAchievementIfEligible(player, "beds_broken_50");
        }
    }

    private void checkWinAchievements(Player player) {
        int wins = getAchievementProgress(player, "wins");
        
        if (wins >= 1) {
            completeAchievementIfEligible(player, "first_win");
        }
        
        if (wins >= 5) {
            completeAchievementIfEligible(player, "wins_5");
        }
        
        if (wins >= 20) {
            completeAchievementIfEligible(player, "wins_20");
        }
        
        if (wins >= 50) {
            completeAchievementIfEligible(player, "wins_50");
        }
    }

    private void checkPerfectGameAchievement(Player player) {
        if (getAchievementProgress(player, "deaths") == 0) {
            completeAchievementIfEligible(player, "perfect_game");
        }
    }

    private void completeAchievementIfEligible(Player player, String achievementId) {
        IAchievement achievement = achievementManager.getAchievement(achievementId);
        if (achievement != null && !achievementManager.isAchievementCompleted(player, achievementId)) {
            int progress = achievementManager.getAchievementProgress(player, achievementId);
            if (progress >= achievement.getRequiredAmount()) {
                achievementManager.completeAchievement(player, achievementId);
            }
        }
    }

    private void incrementAchievementProgress(Player player, String achievementId) {
        int currentProgress = achievementManager.getAchievementProgress(player, achievementId);
        achievementManager.setAchievementProgress(player, achievementId, currentProgress + 1);
    }

    private int getAchievementProgress(Player player, String achievementId) {
        return achievementManager.getAchievementProgress(player, achievementId);
    }
}