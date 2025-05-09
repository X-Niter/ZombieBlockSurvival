package com.seventodie.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.commands.admin.SevenToDieCommand;
import com.seventodie.commands.frame.FrameCommand;
import com.seventodie.commands.quest.QuestCommand;
import com.seventodie.commands.trader.TraderCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all commands for the SevenToDie plugin.
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    
    private final SevenToDiePlugin plugin;
    private final List<BaseCommand> commands = new ArrayList<>();
    
    public CommandManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        
        // Register commands
        registerCommands();
    }
    
    /**
     * Register all commands with the server
     */
    private void registerCommands() {
        // Add all command classes
        commands.add(new SevenToDieCommand(plugin));
        commands.add(new TraderCommand(plugin));
        commands.add(new QuestCommand(plugin));
        commands.add(new FrameCommand(plugin));
        
        // Register commands with Bukkit
        for (BaseCommand cmd : commands) {
            plugin.getCommand(cmd.getName()).setExecutor(this);
            plugin.getCommand(cmd.getName()).setTabCompleter(this);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Find the matching command
        for (BaseCommand cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(command.getName())) {
                return cmd.execute(sender, args);
            }
        }
        return false;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        // Find the matching command
        for (BaseCommand cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(command.getName())) {
                return cmd.tabComplete(sender, args);
            }
        }
        return new ArrayList<>();
    }
}