package com.seventodie.blocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.tools.ToolManager;

/**
 * Represents a frame block with upgrade tiers, similar to 7 Days to Die.
 * Frame blocks can be upgraded from basic frames to wood, cobble, rebar, and concrete.
 */
public class FrameBlock {

    private static final NamespacedKey BLOCK_TIER_KEY = new NamespacedKey(SevenToDiePlugin.getInstance(), "block_tier");
    private static final NamespacedKey BLOCK_TYPE_KEY = new NamespacedKey(SevenToDiePlugin.getInstance(), "block_type");
    
    // Block tier constants
    public static final int TIER_FRAME = 0;
    public static final int TIER_WOOD = 1;
    public static final int TIER_COBBLE = 2;
    public static final int TIER_REBAR = 3;
    public static final int TIER_CONCRETE = 4;
    
    // Block materials for each tier
    private static final Material[] TIER_MATERIALS = {
        Material.SCAFFOLDING,      // Frame (Tier 0)
        Material.OAK_PLANKS,       // Wood (Tier 1)
        Material.COBBLESTONE,      // Cobble (Tier 2)
        Material.IRON_BARS,        // Rebar (Tier 3)
        Material.SMOOTH_STONE      // Concrete (Tier 4)
    };

    /**
     * Creates a new frame block at the specified location.
     *
     * @param location The location to place the frame block
     * @return True if the frame block was successfully created
     */
    public static boolean createFrameBlock(Location location) {
        Block block = location.getBlock();
        block.setType(TIER_MATERIALS[TIER_FRAME]);
        
        // Set block metadata
        setBlockTier(block, TIER_FRAME);
        setIsFrameBlock(block, true);
        
        return true;
    }

    /**
     * Attempts to upgrade a frame block to the next tier.
     *
     * @param block The block to upgrade
     * @param player The player performing the upgrade
     * @param tool The tool being used for the upgrade
     * @return True if the upgrade was successful
     */
    public static boolean upgradeBlock(Block block, Player player, ItemStack tool) {
        if (!isFrameBlock(block)) {
            return false;
        }
        
        int currentTier = getBlockTier(block);
        int nextTier = currentTier + 1;
        
        // Check if we've reached max tier
        if (nextTier >= TIER_MATERIALS.length) {
            return false;
        }
        
        // Check if the tool is capable of this upgrade
        ToolManager toolManager = SevenToDiePlugin.getInstance().getToolManager();
        if (!toolManager.canUpgradeToTier(tool, nextTier)) {
            player.sendMessage("§cYour tool is not strong enough for this upgrade!");
            return false;
        }
        
        // Set the new block material
        block.setType(TIER_MATERIALS[nextTier]);
        
        // Update the tier in metadata
        setBlockTier(block, nextTier);
        
        // Apply tool damage
        toolManager.applyToolDamage(tool, 1);
        
        return true;
    }

    /**
     * Checks if a block is a frame block.
     *
     * @param block The block to check
     * @return True if the block is a frame block
     */
    public static boolean isFrameBlock(Block block) {
        PersistentDataContainer blockData = block.getChunk().getPersistentDataContainer();
        String locationKey = getLocationKey(block);
        return blockData.has(new NamespacedKey(SevenToDiePlugin.getInstance(), "frameblock_" + locationKey), PersistentDataType.INTEGER);
    }

    /**
     * Gets the current tier of a frame block.
     *
     * @param block The frame block
     * @return The current tier (0-4)
     */
    public static int getBlockTier(Block block) {
        PersistentDataContainer blockData = block.getChunk().getPersistentDataContainer();
        String locationKey = getLocationKey(block);
        NamespacedKey key = new NamespacedKey(SevenToDiePlugin.getInstance(), "frametier_" + locationKey);
        
        if (blockData.has(key, PersistentDataType.INTEGER)) {
            return blockData.get(key, PersistentDataType.INTEGER);
        }
        
        return TIER_FRAME; // Default to frame tier if not set
    }

    /**
     * Sets the tier of a frame block.
     *
     * @param block The frame block
     * @param tier The tier to set (0-4)
     */
    public static void setBlockTier(Block block, int tier) {
        PersistentDataContainer blockData = block.getChunk().getPersistentDataContainer();
        String locationKey = getLocationKey(block);
        NamespacedKey key = new NamespacedKey(SevenToDiePlugin.getInstance(), "frametier_" + locationKey);
        
        blockData.set(key, PersistentDataType.INTEGER, tier);
    }

    /**
     * Marks a block as a frame block in the metadata.
     *
     * @param block The block to mark
     * @param isFrameBlock Whether the block is a frame block
     */
    public static void setIsFrameBlock(Block block, boolean isFrameBlock) {
        PersistentDataContainer blockData = block.getChunk().getPersistentDataContainer();
        String locationKey = getLocationKey(block);
        NamespacedKey key = new NamespacedKey(SevenToDiePlugin.getInstance(), "frameblock_" + locationKey);
        
        if (isFrameBlock) {
            blockData.set(key, PersistentDataType.INTEGER, 1);
        } else {
            blockData.remove(key);
        }
    }

    /**
     * Creates a unique key for the block based on its location.
     *
     * @param block The block
     * @return A unique location key string
     */
    private static String getLocationKey(Block block) {
        return block.getX() + "_" + block.getY() + "_" + block.getZ();
    }

    /**
     * Gets the material associated with a specific tier.
     *
     * @param tier The tier (0-4)
     * @return The material for that tier
     */
    public static Material getMaterialForTier(int tier) {
        if (tier >= 0 && tier < TIER_MATERIALS.length) {
            return TIER_MATERIALS[tier];
        }
        return TIER_MATERIALS[0]; // Default to frame material
    }
    
    /**
     * Gets the display name for a specific tier.
     *
     * @param tier The tier (0-4)
     * @return The display name for that tier
     */
    public static String getTierName(int tier) {
        switch (tier) {
            case TIER_FRAME: return "Frame";
            case TIER_WOOD: return "Wood";
            case TIER_COBBLE: return "Cobblestone";
            case TIER_REBAR: return "Rebar";
            case TIER_CONCRETE: return "Concrete";
            default: return "Unknown";
        }
    }
}
