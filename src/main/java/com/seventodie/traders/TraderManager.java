package com.seventodie.traders;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import com.seventodie.SevenToDiePlugin;
import com.seventodie.utils.SchematicUtils;
import com.seventodie.worldgen.StructureManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Chunk;

/**
 * Manages trader NPCs and outposts
 */
public class TraderManager {

    private final SevenToDiePlugin plugin;
    private final Map<UUID, TraderNPC> traders = new HashMap<>();
    private final Map<UUID, TraderOutpost> outposts = new HashMap<>();
    private final boolean useCitizens;

    private BukkitTask outpostTask;
    private final Map<World, List<TraderOutpost>> outpostsByWorld = new HashMap<>();
    private final Map<ChunkCoordinate, List<TraderOutpost>> outpostsByChunk = new HashMap<>();

    /**
     * Constructor for TraderManager
     * 
     * @param plugin The SevenToDie plugin instance
     */
    public TraderManager(SevenToDiePlugin plugin) {
        this.plugin = plugin;
        this.useCitizens = plugin.getServer().getPluginManager().getPlugin("Citizens") != null;

        // Load traders from database
        loadTraders();

        // Start outpost update task
        startOutpostTask();

        // Initialize spatial partitioning
        initializeOutpostMaps();
    }

    private void initializeOutpostMaps() {
        for (TraderOutpost outpost : outposts.values()) {
            World world = outpost.getLocation().getWorld();
            if (world != null) {
                outpostsByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(outpost);
            }
            ChunkCoordinate chunkCoord = new ChunkCoordinate(outpost.getLocation().getChunk());
            outpostsByChunk.computeIfAbsent(chunkCoord, k -> new ArrayList<>()).add(outpost);
        }
    }

    /**
     * Load traders from database
     */
    private void loadTraders() {
        // TODO: Implement database loading
        // This is a placeholder - actual implementation would load from database
    }

