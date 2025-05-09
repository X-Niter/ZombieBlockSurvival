package com.seventodie.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import com.seventodie.SevenToDiePlugin;

/**
 * Manages custom blocks in the Seven to Die plugin.
 */
public class BlockManager {
    
    private final SevenToDiePlugin plugin;
    private final Map<UUID, Long> blockInteractionCooldown = new HashMap<>();
    private static final long INTERACTION_COOLDOWN_MS = 250; // 250ms cooldown to prevent spam clicking
    
    // Custom block identifiers
    private final NamespacedKey frameBlockKey;
    
    public BlockManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        this.frameBlockKey = new NamespacedKey(plugin, "frame_block");
        
        // Register custom items in recipe manager or other initialization
        initializeCustomBlocks();
    }
    
    /**
     * Initialize custom block types and recipes
     */
    private void initializeCustomBlocks() {
        // Create frame block item
        createFrameBlockItem();
        
        // Register recipes for custom blocks
        registerRecipes();
    }
    
    /**
     * Creates the frame block item with custom metadata
     */
    private void createFrameBlockItem() {
        // Frame block recipe will be handled in registerRecipes()
        plugin.getLogger().info("Initialized frame block items");
    }
    
    /**
     * Registers crafting recipes for custom blocks
     */
    private void registerRecipes() {
        // Register crafting recipes through Bukkit recipe manager
        // This would include recipes for frame blocks and tools
        plugin.getLogger().info("Registered custom block recipes");
    }
    
    /**
     * Creates a custom frame block item
     * 
     * @return The frame block item
     */
    public ItemStack createFrameBlockItem() {
        ItemStack frameBlock = new ItemStack(Material.SCAFFOLDING);
        ItemMeta meta = frameBlock.getItemMeta();
        
        meta.setDisplayName("§6Frame Block");
        meta.setLore(java.util.Arrays.asList(
            "§7The base building block",
            "§7Can be upgraded with hammers"
        ));
        
        // Set custom model data for resource pack
        meta.setCustomModelData(7001);
        
        // Store block type in persistent data
        meta.getPersistentDataContainer().set(frameBlockKey, PersistentDataType.STRING, "frame_block");
        
        frameBlock.setItemMeta(meta);
        return frameBlock;
    }
    
    /**
     * Handle block placement event for custom blocks
     * 
     * @param event The block place event
     * @return True if this was a custom block and handled
     */
    public boolean handleBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return false;
        }
        
        // Check if this is our frame block
        if (meta.getPersistentDataContainer().has(frameBlockKey, PersistentDataType.STRING)) {
            Block block = event.getBlockPlaced();
            
            // Initialize the frame block data
            FrameBlock.createFrameBlock(block.getLocation());
            
            plugin.getLogger().info("Player " + event.getPlayer().getName() + " placed a frame block at " + 
                block.getLocation().getBlockX() + ", " + 
                block.getLocation().getBlockY() + ", " + 
                block.getLocation().getBlockZ());
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle block break event for custom blocks
     * 
     * @param event The block break event
     * @return True if this was a custom block and handled
     */
    public boolean handleBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Check if this is a frame block
        if (FrameBlock.isFrameBlock(block)) {
            // Get the current tier to determine drops
            int tier = FrameBlock.getBlockTier(block);
            Material material = FrameBlock.getMaterialForTier(tier);
            
            // Clear the block metadata
            FrameBlock.setIsFrameBlock(block, false);
            
            // Drop appropriate item based on tier
            if (tier == FrameBlock.TIER_FRAME) {
                event.setDropItems(false);
                block.getWorld().dropItemNaturally(block.getLocation(), createFrameBlockItem());
            } else {
                // For higher tiers, drop the vanilla block type
                event.setDropItems(false);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(material));
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle block interaction (upgrading frame blocks)
     * 
     * @param player The player interacting
     * @param block The block being interacted with
     * @param tool The tool being used
     * @return True if the interaction was handled
     */
    public boolean handleBlockInteraction(Player player, Block block, ItemStack tool) {
        // Check cooldown to prevent spam clicking
        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (blockInteractionCooldown.containsKey(playerUUID) && 
            currentTime - blockInteractionCooldown.get(playerUUID) < INTERACTION_COOLDOWN_MS) {
            return false;
        }
        
        // Update cooldown
        blockInteractionCooldown.put(playerUUID, currentTime);
        
        // Check if this is a frame block
        if (FrameBlock.isFrameBlock(block)) {
            // Attempt to upgrade the block
            boolean upgraded = FrameBlock.upgradeBlock(block, player, tool);
            
            if (upgraded) {
                int tier = FrameBlock.getBlockTier(block);
                player.sendMessage("§aUpgraded to " + FrameBlock.getTierName(tier) + " tier.");
                
                // Play sound for feedback
                player.playSound(block.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.5f, 1.0f);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a location contains a frame block
     * 
     * @param location The location to check
     * @return True if the location has a frame block
     */
    public boolean isFrameBlockAt(Location location) {
        Block block = location.getBlock();
        return FrameBlock.isFrameBlock(block);
    }
}
