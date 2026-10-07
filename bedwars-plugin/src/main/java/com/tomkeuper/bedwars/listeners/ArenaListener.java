package com.tomkeuper.bedwars.listeners;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerKillEvent;
import com.tomkeuper.bedwars.api.events.player.PlayerReSpawnEvent;
import com.tomkeuper.bedwars.configuration.InvisConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ArenaListener implements Listener {
    private final Plugin plugin;
    private final InvisConfig invisConfig;
    private final AtomicInteger woodSwordTaskId = new AtomicInteger(-1);
    private final AtomicInteger respawnInvisTaskId = new AtomicInteger(-1);

    public ArenaListener(Plugin plugin, InvisConfig invisConfig) {
        this.plugin = plugin;
        this.invisConfig = invisConfig;
    }

    /**
     * Cancel all running tasks
     */
    public void cancelTasks() {
        int woodTask = woodSwordTaskId.getAndSet(-1);
        int respawnTask = respawnInvisTaskId.getAndSet(-1);
        
        if (woodTask > 0) {
            Bukkit.getScheduler().cancelTask(woodTask);
        }
        if (respawnTask > 0) {
            Bukkit.getScheduler().cancelTask(respawnTask);
        }
    }
    @EventHandler
    public void onArenaStart(GameStateChangeEvent event) {
        IArena arena = event.getArena();
        if (arena != null) {
            List<Player> list = arena.getPlayers();
            if (list != null) {
                // Remove wooden swords from players' inventories if the config option is enabled, to prevent them from being used as a weapon. This is done because wooden swords are often used as a cheap weapon in BedWars, and they can be easily obtained by players. By removing them, it encourages players to use other weapons and adds more variety to the gameplay.
                if (event.getNewState() == GameState.playing) {
                    if (this.invisConfig.isWoodSwordDisappearanceEnabled()) {
                        // Cancel any existing task first
                        cancelTasks();
                        
                        // Create new task with fresh player list
                        int taskId = Bukkit.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
                            List<Player> currentPlayers = arena.getPlayers();
                            if (currentPlayers != null) {
                                currentPlayers.stream()
                                        .filter(arena::isPlayer)
                                        .forEach(p -> {
                                            if (p.getInventory().contains(Material.valueOf(BedWars.getForCurrentVersion("WOOD_SWORD", "WOOD_SWORD", "WOODEN_SWORD"))) &&
                                                    (p.getInventory().contains(Material.STONE_SWORD) ||
                                                            p.getInventory().contains(Material.valueOf(BedWars.getForCurrentVersion("GOLD_SWORD", "GOLD_SWORD", "GOLDEN_SWORD"))) ||
                                                            p.getInventory().contains(Material.IRON_SWORD) ||
                                                            p.getInventory().contains(Material.DIAMOND_SWORD))) {
                                                p.getInventory().remove(Material.valueOf(BedWars.getForCurrentVersion("WOOD_SWORD", "WOOD_SWORD", "WOODEN_SWORD")));
                                            }
                                        });
                            }
                        }, 20L, 10L).getTaskId();
                        
                        woodSwordTaskId.set(taskId);
                    }
                }
                // Apply invisibility effect to players who are currently in the respawn session or spectator mode, if the config option is enabled. This is done to prevent other players from seeing them and to allow them to move around freely without being targeted by enemies. The invisibility effect is applied every 10 ticks (0.5 seconds) to ensure that it remains active as long as the player is in the respawn session or spectator mode.
                if (this.invisConfig.isRespawnSessionInvisibilityEnabled()) {
                    // Cancel any existing task first
                    cancelTasks();
                    
                    int taskId = Bukkit.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
                        List<Player> currentPlayers = arena.getPlayers();
                        if (currentPlayers != null) {
                            currentPlayers.stream()
                                    .filter(arena::isPlayer)
                                    .forEach(p -> {
                                        if (arena.isReSpawning(p)) {
                                            p.addPotionEffect(new PotionEffect(
                                                    PotionEffectType.INVISIBILITY,
                                                    Integer.MAX_VALUE,
                                                    1,
                                                    false,
                                                    false
                                            ));
                                        } else {
                                            p.removePotionEffect(PotionEffectType.INVISIBILITY);
                                        }
                                    });
                        }
                    }, 20L, 10L).getTaskId();
                    
                    respawnInvisTaskId.set(taskId);
                }
                // Apply invisibility effect to players who are currently in the spectator mode, if the config option is enabled. This is done to prevent other players from seeing them and to allow them to move around freely without being targeted by enemies. The invisibility effect is applied every 10 ticks (0.5 seconds) to ensure that it remains active as long as the player is in the spectator mode.
                if (event.getNewState() == GameState.restarting) {
                    Bukkit.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                        list.stream()
                                .filter(arena::isPlayer)
                                .forEach(p -> {
                                    if (arena.isSpectator(p)) {
                                        p.addPotionEffect(new PotionEffect(
                                                PotionEffectType.INVISIBILITY,
                                                Integer.MAX_VALUE,
                                                1,
                                                false,
                                                false
                                        ));
                                    }
                                });
                    }, 10L);
                }
            }
        }
    }
    // this for removing invisibility effect from players when they respawn, to ensure that they are visible to other players and can be targeted by enemies. The invisibility effect is removed 3 ticks (0.15 seconds) after the player respawns to allow them to fully respawn and be ready for combat before becoming visible again.
    @EventHandler
    public void onRespawning(PlayerReSpawnEvent event) {
        Player player = event.getPlayer();
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Bukkit.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }, 3L);
    }
    // This event handler is responsible for handling player kills in the arena. If the config option to disable death animation is enabled, it removes the victim's entity from the game immediately after they are killed, preventing any death animation from playing. Additionally, if the config option to enable kill sound is enabled and the killer is not null, it plays a specified sound at the killer's location with the configured volume and pitch. This enhances the gameplay experience by providing audio feedback for kills and allowing players to customize their experience based on their preferences.
    @EventHandler
    public void onKill(PlayerKillEvent event) {
        if (this.invisConfig.isKillSoundEnabled() && event.getKiller() != null) {
            event.getKiller().playSound(event.getKiller().getLocation(), this.invisConfig.getKillSound(), this.invisConfig.getKillSoundVolume(), this.invisConfig.getKillSoundPitch());
        }
        Player victim = event.getVictim();
        if (victim != null) {
            victim.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }
}
