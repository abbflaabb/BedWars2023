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
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import com.tomkeuper.bedwars.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CmdACHIEVEMENTS extends SubCommand implements Listener {

    public CmdACHIEVEMENTS(ParentCommand parent, String name) {
        super(parent, name);
        setPermission("bw.achievements");
        Bukkit.getPluginManager().registerEvents(this, BedWars.plugin);
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
        
            // If no arguments, show achievement GUI
            if (args.length == 0) {
                showAchievementGUI(player, manager);
                return true;
            }
        
            // Handle subcommands
            switch (args[0].toLowerCase()) {
                case "list":
                    showAchievementListGUI(player, manager);
                    break;
                case "stats":
                    showAchievementStatsGUI(player, manager);
                    break;
                case "progress":
                    showAchievementProgressGUI(player, manager);
                    break;
                case "help":
                default:
                    showHelp(player);
                    break;
            }
            return true;
        }

    private void showAchievementGUI(Player player, IAchievementManager manager) {
        Inventory gui = new AchievementMenu(9, "Achievements").getInventory();
        
        // Create glass border
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue; // Skip center slot
            gui.setItem(i, new ItemBuilder(Material.STAINED_GLASS_PANE).setName(" ").setDurability((short) 7).build());
        }
        
        // Center slot - main achievements button
        ItemBuilder mainItem = new ItemBuilder(Material.BOOK);
        mainItem.setName("§6§lAchievements");
        mainItem.setLore(Arrays.asList(
            "§7View all your achievements",
            "§7and track your progress",
            "",
            "§eClick to browse achievements"
        ));
        gui.setItem(4, mainItem.build());
        
        // Open GUI
        player.openInventory(gui);
    }
    
    private void showAchievementListGUI(Player player, IAchievementManager manager) {
        Inventory gui = new AchievementMenu(54, "Achievement List").getInventory();
        
        // Create glass border
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, new ItemBuilder(Material.STAINED_GLASS_PANE).setName(" ").setDurability((short) 7).build());
        }
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, new ItemBuilder(Material.STAINED_GLASS_PANE).setName(" ").setDurability((short) 7).build());
        }
        
        // Title
        ItemBuilder title = new ItemBuilder(Material.BOOK);
        title.setName("§6§lAchievement List");
        title.setLore(Arrays.asList(
            "§7Browse all available achievements",
            "§7and track your progress"
        ));
        gui.setItem(4, title.build());
        
        // Add achievements by category
        int slot = 10;
        for (AchievementCategory category : AchievementCategory.values()) {
            List<IAchievement> achievements = manager.getAchievementsByCategory(category);
            if (achievements.isEmpty()) continue;
            
            // Category header
            ItemBuilder categoryItem = new ItemBuilder(Material.PAPER);
            categoryItem.setName("§e§l" + category.getDisplayName());
            categoryItem.setLore(Arrays.asList(
                "§7" + achievements.size() + " achievements",
                "",
                "§eClick to view"
            ));
            gui.setItem(slot, categoryItem.build());
            
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
            if (slot >= 45) break;
        }
        
        // Back button
        ItemBuilder backItem = new ItemBuilder(Material.ARROW);
        backItem.setName("§c§lBack");
        backItem.setLore(Arrays.asList("§eClick to return"));
        gui.setItem(49, backItem.build());
        
        // Open GUI
        player.openInventory(gui);
    }
    
    private void showAchievementStatsGUI(Player player, IAchievementManager manager) {
        Inventory gui = new AchievementMenu(54, "Achievement Stats").getInventory();
        
        // Create glass border
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, new ItemBuilder(Material.STAINED_GLASS_PANE).setName(" ").setDurability((short) 7).build());
        }
        
        // Stats
        AchievementStats stats = manager.getAchievementStats(player);
        
        ItemBuilder statsItem = new ItemBuilder(Material.BOOK);
        statsItem.setName("§6§lYour Achievement Stats");
        statsItem.setLore(Arrays.asList(
            "",
            "§eTotal Achievements: §f" + stats.getTotalAchievements(),
            "§eCompleted: §f" + stats.getCompletedAchievements(),
            "§eCompletion: §f" + stats.getCompletionPercentage() + "%",
            "§eRemaining: §f" + stats.getRemainingAchievements(),
            "",
            "§eClick for category breakdown"
        ));
        gui.setItem(4, statsItem.build());
        
        // Category breakdown
        ItemBuilder categoryItem = new ItemBuilder(Material.PAPER);
        categoryItem.setName("§e§lCategory Breakdown");
        StringBuilder lore = new StringBuilder();
        for (Map.Entry<AchievementCategory, Integer> entry : stats.getCategoryStats().entrySet()) {
            lore.append("§7").append(entry.getKey().getDisplayName()).append(": §f").append(entry.getValue()).append("\n");
        }
        categoryItem.setLore(Arrays.asList(lore.toString().split("\n")));
        gui.setItem(22, categoryItem.build());
        
        // Back button
        ItemBuilder backItem = new ItemBuilder(Material.ARROW);
        backItem.setName("§c§lBack");
        backItem.setLore(Arrays.asList("§eClick to return"));
        gui.setItem(49, backItem.build());
        
        // Open GUI
        player.openInventory(gui);
    }
    
    private void showAchievementProgressGUI(Player player, IAchievementManager manager) {
        Inventory gui = new AchievementMenu(54, "Achievement Progress").getInventory();
        
        // Create glass border
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, new ItemBuilder(Material.STAINED_GLASS_PANE).setName(" ").setDurability((short) 7).build());
        }
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, new ItemBuilder(Material.STAINED_GLASS_PANE).setName(" ").setDurability((short) 7).build());
        }
        
        // Title
        ItemBuilder title = new ItemBuilder(Material.BOOK);
        title.setName("§6§lAchievement Progress");
        title.setLore(Arrays.asList(
            "§7Track your progress towards",
            "§7all achievements"
        ));
        gui.setItem(4, title.build());
        
        // Add achievements
        int slot = 10;
        for (IAchievement achievement : manager.getAllAchievements()) {
            if (slot >= 45) break;
            
            int progress = achievement.getProgress(player);
            int required = achievement.getRequiredAmount();
            boolean completed = achievement.isCompleted(player);
            
            ItemBuilder achievementItem = new ItemBuilder(Material.PAPER);
            achievementItem.setName(completed ? "§a§l" + achievement.getName() : "§c§l" + achievement.getName());
            achievementItem.setLore(Arrays.asList(
                "§7" + achievement.getDescription(),
                "",
                "§eProgress: §f" + progress + "/" + required,
                "§eStatus: " + (completed ? "§aCompleted" : "§cIn Progress"),
                "",
                "§eClick for details"
            ));
            gui.setItem(slot, achievementItem.build());
            
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }
        
        // Back button
        ItemBuilder backItem = new ItemBuilder(Material.ARROW);
        backItem.setName("§c§lBack");
        backItem.setLore(Arrays.asList("§eClick to return"));
        gui.setItem(49, backItem.build());
        
        // Open GUI
        player.openInventory(gui);
    }

    private void showHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l=== BedWars Achievements Help ===");
        player.sendMessage("");
        player.sendMessage("§e/bw achievements §7- Show achievement GUI");
        player.sendMessage("§e/bw achievements list §7- Show all achievements");
        player.sendMessage("§e/bw achievements stats §7- Show your achievement stats");
        player.sendMessage("§e/bw achievements progress §7- Show your achievement progress");
        player.sendMessage("");
    }

    private static class AchievementMenu implements InventoryHolder {
        private final Inventory inventory;

        private AchievementMenu(int size, String title) {
            inventory = Bukkit.createInventory(this, size, title);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        Inventory menu = event.getView().getTopInventory();
        if (!(menu.getHolder() instanceof AchievementMenu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= menu.getSize()) return;
        Player player = (Player) event.getWhoClicked();
        IAchievementManager manager = BedWars.getAchievementManager();
        if (manager == null) return;
        Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != menu) return;
            if (slot == 49) {
                showAchievementGUI(player, manager);
            } else if (menu.getSize() == 9 && slot == 4) {
                showAchievementListGUI(player, manager);
            } else if (menu.getName().equals("Achievement List") && slot >= 10 && slot < 45) {
                showAchievementProgressGUI(player, manager);
            }
        });
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AchievementMenu) {
            event.setCancelled(true);
        }
    }

    @Override
    public List<String> getTabComplete() {
        return Arrays.asList("list", "stats", "progress", "help");
    }
}
