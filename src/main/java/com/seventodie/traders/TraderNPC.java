package com.seventodie.traders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.quests.Quest;
import com.seventodie.quests.QuestManager;

/**
 * Represents a trader NPC in the game that offers items and quests.
 */
public class TraderNPC implements Listener {
    
    private final SevenToDiePlugin plugin;
    private final Location location;
    private final Random random;
    
    private UUID npcUuid;
    private Entity npcEntity;
    private String traderName;
    private boolean isTrading;
    
    // Trader inventory
    private List<TraderItem> inventory = new ArrayList<>();
    
    // Persistent data keys
    private final NamespacedKey TRADER_ID_KEY;
    
    /**
     * Represents an item that a trader sells
     */
    public class TraderItem {
        private ItemStack item;
        private ItemStack price;
        
        public TraderItem(ItemStack item, ItemStack price) {
            this.item = item;
            this.price = price;
        }
        
        public ItemStack getItem() {
            return item;
        }
        
        public ItemStack getPrice() {
            return price;
        }
    }
    
    /**
     * Creates a new trader NPC
     * 
     * @param plugin The plugin instance
     * @param location The location to spawn the trader
     */
    public TraderNPC(SevenToDiePlugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
        this.random = new Random();
        this.isTrading = true;
        
        // Initialize keys
        this.TRADER_ID_KEY = new NamespacedKey(plugin, "trader_id");
        
        // Generate a random trader name
        this.traderName = generateTraderName();
        
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Initialize inventory
        generateTraderInventory();
    }
    
