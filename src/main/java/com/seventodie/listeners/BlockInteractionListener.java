package com.seventodie.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.tools.ToolManager;
import com.seventodie.worldgen.StructureManager;
import com.seventodie.worldgen.StructureManager.Structure;

/**
 * Handles block interaction events, such as placing and breaking
 * custom blocks, and using tools on blocks.
 */
public class BlockInteractionListener implements Listener {
    
    private final SevenToDiePlugin plugin;
    
    public BlockInteractionListener(SevenToDiePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle block place events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if this is a custom block
        boolean handled = plugin.getBlockManager().handleBlockPlace(event);
        
        // If it's a custom block, we might want to do additional handling here
        if (handled) {
            // For now, just log for debugging
            plugin.getLogger().fine("Custom block placed by " + event.getPlayer().getName());
        }
    }
    
    /**
     * Handle block break events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if this is a custom block
        boolean handled = plugin.getBlockManager().handleBlockBreak(event);
        
        // If it's a custom block, we might want to do additional handling here
        if (handled) {
            // For now, just log for debugging
            plugin.getLogger().fine("Custom block broken by " + event.getPlayer().getName());
        }
        
        // Check if block is part of a structure
        Structure structure = plugin.getStructureManager().getStructureAt(event.getBlock().getLocation());
        if (structure != null) {
            // For now, allow breaking but we might want to prevent it or track changes
            plugin.getLogger().fine("Block broken in structure: " + structure.getId());
        }
    }
    
    /**
     * Handle player interact events for custom blocks and tools
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Skip if not a right-click on a block with an item
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || 
            event.getClickedBlock() == null || 
            event.getItem() == null ||
            event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        
        // Check if this is a hammer tool being used on a block
        ToolManager toolManager = plugin.getToolManager();
        if (toolManager.isHammerTool(item)) {
            // Handle block upgrade
            boolean handled = plugin.getBlockManager().handleBlockInteraction(player, block, item);
            
            if (handled) {
                // Cancel the event to prevent normal block interaction
                event.setCancelled(true);
                return;
            }
        }
        
        // Handle quest markers
        if (item.getType() == Material.PAPER && item.hasItemMeta() && 
            item.getItemMeta().hasDisplayName() && 
            item.getItemMeta().getDisplayName().contains("Quest")) {
            
            // Check if this block is in a structure with a quest
            Structure structure = plugin.getStructureManager().getStructureAt(block.getLocation());
            if (structure != null && structure.getAssignedQuest() != null) {
                // Process structure quest
                plugin.getQuestManager().processBuildingClearProgress(player, structure.getId());
                event.setCancelled(true);
                return;
            }
        }
        
        // Handle other custom block interactions here
    }
    
    /**
     * Handle player digging in quest areas
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDig(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if this block is part of a dig quest
        // This would need custom implementation to track dig quest locations
        // For now, just a placeholder
        
        // Check if the block broken is a resource typically used in dig quests
        if (block.getType() == Material.IRON_ORE || 
            block.getType() == Material.COAL_ORE || 
            block.getType() == Material.COPPER_ORE || 
            block.getType() == Material.GOLD_ORE) {
            
            // We might want to verify this against active quests
            // For now, just log for debugging
            plugin.getLogger().fine("Player " + player.getName() + " dug a potential quest resource");
        }
    }
}
