package de.thecoolcraft11.commandBundle;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

 
public class VariableManager {
    private final Map<String, String> globalVariables = new HashMap<>();
    private final Map<UUID, Map<String, String>> playerVariables = new HashMap<>();


    public void setGlobal(String key, String value) {
        globalVariables.put(key.toLowerCase(), value);
    }


    public String getGlobal(String key) {
        return globalVariables.getOrDefault(key.toLowerCase(), "");
    }

    public void setPlayer(UUID playerId, String key, String value) {
        playerVariables.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(key.toLowerCase(), value);
    }

    public String getPlayer(UUID playerId, String key) {
        Map<String, String> vars = playerVariables.get(playerId);
        if (vars == null) {
            return "";
        }
        return vars.getOrDefault(key.toLowerCase(), "");
    }

    public void clearPlayer(UUID playerId) {
        playerVariables.remove(playerId);
    }

    public void clearAll() {
        globalVariables.clear();
        playerVariables.clear();
    }

    public void storeLocation(UUID playerId, String key, Location location) {
        String locationString = String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
        setPlayer(playerId, key, locationString);
    }


    public Location getLocation(UUID playerId, String key) {
        String locationString = getPlayer(playerId, key);
        if (locationString.isEmpty()) {
            return null;
        }

        try {
            String[] parts = locationString.split(",");
            if (parts.length < 4) {
                return null;
            }

            org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasGlobal(String key) {
        return globalVariables.containsKey(key.toLowerCase());
    }

    public boolean hasPlayer(UUID playerId, String key) {
        Map<String, String> vars = playerVariables.get(playerId);
        return vars != null && vars.containsKey(key.toLowerCase());
    }
}