    /**
     * Spawns the trader NPC at the specified location
     * 
     * @return The UUID of the spawned NPC
     */
    public UUID spawn() {
        // If Citizens is available, use it for more advanced NPCs
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            return spawnCitizensNPC();
        } else {
            // Fallback to vanilla entities
            return spawnVanillaNPC();
        }
    }
    
    /**
     * Spawns a trader NPC using Citizens plugin
     * 
     * @return The UUID of the spawned NPC
     */
    private UUID spawnCitizensNPC() {
        try {
            // Using Citizens API dynamically to avoid hard dependency
            Class<?> npcRegistryClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object npcRegistry = npcRegistryClass.getMethod("getNPCRegistry").invoke(null);
            
            // Create the NPC
            Object npc = npcRegistry.getClass().getMethod("createNPC", EntityType.class, String.class)
                .invoke(npcRegistry, EntityType.VILLAGER, traderName);
            
            // Set NPC metadata
            npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, true);
            
            // Spawn at location
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, location);
            
            // Get the entity
            Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
            npcEntity = (Entity) entity;
            npcUuid = npcEntity.getUniqueId();
            
            // Set traits and properties if needed
            
            plugin.getLogger().info("Spawned Citizens NPC trader: " + traderName);
            return npcUuid;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to spawn Citizens NPC: " + e.getMessage());
            return spawnVanillaNPC(); // Fallback
        }
    }
    
    /**
     * Spawns a trader NPC using vanilla Minecraft entities
     * 
     * @return The UUID of the spawned NPC
     */
    private UUID spawnVanillaNPC() {
        // Spawn a villager
        Entity entity = location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        Villager villager = (Villager) entity;
        
        // Set villager properties
        villager.setCustomName(ChatColor.GOLD + traderName);
        villager.setCustomNameVisible(true);
        villager.setProfession(getRandomProfession());
        villager.setAI(true);
        villager.setInvulnerable(true);
        
        // Store trader ID in persistent data
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.set(TRADER_ID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        
        npcEntity = villager;
        npcUuid = villager.getUniqueId();
        
        plugin.getLogger().info("Spawned vanilla NPC trader: " + traderName);
        return npcUuid;
    }
    
    /**
     * Generates a random trader name
     * 
     * @return A random trader name
     */
    private String generateTraderName() {
        String[] firstNames = {"Joel", "Sarah", "Tommy", "Jen", "Bob", "Duke", "Hugh", "Rekt", "Traitor", "Miner"};
        String[] lastNames = {"Whyte", "Trader", "Jones", "Smith", "Duke", "Jenkins", "Miner", "Survivor"};
        
        return firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)];
    }
    
    /**
     * Get a random villager profession
     * 
     * @return A random profession
     */
    private Profession getRandomProfession() {
        Profession[] professions = Profession.values();
        return professions[random.nextInt(professions.length)];
    }
    
    /**
     * Generate items for the trader's inventory
     */
    private void generateTraderInventory() {
        // Clear current inventory
        inventory.clear();
        
        // Add basic supplies
        addTraderItem(new ItemStack(Material.BREAD, 3), new ItemStack(Material.IRON_INGOT, 1));
        addTraderItem(new ItemStack(Material.COOKED_BEEF, 2), new ItemStack(Material.IRON_INGOT, 2));
        addTraderItem(new ItemStack(Material.GOLDEN_APPLE, 1), new ItemStack(Material.GOLD_INGOT, 3));
        
        // Add tools and weapons
        addTraderItem(new ItemStack(Material.IRON_PICKAXE), new ItemStack(Material.DIAMOND, 1));
        addTraderItem(new ItemStack(Material.IRON_SWORD), new ItemStack(Material.DIAMOND, 1));
        addTraderItem(new ItemStack(Material.BOW), new ItemStack(Material.EMERALD, 3));
        addTraderItem(new ItemStack(Material.ARROW, 16), new ItemStack(Material.IRON_INGOT, 1));
        
        // Add building materials
        addTraderItem(new ItemStack(Material.OAK_LOG, 16), new ItemStack(Material.IRON_INGOT, 1));
        addTraderItem(new ItemStack(Material.STONE, 32), new ItemStack(Material.IRON_INGOT, 1));
        
        // Add special frame blocks from the plugin
        ItemStack frameBlock = plugin.getBlockManager().createFrameBlockItem();
        frameBlock.setAmount(4);
        addTraderItem(frameBlock, new ItemStack(Material.DIAMOND, 1));
        
        // Add hammer tools
        addTraderItem(plugin.getToolManager().createStoneHammer(), new ItemStack(Material.DIAMOND, 1));
        addTraderItem(plugin.getToolManager().createIronHammer(), new ItemStack(Material.DIAMOND, 2));
        addTraderItem(plugin.getToolManager().createSteelHammer(), new ItemStack(Material.DIAMOND, 4));
    }
    
    /**
     * Add an item to the trader's inventory
     * 
     * @param item The item to add
     * @param price The price of the item
     */
    private void addTraderItem(ItemStack item, ItemStack price) {
        inventory.add(new TraderItem(item, price));
    }
    
    /**
     * Create a trade menu for a player
     * 
     * @param player The player to create the menu for
     * @return The trading inventory
     */
    public Inventory createTradeMenu(Player player) {
        // Create inventory with dynamic size based on items
        int size = (int) Math.ceil(inventory.size() / 9.0) * 9;
        size = Math.max(size, 27); // Minimum 3 rows
        size = Math.min(size, 54); // Maximum 6 rows
        
        Inventory tradeMenu = Bukkit.createInventory(null, size, ChatColor.DARK_GREEN + traderName + "'s Shop");
        
        // Add trading items to inventory
        for (int i = 0; i < inventory.size() && i < size; i++) {
            TraderItem traderItem = inventory.get(i);
            
            // Create display item with price in lore
            ItemStack displayItem = traderItem.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "Price: " + ChatColor.WHITE + 
                     traderItem.getPrice().getAmount() + " " + 
                     formatMaterialName(traderItem.getPrice().getType()));
            lore.add(ChatColor.GRAY + "Click to purchase");
            
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
            
            tradeMenu.setItem(i, displayItem);
        }
        
        // Add quest item if player has active quests
        QuestManager questManager = plugin.getQuestManager();
        if (questManager.hasActiveQuests(player)) {
            ItemStack questItem = createQuestItem(player);
            tradeMenu.setItem(size - 5, questItem);
        }
        
        return tradeMenu;
    }
    
    /**
     * Create a quest item for the trade menu
     * 
     * @param player The player
     * @return The quest item
     */
    private ItemStack createQuestItem(Player player) {
        ItemStack questItem = new ItemStack(Material.PAPER);
        ItemMeta meta = questItem.getItemMeta();
        
        meta.setDisplayName(ChatColor.GOLD + "Active Quests");
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to view your active quests");
        
        // Add active quests to lore
        QuestManager questManager = plugin.getQuestManager();
        List<Quest> activeQuests = questManager.getPlayerActiveQuests(player);
        
        if (activeQuests.isEmpty()) {
            lore.add(ChatColor.RED + "No active quests");
        } else {
            for (Quest quest : activeQuests) {
                lore.add(ChatColor.YELLOW + "- " + quest.getTitle());
            }
        }
        
        meta.setLore(lore);
        questItem.setItemMeta(meta);
        
        return questItem;
    }
    
    /**
     * Format a material name for display
     * 
     * @param material The material
     * @return The formatted name
     */
    private String formatMaterialName(Material material) {
        String name = material.name();
        name = name.replace('_', ' ').toLowerCase();
        
        // Capitalize words
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Process a player's purchase
     * 
     * @param player The player
     * @param itemIndex The index of the item in the trader's inventory
     * @return True if the purchase was successful
     */
    public boolean processPurchase(Player player, int itemIndex) {
        if (itemIndex < 0 || itemIndex >= inventory.size()) {
            return false;
        }
        
        TraderItem traderItem = inventory.get(itemIndex);
        ItemStack item = traderItem.getItem();
        ItemStack price = traderItem.getPrice();
        
        // Check if player has enough of the price item
        if (!playerHasItems(player, price.getType(), price.getAmount())) {
            player.sendMessage(ChatColor.RED + "You don't have enough " + 
                               formatMaterialName(price.getType()) + " to buy this item.");
            return false;
        }
        
        // Take the price items from the player
        removeItems(player, price.getType(), price.getAmount());
        
        // Give the item to the player
        player.getInventory().addItem(item.clone());
        
        // Confirmation message
        player.sendMessage(ChatColor.GREEN + "You bought " + 
                          item.getAmount() + " " + formatMaterialName(item.getType()) + 
                          " for " + price.getAmount() + " " + formatMaterialName(price.getType()) + ".");
        
        // Play sound
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        
        return true;
    }
    
    /**
     * Check if a player has enough of a specific item
     * 
     * @param player The player
     * @param material The material to check
     * @param amount The amount required
     * @return True if the player has enough items
     */
    private boolean playerHasItems(Player player, Material material, int amount) {
        int count = 0;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Remove items from a player's inventory
     * 
     * @param player The player
     * @param material The material to remove
     * @param amount The amount to remove
     */
    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        
        // Loop through inventory slots
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    // Remove the entire stack
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    // Remove part of the stack
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
    }
    
    /**
     * Handle entity damage events to protect trader NPCs
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getUniqueId().equals(npcUuid)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle player interact events with the trader NPC
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getUniqueId().equals(npcUuid)) {
            // Let the trader manager handle the interaction
            plugin.getTraderManager().handleTraderInteraction(event.getPlayer(), npcUuid);
            event.setCancelled(true);
        }
    }
    
    /**
     * Get the trader NPC entity
     * 
     * @return The NPC entity
     */
    public Entity getEntity() {
        return npcEntity;
    }
    
    /**
     * Get the trader NPC UUID
     * 
     * @return The NPC UUID
     */
    public UUID getUuid() {
        return npcUuid;
    }
    
    /**
     * Get the trader's name
     * 
     * @return The trader's name
     */
    public String getName() {
        return traderName;
    }
    
    /**
     * Check if the trader is currently trading
     * 
     * @return True if the trader is trading
     */
    public boolean isTrading() {
        return isTrading;
    }
    
    /**
     * Set whether the trader is currently trading
     * 
     * @param isTrading True if the trader is trading
     */
    public void setTrading(boolean isTrading) {
        this.isTrading = isTrading;
    }
}
