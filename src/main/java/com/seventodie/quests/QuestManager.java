package com.seventodie.quests;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.worldgen.StructureManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all quests in the SevenToDie plugin
 */
public class QuestManager {
    
    private final SevenToDiePlugin plugin;
    private final Map<UUID, Quest> quests = new HashMap<>();
    private final Map<UUID, List<UUID>> playerQuests = new HashMap<>();
    
    /**
     * Types of quest targets
     */
    public enum QuestTargetType {
        KILL_ZOMBIES,
        COLLECT_ITEMS,
        CLEAR_BUILDING,
        DIG_RESOURCES
    }
    
    /**
     * Constructor for QuestManager
     * 
     * @param plugin The SevenToDie plugin instance
     */
    public QuestManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        loadQuests();
    }
    
    /**
     * Load quests from database
     */
    private void loadQuests() {
        // TODO: Implement database loading
        // This is a placeholder - actual implementation would load from database
    }
    
    /**
     * Register a new quest
     * 
     * @param quest The quest
     * @return True if registered successfully
     */
    public boolean registerQuest(Quest quest) {
        if (quest == null || quests.containsKey(quest.getId())) {
            return false;
        }
        
        quests.put(quest.getId(), quest);
        return true;
    }
    
    /**
     * Get a quest by ID
     * 
     * @param id The quest ID
     * @return The quest, or null if not found
     */
    public Quest getQuest(UUID id) {
        return quests.get(id);
    }
    
    /**
     * Get all quests
     * 
     * @return All quests
     */
    public List<Quest> getAllQuests() {
        return new ArrayList<>(quests.values());
    }
    
    /**
     * Get a player's active quests
     * 
     * @param player The player
     * @return The player's quests
     */
    public List<Quest> getPlayerActiveQuests(Player player) {
        List<Quest> activeQuests = new ArrayList<>();
        List<UUID> questIds = playerQuests.get(player.getUniqueId());
        
        if (questIds == null) {
            return activeQuests;
        }
        
        for (UUID id : questIds) {
            Quest quest = quests.get(id);
            if (quest != null && !quest.isCompleted()) {
                activeQuests.add(quest);
            }
        }
        
        return activeQuests;
    }
    
    /**
     * Assign a quest to a player
     * 
     * @param player The player
     * @param questId The quest ID
     * @return True if assigned successfully
     */
    public boolean assignQuestToPlayer(Player player, UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null || quest.isCompleted()) {
            return false;
        }
        
        // Get or create the player's quest list
        List<UUID> questIds = playerQuests.getOrDefault(player.getUniqueId(), new ArrayList<>());
        
        // Check if the player already has the quest
        if (questIds.contains(questId)) {
            return false;
        }
        
        // Check if the player has reached the maximum number of quests
        int maxQuests = plugin.getConfig().getInt("mechanics.quests.max-active-per-player", 5);
        if (getPlayerActiveQuests(player).size() >= maxQuests) {
            return false;
        }
        
        // Assign the quest
        questIds.add(questId);
        playerQuests.put(player.getUniqueId(), questIds);
        
        // Notify the player
        player.sendMessage(ChatColor.GREEN + "New quest: " + ChatColor.YELLOW + quest.getTitle());
        player.sendMessage(ChatColor.GRAY + quest.getDescription());
        
        return true;
    }
    
    /**
     * Check if a player has a quest
     * 
     * @param player The player
     * @param questId The quest ID
     * @return True if the player has the quest
     */
    public boolean hasQuest(Player player, UUID questId) {
        List<UUID> questIds = playerQuests.get(player.getUniqueId());
        return questIds != null && questIds.contains(questId);
    }
    
    /**
     * Update a player's quest progress
     * 
     * @param player The player
     * @param type The target type
     * @param amount The progress amount
     */
    public void updateQuestProgress(Player player, QuestTargetType type, int amount) {
        List<Quest> quests = getPlayerActiveQuests(player);
        
        for (Quest quest : quests) {
            if (quest.getTargetType() == type) {
                int currentProgress = quest.getProgress();
                int newProgress = currentProgress + amount;
                
                quest.setProgress(newProgress);
                
                // Check if the quest is completed
                if (newProgress >= quest.getTargetAmount() && !quest.isCompleted()) {
                    // Complete the quest
                    completeQuest(quest.getId());
                    
                    // Notify the player
                    player.sendMessage(ChatColor.GREEN + "Quest completed: " + ChatColor.YELLOW + quest.getTitle());
                    
                    // Give rewards
                    giveQuestRewards(player, quest);
                } else if (newProgress % 5 == 0 || newProgress == 1) {
                    // Notify the player of progress (only at certain intervals to avoid spam)
                    player.sendMessage(ChatColor.YELLOW + "Quest progress: " + 
                            ChatColor.GREEN + newProgress + "/" + quest.getTargetAmount() + 
                            ChatColor.YELLOW + " - " + quest.getTitle());
                }
            }
        }
    }
    
    /**
     * Complete a quest
     * 
     * @param questId The quest ID
     * @return True if completed successfully
     */
    public boolean completeQuest(UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null || quest.isCompleted()) {
            return false;
        }
        
        quest.setCompleted(true);
        quest.setProgress(quest.getTargetAmount()); // Set progress to max
        return true;
    }
    
    /**
     * Reset a quest
     * 
     * @param questId The quest ID
     * @return True if reset successfully
     */
    public boolean resetQuest(UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return false;
        }
        
        quest.setCompleted(false);
        quest.setProgress(0);
        return true;
    }
    
    /**
     * Give quest rewards to a player
     * 
     * @param player The player
     * @param quest The quest
     */
    private void giveQuestRewards(Player player, Quest quest) {
        // TODO: Implement quest rewards
        // This is a placeholder - actual implementation would give items, XP, etc.
        
        // For now, just give some XP
        int xp = 50;
        switch (quest.getTargetType()) {
            case KILL_ZOMBIES:
                xp = 50 * quest.getTargetAmount() / 10;
                break;
            case COLLECT_ITEMS:
                xp = 30 * quest.getTargetAmount() / 10;
                break;
            case CLEAR_BUILDING:
                xp = 100;
                break;
            case DIG_RESOURCES:
                xp = 20 * quest.getTargetAmount() / 10;
                break;
        }
        
        // Apply reward multiplier from config
        double multiplier = plugin.getConfig().getDouble("mechanics.quests.reward-multiplier", 1.0);
        xp = (int) (xp * multiplier);
        
        player.giveExp(xp);
        player.sendMessage(ChatColor.GREEN + "Received " + xp + " XP for completing the quest!");
    }
    
    /**
     * Save all quests to database
     */
    public void saveQuests() {
        // TODO: Implement database saving
        // This is a placeholder - actual implementation would save to database
    }
    
    /**
     * Check if a player has any active quests
     * 
     * @param player The player
     * @return True if the player has active quests
     */
    public boolean hasActiveQuests(Player player) {
        return !getPlayerActiveQuests(player).isEmpty();
    }
    
    /**
     * Process monster kill progress for a player
     * 
     * @param player The player
     * @param entityType The entity type
     * @param location The location
     */
    public void processKillProgress(Player player, EntityType entityType, Location location) {
        // Only count zombie kills for now
        if (entityType == EntityType.ZOMBIE) {
            updateQuestProgress(player, QuestTargetType.KILL_ZOMBIES, 1);
        }
    }
    
    /**
     * Get quest rewards for a player
     * 
     * @param questId The quest ID
     * @return Array of ItemStack rewards
     */
    public org.bukkit.inventory.ItemStack[] getQuestRewards(UUID questId) {
        // This is a placeholder - actual implementation would generate rewards based on config
        Map<org.bukkit.Material, Integer> rewards = new HashMap<>();
        
        // Just add some random rewards
        rewards.put(org.bukkit.Material.IRON_INGOT, 3);
        rewards.put(org.bukkit.Material.GOLD_INGOT, 1);
        rewards.put(org.bukkit.Material.COOKED_BEEF, 5);
        
        // Convert the map to an array of ItemStacks
        org.bukkit.inventory.ItemStack[] rewardItems = new org.bukkit.inventory.ItemStack[rewards.size()];
        int i = 0;
        for (Map.Entry<org.bukkit.Material, Integer> entry : rewards.entrySet()) {
            rewardItems[i++] = new org.bukkit.inventory.ItemStack(entry.getKey(), entry.getValue());
        }
        
        return rewardItems;
    }
    
    /**
     * Process building clearing progress
     * 
     * @param player The player
     * @param structureId The structure ID
     */
    public void processBuildingClearProgress(Player player, UUID structureId) {
        for (Quest quest : getPlayerActiveQuests(player)) {
            if (quest.getTargetType() == QuestTargetType.CLEAR_BUILDING 
                    && structureId.equals(quest.getStructureId())) {
                // Complete the quest
                quest.setProgress(quest.getTargetAmount());
                completeQuest(quest.getId());
                
                // Notify the player
                player.sendMessage(ChatColor.GREEN + "Building cleared! Quest completed: " + 
                                  ChatColor.YELLOW + quest.getTitle());
                
                // Give rewards
                giveQuestRewards(player, quest);
            }
        }
    }
}