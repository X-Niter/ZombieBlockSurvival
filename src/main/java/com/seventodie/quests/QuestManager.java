package com.seventodie.quests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.traders.TraderNPC;
import com.seventodie.worldgen.StructureManager.Structure;

/**
 * Manages quests, their assignments, tracking, and completion.
 */
public class QuestManager {
    
    private final SevenToDiePlugin plugin;
    private final Random random;
    
    // Quest storage
    private final Map<UUID, Quest> quests = new HashMap<>();
    private final Map<UUID, List<UUID>> playerQuests = new HashMap<>();
    
    // Quest target types
    public enum QuestTargetType {
        KILL_ZOMBIES,
        COLLECT_ITEMS,
        CLEAR_BUILDING,
        DIG_RESOURCES
    }
    
    // Hologram management - will use native methods if no hologram plugins are available
    private boolean useNativeHolograms = true;
    
    public QuestManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        
        // Check for hologram plugins
        if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null) {
            useNativeHolograms = false;
        }
        
        // Load existing quests
        loadQuests();
    }
    
    /**
     * Load existing quests from storage
     */
    private void loadQuests() {
        File questsDir = new File(plugin.getDataFolder(), "quests");
        if (!questsDir.exists()) {
            questsDir.mkdirs();
            return;
        }
        
        File[] files = questsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        
        int count = 0;
        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                UUID questId = UUID.fromString(config.getString("id"));
                
                // Load quest data
                String title = config.getString("title");
                String description = config.getString("description");
                QuestTargetType targetType = QuestTargetType.valueOf(config.getString("targetType"));
                int targetAmount = config.getInt("targetAmount");
                
                // Load location data
                String worldName = config.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Could not load quest location, world not found: " + worldName);
                    continue;
                }
                
                double x = config.getDouble("x");
                double y = config.getDouble("y");
                double z = config.getDouble("z");
                Location location = new Location(world, x, y, z);
                
                // Load structure ID if exists
                UUID structureId = null;
                if (config.contains("structureId")) {
                    structureId = UUID.fromString(config.getString("structureId"));
                }
                
                // Create the quest
                Quest quest = new Quest(questId, title, description, targetType, targetAmount, location, structureId);
                
                // Set completion status
                if (config.contains("completed") && config.getBoolean("completed")) {
                    quest.setCompleted(true);
                }
                
                // Store quest
                quests.put(questId, quest);
                count++;
                
                // Load player associations
                if (config.contains("assignedPlayers")) {
                    List<String> playerUuids = config.getStringList("assignedPlayers");
                    for (String playerUuid : playerUuids) {
                        UUID playerId = UUID.fromString(playerUuid);
                        if (!playerQuests.containsKey(playerId)) {
                            playerQuests.put(playerId, new ArrayList<>());
                        }
                        playerQuests.get(playerId).add(questId);
                    }
                }
                
                // Re-create hologram marker if needed
                if (!quest.isCompleted()) {
                    createQuestMarker(quest);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading quest from " + file.getName() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + count + " quests");
    }
    
    /**
     * Save a quest to storage
     * 
     * @param quest The quest to save
     */
    private void saveQuest(Quest quest) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            // Save basic quest data
            config.set("id", quest.getId().toString());
            config.set("title", quest.getTitle());
            config.set("description", quest.getDescription());
            config.set("targetType", quest.getTargetType().name());
            config.set("targetAmount", quest.getTargetAmount());
            config.set("completed", quest.isCompleted());
            
            // Save location data
            config.set("world", quest.getLocation().getWorld().getName());
            config.set("x", quest.getLocation().getX());
            config.set("y", quest.getLocation().getY());
            config.set("z", quest.getLocation().getZ());
            
            // Save structure ID if exists
            if (quest.getStructureId() != null) {
                config.set("structureId", quest.getStructureId().toString());
            }
            
            // Save player associations
            List<String> playerUuids = new ArrayList<>();
            for (Map.Entry<UUID, List<UUID>> entry : playerQuests.entrySet()) {
                if (entry.getValue().contains(quest.getId())) {
                    playerUuids.add(entry.getKey().toString());
                }
            }
            
            if (!playerUuids.isEmpty()) {
                config.set("assignedPlayers", playerUuids);
            }
            
            // Save to quests folder
            File questsDir = new File(plugin.getDataFolder(), "quests");
            if (!questsDir.exists()) {
                questsDir.mkdirs();
            }
            
            File questFile = new File(questsDir, quest.getId() + ".yml");
            config.save(questFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save quest: " + e.getMessage());
        }
    }
    
    /**
     * Create a random quest for a structure
     * 
     * @param structure The structure to create a quest for
     * @return The created quest
     */
    public Quest createRandomQuest(Structure structure) {
        QuestTargetType targetType;
        int targetAmount;
        String title;
        String description;
        
        // Determine quest type based on structure type
        switch (structure.getType()) {
            case RESIDENTIAL:
                targetType = random.nextInt(10) < 7 ? QuestTargetType.CLEAR_BUILDING : QuestTargetType.KILL_ZOMBIES;
                targetAmount = targetType == QuestTargetType.CLEAR_BUILDING ? 1 : 5 + random.nextInt(10);
                break;
            case COMMERCIAL:
                targetType = random.nextInt(10) < 6 ? QuestTargetType.COLLECT_ITEMS : QuestTargetType.KILL_ZOMBIES;
                targetAmount = targetType == QuestTargetType.COLLECT_ITEMS ? 3 + random.nextInt(10) : 8 + random.nextInt(12);
                break;
            case INDUSTRIAL:
                targetType = random.nextInt(10) < 7 ? QuestTargetType.DIG_RESOURCES : QuestTargetType.KILL_ZOMBIES;
                targetAmount = targetType == QuestTargetType.DIG_RESOURCES ? 10 + random.nextInt(20) : 10 + random.nextInt(15);
                break;
            default:
                targetType = QuestTargetType.KILL_ZOMBIES;
                targetAmount = 5 + random.nextInt(10);
        }
        
        // Generate title and description
        title = generateQuestTitle(targetType, structure);
        description = generateQuestDescription(targetType, targetAmount, structure);
        
        // Create quest
        UUID questId = UUID.randomUUID();
        Location questLocation = structure.getLocation().clone().add(
            structure.getSize().getX() / 2, 
            1, 
            structure.getSize().getZ() / 2
        );
        
        Quest quest = new Quest(questId, title, description, targetType, targetAmount, questLocation, structure.getId());
        
        // Save and register quest
        quests.put(questId, quest);
        saveQuest(quest);
        
        // Create hologram marker
        createQuestMarker(quest);
        
        return quest;
    }
    
    /**
     * Generate a title for a quest
     * 
     * @param targetType The quest target type
     * @param structure The structure
     * @return The generated title
     */
    private String generateQuestTitle(QuestTargetType targetType, Structure structure) {
        String[] killPrefixes = {"Clear Out", "Exterminate", "Eliminate", "Purge", "Kill"};
        String[] collectPrefixes = {"Gather", "Collect", "Salvage", "Find", "Scavenge"};
        String[] clearPrefixes = {"Clear", "Secure", "Reclaim", "Liberate", "Search"};
        String[] digPrefixes = {"Excavate", "Mine", "Dig Up", "Extract", "Harvest"};
        
        String prefix;
        String suffix;
        
        switch (targetType) {
            case KILL_ZOMBIES:
                prefix = killPrefixes[random.nextInt(killPrefixes.length)];
                suffix = "Zombies";
                break;
            case COLLECT_ITEMS:
                prefix = collectPrefixes[random.nextInt(collectPrefixes.length)];
                suffix = getRandomCollectableName();
                break;
            case CLEAR_BUILDING:
                prefix = clearPrefixes[random.nextInt(clearPrefixes.length)];
                suffix = getStructureDescription(structure);
                break;
            case DIG_RESOURCES:
                prefix = digPrefixes[random.nextInt(digPrefixes.length)];
                suffix = getRandomResourceName();
                break;
            default:
                prefix = "Complete";
                suffix = "Quest";
        }
        
        return prefix + " " + suffix;
    }
    
    /**
     * Generate a description for a quest
     * 
     * @param targetType The quest target type
     * @param targetAmount The target amount
     * @param structure The structure
     * @return The generated description
     */
    private String generateQuestDescription(QuestTargetType targetType, int targetAmount, Structure structure) {
        switch (targetType) {
            case KILL_ZOMBIES:
                return "Eliminate " + targetAmount + " zombies in the area surrounding " + 
                       getStructureDescription(structure) + ".";
            case COLLECT_ITEMS:
                return "Find and collect " + targetAmount + " " + getRandomCollectableName() + 
                       " from " + getStructureDescription(structure) + ".";
            case CLEAR_BUILDING:
                return "Clear out and secure " + getStructureDescription(structure) + 
                       ". Eliminate all threats inside.";
            case DIG_RESOURCES:
                return "Dig up " + targetAmount + " " + getRandomResourceName() + 
                       " from the marked locations near " + getStructureDescription(structure) + ".";
            default:
                return "Complete the task at the marked location.";
        }
    }
    
    /**
     * Get a description of a structure
     * 
     * @param structure The structure
     * @return A description of the structure
     */
    private String getStructureDescription(Structure structure) {
        switch (structure.getType()) {
            case RESIDENTIAL:
                String[] residentialTypes = {"house", "apartment", "cabin", "mansion", "residence"};
                return "the " + residentialTypes[random.nextInt(residentialTypes.length)];
            case COMMERCIAL:
                String[] commercialTypes = {"store", "office building", "shop", "mall", "market"};
                return "the " + commercialTypes[random.nextInt(commercialTypes.length)];
            case INDUSTRIAL:
                String[] industrialTypes = {"factory", "warehouse", "processing plant", "refinery", "workshop"};
                return "the " + industrialTypes[random.nextInt(industrialTypes.length)];
            default:
                return "the building";
        }
    }
    
    /**
     * Get a random collectable item name
     * 
     * @return A random collectable name
     */
    private String getRandomCollectableName() {
        String[] collectables = {"supplies", "medicine", "food", "weapons", "tools", "electronics", 
                                "clothing", "books", "documents", "schematics"};
        return collectables[random.nextInt(collectables.length)];
    }
    
    /**
     * Get a random resource name
     * 
     * @return A random resource name
     */
    private String getRandomResourceName() {
        String[] resources = {"iron", "coal", "copper", "lead", "potassium nitrate", "oil shale", 
                             "stone", "clay", "silver", "gold"};
        return resources[random.nextInt(resources.length)];
    }
    
    /**
     * Create a holographic marker for a quest
     * 
     * @param quest The quest to create a marker for
     */
    private void createQuestMarker(Quest quest) {
        Location markerLocation = quest.getLocation().clone().add(0, 2.5, 0);
        
        if (!useNativeHolograms) {
            // Try to use HolographicDisplays if available
            try {
                Class<?> hdAPI = Class.forName("com.gmail.filoghost.holographicdisplays.api.HologramsAPI");
                Object hologram = hdAPI.getMethod("createHologram", org.bukkit.plugin.Plugin.class, Location.class)
                    .invoke(null, plugin, markerLocation);
                
                // Add text lines
                hologram.getClass().getMethod("appendTextLine", String.class)
                    .invoke(hologram, ChatColor.GOLD + "! " + ChatColor.YELLOW + "Quest" + ChatColor.GOLD + " !");
                hologram.getClass().getMethod("appendTextLine", String.class)
                    .invoke(hologram, ChatColor.WHITE + quest.getTitle());
                
                // Store reference to hologram in quest object
                quest.setMarkerRef(hologram);
                
                return;
            } catch (Exception e) {
                // Fallback to native method
                plugin.getLogger().warning("Failed to create HolographicDisplays marker: " + e.getMessage());
                useNativeHolograms = true;
            }
        }
        
        // Use native marker (armor stand)
        try {
            // Spawn armor stand as marker
            org.bukkit.entity.ArmorStand marker = markerLocation.getWorld().spawn(
                markerLocation, org.bukkit.entity.ArmorStand.class
            );
            
            marker.setCustomName(ChatColor.GOLD + "! " + ChatColor.YELLOW + "Quest" + ChatColor.GOLD + " !");
            marker.setCustomNameVisible(true);
            marker.setVisible(false);
            marker.setGravity(false);
            marker.setInvulnerable(true);
            
            // Add a second marker below for title
            org.bukkit.entity.ArmorStand titleMarker = markerLocation.getWorld().spawn(
                markerLocation.clone().subtract(0, 0.3, 0), org.bukkit.entity.ArmorStand.class
            );
            
            titleMarker.setCustomName(ChatColor.WHITE + quest.getTitle());
            titleMarker.setCustomNameVisible(true);
            titleMarker.setVisible(false);
            titleMarker.setGravity(false);
            titleMarker.setInvulnerable(true);
            
            // Store references in quest object
            quest.setMarkerRef(marker);
            quest.setSecondaryMarkerRef(titleMarker);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create native quest marker: " + e.getMessage());
        }
    }
    
    /**
     * Remove a quest marker
     * 
     * @param quest The quest to remove the marker for
     */
    private void removeQuestMarker(Quest quest) {
        if (!useNativeHolograms) {
            // Try to remove HolographicDisplays hologram
            try {
                Object hologram = quest.getMarkerRef();
                if (hologram != null) {
                    hologram.getClass().getMethod("delete").invoke(hologram);
                    quest.setMarkerRef(null);
                }
                return;
            } catch (Exception e) {
                // Fallback to native method
                plugin.getLogger().warning("Failed to remove HolographicDisplays marker: " + e.getMessage());
            }
        }
        
        // Remove native markers (armor stands)
        try {
            Object marker = quest.getMarkerRef();
            if (marker instanceof org.bukkit.entity.ArmorStand) {
                ((org.bukkit.entity.ArmorStand) marker).remove();
                quest.setMarkerRef(null);
            }
            
            Object secondaryMarker = quest.getSecondaryMarkerRef();
            if (secondaryMarker instanceof org.bukkit.entity.ArmorStand) {
                ((org.bukkit.entity.ArmorStand) secondaryMarker).remove();
                quest.setSecondaryMarkerRef(null);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove native quest marker: " + e.getMessage());
        }
    }
    
    /**
     * Assign a quest to a player
     * 
     * @param player The player
     * @param quest The quest
     */
    public void assignQuestToPlayer(Player player, Quest quest) {
        UUID playerId = player.getUniqueId();
        
        if (!playerQuests.containsKey(playerId)) {
            playerQuests.put(playerId, new ArrayList<>());
        }
        
        if (!playerQuests.get(playerId).contains(quest.getId())) {
            playerQuests.get(playerId).add(quest.getId());
            saveQuest(quest);
            
            // Notify player
            player.sendMessage(ChatColor.GREEN + "New quest: " + ChatColor.GOLD + quest.getTitle());
            player.sendMessage(ChatColor.GRAY + quest.getDescription());
        }
    }
    
    /**
     * Assign a random quest to a player from a trader
     * 
     * @param player The player
     * @param trader The trader NPC
     * @return The assigned quest, or null if no quest was assigned
     */
    public Quest assignRandomQuestToPlayer(Player player, TraderNPC trader) {
        // Find uncompleted quests that aren't assigned to this player
        List<Quest> availableQuests = new ArrayList<>();
        
        for (Quest quest : quests.values()) {
            if (!quest.isCompleted() && 
                (playerQuests.get(player.getUniqueId()) == null || 
                 !playerQuests.get(player.getUniqueId()).contains(quest.getId()))) {
                availableQuests.add(quest);
            }
        }
        
        if (availableQuests.isEmpty()) {
            // Create a new quest if none are available
            Structure randomStructure = findRandomStructureForQuest();
            if (randomStructure != null) {
                Quest newQuest = createRandomQuest(randomStructure);
                assignQuestToPlayer(player, newQuest);
                return newQuest;
            } else {
                player.sendMessage(ChatColor.RED + "No quests available at this time.");
                return null;
            }
        } else {
            // Assign a random existing quest
            Quest quest = availableQuests.get(random.nextInt(availableQuests.size()));
            assignQuestToPlayer(player, quest);
            return quest;
        }
    }
    
    /**
     * Find a random structure for creating a new quest
     * 
     * @return A random structure suitable for a quest
     */
    private Structure findRandomStructureForQuest() {
        List<Structure> structures = new ArrayList<>();
        
        // Get all non-trader structures
        structures.addAll(plugin.getStructureManager().getStructuresByType(
            com.seventodie.worldgen.StructureManager.StructureType.RESIDENTIAL));
        structures.addAll(plugin.getStructureManager().getStructuresByType(
            com.seventodie.worldgen.StructureManager.StructureType.COMMERCIAL));
        structures.addAll(plugin.getStructureManager().getStructuresByType(
            com.seventodie.worldgen.StructureManager.StructureType.INDUSTRIAL));
        
        if (structures.isEmpty()) {
            return null;
        }
        
        return structures.get(random.nextInt(structures.size()));
    }
    
    /**
     * Complete a quest
     * 
     * @param questId The quest ID
     */
    public void completeQuest(UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return;
        }
        
        quest.setCompleted(true);
        saveQuest(quest);
        
        // Remove the quest marker
        removeQuestMarker(quest);
        
        // Reset the structure if this is a building quest
        if (quest.getStructureId() != null) {
            plugin.getStructureManager().resetStructure(quest.getStructureId());
        }
    }
    
    /**
     * Check if a player has active quests
     * 
     * @param player The player
     * @return True if the player has active quests
     */
    public boolean hasActiveQuests(Player player) {
        UUID playerId = player.getUniqueId();
        return playerQuests.containsKey(playerId) && !playerQuests.get(playerId).isEmpty();
    }
    
    /**
     * Get a player's active quests
     * 
     * @param player The player
     * @return The player's active quests
     */
    public List<Quest> getPlayerActiveQuests(Player player) {
        UUID playerId = player.getUniqueId();
        List<Quest> activeQuests = new ArrayList<>();
        
        if (playerQuests.containsKey(playerId)) {
            for (UUID questId : playerQuests.get(playerId)) {
                Quest quest = quests.get(questId);
                if (quest != null && !quest.isCompleted()) {
                    activeQuests.add(quest);
                }
            }
        }
        
        return activeQuests;
    }
    
    /**
     * Get quest rewards based on quest type
     * 
     * @param questId The quest ID
     * @return The rewards for completing the quest
     */
    public ItemStack[] getQuestRewards(UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return new ItemStack[0];
        }
        
        List<ItemStack> rewards = new ArrayList<>();
        
        // Base rewards for all quests
        rewards.add(new ItemStack(Material.IRON_INGOT, 3 + random.nextInt(8)));
        
        // Additional rewards based on quest type
        switch (quest.getTargetType()) {
            case KILL_ZOMBIES:
                rewards.add(new ItemStack(Material.BONE, 5 + random.nextInt(15)));
                if (random.nextInt(100) < 20) {
                    rewards.add(new ItemStack(Material.DIAMOND, 1 + random.nextInt(2)));
                }
                break;
            case COLLECT_ITEMS:
                rewards.add(new ItemStack(Material.EMERALD, 2 + random.nextInt(3)));
                if (random.nextInt(100) < 30) {
                    rewards.add(new ItemStack(Material.ENCHANTED_BOOK));
                }
                break;
            case CLEAR_BUILDING:
                rewards.add(new ItemStack(Material.GOLD_INGOT, 2 + random.nextInt(5)));
                if (random.nextInt(100) < 40) {
                    rewards.add(plugin.getBlockManager().createFrameBlockItem());
                }
                break;
            case DIG_RESOURCES:
                rewards.add(new ItemStack(Material.DIAMOND, 1 + random.nextInt(3)));
                if (random.nextInt(100) < 25) {
                    rewards.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
                }
                break;
        }
        
        // Rare chance for special rewards
        if (random.nextInt(100) < 10) {
            // Randomly select one of the hammers
            int hammerType = random.nextInt(3);
            switch (hammerType) {
                case 0:
                    rewards.add(plugin.getToolManager().createStoneHammer());
                    break;
                case 1:
                    rewards.add(plugin.getToolManager().createIronHammer());
                    break;
                case 2:
                    rewards.add(plugin.getToolManager().createSteelHammer());
                    break;
            }
        }
        
        return rewards.toArray(new ItemStack[0]);
    }
    
    /**
     * Process player progress for kill quests
     * 
     * @param player The player
     * @param entityType The entity type killed
     * @param location The location of the kill
     */
    public void processKillProgress(Player player, EntityType entityType, Location location) {
        if (entityType != EntityType.ZOMBIE && entityType != EntityType.ZOMBIE_VILLAGER &&
            entityType != EntityType.HUSK && entityType != EntityType.DROWNED) {
            return;
        }
        
        List<Quest> activeQuests = getPlayerActiveQuests(player);
        for (Quest quest : activeQuests) {
            if (quest.getTargetType() == QuestTargetType.KILL_ZOMBIES) {
                // Check if kill is near the quest location
                if (quest.getLocation().getWorld().equals(location.getWorld()) &&
                    quest.getLocation().distance(location) < 50) {
                    
                    // Increment progress
                    int currentProgress = quest.getProgress(player.getUniqueId());
                    currentProgress++;
                    quest.setProgress(player.getUniqueId(), currentProgress);
                    
                    // Check if quest is complete
                    if (currentProgress >= quest.getTargetAmount()) {
                        // Notify player
                        player.sendMessage(ChatColor.GREEN + "Quest complete: " + ChatColor.GOLD + quest.getTitle());
                        player.sendMessage(ChatColor.GRAY + "Return to a trader to claim your reward.");
                    } else {
                        // Update progress
                        player.sendMessage(ChatColor.YELLOW + "Quest progress: " + ChatColor.WHITE + 
                                           currentProgress + "/" + quest.getTargetAmount() + 
                                           ChatColor.YELLOW + " zombies killed");
                    }
                }
            }
        }
    }
    
    /**
     * Process player progress for building clear quests
     * 
     * @param player The player
     * @param structureId The structure ID being cleared
     */
    public void processBuildingClearProgress(Player player, UUID structureId) {
        List<Quest> activeQuests = getPlayerActiveQuests(player);
        for (Quest quest : activeQuests) {
            if (quest.getTargetType() == QuestTargetType.CLEAR_BUILDING && 
                quest.getStructureId() != null && 
                quest.getStructureId().equals(structureId)) {
                
                // Mark as completed (these are binary - either cleared or not)
                quest.setProgress(player.getUniqueId(), quest.getTargetAmount());
                
                // Notify player
                player.sendMessage(ChatColor.GREEN + "Quest complete: " + ChatColor.GOLD + quest.getTitle());
                player.sendMessage(ChatColor.GRAY + "Return to a trader to claim your reward.");
            }
        }
    }
    
    /**
     * Get a quest by ID
     * 
     * @param questId The quest ID
     * @return The quest, or null if not found
     */
    public Quest getQuest(UUID questId) {
        return quests.get(questId);
    }
}
