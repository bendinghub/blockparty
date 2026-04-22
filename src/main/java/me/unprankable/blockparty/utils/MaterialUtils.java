package me.unprankable.blockparty.utils;

import org.bukkit.Material;

public class MaterialUtils {

    /**
     * Converts a friendly block name (e.g., "diamond block") to a Bukkit Material.
     *
     * @param friendlyName The user-friendly name of the material.
     * @return The corresponding Material, or null if not found.
     */
    public static Material fromFriendlyName(String friendlyName) {
        if (friendlyName == null || friendlyName.isEmpty()) {
            return null;
        }
        try {
            // Direct match
            return Material.valueOf(friendlyName.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Allow for some aliases or common names if needed, for now, just log it.
            return null;
        }
    }
}

