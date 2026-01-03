package de.thecoolcraft11.commandBundle;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class ConditionEvaluator {


    public static boolean evaluate(CommandSender sender, String condition, VariableManager variableManager) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        if (sender instanceof Player player) {
            condition = condition.replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("%player_uuid%", player.getUniqueId().toString());
        }
        String[] parts = condition.split(":", 3);
        if (parts.length < 2) {
            return false;
        }
        String type = parts[0].toLowerCase();
        String value = parts[1];
        String param = parts.length > 2 ? parts[2] : null;

        if (type.equals("var")) {
            if (sender instanceof Player player) {
                value = value.replace("%player%", player.getName());
                if (param != null) {
                    param = param.replace("%player%", player.getName());
                }
            }
        }
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
            case "var" -> evaluateVariable(sender, value, param, variableManager);
            default -> false;
        };
    }

    private static boolean evaluateVariable(CommandSender sender, String varName, String expectedValue, VariableManager variableManager) {
        if (variableManager == null || expectedValue == null) {
            return false;
        }

        String jsonPath = null;
        int dot = varName.indexOf('.');
        if (dot != -1) {
            jsonPath = varName.substring(dot + 1);
            varName = varName.substring(0, dot);
        }

        String actualValue;
        if (sender instanceof Player player) {
            actualValue = variableManager.getPlayer(player.getUniqueId(), varName);
        } else {
            actualValue = variableManager.getGlobal(varName);
        }

        if (jsonPath != null && actualValue != null && !actualValue.isEmpty()) {
            actualValue = extractJsonPath(actualValue, jsonPath);
        }

        if (actualValue == null) {
            return false;
        }


        if (expectedValue.equals("!=")) {
            return !actualValue.isEmpty();
        }

        if (expectedValue.startsWith("!=")) {
            String compareTo = expectedValue.substring(2);
            return !actualValue.equalsIgnoreCase(compareTo);
        }


        return actualValue.equalsIgnoreCase(expectedValue);
    }

    private static String extractJsonPath(String json, String path) {
        String current = json.trim();
        for (String key : path.split("\\.")) {
            current = findJsonValue(current, key);
            if (current == null || current.isEmpty()) {
                return "";
            }
            current = stripQuotes(current.trim());
        }
        return current;
    }

    private static String findJsonValue(String json, String key) {
        int idx = 0;
        while (true) {
            idx = json.indexOf("\"" + key + "\"", idx);
            if (idx == -1) return null;
            int colon = json.indexOf(":", idx + key.length() + 2);
            if (colon == -1) return null;
            int pos = colon + 1;
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            if (pos >= json.length()) return null;
            char ch = json.charAt(pos);
            if (ch == '"') {
                int end = pos + 1;
                boolean escape = false;
                while (end < json.length()) {
                    char c = json.charAt(end);
                    if (c == '"' && !escape) break;
                    escape = c == '\\' && !escape;
                    end++;
                }
                if (end >= json.length()) return null;
                return json.substring(pos + 1, end);
            } else if (ch == '{') {
                int depth = 1, end = pos + 1;
                while (end < json.length() && depth > 0) {
                    char c = json.charAt(end);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    end++;
                }
                if (depth != 0) return null;
                return json.substring(pos, end);
            } else if (ch == '[') {
                int depth = 1, end = pos + 1;
                while (end < json.length() && depth > 0) {
                    char c = json.charAt(end);
                    if (c == '[') depth++;
                    else if (c == ']') depth--;
                    end++;
                }
                if (depth != 0) return null;
                return json.substring(pos, end);
            } else {
                int end = pos;
                while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
                return json.substring(pos, end).trim();
            }
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
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