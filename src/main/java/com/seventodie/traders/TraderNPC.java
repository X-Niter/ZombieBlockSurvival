package com.seventodie.traders;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.seventodie.traders.TraderManager.TraderOutpost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a trader NPC
 */
public class TraderNPC {
    
    private final UUID id;
    private final Entity entity;
    private final TraderOutpost outpost;
    private final Map<String, List<MerchantRecipe>> tradeCategories = new HashMap<>();
    
    /**
     * Constructor for a TraderNPC
     * 
     * @param id The unique ID
     * @param entity The entity
     * @param outpost The outpost
     */
    public TraderNPC(UUID id, Entity entity, TraderOutpost outpost) {
        this.id = id;
        this.entity = entity;
        this.outpost = outpost;
        
        // Initialize trade categories
        initializeTradeCategories();
    }
    
    /**
     * Initialize trade categories
     */
    private void initializeTradeCategories() {
        // This is a placeholder - actual implementation would load trades from config
        List<MerchantRecipe> weapons = new ArrayList<>();
        List<MerchantRecipe> tools = new ArrayList<>();
        List<MerchantRecipe> resources = new ArrayList<>();
        List<MerchantRecipe> food = new ArrayList<>();
        
        // Add some placeholder trades (these would normally be configured)
        // In a real implementation, we would use MerchantRecipe to set up proper trades
        
        tradeCategories.put("Weapons", weapons);
        tradeCategories.put("Tools", tools);
        tradeCategories.put("Resources", resources);
        tradeCategories.put("Food", food);
    }
    
    /**
     * Get the trader ID
     * 
     * @return The ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Get the entity
     * 
     * @return The entity
     */
    public Entity getEntity() {
        return entity;
    }
    
    /**
     * Get the outpost
     * 
     * @return The outpost
     */
    public TraderOutpost getOutpost() {
        return outpost;
    }
    
    /**
     * Get the location
     * 
     * @return The location
     */
    public Location getLocation() {
        return entity.getLocation();
    }
    
    /**
     * Open the trader menu for a player
     * 
     * @param player The player
     * @return True if opened
     */
    public boolean openMenu(Player player) {
        // Check if the outpost is open
        if (!outpost.isOpen()) {
            player.sendMessage(ChatColor.RED + "This trader is closed. Come back during the day.");
            return false;
        }
        
        // In a real implementation, we would either:
        // 1. Use Citizens API to open a custom menu
        // 2. Use the vanilla merchant API
        
        // This is a placeholder - we're just opening a GUI with category buttons
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_GREEN + "Trader");
        
        // Add category buttons
        int slot = 0;
        for (String category : tradeCategories.keySet()) {
            ItemStack button = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + category);
            button.setItemMeta(meta);
            
            inv.setItem(slot++, button);
        }
        
        player.openInventory(inv);
        return true;
    }
    
    /**
     * Open a specific trade category for a player
     * 
     * @param player The player
     * @param category The category
     * @return True if opened
     */
    public boolean openCategory(Player player, String category) {
        if (!outpost.isOpen()) {
            player.sendMessage(ChatColor.RED + "This trader is closed. Come back during the day.");
            return false;
        }
        
        List<MerchantRecipe> trades = tradeCategories.get(category);
        if (trades == null) {
            player.sendMessage(ChatColor.RED + "Unknown trade category.");
            return false;
        }
        
        // In a real implementation, we would use the trades to create a merchant
        // For now, we just send a message
        player.sendMessage(ChatColor.GREEN + "Opened " + category + " trades.");
        return true;
    }
    
    /**
     * Remove the trader
     */
    public void remove() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }
}