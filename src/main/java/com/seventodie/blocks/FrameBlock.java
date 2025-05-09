package com.seventodie.blocks;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Represents a frame block that can be upgraded
 */
public class FrameBlock {
    /** Wood tier constant (tier 0) */
    public static final int TIER_WOOD = 0;
    
    /** Stone tier constant (tier 1) */
    public static final int TIER_STONE = 1;
    
    /** Metal/Rebar tier constant (tier 2) */
    public static final int TIER_REBAR = 2;
    
    /** Concrete tier constant (tier 3) */
    public static final int TIER_CONCRETE = 3;
    
    private final Location location;
    private final UUID ownerUuid;
    private int tier;
    private long lastUpgrade;
    private int durability;
    private int maxDurability;
    private int upgradeProgress;
    private int upgradeRequirement;
    
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
        return getTierName(tier);
    }
    
    /**
     * Get the material name for a specific tier (static)
     * 
     * @param tierLevel The tier level
     * @return The material name
     */
    public static String getTierName(int tierLevel) {
        switch (tierLevel) {
            case TIER_WOOD:
                return "Wood";
            case TIER_STONE:
                return "Stone";
            case TIER_REBAR:
                return "Metal";
            case TIER_CONCRETE:
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
    
    /**
     * Get the current durability of the block
     * 
     * @return The current durability
     */
    public int getDurability() {
        return durability;
    }
    
    /**
     * Set the current durability of the block
     * 
     * @param durability The new durability value
     */
    public void setDurability(int durability) {
        this.durability = durability;
    }
    
    /**
     * Get the maximum durability of the block
     * 
     * @return The maximum durability
     */
    public int getMaxDurability() {
        return maxDurability;
    }
    
    /**
     * Set the maximum durability of the block
     * 
     * @param maxDurability The new maximum durability value
     */
    public void setMaxDurability(int maxDurability) {
        this.maxDurability = maxDurability;
    }
    
    /**
     * Get the current upgrade progress
     * 
     * @return The current upgrade progress
     */
    public int getUpgradeProgress() {
        return upgradeProgress;
    }
    
    /**
     * Set the current upgrade progress
     * 
     * @param upgradeProgress The new upgrade progress value
     */
    public void setUpgradeProgress(int upgradeProgress) {
        this.upgradeProgress = upgradeProgress;
    }
    
    /**
     * Get the upgrade requirement (materials needed)
     * 
     * @return The upgrade requirement
     */
    public int getUpgradeRequirement() {
        return upgradeRequirement;
    }
    
    /**
     * Set the upgrade requirement
     * 
     * @param upgradeRequirement The new upgrade requirement value
     */
    public void setUpgradeRequirement(int upgradeRequirement) {
        this.upgradeRequirement = upgradeRequirement;
    }
    
    /**
     * Get the block type (for display in UI)
     * 
     * @return The block type
     */
    public String getType() {
        return "Frame";
    }
}