    /**
     * Start the outpost update task
     */
    private void startOutpostTask() {
        // Run every minute
        outpostTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            updateOutposts();
        }, 20 * 60, 20 * 60);
    }

    private static final int UPDATE_RADIUS = 128;
    private static final int BATCH_SIZE = 16;
    
    private void updateOutposts() {
        // Use spatial indexing for efficient updates
        for (Map.Entry<World, List<TraderOutpost>> entry : outpostsByWorld.entrySet()) {
            World world = entry.getKey();
            List<TraderOutpost> outposts = entry.getValue();
            
            // Process in batches
            for (int i = 0; i < outposts.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, outposts.size());
                List<TraderOutpost> batch = outposts.subList(i, end);
        for (TraderOutpost outpost : outposts.values()) {
            outpostsByWorld.computeIfAbsent(outpost.getLocation().getWorld(), w -> new ArrayList<>()).add(outpost);
        }

        // Process each world's outposts together
        for (Map.Entry<World, List<TraderOutpost>> entry : outpostsByWorld.entrySet()) {
            World world = entry.getKey();
            long worldTime = world.getTime();
            boolean shouldBeOpen = worldTime >= 0 && worldTime < 12000;

            for (TraderOutpost outpost : entry.getValue()) {
                if (shouldBeOpen != outpost.isOpen()) {
                    if (shouldBeOpen) {
                        openOutpost(outpost);
                    } else {
                        closeOutpost(outpost);
                    }
                }
            }
        }
    }

    /**
     * Open an outpost
     * 
     * @param outpost The outpost
     */
    private void openOutpost(TraderOutpost outpost) {
        outpost.setOpen(true);

        // Notify nearby players
        List<Player> nearbyPlayers = getNearbyPlayers(outpost.getLocation(), 50);
        for (Player player : nearbyPlayers) {
            player.sendMessage(ChatColor.GREEN + "Trader at " + 
                    ChatColor.YELLOW + formatLocation(outpost.getLocation()) + 
                    ChatColor.GREEN + " is now open for business!");
        }
    }

    /**
     * Close an outpost
     * 
     * @param outpost The outpost
     */
    private void closeOutpost(TraderOutpost outpost) {
        outpost.setOpen(false);

        // Notify nearby players
        List<Player> nearbyPlayers = getNearbyPlayers(outpost.getLocation(), 50);
        for (Player player : nearbyPlayers) {
            player.sendMessage(ChatColor.RED + "Trader at " + 
                    ChatColor.YELLOW + formatLocation(outpost.getLocation()) + 
                    ChatColor.RED + " is now closed. Come back during the day!");
        }

        // Close any open menus for this trader
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // TODO: Implement menu closing
        }
    }

    /**
     * Get players near a location
     * 
     * @param location The location
     * @param radius The radius
     * @return The nearby players
     */
    private List<Player> getNearbyPlayers(Location location, double radius) {
        List<Player> nearbyPlayers = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                nearbyPlayers.add(player);
            }
        }

        return nearbyPlayers;
    }

    /**
     * Format a location for display
     * 
     * @param location The location
     * @return The formatted location
     */
    private String formatLocation(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    /**
     * Create a new trader outpost
     * 
     * @param location The location
     * @return The outpost ID, or null if failed
     */
    public UUID createOutpost(Location location) {
        try {
            // Generate unique ID
            UUID outpostId = UUID.randomUUID();

            // Create outpost
            TraderOutpost outpost = new TraderOutpost(outpostId, location);
            outposts.put(outpostId, outpost);

            // Add to spatial partitioning maps
            World world = location.getWorld();
            if (world != null) {
                outpostsByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(outpost);
            }
            ChunkCoordinate chunkCoord = new ChunkCoordinate(location.getChunk());
            outpostsByChunk.computeIfAbsent(chunkCoord, k -> new ArrayList<>()).add(outpost);

            // Place outpost structure
            Object schematic = plugin.getSchematicUtils().loadSchematic("trader_outpost");
            if (schematic != null) {
                plugin.getSchematicUtils().placeSchematic(schematic, location.getWorld(), 
                        location.getBlockX(), location.getBlockY(), location.getBlockZ(), 0);
            }

            // Spawn trader NPC
            spawnTraderNPC(outpost);

            return outpostId;
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating trader outpost: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Spawn a trader NPC at an outpost
     * 
     * @param outpost The outpost
     * @return The trader ID, or null if failed
     */
    private UUID spawnTraderNPC(TraderOutpost outpost) {
        try {
            Location spawnLoc = outpost.getLocation().clone().add(0.5, 1, 0.5);
            Entity entity;

            if (useCitizens) {
                // If Citizens is available, use it
                // This is a placeholder - actual implementation would use Citizens API
                entity = spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);
            } else {
                // Fall back to vanilla entities
                entity = spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.VILLAGER);

                // Configure the villager
                Villager villager = (Villager) entity;
                villager.setCustomName(ChatColor.GOLD + "Trader");
                villager.setCustomNameVisible(true);
                villager.setProfession(Villager.Profession.WEAPONSMITH);
                villager.setVillagerType(Villager.Type.PLAINS);
                villager.setVillagerLevel(5);
                villager.setAI(false);
                villager.setInvulnerable(true);
            }

            // Create the trader NPC
            UUID traderId = UUID.randomUUID();
            TraderNPC trader = new TraderNPC(traderId, entity, outpost);
            traders.put(traderId, trader);

            // Update the outpost with the NPC ID
            outpost.setNpcId(traderId);

            return traderId;
        } catch (Exception e) {
            plugin.getLogger().severe("Error spawning trader NPC: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Handle a player interacting with a trader
     * 
     * @param player The player
     * @param entityId The entity ID
     * @return True if handled
     */
    public boolean handleTraderInteraction(Player player, UUID entityId) {
        TraderNPC trader = traders.get(entityId);
        if (trader == null) {
            return false;
        }

        return trader.openMenu(player);
    }

    /**
     * Get a trader by ID
     * 
     * @param id The trader ID
     * @return The trader, or null if not found
     */
    public TraderNPC getTrader(UUID id) {
        return traders.get(id);
    }

    /**
     * Get an outpost by ID
     * 
     * @param id The outpost ID
     * @return The outpost, or null if not found
     */
    public TraderOutpost getOutpost(UUID id) {
        return outposts.get(id);
    }

    /**
     * Get all trader outposts
     * 
     * @return All trader outposts
     */
    public List<TraderOutpost> getAllTraderOutposts() {
        return new ArrayList<>(outposts.values());
    }

    /**
     * Register a trader outpost from a structure
     * 
     * @param structure The structure
     * @return The outpost ID, or null if failed
     */
    public UUID registerTraderOutpost(StructureManager.Structure structure) {
        Location location = structure.getLocation();
        UUID outpostId = structure.getId();

        // Create outpost
        TraderOutpost outpost = new TraderOutpost(outpostId, location);
        outposts.put(outpostId, outpost);

        // Add to spatial partitioning maps
        World world = location.getWorld();
        if (world != null) {
            outpostsByWorld.computeIfAbsent(world, k -> new ArrayList<>()).add(outpost);
        }
        ChunkCoordinate chunkCoord = new ChunkCoordinate(location.getChunk());
        outpostsByChunk.computeIfAbsent(chunkCoord, k -> new ArrayList<>()).add(outpost);

        // Spawn trader NPC
        spawnTraderNPC(outpost);

        return outpostId;
    }

    /**
     * Get random trade stock
     * 
     * @param tier The tier
     * @param category The category
     * @return The trade stock (item, price)
     */
    public Map<org.bukkit.inventory.ItemStack, Integer> getRandomStock(int tier, String category) {
        // This is a placeholder - actual implementation would generate stock based on config
        Map<org.bukkit.inventory.ItemStack, Integer> stock = new HashMap<>();

        // Just add some random items
        org.bukkit.Material[] materials = {
            org.bukkit.Material.DIAMOND_SWORD,
            org.bukkit.Material.IRON_SWORD,
            org.bukkit.Material.IRON_PICKAXE,
            org.bukkit.Material.IRON_AXE,
            org.bukkit.Material.COOKED_BEEF,
            org.bukkit.Material.GOLDEN_APPLE
        };

        for (int i = 0; i < 3 + tier; i++) {
            int index = ThreadLocalRandom.current().nextInt(materials.length);
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(materials[index]);
            int price = (tier + 1) * 10 + ThreadLocalRandom.current().nextInt(50);

            stock.put(item, price);
        }

        return stock;
    }

    /**
     * Save traders to database
     */
    public void saveTraders() {
        // TODO: Implement database saving
        // This is a placeholder - actual implementation would save to database
    }

    /**
     * Clean up resources when the plugin is disabled
     */
    public void cleanup() {
        // Cancel scheduled tasks
        if (outpostTask != null) {
            outpostTask.cancel();
        }

        // Save data
        saveTraders();

        // Remove NPCs
        for (TraderNPC trader : traders.values()) {
            if (trader.getEntity() != null && trader.getEntity().isValid()) {
                trader.getEntity().remove();
            }
        }

        // Clear collections
        traders.clear();
        outposts.clear();

        outpostsByWorld.clear();
        outpostsByChunk.clear();
    }

    /**
     * Represents a trader outpost
     */
    public class TraderOutpost {

        private final UUID id;
        private final Location location;
        private UUID npcId;
        private boolean open;

        /**
         * Constructor for TraderOutpost
         * 
         * @param id The outpost ID
         * @param location The location
         */
        public TraderOutpost(UUID id, Location location) {
            this.id = id;
            this.location = location;
            this.open = false;
        }

        /**
         * Get the outpost ID
         * 
         * @return The ID
         */
        public UUID getId() {
            return id;
        }

        /**
         * Get the structure ID (alias for getId for compatibility)
         * 
         * @return The structure ID
         */
        public UUID getStructureId() {
            return id;
        }

        /**
         * Get the location
         * 
         * @return The location
         */
        public Location getLocation() {
            return location;
        }

        /**
         * Check if the outpost is open
         * 
         * @return True if open
         */
        public boolean isOpen() {
            return open;
        }

        /**
         * Set whether the outpost is open
         * 
         * @param open True if open
         */
        public void setOpen(boolean open) {
            this.open = open;
        }

        /**
         * Get the NPC ID
         * 
         * @return The NPC ID
         */
        public UUID getNpcId() {
            return npcId;
        }

        /**
         * Set the NPC ID
         * 
         * @param npcId The NPC ID
         */
        public void setNpcId(UUID npcId) {
            this.npcId = npcId;
        }
    }

    private static class ChunkCoordinate {
        private final int x;
        private final int z;

        public ChunkCoordinate(Chunk chunk) {
            this.x = chunk.getX();
            this.z = chunk.getZ();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ChunkCoordinate that = (ChunkCoordinate) o;

            if (x != that.x) return false;
            return z == that.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + z;
            return result;
        }
    }
}