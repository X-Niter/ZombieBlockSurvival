package com.seventodie.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import com.seventodie.SevenToDiePlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all SevenToDie commands.
 */
public abstract class BaseCommand {
    
    protected final SevenToDiePlugin plugin;
    private final String name;
    private final String permission;
    private final boolean playerOnly;
    
    /**
     * Constructor for a command
     * 
     * @param plugin The plugin instance
     * @param name The command name
     * @param permission The permission required to use this command
     * @param playerOnly Whether this command can only be used by players
     */
    public BaseCommand(SevenToDiePlugin plugin, String name, String permission, boolean playerOnly) {
        this.plugin = plugin;
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }
    
    /**
     * Execute the command
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    public boolean execute(CommandSender sender, String[] args) {
        // Check if command is player-only
        if (playerOnly && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        // Check permissions
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Execute the command implementation
        return onCommand(sender, args);
    }
    
    /**
     * Command implementation to be overridden by subclasses
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    protected abstract boolean onCommand(CommandSender sender, String[] args);
    
    /**
     * Tab completion for the command
     * 
     * @param sender The command sender
     * @param args The current command arguments
     * @return List of tab completions
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
    
    /**
     * Get the command name
     * 
     * @return The command name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the required permission
     * 
     * @return The permission
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * Check if command is player-only
     * 
     * @return True if player-only
     */
    public boolean isPlayerOnly() {
        return playerOnly;
    }
    
    /**
     * Send an error message to the sender
     * 
     * @param sender The command sender
     * @param message The error message
     */
    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
    }
    
    /**
     * Send a success message to the sender
     * 
     * @param sender The command sender
     * @param message The success message
     */
    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.GREEN + message);
    }
    
    /**
     * Send an info message to the sender
     * 
     * @param sender The command sender
     * @param message The info message
     */
    protected void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.YELLOW + message);
    }
    
    /**
     * Send the command usage to the sender
     * 
     * @param sender The command sender
     */
    protected void sendUsage(CommandSender sender) {
        if (plugin.getCommand(name) != null) {
            sender.sendMessage(ChatColor.RED + "Usage: " + plugin.getCommand(name).getUsage());
        }
    }
    
    /**
     * Filter a list of tab completions by the start of the last argument
     * 
     * @param args The command arguments
     * @param possibleCompletions The possible completions
     * @return The filtered completions
     */
    protected List<String> filterTabCompletions(String[] args, List<String> possibleCompletions) {
        String lastArg = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        
        for (String completion : possibleCompletions) {
            if (completion.toLowerCase().startsWith(lastArg)) {
                filtered.add(completion);
            }
        }
        
        return filtered;
    }
}