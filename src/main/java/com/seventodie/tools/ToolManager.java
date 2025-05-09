package com.seventodie.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.blocks.FrameBlock;

/**
 * Manages custom tools for block upgrades in the 7 Days to Die plugin.
 */
public class ToolManager {
    
    private final SevenToDiePlugin plugin;
    
    // Tool type keys for persistent data
    private final NamespacedKey TOOL_TYPE_KEY;
    private final NamespacedKey MAX_TIER_KEY;
    
    // Tool type constants
    public static final String STONE_HAMMER = "stone_hammer";
    public static final String IRON_HAMMER = "iron_hammer";
    public static final String STEEL_HAMMER = "steel_hammer";
    
    // Custom model data IDs for resource pack
    private static final int STONE_HAMMER_MODEL = 7101;
    private static final int IRON_HAMMER_MODEL = 7102;
    private static final int STEEL_HAMMER_MODEL = 7103;
    
    // Map tool types to their max upgrade tier
    private final Map<String, Integer> toolMaxTiers = new HashMap<>();
    
    public ToolManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        
        // Initialize keys
        TOOL_TYPE_KEY = new NamespacedKey(plugin, "tool_type");
        MAX_TIER_KEY = new NamespacedKey(plugin, "max_tier");
        
        // Setup tool tier mappings
        initToolTiers();
        
