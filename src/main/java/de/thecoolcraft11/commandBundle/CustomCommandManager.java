package de.thecoolcraft11.commandBundle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;


public class CustomCommandManager {
    private final CommandBundle plugin;
    private final File commandsFile;
    private final File configFile;
    private FileConfiguration commandsConfig;
    private final Map<String, List<String>> customCommands = new HashMap<>();
    private final Map<String, Map<String, List<String>>> subCommands = new HashMap<>();
    private final Map<String, String> commandPermissions = new HashMap<>();
    private final Map<String, CustomCommand> registeredCommands = new HashMap<>();
    private final VariableManager variableManager;
    private final Set<String> blacklistedCommands = new HashSet<>();

    public CustomCommandManager(CommandBundle plugin) {
        this.plugin = plugin;
        this.commandsFile = new File(plugin.getDataFolder(), "commands.yml");
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.variableManager = new VariableManager();
        loadConfig();
        loadCommands();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);


        blacklistedCommands.clear();
        blacklistedCommands.addAll(config.getStringList("blacklisted-commands"));
    }

    public void loadCommands() {
        if (!commandsFile.exists()) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try {
                commandsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create commands.yml: " + e.getMessage());
            }
        }

        commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
        customCommands.clear();
        subCommands.clear();
        commandPermissions.clear();


        if (commandsConfig.contains("commands")) {
            ConfigurationSection commandsSection = commandsConfig.getConfigurationSection("commands");
            if (commandsSection != null) {
                for (String cmdName : commandsSection.getKeys(false)) {
                    List<String> actions = commandsConfig.getStringList("commands." + cmdName + ".actions");
                    customCommands.put(cmdName.toLowerCase(), actions);


                    String permission = commandsConfig.getString("commands." + cmdName + ".permission");
                    if (permission != null && !permission.isEmpty()) {
                        commandPermissions.put(cmdName.toLowerCase(), permission);
                    }


                    if (commandsConfig.contains("commands." + cmdName + ".subcommands")) {
                        ConfigurationSection subCmdSection = commandsConfig.getConfigurationSection("commands." + cmdName + ".subcommands");
                        if (subCmdSection != null) {
                            Map<String, List<String>> subs = new HashMap<>();
                            for (String subName : subCmdSection.getKeys(false)) {
                                List<String> subActions = commandsConfig.getStringList("commands." + cmdName + ".subcommands." + subName);
                                subs.put(subName.toLowerCase(), subActions);
                            }
                            subCommands.put(cmdName.toLowerCase(), subs);
                        }
                    }
                }
            }
        }


        registerAllCommands();
    }

    public void saveCommands() {
        commandsConfig.set("commands", null);

        for (Map.Entry<String, List<String>> entry : customCommands.entrySet()) {
            commandsConfig.set("commands." + entry.getKey() + ".actions", entry.getValue());


            String permission = commandPermissions.get(entry.getKey());
            if (permission != null) {
                commandsConfig.set("commands." + entry.getKey() + ".permission", permission);
            }


            Map<String, List<String>> subs = subCommands.get(entry.getKey());
            if (subs != null && !subs.isEmpty()) {
                for (Map.Entry<String, List<String>> sub : subs.entrySet()) {
                    commandsConfig.set("commands." + entry.getKey() + ".subcommands." + sub.getKey(), sub.getValue());
                }
            }
        }

        try {
            commandsConfig.save(commandsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save commands.yml: " + e.getMessage());
        }
    }

    public boolean addCommand(String commandName, List<String> actions) {
        commandName = commandName.toLowerCase();

        if (customCommands.containsKey(commandName)) {
            return false;
        }

        customCommands.put(commandName, new ArrayList<>(actions));
        saveCommands();
        registerCommand(commandName);
        return true;
    }

    public boolean removeCommand(String commandName) {
        commandName = commandName.toLowerCase();

        if (!customCommands.containsKey(commandName)) {
            return false;
        }

        customCommands.remove(commandName);
        subCommands.remove(commandName);
        commandPermissions.remove(commandName);
        unregisterCommand(commandName);
        saveCommands();
        return true;
    }

    public boolean editCommand(String commandName, String operation, int index, String action) {
        commandName = commandName.toLowerCase();
        List<String> actions = customCommands.get(commandName);

        if (actions == null) {
            return false;
        }

        switch (operation.toLowerCase()) {
            case "add":
                actions.add(action);
                break;
            case "insert":
                if (index < 0 || index > actions.size()) {
                    return false;
                }
                actions.add(index, action);
                break;
            case "remove":
                if (index < 0 || index >= actions.size()) {
                    return false;
                }
                actions.remove(index);
                break;
            case "replace":
                if (index < 0 || index >= actions.size()) {
                    return false;
                }
                actions.set(index, action);
                break;
            default:
                return false;
        }

        saveCommands();
        return true;
    }

    public boolean addSubCommand(String commandName, String subCommandName, List<String> actions) {
        commandName = commandName.toLowerCase();
        subCommandName = subCommandName.toLowerCase();

        if (!customCommands.containsKey(commandName)) {
            return false;
        }

        subCommands.computeIfAbsent(commandName, k -> new HashMap<>())
                .put(subCommandName, new ArrayList<>(actions));
        saveCommands();
        return true;
    }

    public boolean setCommandPermission(String commandName, String permission) {
        commandName = commandName.toLowerCase();

        if (!customCommands.containsKey(commandName)) {
            return false;
        }

        if (permission == null || permission.isEmpty()) {
            commandPermissions.remove(commandName);
        } else {
            commandPermissions.put(commandName, permission);
        }

        saveCommands();
        return true;
    }

    public String getCommandPermission(String commandName) {
        return commandPermissions.get(commandName.toLowerCase());
    }

    public List<String> getCommandActions(String commandName) {
        return customCommands.get(commandName.toLowerCase());
    }

    public Map<String, List<String>> getSubCommands(String commandName) {
        return subCommands.get(commandName.toLowerCase());
    }

    public Set<String> getCommandNames() {
        return new HashSet<>(customCommands.keySet());
    }

    public VariableManager getVariableManager() {
        return variableManager;
    }

    private void registerAllCommands() {
        for (String cmdName : customCommands.keySet()) {
            registerCommand(cmdName);
        }
    }

    private void registerCommand(String commandName) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            CustomCommand customCommand = new CustomCommand(commandName, this);
            commandMap.register(commandName, "commandbundle", customCommand);
            registeredCommands.put(commandName, customCommand);

            Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command: " + commandName);
            plugin.getLogger().severe(e.getMessage());
        }
    }

    private void unregisterCommand(String commandName) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            if (commandMap instanceof SimpleCommandMap) {
                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

                knownCommands.remove(commandName);
                knownCommands.remove("commandbundle:" + commandName);
            }

            registeredCommands.remove(commandName);
            Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to unregister command: " + commandName);
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public void executeCustomCommand(CommandSender sender, String commandName, String[] args) {
        commandName = commandName.toLowerCase();


        String permission = commandPermissions.get(commandName);
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return;
        }


        List<String> actions;
        if (args.length > 0) {
            Map<String, List<String>> subs = subCommands.get(commandName);
            if (subs != null && subs.containsKey(args[0].toLowerCase())) {
                actions = subs.get(args[0].toLowerCase());
            } else {
                actions = customCommands.get(commandName);
            }
        } else {
            actions = customCommands.get(commandName);
        }

        if (actions == null || actions.isEmpty()) {
            return;
        }

        executeActions(sender, actions, args);
    }

    private void executeActions(CommandSender sender, List<String> actions, String[] args) {
        int currentDelay = 0;
        List<CommandAction> parsedActions = actions.stream()
                .map(CommandAction::new)
                .collect(Collectors.toList());


        List<CommandAction> randomActions = parsedActions.stream()
                .filter(CommandAction::isRandom)
                .collect(Collectors.toList());

        if (!randomActions.isEmpty()) {
            parsedActions.removeAll(randomActions);
            CommandAction selected = selectRandomAction(randomActions);
            if (selected != null) {
                parsedActions.add(selected);
            }
        }

        for (CommandAction action : parsedActions) {
            int delay = currentDelay + action.getDelay();

            if (delay > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        executeSingleAction(sender, action, args);
                    }
                }.runTaskLater(plugin, delay * 20L);
                currentDelay = delay;
            } else {
                executeSingleAction(sender, action, args);
            }
        }
    }

    private void executeSingleAction(CommandSender sender, CommandAction action, String[] args) {

        if (action.hasCondition()) {
            if (!ConditionEvaluator.evaluate(sender, action.getCondition())) {
                return;
            }
        }

        String processedAction = action.getProcessedAction();


        processedAction = replacePlaceholders(sender, processedAction, args);


        String baseCommand = processedAction.split(" ")[0].toLowerCase();
        if (blacklistedCommands.contains(baseCommand)) {
            plugin.getLogger().warning("Attempted to execute blacklisted command: " + baseCommand);
            return;
        }


        if (action.isConsoleCommand()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedAction);
        } else {
            Bukkit.dispatchCommand(sender, processedAction);
        }
    }

    private String replacePlaceholders(CommandSender sender, String text, String[] args) {

        if (sender instanceof Player player) {
            text = text.replace("%player%", player.getName());
            text = text.replace("%uuid%", player.getUniqueId().toString());
            text = text.replace("%world%", player.getWorld().getName());
            text = text.replace("%x%", String.valueOf(player.getLocation().getBlockX()));
            text = text.replace("%y%", String.valueOf(player.getLocation().getBlockY()));
            text = text.replace("%z%", String.valueOf(player.getLocation().getBlockZ()));
            text = text.replace("%health%", String.valueOf(player.getHealth()));
            text = text.replace("%level%", String.valueOf(player.getLevel()));
            text = text.replace("%gamemode%", player.getGameMode().name());


            text = replaceVariables(text, player.getUniqueId());
        }


        text = replaceArguments(text, args);

        return text;
    }

    private String replaceArguments(String text, String[] args) {

        if (args.length > 0) {
            text = text.replace("%args%", String.join(" ", args));
        } else {
            text = text.replace("%args%", "");
        }

        for (int i = 1; i <= args.length; i++) {
            String pattern = "%arg" + i + "-%";
            if (text.contains(pattern)) {
                String[] remaining = Arrays.copyOfRange(args, i - 1, args.length);
                text = text.replace(pattern, String.join(" ", remaining));
            }
        }


        for (int i = 0; i < args.length; i++) {
            text = text.replace("%arg" + (i + 1) + "%", args[i]);
        }


        while (text.contains("%arg")) {
            int start = text.indexOf("%arg");
            int end = text.indexOf("%", start + 1);
            if (end == -1) break;

            String placeholder = text.substring(start, end + 1);
            String[] parts = placeholder.substring(1, placeholder.length() - 1).split("\\|");

            if (parts.length >= 2) {
                String argPart = parts[0];
                String defaultValue = parts[1];

                try {
                    int argIndex = Integer.parseInt(argPart.substring(3)) - 1;
                    String value = (argIndex >= 0 && argIndex < args.length) ? args[argIndex] : defaultValue;
                    text = text.replace(placeholder, value);
                } catch (NumberFormatException e) {
                    text = text.replace(placeholder, defaultValue);
                }
            } else {
                break;
            }
        }

        return text;
    }

    private String replaceVariables(String text, UUID playerId) {

        while (text.contains("%var:")) {
            int start = text.indexOf("%var:");
            int end = text.indexOf("%", start + 1);
            if (end == -1) break;

            String varName = text.substring(start + 5, end);
            String value = variableManager.getPlayer(playerId, varName);

            if (value.isEmpty()) {
                value = variableManager.getGlobal(varName);
            }

            text = text.replace("%var:" + varName + "%", value);
        }

        return text;
    }

    private CommandAction selectRandomAction(List<CommandAction> actions) {
        if (actions.isEmpty()) {
            return null;
        }

        int totalWeight = actions.stream().mapToInt(CommandAction::getRandomWeight).sum();
        int random = new Random().nextInt(totalWeight);
        int currentWeight = 0;

        for (CommandAction action : actions) {
            currentWeight += action.getRandomWeight();
            if (random < currentWeight) {
                return action;
            }
        }

        return actions.getFirst();
    }


    private static class CustomCommand extends Command {
        private final CustomCommandManager manager;

        protected CustomCommand(String name, CustomCommandManager manager) {
            super(name);
            this.manager = manager;
            this.setDescription("Custom command bundle");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String @NotNull [] args) {
            manager.executeCustomCommand(sender, commandLabel, args);
            return true;
        }
    }
}

