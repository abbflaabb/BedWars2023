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

package com.tomkeuper.bedwars.commands.bedwars.subcmds.regular;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.achievements.IAchievement;
import com.tomkeuper.bedwars.api.achievements.IAchievementManager;
import com.tomkeuper.bedwars.api.achievements.AchievementCategory;
import com.tomkeuper.bedwars.api.achievements.AchievementStats;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CmdACHIEVEMENTS extends SubCommand {

    public CmdACHIEVEMENTS(ParentCommand parent, String name) {
        super(parent, name);
        setPermission("bw.achievements");
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (!(s instanceof Player)) {
            s.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) s;
        IAchievementManager manager = BedWars.getAchievementManager();
        
        // Check if achievements are enabled
        if (manager == null) {
            player.sendMessage("§cAchievements are not enabled.");
            return true;
        }

        // If no arguments, show achievement menu
        if (args.length == 0) {
            showAchievementMenu(player, manager);
            return true;
        }

        // Handle subcommands
        switch (args[0].toLowerCase()) {
            case "list":
                showAchievementList(player, manager);
                break;
            case "stats":
                showAchievementStats(player, manager);
                break;
            case "progress":
                showAchievementProgress(player, manager);
                break;
            case "help":
            default:
                showHelp(player);
                break;
        }
        return true;
    }

    private void showAchievementMenu(Player player, IAchievementManager manager) {
        player.sendMessage("");
        player.sendMessage("§6§l=== BedWars Achievements ===");
        player.sendMessage("");
        player.sendMessage("§e/bw achievements list §7- Show all achievements");
        player.sendMessage("§e/bw achievements stats §7- Show your achievement stats");
        player.sendMessage("§e/bw achievements progress §7- Show your achievement progress");
        player.sendMessage("");
    }

    private void showAchievementList(Player player, IAchievementManager manager) {
        player.sendMessage("");
        player.sendMessage("§6§l=== Achievement List ===");
        player.sendMessage("");
        
        // Group achievements by category
        for (AchievementCategory category : AchievementCategory.values()) {
            List<IAchievement> achievements = manager.getAchievementsByCategory(category);
            if (achievements.isEmpty()) continue;
            
            player.sendMessage("§e§l" + category.getDisplayName() + ":");
            for (IAchievement achievement : achievements) {
                boolean completed = achievement.isCompleted(player);
                String status = completed ? "§a✔" : "§c✘";
                player.sendMessage("  " + status + " §f" + achievement.getName() + 
                        " §7- " + achievement.getDescription() + 
                        " §8(" + achievement.getFormattedProgress(player) + ")");
            }
            player.sendMessage("");
        }
    }

    private void showAchievementStats(Player player, IAchievementManager manager) {
        AchievementStats stats = manager.getAchievementStats(player);
        
        player.sendMessage("");
        player.sendMessage("§6§l=== Achievement Stats ===");
        player.sendMessage("");
        player.sendMessage("§eTotal Achievements: §f" + stats.getTotalAchievements());
        player.sendMessage("§eCompleted: §f" + stats.getCompletedAchievements());
        player.sendMessage("§eCompletion: §f" + stats.getCompletionPercentage() + "%");
        player.sendMessage("§eRemaining: §f" + stats.getRemainingAchievements());
        player.sendMessage("");
        
        // Show category stats
        player.sendMessage("§6§lCategory Breakdown:");
        for (Map.Entry<AchievementCategory, Integer> entry : stats.getCategoryStats().entrySet()) {
            player.sendMessage("  §e" + entry.getKey().getDisplayName() + ": §f" + entry.getValue());
        }
        player.sendMessage("");
    }

    private void showAchievementProgress(Player player, IAchievementManager manager) {
        player.sendMessage("");
        player.sendMessage("§6§l=== Achievement Progress ===");
        player.sendMessage("");
        
        for (IAchievement achievement : manager.getAllAchievements()) {
            int progress = achievement.getProgress(player);
            int required = achievement.getRequiredAmount();
            boolean completed = achievement.isCompleted(player);
            
            String bar = getProgressBar(progress, required);
            String status = completed ? "§a✔" : "§c✘";
            
            player.sendMessage("  " + status + " §f" + achievement.getName() + 
                    " §7" + bar + " §8(" + progress + "/" + required + ")");
        }
        player.sendMessage("");
    }

    private String getProgressBar(int progress, int required) {
        int totalBars = 10;
        int filledBars = (int) ((progress * 1.0 / required) * totalBars);
        if (filledBars > totalBars) filledBars = totalBars;
        
        StringBuilder bar = new StringBuilder();
        bar.append("§a");
        for (int i = 0; i < filledBars; i++) {
            bar.append("■");
        }
        bar.append("§7");
        for (int i = filledBars; i < totalBars; i++) {
            bar.append("■");
        }
        return bar.toString();
    }

    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l=== BedWars Achievements Help ===");
        player.sendMessage("");
        player.sendMessage("§e/bw achievements §7- Show achievement menu");
        player.sendMessage("§e/bw achievements list §7- Show all achievements");
        player.sendMessage("§e/bw achievements stats §7- Show your achievement stats");
        player.sendMessage("§e/bw achievements progress §7- Show your achievement progress");
        player.sendMessage("");
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("list", "stats", "progress", "help");
    }
}