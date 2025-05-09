package com.seventodie.traders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.worldgen.StructureManager.Structure;

/**
 * Manages trader NPCs and trader outposts in the world.
 */
public class TraderManager {
    
    private final SevenToDiePlugin plugin;
    private final Random random;
    
    // Trader outposts and NPCs
    private final Map<UUID, TraderOutpost> traderOutposts = new HashMap<>();
    private final Map<UUID, TraderNPC> traderNPCs = new HashMap<>();
    
    // Day/Night cycle constants
    private static final long DAY_TIME = 0;
    private static final long NIGHT_TIME = 13000;
    
    /**
     * Represents a trader outpost in the world
     */
    public class TraderOutpost {
        private UUID structureId;
        private Location location;
        private UUID npcId;
        private boolean isOpen;
        
        public TraderOutpost(UUID structureId, Location location) {
            this.structureId = structureId;
            this.location = location;
            this.isOpen = true; // Default to open during day
        }
        
        public UUID getStructureId() {
            return structureId;
        }
        
        public Location getLocation() {
            return location;
        }
        
        public UUID getNpcId() {
            return npcId;
        }
        
        public void setNpcId(UUID npcId) {
            this.npcId = npcId;
        }
        
        public boolean isOpen() {
            return isOpen;
        }
        
        public void setOpen(boolean isOpen) {
            this.isOpen = isOpen;
        }
    }
    
    /**
     * List of possible trader voice lines
     */
    private final List<String> greetingLines = new ArrayList<>();
    private final List<String> farewellLines = new ArrayList<>();
    private final List<String> dayEndingLines = new ArrayList<>();
    private final List<String> questCompletedLines = new ArrayList<>();
    
