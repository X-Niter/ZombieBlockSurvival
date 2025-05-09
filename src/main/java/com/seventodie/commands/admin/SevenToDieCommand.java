package com.seventodie.commands.admin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.commands.BaseCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command for the SevenToDie plugin.
 */
public class SevenToDieCommand extends BaseCommand {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "info", "trader", "quest", "reset");
    
    public SevenToDieCommand(SevenToDiePlugin plugin) {
        super(plugin, "seventodie", "seventodie.admin", false);
    }
    
    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return showInfo(sender);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return reloadPlugin(sender);
            case "info":
                return showInfo(sender);
            case "trader":
                return redirectToTraderCommand(sender, args);
            case "quest":
                return redirectToQuestCommand(sender, args);
            case "reset":
                return resetPlugin(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }
    
    /**
     * Show plugin information
     * 
     * @param sender The command sender
     * @return True (command handled)
     */
    private boolean showInfo(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "======= " + ChatColor.GREEN + "SevenToDie" + ChatColor.GOLD + " =======");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + plugin.getDescription().getAuthors().get(0));
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + plugin.getDescription().getDescription());
        
        // Display dependency status
        sender.sendMessage(ChatColor.YELLOW + "Dependencies:");
        
        boolean worldEditPresent = plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
        boolean citizensPresent = plugin.getServer().getPluginManager().getPlugin("Citizens") != null;
        boolean protocolLibPresent = plugin.getServer().getPluginManager().getPlugin("ProtocolLib") != null;
        
        sender.sendMessage(ChatColor.YELLOW + " - WorldEdit: " + 
                         (worldEditPresent ? ChatColor.GREEN + "Installed" : ChatColor.RED + "Missing"));
        sender.sendMessage(ChatColor.YELLOW + " - Citizens: " + 
                         (citizensPresent ? ChatColor.GREEN + "Installed" : ChatColor.RED + "Missing"));
        sender.sendMessage(ChatColor.YELLOW + " - ProtocolLib: " + 
                         (protocolLibPresent ? ChatColor.GREEN + "Installed" : ChatColor.RED + "Missing"));
        
        // Display statistics
        int frameBlocks = 0; // Placeholder - would count frame blocks from database
        int traders = plugin.getTraderManager().getAllTraderOutposts().size();
        
        sender.sendMessage(ChatColor.YELLOW + "Statistics:");
        sender.sendMessage(ChatColor.YELLOW + " - Frame Blocks: " + ChatColor.WHITE + frameBlocks);
        sender.sendMessage(ChatColor.YELLOW + " - Traders: " + ChatColor.WHITE + traders);
        
        sender.sendMessage(ChatColor.GOLD + "=============================");
        return true;
    }
    
    /**
     * Reload the plugin
     * 
     * @param sender The command sender
     * @return True (command handled)
     */
    private boolean reloadPlugin(CommandSender sender) {
        try {
            // Reload config
            plugin.getConfigManager().reloadMainConfig();
            
            // Reload any other data as needed
            
            sendSuccess(sender, "SevenToDie plugin reloaded successfully!");
        } catch (Exception e) {
            sendError(sender, "Error reloading plugin: " + e.getMessage());
            plugin.getLogger().severe("Error during plugin reload: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * Reset the plugin (clean database, etc.)
     * 
     * @param sender The command sender
     * @return True (command handled)
     */
    private boolean resetPlugin(CommandSender sender) {
        // This is a dangerous operation, so we want to confirm
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sendError(sender, "This command can only be run by a player for safety reasons.");
            return true;
        }
        
        // TODO: Implement a confirmation system for this command
        
        sendError(sender, "This command is not yet implemented for safety reasons.");
        sendInfo(sender, "It would reset all plugin data including structures, quests, and traders.");
        return true;
    }
    
    /**
     * Redirect to trader command
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean redirectToTraderCommand(CommandSender sender, String[] args) {
        // Remove the first argument (trader) and pass the rest to the trader command
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        plugin.getServer().dispatchCommand(sender, "trader " + String.join(" ", newArgs));
        return true;
    }
    
    /**
     * Redirect to quest command
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean redirectToQuestCommand(CommandSender sender, String[] args) {
        // Remove the first argument (quest) and pass the rest to the quest command
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        plugin.getServer().dispatchCommand(sender, "quest " + String.join(" ", newArgs));
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabCompletions(args, SUBCOMMANDS);
        }
        
        // Handle subcommand tab completions
        if (args.length > 1) {
            String subCommand = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            
            switch (subCommand) {
                case "trader":
                    // Forward to trader command
                    // Ideally, we would directly call the trader command's tab complete method
                    break;
                case "quest":
                    // Forward to quest command
                    // Ideally, we would directly call the quest command's tab complete method
                    break;
            }
        }
        
        return new ArrayList<>();
    }
}