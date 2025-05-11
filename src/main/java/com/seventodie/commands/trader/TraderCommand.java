package com.seventodie.commands.trader;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.commands.BaseCommand;
import com.seventodie.traders.TraderManager;
import com.seventodie.worldgen.StructureManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Command for managing traders.
 */
public class TraderCommand extends BaseCommand {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "spawn", "remove");
    
    public TraderCommand(SevenToDiePlugin plugin) {
        super(plugin, "trader", "seventodie.trader", false);
    }
    
    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return listTraders(sender);
            case "spawn":
                return spawnTrader(sender, args);
            case "remove":
                return removeTrader(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }
    
    /**
     * List all traders in the world
     * 
     * @param sender The command sender
     * @return True (command handled)
     */
    private boolean listTraders(CommandSender sender) {
        List<TraderManager.TraderOutpost> traders = plugin.getTraderManager().getAllTraderOutposts();
        
        if (traders.isEmpty()) {
            sendInfo(sender, "There are no traders in the world.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "===== Traders (" + traders.size() + ") =====");
        
        int count = 0;
        for (TraderManager.TraderOutpost trader : traders) {
            Location loc = trader.getLocation();
            boolean isOpen = trader.isOpen();
            
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(count + 1) + ". " + 
                             ChatColor.WHITE + trader.getStructureId() + " " +
                             ChatColor.GRAY + "(" + loc.getWorld().getName() + ", " + 
                             loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ") " +
                             (isOpen ? ChatColor.GREEN + "Open" : ChatColor.RED + "Closed"));
            count++;
        }
        
        return true;
    }
    
    /**
     * Spawn a trader at the player's location
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean spawnTrader(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendError(sender, "This command can only be run by a player.");
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getLocation();
        
        try {
            // Create a trader outpost structure
            StructureManager structureManager = plugin.getStructureManager();
            StructureManager.Structure structure = structureManager.placeRandomStructure(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                StructureManager.StructureType.TRADER_OUTPOST,
                0 // No rotation
            );
            
            if (structure == null) {
                sendError(sender, "Failed to create trader outpost structure.");
                return true;
            }
            
            // Register the trader outpost with the trader manager
            plugin.getTraderManager().registerTraderOutpost(structure);
            
            sendSuccess(sender, "Spawned a new trader outpost at your location.");
            
        } catch (Exception e) {
            sendError(sender, "Error spawning trader: " + e.getMessage());
            plugin.getLogger().severe("Error spawning trader: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    /**
     * Remove a trader
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean removeTrader(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /trader remove <id>");
            return true;
        }
        
        String idString = args[1];
        UUID id;
        
        try {
            id = UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            sendError(sender, "Invalid UUID format. Use the UUID from the trader list.");
            return true;
        }
        
        // Find the trader outpost
        TraderManager traderManager = plugin.getTraderManager();
        for (TraderManager.TraderOutpost outpost : traderManager.getAllTraderOutposts()) {
            if (outpost.getStructureId().equals(id)) {
                // TODO: Implement trader removal in TraderManager
                sendError(sender, "Trader removal is not yet implemented.");
                return true;
            }
        }
        
        sendError(sender, "Could not find a trader with the ID: " + idString);
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabCompletions(args, SUBCOMMANDS);
        }
        
        // Handle subcommand tab completions
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("remove")) {
                // Complete with trader IDs
                List<String> traderIds = new ArrayList<>();
                for (TraderManager.TraderOutpost outpost : plugin.getTraderManager().getAllTraderOutposts()) {
                    traderIds.add(outpost.getStructureId().toString());
                }
                return filterTabCompletions(args, traderIds);
            }
        }
        
        return new ArrayList<>();
    }
}