    public TraderManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        
        // Initialize trader voice lines
        initVoiceLines();
    }
    
    /**
     * Initialize trader voice lines
     */
    private void initVoiceLines() {
        // Greeting lines
        greetingLines.add("Welcome to my shop, stranger!");
        greetingLines.add("Got some rare items today, take a look!");
        greetingLines.add("What are ya buyin'?");
        greetingLines.add("Need supplies? I've got what you need.");
        greetingLines.add("Another survivor! Good to see ya.");
        
        // Farewell lines
        farewellLines.add("Come back soon!");
        farewellLines.add("Stay safe out there.");
        farewellLines.add("Don't get yourself killed out there.");
        farewellLines.add("Thanks for the business.");
        farewellLines.add("Be careful, it's a tough world.");
        
        // Day ending lines
        dayEndingLines.add("It's getting dark, you need to leave now!");
        dayEndingLines.add("Shop's closing, get out before dark!");
        dayEndingLines.add("Time to go! I don't let anyone stay after dark.");
        dayEndingLines.add("Sun's setting, time for you to leave.");
        dayEndingLines.add("I'm closing up shop. Come back tomorrow!");
        
        // Quest completed lines
        questCompletedLines.add("Good job on that quest!");
        questCompletedLines.add("Didn't think you'd make it back alive!");
        questCompletedLines.add("You've earned this reward.");
        questCompletedLines.add("Not bad for an amateur.");
        questCompletedLines.add("Impressive work, I might have more jobs for you.");
    }
    
    /**
     * Register a trader outpost from a structure
     * 
     * @param structure The trader outpost structure
     */
    public void registerTraderOutpost(Structure structure) {
        if (structure == null) {
            return;
        }
        
        // Create the trader outpost
        TraderOutpost outpost = new TraderOutpost(
            structure.getId(),
            structure.getLocation().clone().add(structure.getSize().getX() / 2, 1, structure.getSize().getZ() / 2)
        );
        
        // Store the outpost
        traderOutposts.put(structure.getId(), outpost);
        
        // Create the trader NPC
        createTraderNPC(outpost);
        
        plugin.getLogger().info("Registered trader outpost at " + outpost.getLocation());
    }
    
    /**
     * Create a trader NPC at a trader outpost
     * 
     * @param outpost The trader outpost
     */
    private void createTraderNPC(TraderOutpost outpost) {
        // Scheduled task to ensure chunks are loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                // Find a suitable location inside the structure
                Location npcLoc = outpost.getLocation().clone();
                
                // Get the world
                if (npcLoc.getWorld() == null) {
                    plugin.getLogger().warning("Could not create trader NPC: world is null");
                    return;
                }
                
                // Check if Citizens is available
                if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
                    plugin.getLogger().warning("Citizens not found, cannot create trader NPC");
                    return;
                }
                
                // Create the trader NPC
                try {
                    TraderNPC trader = new TraderNPC(plugin, npcLoc);
                    UUID npcId = trader.spawn();
                    
                    if (npcId != null) {
                        // Store the NPC
                        traderNPCs.put(npcId, trader);
                        outpost.setNpcId(npcId);
                        
                        plugin.getLogger().info("Created trader NPC at " + npcLoc);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error creating trader NPC: " + e.getMessage());
                }
            }
        }.runTaskLater(plugin, 40L); // 2 second delay
    }
    
    /**
     * Handle day/night cycle for trader outposts
     */
    public void handleDayNightCycle() {
        // Check the time in each world
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            long time = world.getTime();
            boolean isNight = time >= NIGHT_TIME && time <= 24000;
            
            // Update all trader outposts in this world
            for (TraderOutpost outpost : traderOutposts.values()) {
                if (outpost.getLocation().getWorld().equals(world)) {
                    // If night is falling and outpost is open, close it
                    if (isNight && outpost.isOpen()) {
                        closeTraderOutpost(outpost);
                    }
                    // If day is breaking and outpost is closed, open it
                    else if (!isNight && !outpost.isOpen()) {
                        openTraderOutpost(outpost);
                    }
                }
            }
        }
    }
    
    /**
     * Close a trader outpost (night time)
     * 
     * @param outpost The trader outpost to close
     */
    private void closeTraderOutpost(TraderOutpost outpost) {
        outpost.setOpen(false);
        
        // Find all players inside the structure
        Structure structure = plugin.getStructureManager().getStructure(outpost.getStructureId());
        if (structure == null) {
            return;
        }
        
        // Create a list to store players that need to be ejected
        List<Player> playersToEject = new ArrayList<>();
        
        // Find players inside
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (structure.containsLocation(player.getLocation())) {
                playersToEject.add(player);
            }
        }
        
        // Teleport players outside and show message
        for (Player player : playersToEject) {
            // Get a random trader leaving line
            String line = dayEndingLines.get(random.nextInt(dayEndingLines.size()));
            player.sendMessage("§e[Trader] §f" + line);
            
            // Play sound
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            
            // Calculate outside location (8 blocks away from trader in random direction)
            Location outsideLoc = getOutsideLocation(structure);
            player.teleport(outsideLoc);
            
            player.sendMessage("§cThe trader has closed for the night. Return during the day.");
        }
        
        // Let the trader NPC know it's closed
        TraderNPC npc = traderNPCs.get(outpost.getNpcId());
        if (npc != null) {
            npc.setTrading(false);
        }
    }
    
    /**
     * Open a trader outpost (day time)
     * 
     * @param outpost The trader outpost to open
     */
    private void openTraderOutpost(TraderOutpost outpost) {
        outpost.setOpen(true);
        
        // Let the trader NPC know it's open
        TraderNPC npc = traderNPCs.get(outpost.getNpcId());
        if (npc != null) {
            npc.setTrading(true);
        }
    }
    
    /**
     * Calculate a safe location outside of a structure
     * 
     * @param structure The structure
     * @return A safe location outside the structure
     */
    private Location getOutsideLocation(Structure structure) {
        Location center = structure.getLocation().clone().add(
            structure.getSize().getX() / 2,
            0,
            structure.getSize().getZ() / 2
        );
        
        // Get a point 10-15 blocks away in a random direction
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 10 + random.nextDouble() * 5;
        
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        
        // Find safe Y position
        int y = center.getWorld().getHighestBlockYAt((int)x, (int)z) + 1;
        
        return new Location(center.getWorld(), x, y, z);
    }
    
    /**
     * Check if a player can interact with a trader
     * 
     * @param player The player
     * @param npcId The trader NPC ID
     * @return True if the player can interact with the trader
     */
    public boolean canInteractWithTrader(Player player, UUID npcId) {
        // Find the trader NPC
        TraderNPC npc = traderNPCs.get(npcId);
        if (npc == null) {
            return false;
        }
        
        // Find which outpost this NPC belongs to
        TraderOutpost outpost = null;
        for (TraderOutpost o : traderOutposts.values()) {
            if (npcId.equals(o.getNpcId())) {
                outpost = o;
                break;
            }
        }
        
        if (outpost == null) {
            return false;
        }
        
        // Check if the outpost is open
        return outpost.isOpen();
    }
    
    /**
     * Handle a player interacting with a trader NPC
     * 
     * @param player The player
     * @param npcId The trader NPC ID
     */
    public void handleTraderInteraction(Player player, UUID npcId) {
        // Check if the interaction is allowed
        if (!canInteractWithTrader(player, npcId)) {
            player.sendMessage("§cThe trader is not available at night. Come back during the day.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Get the trader NPC
        TraderNPC npc = traderNPCs.get(npcId);
        if (npc == null) {
            return;
        }
        
        // Say a greeting
        String greeting = greetingLines.get(random.nextInt(greetingLines.size()));
        player.sendMessage("§e[Trader] §f" + greeting);
        
        // Play voice sound
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        
        // Open the trading menu
        Inventory tradeMenu = npc.createTradeMenu(player);
        player.openInventory(tradeMenu);
    }
    
    /**
     * Handle trader quest completion
     * 
     * @param player The player
     * @param npcId The trader NPC ID
     * @param questId The completed quest ID
     */
    public void handleQuestCompletion(Player player, UUID npcId, UUID questId) {
        // Get the trader NPC
        TraderNPC npc = traderNPCs.get(npcId);
        if (npc == null) {
            return;
        }
        
        // Say a congratulation
        String line = questCompletedLines.get(random.nextInt(questCompletedLines.size()));
        player.sendMessage("§e[Trader] §f" + line);
        
        // Play voice sound
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
        
        // Give rewards
        ItemStack[] rewards = plugin.getQuestManager().getQuestRewards(questId);
        if (rewards != null) {
            for (ItemStack reward : rewards) {
                if (reward != null) {
                    player.getInventory().addItem(reward);
                }
            }
        }
        
        // Mark quest as completed
        plugin.getQuestManager().completeQuest(questId);
        
        // Assign a new quest
        plugin.getQuestManager().assignRandomQuestToPlayer(player, npc);
    }
    
    /**
     * Get a trader NPC entity by ID
     * 
     * @param npcId The NPC ID
     * @return The trader NPC entity, or null if not found
     */
    public Entity getTraderEntity(UUID npcId) {
        TraderNPC npc = traderNPCs.get(npcId);
        if (npc != null) {
            return npc.getEntity();
        }
        return null;
    }
    
    /**
     * Get all trader outposts
     * 
     * @return A list of all trader outposts
     */
    public List<TraderOutpost> getAllTraderOutposts() {
        return new ArrayList<>(traderOutposts.values());
    }
    
    /**
     * Get a random greeting line
     * 
     * @return A random greeting line
     */
    public String getRandomGreeting() {
        return greetingLines.get(random.nextInt(greetingLines.size()));
    }
    
    /**
     * Get a random farewell line
     * 
     * @return A random farewell line
     */
    public String getRandomFarewell() {
        return farewellLines.get(random.nextInt(farewellLines.size()));
    }
}
