package com.seventodie.blocks;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Represents a frame block that can be upgraded
 */
public class FrameBlock {
    
    private final Location location;
    private final UUID ownerUuid;
    private int tier;
    private long lastUpgrade;
    
    /**
     * Constructor for a FrameBlock
     * 
     * @param location The location
     * @param tier The initial tier
     * @param ownerUuid The owner's UUID
     */
    public FrameBlock(Location location, int tier, UUID ownerUuid) {
        this.location = location;
        this.tier = tier;
        this.ownerUuid = ownerUuid;
        this.lastUpgrade = System.currentTimeMillis();
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
     * Get the tier
     * 
     * @return The tier
     */
    public int getTier() {
        return tier;
    }
    
    /**
     * Set the tier
     * 
     * @param tier The new tier
     */
    public void setTier(int tier) {
        this.tier = tier;
        this.lastUpgrade = System.currentTimeMillis();
    }
    
    /**
     * Get the owner's UUID
     * 
     * @return The owner's UUID
     */
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    /**
     * Get the last upgrade time
     * 
     * @return The last upgrade time (milliseconds)
     */
    public long getLastUpgrade() {
        return lastUpgrade;
    }
    
    /**
     * Check if this block is at the maximum tier
     * 
     * @return True if at max tier
     */
    public boolean isMaxTier() {
        return tier >= 3; // Max tier is 3 (concrete)
    }
    
    /**
     * Get the material name for this tier
     * 
     * @return The material name
     */
    public String getTierName() {
        switch (tier) {
            case 0:
                return "Wood";
            case 1:
                return "Stone";
            case 2:
                return "Metal";
            case 3:
                return "Concrete";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Get the tier HP value
     * 
     * @return The HP value
     */
    public int getTierHp() {
        switch (tier) {
            case 0:
                return 250;
            case 1:
                return 500;
            case 2:
                return 1000;
            case 3:
                return 2000;
            default:
                return 100;
        }
    }
}