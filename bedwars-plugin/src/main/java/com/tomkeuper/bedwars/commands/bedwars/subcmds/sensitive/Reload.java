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

package com.tomkeuper.bedwars.commands.bedwars.subcmds.sensitive;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.command.ParentCommand;
import com.tomkeuper.bedwars.api.command.SubCommand;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.arena.Misc;
import com.tomkeuper.bedwars.commands.bedwars.MainCommand;
import com.tomkeuper.bedwars.configuration.Permissions;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;

import java.util.List;

public class Reload extends SubCommand {

    public Reload(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(11);
        showInList(true);
        setPermission(Permissions.PERMISSION_RELOAD);
        setDisplayInfo(Misc.msgHoverClick("§6 ▪ §7/" + getParent().getName() + " "+getSubCommandName()+"       §8 - §ereload messages and chest settings",
                "§fReload messages and chest settings.\n§7Use /bw reload resourcechest for chest settings only.", "/"+ getParent().getName() + " "+getSubCommandName(), ClickEvent.Action.RUN_COMMAND));
    }

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (!hasPermission(s)) return true;
        if (args.length > 0 && !args[0].equalsIgnoreCase("resourcechest")) {
            s.sendMessage("§cUsage: /bw reload [resourcechest]");
            return true;
        }
        try {
            com.tomkeuper.bedwars.arena.feature.ResourceChestFeature.reloadConfiguration();
            s.sendMessage("§aResource chest settings reloaded; active holograms refreshed.");
        } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException | IllegalArgumentException ex) {
            s.sendMessage("§cResource chest reload failed: " + ex.getMessage());
            return true;
        }
        if (args.length > 0) return true;
        if (!MainCommand.isLobbySet()) {
            s.sendMessage("§c▪ §7You have to set the lobby location first!");
            return true;
        }
        for (Language l : Language.getLanguages()){
            l.reload();
            s.sendMessage("§6 ▪ §7"+l.getLangName()+" reloaded!");
        }
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        return java.util.Collections.singletonList("resourcechest");
    }

    @Override
    public boolean canSee(CommandSender s, BedWars api) {
        return hasPermission(s);
    }
}
