package com.seventodie.blocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;

import com.seventodie.SevenToDiePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages block upgrades and special blocks (frames)
 */
public class BlockManager {
    
    /**
     * Types of frame blocks
     */
    public enum FrameType {
        WOOD,
        COBBLE,
        REBAR,
        CONCRETE
    }
    
    private final SevenToDiePlugin plugin;
    private final Map<Location, FrameBlock> blocks = new HashMap<>();
    
    /**
     * Material tiers for upgrades (in order)
     */
    private static final Material[][] UPGRADE_TIERS = {
        // Wood tier
        { Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS, 
          Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS },
        
        // Stone tier
        { Material.COBBLESTONE, Material.STONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS,
          Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS },
        
        // Metal tier
        { Material.IRON_BLOCK, Material.COPPER_BLOCK, Material.GOLD_BLOCK },
        
        // Concrete tier
        { Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.GRAY_CONCRETE, Material.BLACK_CONCRETE }
    };
    
    /**
     * Constructor for BlockManager
     * 
     * @param plugin The SevenToDie plugin instance
     */
    public BlockManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        loadBlocks();
    }
    
    /**
     * Load blocks from database
     */
    private void loadBlocks() {
        // TODO: Implement database loading
        // For now, just initialize empty
    }
    
    /**
     * Handle a block place event
     * 
     * @param event The block place event
     * @return True if handled
     */
    public boolean handleBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();
        
        // Check if this is a frame block
        NamespacedKey frameKey = new NamespacedKey(plugin, "frame_block");
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(frameKey, PersistentDataType.INTEGER)) {
            int tier = item.getItemMeta().getPersistentDataContainer().get(frameKey, PersistentDataType.INTEGER);
            
            // Create a frame block
            FrameBlock frameBlock = new FrameBlock(block.getLocation(), tier, player.getUniqueId());
            blocks.put(block.getLocation(), frameBlock);
            
            player.sendMessage(ChatColor.GREEN + "Placed a frame block (tier " + tier + ")");
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle a block break event
     * 
     * @param event The block break event
     * @return True if handled
     */
    public boolean handleBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        
        // Check if this is a frame block
        if (blocks.containsKey(location)) {
            FrameBlock frameBlock = blocks.get(location);
            
            // Drop the appropriate items
            event.setDropItems(false);
            ItemStack drop = createFrameBlockItem(frameBlock.getTier());
            location.getWorld().dropItemNaturally(location, drop);
            
            // Remove the frame block
            blocks.remove(location);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle block interaction (upgrade)
     * 
     * @param player The player
     * @param block The block
     * @param item The item used
     * @return True if handled
     */
    public boolean handleBlockInteraction(Player player, Block block, ItemStack item) {
        if (item == null) {
            return false;
        }
        
        Location location = block.getLocation();
        
        // Check if this is a frame block
        if (blocks.containsKey(location)) {
            FrameBlock frameBlock = blocks.get(location);
            
            // Check if the item is a valid upgrade material
            Material material = item.getType();
            int currentTier = frameBlock.getTier();
            
            // Try to upgrade the block
            if (tryUpgradeBlock(frameBlock, material)) {
                player.sendMessage(ChatColor.GREEN + "Upgraded frame block to tier " + frameBlock.getTier());
                
                // Consume one item
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    item.setAmount(item.getAmount() - 1);
                }
                
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "This material cannot be used to upgrade the frame block.");
            }
        }
        
        return false;
    }
    
    /**
     * Try to upgrade a block with a material
     * 
     * @param frameBlock The frame block
     * @param material The material
     * @return True if upgraded
     */
    private boolean tryUpgradeBlock(FrameBlock frameBlock, Material material) {
        int currentTier = frameBlock.getTier();
        
        // Check if the material is a valid upgrade for the current tier
        for (int tierIndex = 0; tierIndex < UPGRADE_TIERS.length; tierIndex++) {
            Material[] tierMaterials = UPGRADE_TIERS[tierIndex];
            
            for (Material tierMaterial : tierMaterials) {
                if (tierMaterial == material) {
                    // Found the material in a tier
                    if (tierIndex > currentTier) {
                        // Upgrade to the new tier
                        frameBlock.setTier(tierIndex);
                        return true;
                    } else if (tierIndex == currentTier) {
                        // Already at this tier, but we can still consume the material
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Create a frame block item
     * 
     * @param tier The tier
     * @return The item
     */
    public ItemStack createFrameBlockItem(int tier) {
        ItemStack item = new ItemStack(Material.STICK);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        
        // Set name based on tier
        String tierName;
        switch (tier) {
            case 0:
                tierName = "Wood";
                break;
            case 1:
                tierName = "Stone";
                break;
            case 2:
                tierName = "Metal";
                break;
            case 3:
                tierName = "Concrete";
                break;
            default:
                tierName = "Unknown";
        }
        
        meta.setDisplayName(ChatColor.YELLOW + tierName + " Frame");
        
        // Add lore
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GRAY + "Place and upgrade this frame");
        lore.add(ChatColor.GRAY + "with materials to build structures.");
        lore.add("");
        lore.add(ChatColor.GREEN + "Current Tier: " + tierName);
        meta.setLore(lore);
        
        // Store tier in persistent data
        NamespacedKey frameKey = new NamespacedKey(plugin, "frame_block");
        meta.getPersistentDataContainer().set(frameKey, PersistentDataType.INTEGER, tier);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Get a frame block at a location
     * 
     * @param location The location
     * @return The frame block, or null if not found
     */
    public FrameBlock getFrameBlock(Location location) {
        return blocks.get(location);
    }
    
    /**
     * Save blocks to database
     */
    public void saveBlocks() {
        // TODO: Implement database saving
    }
    
    /**
     * Create a frame block item with a specific type
     * 
     * @param frameType The frame type
     * @param amount The amount
     * @return The item
     */
    public ItemStack createFrameBlockItem(FrameType frameType, int amount) {
        ItemStack item = createFrameBlockItem(frameType.ordinal());
        item.setAmount(amount);
        return item;
    }
    
    /**
     * Get the material for a frame block type
     * 
     * @param frameType The frame type
     * @return The material
     */
    public Material getFrameBlockMaterial(FrameType frameType) {
        switch (frameType) {
            case WOOD:
                return Material.OAK_PLANKS;
            case COBBLE:
                return Material.COBBLESTONE;
            case REBAR:
                return Material.IRON_BLOCK;
            case CONCRETE:
                return Material.GRAY_CONCRETE;
            default:
                return Material.STONE;
        }
    }
    
    /**
     * Get the durability for a frame block type
     * 
     * @param frameType The frame type
     * @return The durability
     */
    public int getFrameBlockDurability(FrameType frameType) {
        switch (frameType) {
            case WOOD:
                return 250;
            case COBBLE:
                return 500;
            case REBAR:
                return 1000;
            case CONCRETE:
                return 2000;
            default:
                return 100;
        }
    }
    
    /**
     * Check if an item is a frame block
     * 
     * @param item The item
     * @return True if it's a frame block
     */
    public boolean isFrameBlock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        NamespacedKey frameKey = new NamespacedKey(plugin, "frame_block");
        return item.getItemMeta().getPersistentDataContainer().has(frameKey, PersistentDataType.INTEGER);
    }
    
    /**
     * Get a frame block from an item
     * 
     * @param item The item
     * @return The frame block, or null if not a frame block
     */
    public FrameBlock getFrameBlockFromItem(ItemStack item) {
        if (!isFrameBlock(item)) {
            return null;
        }
        
        NamespacedKey frameKey = new NamespacedKey(plugin, "frame_block");
        int tier = item.getItemMeta().getPersistentDataContainer().get(frameKey, PersistentDataType.INTEGER);
        
        // Since we don't have a real block placement location, create a dummy location
        Location location = new Location(plugin.getServer().getWorlds().get(0), 0, 0, 0);
        
        return new FrameBlock(location, tier, UUID.randomUUID());
    }
}