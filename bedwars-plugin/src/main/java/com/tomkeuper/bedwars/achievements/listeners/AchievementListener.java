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
import org.bukkit.Bukkit;
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
        this.plugin = BedWars.plugin;
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
        achievementManager.saveAchievementData(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerArenaJoin(PlayerJoinArenaEvent event) {
        // Spectators are not playing a game, so they must not count towards games played
        if (event.isSpectator()) return;

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

        // Save achievement data for player
        achievementManager.saveAchievementData(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(GameEndEvent event) {
        List<UUID> winners = event.getWinners();
        IArena arena = event.getArena();

        // Increment wins counter for all winners
        for (UUID winnerUUID : winners) {
            Player winner = arena.getPlayer(winnerUUID);
            if (winner == null) {
                // The player may already have been removed from the arena when the game ends
                winner = Bukkit.getPlayer(winnerUUID);
            }
            if (winner == null) continue;

            // Increment wins counter
            incrementAchievementProgress(winner, "wins");

            // Check for win achievements
            checkWinAchievements(winner);

            // Check for perfect game achievement
            checkPerfectGameAchievement(winner);
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
            if (event.getCause() != null && event.getCause().isFinalKill()) {
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
        completeAchievementIfEligible(player, "first_game", getAchievementProgress(player, "games_played"));
    }

    private void checkFirstKillAchievement(Player player) {
        completeAchievementIfEligible(player, "first_kill", getAchievementProgress(player, "kills"));
    }

    private void checkGamesPlayedAchievements(Player player) {
        int gamesPlayed = getAchievementProgress(player, "games_played");

        completeAchievementIfEligible(player, "games_played_10", gamesPlayed);
        completeAchievementIfEligible(player, "games_played_50", gamesPlayed);
        completeAchievementIfEligible(player, "games_played_100", gamesPlayed);
    }

    private void checkKillAchievements(Player player) {
        int kills = getAchievementProgress(player, "kills");

        completeAchievementIfEligible(player, "kills_10", kills);
        completeAchievementIfEligible(player, "kills_50", kills);
    }

    private void checkFinalKillAchievements(Player player) {
        int finalKills = getAchievementProgress(player, "final_kills");

        completeAchievementIfEligible(player, "final_kills_5", finalKills);
        completeAchievementIfEligible(player, "final_kills_20", finalKills);
    }

    private void checkBedBreakAchievements(Player player) {
        int bedsBroken = getAchievementProgress(player, "beds_broken");

        completeAchievementIfEligible(player, "first_bed_break", bedsBroken);
        completeAchievementIfEligible(player, "beds_broken_10", bedsBroken);
        completeAchievementIfEligible(player, "beds_broken_50", bedsBroken);
    }

    private void checkWinAchievements(Player player) {
        int wins = getAchievementProgress(player, "wins");

        completeAchievementIfEligible(player, "first_win", wins);
        completeAchievementIfEligible(player, "wins_5", wins);
        completeAchievementIfEligible(player, "wins_20", wins);
        completeAchievementIfEligible(player, "wins_50", wins);
    }

    private void checkPerfectGameAchievement(Player player) {
        int deaths = getAchievementProgress(player, "deaths");
        // A perfect game is a win without dying. The achievement requires a single occurrence.
        completeAchievementIfEligible(player, "perfect_game", deaths == 0 ? 1 : 0);
    }

    /**
     * Sync the tracked stat {@code progress} onto the given achievement and complete it when
     * the progress reaches the achievement's required amount.
     *
     * @param player        the player
     * @param achievementId the achievement id
     * @param progress      the current value of the stat backing the achievement
     */
    private void completeAchievementIfEligible(Player player, String achievementId, int progress) {
        IAchievement achievement = achievementManager.getAchievement(achievementId);
        if (achievement == null) return;

        // Keep the achievement's own progress in sync with the tracked stat so the GUI shows correct values
        achievementManager.setAchievementProgress(player, achievementId, progress);

        if (!achievementManager.isAchievementCompleted(player, achievementId)
                && progress >= achievement.getRequiredAmount()) {
            achievementManager.completeAchievement(player, achievementId);
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