package com.seventodie.commands.frame;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.blocks.BlockManager;
import com.seventodie.blocks.FrameBlock;
import com.seventodie.commands.BaseCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command for managing frame blocks.
 */
public class FrameCommand extends BaseCommand {
    
    private static final List<String> SUBCOMMANDS = Arrays.asList("give", "list", "info");
    private static final List<String> FRAME_TYPES = Arrays.asList("wood", "cobble", "rebar", "concrete");
    
    public FrameCommand(SevenToDiePlugin plugin) {
        super(plugin, "frame", "seventodie.frame", true);
    }
    
    @Override
    protected boolean onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendError(sender, "This command can only be run by a player.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                return giveFrameBlock(player, args);
            case "list":
                return listFrameBlocks(player);
            case "info":
                return blockInfo(player, args);
            default:
                sendUsage(sender);
                return true;
        }
    }
    
    /**
     * Give a frame block to the player
     * 
     * @param player The player
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean giveFrameBlock(Player player, String[] args) {
        if (args.length < 2) {
            sendError(player, "Usage: /frame give <type> [amount]");
            sendInfo(player, "Types: wood, cobble, rebar, concrete");
            return true;
        }
        
        String type = args[1].toLowerCase();
        int amount = 1;
        
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                amount = Math.min(amount, 64); // Limit stack size
            } catch (NumberFormatException e) {
                sendError(player, "Amount must be a number.");
                return true;
            }
        }
        
        // Validate frame type
        BlockManager.FrameType frameType;
        try {
            frameType = BlockManager.FrameType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            sendError(player, "Invalid frame type. Valid types: wood, cobble, rebar, concrete");
            return true;
        }
        
        // Get the block item from BlockManager
        ItemStack frameItem = plugin.getBlockManager().createFrameBlockItem(frameType, amount);
        if (frameItem == null) {
            sendError(player, "Error creating frame block.");
            return true;
        }
        
        // Give item to player
        player.getInventory().addItem(frameItem);
        sendSuccess(player, "Gave you " + amount + " " + type + " frame blocks.");
        
        return true;
    }
    
    /**
     * List all frame block types
     * 
     * @param player The player
     * @return True (command handled)
     */
    private boolean listFrameBlocks(Player player) {
        player.sendMessage(ChatColor.GOLD + "===== Frame Block Types =====");
        
        for (BlockManager.FrameType type : BlockManager.FrameType.values()) {
            String name = type.name().toLowerCase();
            Material material = plugin.getBlockManager().getFrameBlockMaterial(type);
            int durability = plugin.getBlockManager().getFrameBlockDurability(type);
            
            player.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + name + 
                      ChatColor.GRAY + " (" + material.name() + ", Durability: " + durability + ")");
        }
        
        return true;
    }
    
    /**
     * Show information about the frame block in hand
     * 
     * @param player The player
     * @param args The command arguments
     * @return True (command handled)
     */
    private boolean blockInfo(Player player, String[] args) {
        ItemStack handItem = player.getInventory().getItemInMainHand();
        
        // Check if the item is a frame block
        if (!plugin.getBlockManager().isFrameBlock(handItem)) {
            sendError(player, "You are not holding a frame block.");
            return true;
        }
        
        // Get frame block data
        FrameBlock frameBlock = plugin.getBlockManager().getFrameBlockFromItem(handItem);
        if (frameBlock == null) {
            sendError(player, "Could not get frame block data.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "===== Frame Block Info =====");
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + frameBlock.getType().name().toLowerCase());
        player.sendMessage(ChatColor.YELLOW + "Durability: " + ChatColor.WHITE + frameBlock.getDurability() + 
                         "/" + frameBlock.getMaxDurability());
        
        int upgradeProgress = frameBlock.getUpgradeProgress();
        int upgradeRequired = frameBlock.getUpgradeRequirement();
        
        if (upgradeRequired > 0) {
            player.sendMessage(ChatColor.YELLOW + "Upgrade Progress: " + ChatColor.WHITE + 
                             upgradeProgress + "/" + upgradeRequired + 
                             ChatColor.GRAY + " (" + (upgradeProgress * 100 / upgradeRequired) + "%)");
        }
        
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
            
            if (subCommand.equals("give")) {
                return filterTabCompletions(args, FRAME_TYPES);
            }
        }
        
        // Amount suggestions for the give command
        if (args.length == 3 && args[0].toLowerCase().equals("give")) {
            return filterTabCompletions(args, Arrays.asList("1", "16", "32", "64"));
        }
        
        return new ArrayList<>();
    }
}