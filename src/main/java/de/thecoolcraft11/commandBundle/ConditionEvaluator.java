package de.thecoolcraft11.commandBundle;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class ConditionEvaluator {


    public static boolean evaluate(CommandSender sender, String condition) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        String[] parts = condition.split(":", 3);
        if (parts.length < 2) {
            return false;
        }

        String type = parts[0].toLowerCase();
        String value = parts[1];
        String param = parts.length > 2 ? parts[2] : null;

        return switch (type) {
            case "permission", "perm" -> sender.hasPermission(value);
            case "item" -> evaluateItem(sender, value, param);
            case "world" -> evaluateWorld(sender, value);
            case "gamemode", "gm" -> evaluateGamemode(sender, value);
            case "health" -> evaluateHealth(sender, value);
            case "level", "xp" -> evaluateLevel(sender, value);
            case "flying" -> evaluateFlying(sender, value);
            case "sneaking" -> evaluateSneaking(sender, value);
            case "op" -> sender.isOp() == Boolean.parseBoolean(value);
            case "player" -> evaluatePlayerName(sender, value);
            default -> false;
        };
    }

    private static boolean evaluateItem(CommandSender sender, String itemName, String amount) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        Material material;

        try {
            material = Material.valueOf(itemName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }

        int requiredAmount = 1;
        if (amount != null) {
            try {
                requiredAmount = Integer.parseInt(amount);
            } catch (NumberFormatException ignored) {
            }
        }

        int totalAmount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                totalAmount += item.getAmount();
            }
        }

        return totalAmount >= requiredAmount;
    }

    private static boolean evaluateWorld(CommandSender sender, String worldName) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return player.getWorld().getName().equalsIgnoreCase(worldName);
    }

    private static boolean evaluateGamemode(CommandSender sender, String gamemode) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return player.getGameMode().name().equalsIgnoreCase(gamemode);
    }

    private static boolean evaluateHealth(CommandSender sender, String condition) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return evaluateNumericCondition(player.getHealth(), condition);
    }

    private static boolean evaluateLevel(CommandSender sender, String condition) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return evaluateNumericCondition(player.getLevel(), condition);
    }

    private static boolean evaluateFlying(CommandSender sender, String value) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return player.isFlying() == Boolean.parseBoolean(value);
    }

    private static boolean evaluateSneaking(CommandSender sender, String value) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return player.isSneaking() == Boolean.parseBoolean(value);
    }

    private static boolean evaluatePlayerName(CommandSender sender, String name) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        return player.getName().equalsIgnoreCase(name);
    }

    private static boolean evaluateNumericCondition(double actual, String condition) {
        condition = condition.trim();

        if (condition.startsWith(">=")) {
            double value = parseDouble(condition.substring(2));
            return actual >= value;
        } else if (condition.startsWith("<=")) {
            double value = parseDouble(condition.substring(2));
            return actual <= value;
        } else if (condition.startsWith(">")) {
            double value = parseDouble(condition.substring(1));
            return actual > value;
        } else if (condition.startsWith("<")) {
            double value = parseDouble(condition.substring(1));
            return actual < value;
        } else if (condition.startsWith("==") || condition.startsWith("=")) {
            String numberPart = condition.startsWith("==") ? condition.substring(2) : condition.substring(1);
            double value = parseDouble(numberPart);
            return Math.abs(actual - value) < 0.01;
        } else {

            double value = parseDouble(condition);
            return Math.abs(actual - value) < 0.01;
        }
    }

    private static double parseDouble(String str) {
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
