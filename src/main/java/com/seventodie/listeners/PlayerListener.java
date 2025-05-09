package com.seventodie.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.traders.TraderNPC;
import com.seventodie.worldgen.StructureManager.Structure;

/**
 * Handles player-specific events such as interactions, movement, and combat
 * relevant to the 7 Days to Die gameplay mechanics.
 */
public class PlayerListener implements Listener {
    
    private final SevenToDiePlugin plugin;
    
    // Maps to track player state and interactions
    private final Map<UUID, UUID> playerTradingWith = new HashMap<>();
    private final Map<UUID, Long> lastZombieKillTime = new HashMap<>();
    
    public PlayerListener(SevenToDiePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player join events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Send welcome message with plugin info
        player.sendMessage(ChatColor.GREEN + "Welcome to " + ChatColor.GOLD + "7 Days to Die" + 
                          ChatColor.GREEN + " in Minecraft!");
        player.sendMessage(ChatColor.GRAY + "Find traders, complete quests, upgrade buildings, " +
                          "and survive the zombie apocalypse!");
        
        // Check if player has active quests and notify them
        if (plugin.getQuestManager().hasActiveQuests(player)) {
            player.sendMessage(ChatColor.YELLOW + "You have active quests! Check with a trader to view them.");
        }
    }
    
    /**
     * Handle player quit events
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Clear any trading sessions
        playerTradingWith.remove(playerId);
        
        // Clear any other player-specific tracked data
        lastZombieKillTime.remove(playerId);
    }
    
    /**
     * Handle player movement for quest detection and trader zones
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process if the player has moved between blocks
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location to = event.getTo();
        
        // Check if player entered a structure
        Structure structure = plugin.getStructureManager().getStructureAt(to);
        if (structure != null) {
            // Check if it's a trader outpost and it's nighttime
            if (structure.getType() == plugin.getStructureManager().StructureType.TRADER_OUTPOST) {
                long time = player.getWorld().getTime();
                boolean isNight = time >= 13000 && time <= 24000;
                
                if (isNight) {
                    // Find if there's a trader outpost for this structure
                    for (Object outpost : plugin.getTraderManager().getAllTraderOutposts()) {
                        if (((com.seventodie.traders.TraderManager.TraderOutpost)outpost).getStructureId().equals(structure.getId())) {
                            if (!((com.seventodie.traders.TraderManager.TraderOutpost)outpost).isOpen()) {
                                // Prevent entry at night
                                player.sendMessage(ChatColor.RED + "This trader outpost is closed at night. Come back during the day.");
                                
                                // Teleport player back to previous location
                                event.setCancelled(true);
                                player.teleport(event.getFrom());
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handle player interaction with entities (traders, etc.)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        // Check if this is a trader NPC
        for (Object outpost : plugin.getTraderManager().getAllTraderOutposts()) {
            UUID traderNpcId = ((com.seventodie.traders.TraderManager.TraderOutpost)outpost).getNpcId();
            
            if (traderNpcId != null && entity.getUniqueId().equals(traderNpcId)) {
                // Record that this player is trading with this trader
                playerTradingWith.put(player.getUniqueId(), traderNpcId);
                
                // Let the trader manager handle the interaction
                plugin.getTraderManager().handleTraderInteraction(player, traderNpcId);
                event.setCancelled(true);
                return;
            }
        }
    }
    
    /**
     * Handle inventory click events for trading UI
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && event.getView().getTitle().contains("Shop")) {
            Player player = (Player) event.getWhoClicked();
            UUID traderId = playerTradingWith.get(player.getUniqueId());
            
            if (traderId != null) {
                // Check if this is a shop item
                if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                    ItemMeta meta = event.getCurrentItem().getItemMeta();
                    
                    // Process purchase
                    for (Object trader : plugin.getTraderManager().getAllTraderOutposts()) {
                        UUID npcId = ((com.seventodie.traders.TraderManager.TraderOutpost)trader).getNpcId();
                        if (npcId != null && npcId.equals(traderId)) {
                            // Find the NPC and process the purchase
                            TraderNPC npc = null; // We would need a getter for this from the trader manager
                            
                            // For now, just prevent the click and check for quest item
                            event.setCancelled(true);
                            
                            // Check if this is the quest item
                            if (meta.getDisplayName().contains("Quest")) {
                                // Show quests to player
                                showPlayerQuests(player);
                                return;
                            }
                            
                            return;
                        }
                    }
                }
                
                // Cancel the event to prevent taking items
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handle inventory close events for traders
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player && event.getView().getTitle().contains("Shop")) {
            Player player = (Player) event.getPlayer();
            
            // Clear trading record
            playerTradingWith.remove(player.getUniqueId());
        }
    }
    
    /**
     * Handle entity death events for quest progression
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();
        
        if (killer != null && (entity.getType() == EntityType.ZOMBIE || 
                              entity.getType() == EntityType.ZOMBIE_VILLAGER ||
                              entity.getType() == EntityType.HUSK ||
                              entity.getType() == EntityType.DROWNED)) {
            
            // Update kill time tracking for this player
            lastZombieKillTime.put(killer.getUniqueId(), System.currentTimeMillis());
            
            // Process quest progress for zombie kills
            plugin.getQuestManager().processKillProgress(killer, entity.getType(), entity.getLocation());
        }
    }
    
    /**
     * Show active quests to a player
     * 
     * @param player The player to show quests to
     */
    private void showPlayerQuests(Player player) {
        player.sendMessage(ChatColor.GOLD + "Your Active Quests:");
        
        int questCount = 0;
        for (com.seventodie.quests.Quest quest : plugin.getQuestManager().getPlayerActiveQuests(player)) {
            int progress = quest.getProgress(player.getUniqueId());
            int target = quest.getTargetAmount();
            int percent = (int)((progress / (double)target) * 100);
            
            player.sendMessage(ChatColor.YELLOW + "- " + quest.getTitle() + 
                              ChatColor.GRAY + " (" + progress + "/" + target + ", " + percent + "%)");
            player.sendMessage(ChatColor.WHITE + "  " + quest.getDescription());
            
            // If quest is complete, show completion message
            if (quest.isCompletedByPlayer(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "  This quest is complete! Talk to a trader to claim your reward.");
            }
            
            questCount++;
        }
        
        if (questCount == 0) {
            player.sendMessage(ChatColor.RED + "You have no active quests. Talk to a trader to get some!");
        }
    }
    
    /**
     * Get quest rewards for a completed quest
     * 
     * @param questId The quest ID
     * @return Array of ItemStack rewards
     */
    private ItemStack[] getQuestRewards(UUID questId) {
        return plugin.getQuestManager().getQuestRewards(questId);
    }
}
