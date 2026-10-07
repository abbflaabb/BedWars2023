package com.tomkeuper.bedwars.support.version.v1_8_R3;

import com.tomkeuper.bedwars.support.version.common.VersionCommon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

/** Prevent survival achievement announcements for BedWars participants. */
public class VanillaAchievementListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAchievement(PlayerAchievementAwardedEvent event) {
        if (VersionCommon.api.getArenaUtil().getArenaByPlayer(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }
}