        // Register tool recipes
        registerRecipes();
    }
    
    /**
     * Initialize tool tier mappings
     */
    private void initToolTiers() {
        toolMaxTiers.put(STONE_HAMMER, FrameBlock.TIER_WOOD);       // Stone hammer can upgrade to wood
        toolMaxTiers.put(IRON_HAMMER, FrameBlock.TIER_REBAR);       // Iron hammer can upgrade to rebar
        toolMaxTiers.put(STEEL_HAMMER, FrameBlock.TIER_CONCRETE);   // Steel hammer can upgrade to concrete
    }
    
    /**
     * Register crafting recipes for tools
     */
    private void registerRecipes() {
        // Stone Hammer Recipe
        ItemStack stoneHammer = createStoneHammer();
        NamespacedKey stoneHammerKey = new NamespacedKey(plugin, "stone_hammer");
        ShapedRecipe stoneHammerRecipe = new ShapedRecipe(stoneHammerKey, stoneHammer);
        
        stoneHammerRecipe.shape("CCC", "CSC", " S ");
        stoneHammerRecipe.setIngredient('C', Material.COBBLESTONE);
        stoneHammerRecipe.setIngredient('S', Material.STICK);
        
        plugin.getServer().addRecipe(stoneHammerRecipe);
        
        // Iron Hammer Recipe
        ItemStack ironHammer = createIronHammer();
        NamespacedKey ironHammerKey = new NamespacedKey(plugin, "iron_hammer");
        ShapedRecipe ironHammerRecipe = new ShapedRecipe(ironHammerKey, ironHammer);
        
        ironHammerRecipe.shape("III", "ISI", " S ");
        ironHammerRecipe.setIngredient('I', Material.IRON_INGOT);
        ironHammerRecipe.setIngredient('S', Material.STICK);
        
        plugin.getServer().addRecipe(ironHammerRecipe);
        
        // Steel Hammer Recipe
        ItemStack steelHammer = createSteelHammer();
        NamespacedKey steelHammerKey = new NamespacedKey(plugin, "steel_hammer");
        ShapedRecipe steelHammerRecipe = new ShapedRecipe(steelHammerKey, steelHammer);
        
        steelHammerRecipe.shape("DDD", "DSD", " S ");
        steelHammerRecipe.setIngredient('D', Material.DIAMOND);
        steelHammerRecipe.setIngredient('S', Material.BLAZE_ROD);
        
        plugin.getServer().addRecipe(steelHammerRecipe);
        
        plugin.getLogger().info("Registered hammer tool recipes");
    }
    
    /**
     * Create a stone hammer item
     * 
     * @return Stone hammer item
     */
    public ItemStack createStoneHammer() {
        ItemStack hammer = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta meta = hammer.getItemMeta();
        
        meta.setDisplayName("§7Stone Hammer");
        meta.setLore(Arrays.asList(
            "§7Upgrades frame blocks to wood tier",
            "§8Max Tier: " + FrameBlock.getTierName(FrameBlock.TIER_WOOD)
        ));
        
        // Set custom model data for resource pack
        meta.setCustomModelData(STONE_HAMMER_MODEL);
        
        // Store tool data
        meta.getPersistentDataContainer().set(TOOL_TYPE_KEY, PersistentDataType.STRING, STONE_HAMMER);
        meta.getPersistentDataContainer().set(MAX_TIER_KEY, PersistentDataType.INTEGER, toolMaxTiers.get(STONE_HAMMER));
        
        // Add decorative enchant glow
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        hammer.setItemMeta(meta);
        return hammer;
    }
    
    /**
     * Create an iron hammer item
     * 
     * @return Iron hammer item
     */
    public ItemStack createIronHammer() {
        ItemStack hammer = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = hammer.getItemMeta();
        
        meta.setDisplayName("§fIron Hammer");
        meta.setLore(Arrays.asList(
            "§7Upgrades frame blocks to rebar tier",
            "§8Max Tier: " + FrameBlock.getTierName(FrameBlock.TIER_REBAR)
        ));
        
        // Set custom model data for resource pack
        meta.setCustomModelData(IRON_HAMMER_MODEL);
        
        // Store tool data
        meta.getPersistentDataContainer().set(TOOL_TYPE_KEY, PersistentDataType.STRING, IRON_HAMMER);
        meta.getPersistentDataContainer().set(MAX_TIER_KEY, PersistentDataType.INTEGER, toolMaxTiers.get(IRON_HAMMER));
        
        // Add decorative enchant glow
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        hammer.setItemMeta(meta);
        return hammer;
    }
    
    /**
     * Create a steel hammer item
     * 
     * @return Steel hammer item
     */
    public ItemStack createSteelHammer() {
        ItemStack hammer = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = hammer.getItemMeta();
        
        meta.setDisplayName("§bSteel Hammer");
        meta.setLore(Arrays.asList(
            "§7Upgrades frame blocks to concrete tier",
            "§8Max Tier: " + FrameBlock.getTierName(FrameBlock.TIER_CONCRETE)
        ));
        
        // Set custom model data for resource pack
        meta.setCustomModelData(STEEL_HAMMER_MODEL);
        
        // Store tool data
        meta.getPersistentDataContainer().set(TOOL_TYPE_KEY, PersistentDataType.STRING, STEEL_HAMMER);
        meta.getPersistentDataContainer().set(MAX_TIER_KEY, PersistentDataType.INTEGER, toolMaxTiers.get(STEEL_HAMMER));
        
        // Add decorative enchant glow
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        hammer.setItemMeta(meta);
        return hammer;
    }
    
    /**
     * Check if an item is a hammer tool
     * 
     * @param item The item to check
     * @return True if the item is a hammer
     */
    public boolean isHammerTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(TOOL_TYPE_KEY, PersistentDataType.STRING);
    }
    
    /**
     * Get the tool type of an item
     * 
     * @param item The item to check
     * @return The tool type, or null if not a tool
     */
    public String getToolType(ItemStack item) {
        if (!isHammerTool(item)) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(TOOL_TYPE_KEY, PersistentDataType.STRING);
    }
    
    /**
     * Check if a tool can upgrade blocks to a specific tier
     * 
     * @param tool The tool to check
     * @param targetTier The tier to upgrade to
     * @return True if the tool can perform the upgrade
     */
    public boolean canUpgradeToTier(ItemStack tool, int targetTier) {
        if (!isHammerTool(tool)) {
            return false;
        }
        
        ItemMeta meta = tool.getItemMeta();
        int maxTier = meta.getPersistentDataContainer().get(MAX_TIER_KEY, PersistentDataType.INTEGER);
        
        return targetTier <= maxTier;
    }
    
    /**
     * Apply damage to a tool when used
     * 
     * @param tool The tool to damage
     * @param amount The amount of damage to apply
     */
    public void applyToolDamage(ItemStack tool, int amount) {
        if (tool == null || !tool.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            
            int currentDamage = damageable.getDamage();
            int maxDurability = tool.getType().getMaxDurability();
            
            // Apply damage
            int newDamage = currentDamage + amount;
            
            // Check if tool should break
            if (newDamage >= maxDurability) {
                tool.setAmount(0); // Break the tool
            } else {
                damageable.setDamage(newDamage);
                tool.setItemMeta(meta);
            }
        }
    }
}
