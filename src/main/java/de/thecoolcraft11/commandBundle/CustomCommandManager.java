package de.thecoolcraft11.commandBundle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private boolean hostCommandsEnabled = false;
    private boolean webhooksEnabled = false;

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

        hostCommandsEnabled = config.getBoolean("host-commands-enabled", false);
        webhooksEnabled = config.getBoolean("webhooks-enabled", false);
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

        List<String> commandNames = new ArrayList<>(customCommands.keySet());
        for (String cmdName : commandNames) {
            registerCommand(cmdName, false);
        }

        updatePlayerCommands();
    }

    private void registerCommand(String commandName) {
        registerCommand(commandName, true);
    }

    private void registerCommand(String commandName, boolean updatePlayers) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            CustomCommand customCommand = new CustomCommand(commandName, this);
            commandMap.register(commandName, "commandbundle", customCommand);
            registeredCommands.put(commandName, customCommand);

            if (updatePlayers) {
                updatePlayerCommands();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command: " + commandName);
            plugin.getLogger().severe(e.getMessage());
        }
    }

    private void updatePlayerCommands() {

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            for (Player player : players) {
                player.updateCommands();
            }
        });
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
            updatePlayerCommands();
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


        if (action.hasElseIf()) {
            if (!ConditionEvaluator.evaluate(sender, action.getElseIfCondition(), variableManager)) {
                return;
            }
        } else if (action.hasCondition()) {
            if (!ConditionEvaluator.evaluate(sender, action.getCondition(), variableManager)) {
                return;
            }
        }


        if (action.isMessage()) {
            String colors = action.getMessageColor();
            String text = action.getMessageText();
            String targetPlayer = action.getMessagePlayer();

            if (text != null) {
                text = replacePlaceholders(sender, text, args);


                CommandSender messageSender = sender;
                if (targetPlayer != null && !targetPlayer.isEmpty()) {

                    String resolvedPlayerName = replacePlaceholders(sender, targetPlayer, args);
                    Player targetPlayerObj = Bukkit.getPlayer(resolvedPlayerName);
                    if (targetPlayerObj != null) {
                        messageSender = targetPlayerObj;
                    } else {
                        plugin.getLogger().warning("Player not found: " + resolvedPlayerName);
                        return;
                    }
                }

                sendColoredMessage(messageSender, colors, text);
            }
            return;
        }

        if (action.isLoop()) {
            executeLoopAction(sender, action, args);
            return;
        }


        if (action.isSetVariable()) {
            String varName = action.getSetVariableName();
            String varValue = action.getSetVariableValue();

            if (varName != null && varValue != null) {

                varName = replacePlaceholders(sender, varName, args);
                varValue = replacePlaceholders(sender, varValue, args);


                if (sender instanceof Player player) {
                    variableManager.setPlayer(player.getUniqueId(), varName, varValue);
                } else {
                    variableManager.setGlobal(varName, varValue);
                }


                if (!action.isSuppressVariableOutput()) {
                    sender.sendMessage(Component.text("Variable set: ", NamedTextColor.GREEN)
                            .append(Component.text(varName, NamedTextColor.YELLOW))
                            .append(Component.text(" = ", NamedTextColor.GRAY))
                            .append(Component.text(varValue, NamedTextColor.WHITE)));
                }
            }
            return;
        }

        String processedAction = action.getProcessedAction();

        processedAction = replacePlaceholders(sender, processedAction, args);

        if (action.isHostCommand()) {
            String storeVar = action.getHostStoreVariable();
            if (storeVar != null && !storeVar.isEmpty()) {
                storeVar = replacePlaceholders(sender, storeVar, args);
            }
            executeHostCommand(sender, processedAction, storeVar, action.isSuppressCommandOutput());
        } else if (action.isWebhook()) {
            WebhookData webhookData = action.getWebhookData();
            if (webhookData != null) {

                String url = replacePlaceholders(sender, webhookData.getUrl(), args);
                String body = replacePlaceholders(sender, webhookData.getBody(), args);


                Map<String, String> processedHeaders = new HashMap<>();
                for (Map.Entry<String, String> entry : webhookData.getHeaders().entrySet()) {
                    String headerValue = replacePlaceholders(sender, entry.getValue(), args);
                    processedHeaders.put(entry.getKey(), headerValue);
                }


                String processedVarName = null;
                if (webhookData.shouldStoreResponse()) {
                    processedVarName = replacePlaceholders(sender, webhookData.getStoreVariable(), args);
                }

                executeWebhook(sender, webhookData, url, body, processedHeaders, processedVarName);
            }
        } else {


            if (processedAction.trim().isEmpty()) {
                plugin.getLogger().warning("Attempted to execute empty command - likely from failed @() substitution");
                sender.sendMessage(Component.text("Command execution failed - no output from command substitution", NamedTextColor.RED));
                return;
            }

            String baseCommand = processedAction.split(" ")[0].toLowerCase();
            if (blacklistedCommands.contains(baseCommand)) {
                plugin.getLogger().warning("Attempted to execute blacklisted command: " + baseCommand);
                return;
            }


            if (action.isSuppressCommandOutput()) {
                executeCommandSilently(sender, processedAction, action.isConsoleCommand());
            } else {
                if (action.isConsoleCommand()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedAction);
                } else {
                    Bukkit.dispatchCommand(sender, processedAction);
                }
            }
        }
    }

    /**
     * Execute a command silently by suppressing console output
     * Temporarily redirects System.out and System.err
     */
    private void executeCommandSilently(CommandSender sender, String command, boolean isConsoleCommand) {
        java.io.PrintStream oldOut = System.out;
        java.io.PrintStream oldErr = System.err;

        try {

            java.io.PrintStream nullStream = new java.io.PrintStream(new java.io.OutputStream() {
                @Override
                public void write(int b) {
                }

                @Override
                public void write(byte @NonNull [] b) {
                }

                @Override
                public void write(byte @NonNull [] b, int off, int len) {
                }
            });


            System.setOut(nullStream);
            System.setErr(nullStream);


            if (isConsoleCommand) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } else {
                Bukkit.dispatchCommand(sender, command);
            }
        } finally {

            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }

    /**
     * Execute an action in a loop over a list
     * Supports: [foreach:list:variable]action
     * Example: [foreach:Player1,Player2,Player3:player]say Hello %player%
     */
    private void executeLoopAction(CommandSender sender, CommandAction action, String[] args) {
        String listOutput = action.getLoopList();
        String variable = action.getLoopVariable();
        String actionTemplate = action.getProcessedAction();

        if (listOutput == null || variable == null) {
            plugin.getLogger().warning("Invalid loop specification");
            return;
        }


        listOutput = replacePlaceholders(sender, listOutput, args);

        plugin.getLogger().fine("Loop list after placeholder replacement: '" + listOutput + "'");


        String[] items;
        if (listOutput.startsWith("@(")) {

            items = listOutput.split("[,\\n]");
        } else {

            items = listOutput.split(",");
        }

        plugin.getLogger().fine("Loop will execute " + items.length + " times");


        for (String item : items) {
            item = item.trim();
            if (item.isEmpty()) continue;

            plugin.getLogger().fine("Processing loop item: '" + item + "'");


            String processedAction = actionTemplate.replace("%" + variable + "%", item);


            processedAction = replacePlaceholders(sender, processedAction, args);


            executeProcessedAction(sender, processedAction);
        }
    }

    /**
     * Execute a processed action string
     */
    private void executeProcessedAction(CommandSender sender, String processedAction) {

        if (processedAction == null || processedAction.trim().isEmpty()) {
            plugin.getLogger().warning("Attempted to execute empty command in loop");
            return;
        }

        String baseCommand = processedAction.split(" ")[0].toLowerCase();
        if (blacklistedCommands.contains(baseCommand)) {
            plugin.getLogger().warning("Attempted to execute blacklisted command: " + baseCommand);
            return;
        }

        Bukkit.dispatchCommand(sender, processedAction);
    }

    private void executeHostCommand(CommandSender sender, String command, String storeVariable, boolean suppressOutput) {
        if (!hostCommandsEnabled) {
            plugin.getLogger().warning("Host command execution is disabled. Command: " + command);
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            String result = output.toString().trim();
            if (exitCode == 0) {
                if (storeVariable != null && !storeVariable.isEmpty()) {
                    if (sender instanceof Player p) {
                        variableManager.setPlayer(p.getUniqueId(), storeVariable, result);
                    } else {
                        variableManager.setGlobal(storeVariable, result);
                    }
                    if (!suppressOutput) {
                        sender.sendMessage(Component.text("Host output stored in variable: ", NamedTextColor.GREEN)
                                .append(Component.text(storeVariable, NamedTextColor.YELLOW)));
                    }
                } else {
                    if (!suppressOutput) {
                        if (!result.isEmpty()) {
                            for (String line : result.split("\n")) {
                                sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
                            }
                        } else {
                            sender.sendMessage(Component.text("Host command executed successfully (no output).", NamedTextColor.GREEN));
                        }
                    }
                }
            } else {
                if (!suppressOutput) {
                    sender.sendMessage(Component.text("Host command failed (exit code: " + exitCode + ")", NamedTextColor.RED));
                    if (!result.isEmpty()) {
                        sender.sendMessage(Component.text(result, NamedTextColor.DARK_RED));
                    }
                }
            }
        } catch (Exception e) {
            if (!suppressOutput) {
                sender.sendMessage(Component.text("Failed to execute host command: " + e.getMessage(), NamedTextColor.RED));
            }
        }
    }

    private void executeWebhook(CommandSender sender, WebhookData webhookData, String webhookUrl, String body, Map<String, String> headers, String processedVarName) {
        if (!webhooksEnabled) {
            plugin.getLogger().warning("Webhook execution is disabled. URL: " + webhookUrl);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = getHttpURLConnection();

                    int responseCode = connection.getResponseCode();


                    final String responseBody = getString(responseCode, connection);

                    if (responseCode >= 200 && responseCode < 300) {
                        plugin.getLogger().info("Webhook executed successfully. URL: " + webhookUrl);


                        if (processedVarName != null && !processedVarName.isEmpty() && sender instanceof Player player) {
                            variableManager.setPlayer(player.getUniqueId(), processedVarName, responseBody);

                            if (webhookData.isNotSilent() && !webhookData.isDynamicStoreName()) {
                                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text("Webhook response stored in variable: ", NamedTextColor.GREEN)
                                        .append(Component.text(processedVarName, NamedTextColor.YELLOW))));
                            }
                        } else if (!responseBody.isEmpty() && webhookData.isNotSilent()) {

                            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text("Webhook response: ", NamedTextColor.GREEN)
                                    .append(Component.text(responseBody, NamedTextColor.GRAY))));
                        }
                    } else {
                        plugin.getLogger().warning("Webhook returned status code " + responseCode + ". URL: " + webhookUrl);
                        if (webhookData.isNotSilent()) {
                            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text("Webhook failed (" + responseCode + "): " + responseBody, NamedTextColor.RED)));
                        }
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to execute webhook: " + webhookUrl);
                    plugin.getLogger().severe("Error: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(Component.text("Webhook error: " + e.getMessage(), NamedTextColor.RED)));
                }
            }

            private static @NonNull String getString(int responseCode, HttpURLConnection connection) {
                StringBuilder response = new StringBuilder();
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                                responseCode >= 200 && responseCode < 300
                                        ? connection.getInputStream()
                                        : connection.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                } catch (Exception ignored) {

                }

                return response.toString();
            }

            private @NonNull HttpURLConnection getHttpURLConnection() throws URISyntaxException, IOException {
                URL url = new java.net.URI(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);


                if (!headers.containsKey("Content-Type")) {
                    connection.setRequestProperty("Content-Type", "application/json");
                }


                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }


                if (!body.isEmpty()) {
                    byte[] outputBytes = body.getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(outputBytes.length);
                    try (var os = connection.getOutputStream()) {
                        os.write(outputBytes);
                    }
                }
                return connection;
            }
        }.runTaskAsynchronously(plugin);
    }

    private String replacePlaceholders(CommandSender sender, String text, String[] args) {

        text = processEscapeSequences(text);


        text = replaceArguments(text, args);

        if (sender instanceof Player player) {
            text = text.replace("%player%", player.getName());
            text = text.replace("%uuid%", player.getUniqueId().toString());
            text = text.replace("%player_uuid%", player.getUniqueId().toString());
            text = text.replace("%world%", player.getWorld().getName());
            text = text.replace("%x%", String.valueOf(player.getLocation().getBlockX()));
            text = text.replace("%y%", String.valueOf(player.getLocation().getBlockY()));
            text = text.replace("%z%", String.valueOf(player.getLocation().getBlockZ()));
            text = text.replace("%health%", String.valueOf(player.getHealth()));
            text = text.replace("%level%", String.valueOf(player.getLevel()));
            text = text.replace("%gamemode%", player.getGameMode().name());

            text = replaceVariables(text, player.getUniqueId());
        } else {

            text = replaceVariablesForConsole(text);
        }


        text = replaceServerConstants(text);


        text = replaceFileRead(text);


        text = handleFileWrite(text);


        text = replaceCommandSubstitution(text);


        text = replaceMathExpressions(text);
        text = restoreEscapedCharacters(text);

        return text;
    }

    /**
     * Replace variables when command is executed from console/server
     * Uses global variables only
     */
    private String replaceVariablesForConsole(String text) {
        while (text.contains("%var:")) {
            int start = text.indexOf("%var:");
            int end = text.indexOf("%", start + 1);
            if (end == -1) break;

            String varSpec = text.substring(start + 5, end);
            String value = "";


            if (varSpec.contains(".")) {
                String[] parts = varSpec.split("\\.", 2);
                String varName = parts[0];
                String jsonPath = parts[1];

                String jsonData = variableManager.getGlobal(varName);
                if (!jsonData.isEmpty()) {
                    value = extractJsonValue(jsonData, jsonPath);
                }
            } else {

                value = variableManager.getGlobal(varSpec);
            }

            text = text.replace("%var:" + varSpec + "%", value);
        }

        return text;
    }

    private String replaceServerConstants(String text) {

        if (text.contains("%players%")) {
            StringBuilder playerList = new StringBuilder();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!playerList.isEmpty()) playerList.append(",");
                playerList.append(p.getName());
            }
            text = text.replace("%players%", playerList.toString());
        }


        if (text.contains("%players_uuid%")) {
            StringBuilder uuidList = new StringBuilder();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!uuidList.isEmpty()) uuidList.append(",");
                uuidList.append(p.getUniqueId());
            }
            text = text.replace("%players_uuid%", uuidList.toString());
        }


        if (text.contains("%playercount%")) {
            text = text.replace("%playercount%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }


        if (text.contains("%teams%")) {
            StringBuilder teamList = new StringBuilder();
            org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
                if (!teamList.isEmpty()) teamList.append(",");
                teamList.append(team.getName());
            }
            text = text.replace("%teams%", teamList.toString());
        }


        while (text.contains("%teamplayers:")) {
            int start = text.indexOf("%teamplayers:");
            int end = text.indexOf("%", start + 1);
            if (end == -1) break;

            String teamName = text.substring(start + 13, end);
            String teamPlayers = getTeamPlayers(teamName, false);
            text = text.substring(0, start) + teamPlayers + text.substring(end + 1);
        }


        while (text.contains("%teamplayers_uuid:")) {
            int start = text.indexOf("%teamplayers_uuid:");
            int end = text.indexOf("%", start + 1);
            if (end == -1) break;

            String teamName = text.substring(start + 18, end);
            String teamPlayers = getTeamPlayers(teamName, true);
            text = text.substring(0, start) + teamPlayers + text.substring(end + 1);
        }


        text = resolveNestedTeamPlayers(text, false);
        text = resolveNestedTeamPlayers(text, true);

        return text;
    }

    private String resolveNestedTeamPlayers(String text, boolean uuid) {
        String prefix = uuid ? "%teamplayers_uuid:" : "%teamplayers:";
        while (true) {
            int idx = text.indexOf(prefix + "%");
            if (idx == -1) break;

            int innerStart = idx + prefix.length();
            if (innerStart >= text.length() || text.charAt(innerStart) != '%') break;
            int innerEnd = text.indexOf("%", innerStart + 1);
            if (innerEnd == -1) break;


            String resolvedInner;

            resolvedInner = text.substring(innerStart, innerEnd + 1);

            if (resolvedInner.startsWith("%") && resolvedInner.endsWith("%")) {
                resolvedInner = resolvedInner.substring(1, resolvedInner.length() - 1);
            }
            String playersList = getTeamPlayers(resolvedInner, uuid);

            int patternEnd = innerEnd + 1;

            int replaceEnd = patternEnd + 1 <= text.length() ? patternEnd + 1 : patternEnd;
            text = text.substring(0, idx) + playersList + text.substring(replaceEnd);
        }
        return text;
    }

    private String getTeamPlayers(String teamName, boolean returnUuids) {
        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);

            if (team == null) {
                return "";
            }

            StringBuilder playerList = new StringBuilder();
            for (String entry : team.getEntries()) {
                if (!playerList.isEmpty()) playerList.append(",");

                if (returnUuids) {

                    Player player = Bukkit.getPlayer(entry);
                    if (player != null) {
                        playerList.append(player.getUniqueId());
                    } else {
                        playerList.append(entry);
                    }
                } else {
                    playerList.append(entry);
                }
            }

            return playerList.toString();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get team players for " + teamName + ": " + e.getMessage());
            return "";
        }
    }

    private String replaceArguments(String text, String[] args) {

        if (args.length > 0) {
            text = text.replace("%args%", String.join(" ", args));
        } else {
            text = text.replace("%args%", "");
        }


        for (int i = 1; i <= Math.max(args.length + 5, 10); i++) {

            String patternWithDefault = "%arg" + i + "-::";
            int defaultStart = text.indexOf(patternWithDefault);
            if (defaultStart != -1) {
                int defaultEnd = text.indexOf("%", defaultStart + patternWithDefault.length());
                if (defaultEnd != -1) {
                    String fullPlaceholder = text.substring(defaultStart, defaultEnd + 1);
                    String defaultValue = text.substring(defaultStart + patternWithDefault.length(), defaultEnd);

                    if (i <= args.length) {
                        String[] remaining = Arrays.copyOfRange(args, i - 1, args.length);
                        text = text.replace(fullPlaceholder, String.join(" ", remaining));
                    } else {
                        text = text.replace(fullPlaceholder, defaultValue);
                    }
                }
            }


            String pattern = "%arg" + i + "-%";
            if (text.contains(pattern)) {
                if (i <= args.length) {
                    String[] remaining = Arrays.copyOfRange(args, i - 1, args.length);
                    text = text.replace(pattern, String.join(" ", remaining));
                } else {
                    text = text.replace(pattern, "");
                }
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
            String[] parts = placeholder.substring(1, placeholder.length() - 1).split("::");

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

            String varSpec = text.substring(start + 5, end);
            String value = "";


            if (varSpec.contains(".")) {
                String[] parts = varSpec.split("\\.", 2);
                String varName = parts[0];
                String jsonPath = parts[1];

                String jsonData = variableManager.getPlayer(playerId, varName);
                if (jsonData.isEmpty()) {
                    jsonData = variableManager.getGlobal(varName);
                }

                if (!jsonData.isEmpty()) {
                    value = extractJsonValue(jsonData, jsonPath);
                }
            } else {

                value = variableManager.getPlayer(playerId, varSpec);
                if (value.isEmpty()) {
                    value = variableManager.getGlobal(varSpec);
                }
            }

            text = text.replace("%var:" + varSpec + "%", value);
        }

        text = applyVariableFallback(text, playerId);

        return text;
    }

    private String applyVariableFallback(String text, UUID playerId) {

        Set<String> reservedPrefixes = Set.of("player", "uuid", "players", "players_uuid", "playercount", "teams", "teamplayers", "teamplayers_uuid", "arg", "var", "count");
        int idx = 0;
        while ((idx = text.indexOf('%', idx)) != -1) {
            int end = text.indexOf('%', idx + 1);
            if (end == -1) break;
            String token = text.substring(idx + 1, end);
            if (token.contains(":") || token.contains("(") || token.contains("-::") || token.endsWith("-%")) {
                idx = end + 1;
                continue;
            }
            String lower = token.toLowerCase();
            boolean reserved = reservedPrefixes.stream().anyMatch(lower::startsWith);
            if (!reserved) {
                String value = variableManager.getPlayer(playerId, token);
                if (value.isEmpty()) value = variableManager.getGlobal(token);
                if (!value.isEmpty()) {
                    text = text.substring(0, idx) + value + text.substring(end + 1);
                    idx += value.length();
                    continue;
                }
            }
            idx = end + 1;
        }
        return text;
    }


    private String extractJsonValue(String json, String path) {
        try {
            String[] pathParts = path.split("\\.");
            String current = json.trim();

            for (String key : pathParts) {

                String searchKey = "\"" + key + "\"";
                int keyIndex = current.indexOf(searchKey);
                if (keyIndex == -1) {
                    return "";
                }


                int colonIndex = current.indexOf(":", keyIndex);
                if (colonIndex == -1) {
                    return "";
                }


                int valueStart = colonIndex + 1;
                while (valueStart < current.length() && Character.isWhitespace(current.charAt(valueStart))) {
                    valueStart++;
                }

                char firstChar = current.charAt(valueStart);


                if (firstChar == '"') {

                    int stringEnd = current.indexOf('"', valueStart + 1);
                    if (stringEnd == -1) {
                        return "";
                    }
                    current = current.substring(valueStart + 1, stringEnd);
                } else if (firstChar == '{') {

                    int braceCount = 1;
                    int objEnd = valueStart + 1;
                    while (objEnd < current.length() && braceCount > 0) {
                        if (current.charAt(objEnd) == '{') braceCount++;
                        else if (current.charAt(objEnd) == '}') braceCount--;
                        objEnd++;
                    }
                    current = current.substring(valueStart, objEnd);
                } else if (firstChar == '[') {

                    int bracketCount = 1;
                    int arrEnd = valueStart + 1;
                    while (arrEnd < current.length() && bracketCount > 0) {
                        if (current.charAt(arrEnd) == '[') bracketCount++;
                        else if (current.charAt(arrEnd) == ']') bracketCount--;
                        arrEnd++;
                    }
                    current = current.substring(valueStart, arrEnd);
                } else {

                    int valueEnd = valueStart;
                    while (valueEnd < current.length()) {
                        char c = current.charAt(valueEnd);
                        if (c == ',' || c == '}' || c == ']') {
                            break;
                        }
                        valueEnd++;
                    }
                    current = current.substring(valueStart, valueEnd).trim();
                }
            }

            return current;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to extract JSON path '" + path + "': " + e.getMessage());
            return "";
        }
    }

    /**
     * Replace command substitution &(...) with command output
     * Example: &(echo hello) -> hello
     * Note: Changed from $() to &() to avoid conflicts with $ host commands
     */
    private String replaceCommandSubstitution(String text) {
        if (!hostCommandsEnabled) {
            return text;
        }

        while (text.contains("&(")) {
            int start = text.indexOf("&(");
            int end = findMatchingParenthesis(text, start + 2);
            if (end == -1) break;

            String command = text.substring(start + 2, end);
            String result = executeCommandSubstitution(command);
            text = text.substring(0, start) + result + text.substring(end + 1);
        }

        return text;
    }


    private String executeCommandSubstitution(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!output.isEmpty()) output.append(" ");
                    output.append(line);
                }
            }

            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            plugin.getLogger().warning("Command substitution failed: " + command + " - " + e.getMessage());
            return "";
        }
    }


    private String replaceFileRead(String text) {
        while (text.contains(",,")) {
            int start = text.indexOf(",,");
            int end = findNextWhitespaceOrEnd(text, start + 2);

            String fileSpec = text.substring(start + 2, end);
            String result = readFileOrYaml(fileSpec);
            text = text.substring(0, start) + result + text.substring(end);
        }

        return text;
    }

    private String readFileOrYaml(String fileSpec) {
        String[] parts = fileSpec.split("::", 2);
        String filePath = parts[0];

        try {
            File file = new File(filePath);
            if (!file.exists()) {

                file = new File(plugin.getDataFolder(), filePath);
            }

            if (!file.exists()) {
                plugin.getLogger().warning("File not found: " + filePath);
                return "";
            }


            if (parts.length > 1) {
                String yamlPath = parts[1];
                FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                Object value = yaml.get(yamlPath);
                return value != null ? value.toString() : "";
            } else {

                return java.nio.file.Files.readString(file.toPath()).trim();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read file: " + filePath + " - " + e.getMessage());
            return "";
        }
    }


    private String handleFileWrite(String text) {
        while (text.contains(";;")) {
            int start = text.indexOf(";;");
            int end = findNextWhitespaceOrEnd(text, start + 2);

            String fileSpec = text.substring(start + 2, end);
            writeFileOrYaml(fileSpec);


            text = text.substring(0, start) + text.substring(end);
        }

        return text;
    }

    private void writeFileOrYaml(String fileSpec) {
        String[] parts = fileSpec.split("::", 3);
        if (parts.length < 2) {
            plugin.getLogger().warning("Invalid file write syntax: " + fileSpec);
            return;
        }

        String filePath = parts[0];

        try {
            File file = new File(filePath);
            if (!file.isAbsolute()) {

                file = new File(plugin.getDataFolder(), filePath);
            }


            if (parts.length == 3) {
                String yamlPath = parts[1];
                String value = parts[2];

                FileConfiguration yaml;
                if (file.exists()) {
                    yaml = YamlConfiguration.loadConfiguration(file);
                } else {
                    yaml = new YamlConfiguration();
                    file.getParentFile().mkdirs();
                }


                Object parsedValue = parseValue(value);
                yaml.set(yamlPath, parsedValue);
                yaml.save(file);
                plugin.getLogger().info("Updated YAML: " + filePath + " - " + yamlPath + " = " + value);
            } else {

                String content = parts[1];
                file.getParentFile().mkdirs();
                java.nio.file.Files.writeString(file.toPath(), content);
                plugin.getLogger().info("Wrote to file: " + filePath);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to write file: " + filePath + " - " + e.getMessage());
        }
    }


    private Object parseValue(String value) {

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }


        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }


        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }


        return value;
    }


    private int findMatchingParenthesis(String text, int start) {
        int depth = 1;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private int findNextWhitespaceOrEnd(String text, int start) {
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isWhitespace(c) || c == '"' || c == '\'' || c == ',' || c == ']' || c == '}' || c == ')') {
                return i;
            }
        }
        return text.length();
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


    private String processEscapeSequences(String text) {

        text = text.replace("\\ ", "\u0000SHELL_CONTINUATION_SPACE\u0000");
        text = text.replace("\\\n", "\u0000SHELL_CONTINUATION_NEWLINE\u0000");


        text = text.replace("\\\\", "\u0000ESCAPED_BACKSLASH\u0000");


        text = text.replace("\\|", "\u0000ESCAPED_PIPE\u0000");
        text = text.replace("\\%", "\u0000ESCAPED_PERCENT\u0000");
        text = text.replace("\\:", "\u0000ESCAPED_COLON\u0000");
        text = text.replace("\\[", "\u0000ESCAPED_LBRACKET\u0000");
        text = text.replace("\\]", "\u0000ESCAPED_RBRACKET\u0000");

        return text;
    }

    private String restoreEscapedCharacters(String text) {
        text = text.replace("\u0000ESCAPED_PIPE\u0000", "|");
        text = text.replace("\u0000ESCAPED_PERCENT\u0000", "%");
        text = text.replace("\u0000ESCAPED_COLON\u0000", ":");
        text = text.replace("\u0000ESCAPED_LBRACKET\u0000", "[");
        text = text.replace("\u0000ESCAPED_RBRACKET\u0000", "]");
        text = text.replace("\u0000ESCAPED_BACKSLASH\u0000", "\\");


        text = text.replace("\u0000SHELL_CONTINUATION_SPACE\u0000", "\\ ");
        text = text.replace("\u0000SHELL_CONTINUATION_NEWLINE\u0000", "\\\n");

        return text;
    }


    private String replaceMathExpressions(String text) {
        while (text.contains("{math:")) {
            int start = text.indexOf("{math:");
            int end = text.indexOf("}", start);
            if (end == -1) break;

            String expression = text.substring(start + 6, end);
            String result = MathEvaluator.evaluate(expression);

            text = text.substring(0, start) + result + text.substring(end + 1);
        }

        return text;
    }


    private void sendColoredMessage(CommandSender sender, String colorSpec, String text) {
        try {
            Component component = Component.text(text);

            if (colorSpec != null && !colorSpec.isEmpty()) {
                String[] parts = colorSpec.split(",");

                for (String part : parts) {
                    part = part.trim().toUpperCase();


                    NamedTextColor color = getNamedTextColor(part);
                    if (color != null) {
                        component = component.color(color);
                        continue;
                    }


                    switch (part) {
                        case "BOLD" -> component = component.decorate(TextDecoration.BOLD);
                        case "ITALIC" -> component = component.decorate(TextDecoration.ITALIC);
                        case "UNDERLINED" -> component = component.decorate(TextDecoration.UNDERLINED);
                        case "STRIKETHROUGH" -> component = component.decorate(TextDecoration.STRIKETHROUGH);
                        case "OBFUSCATED" -> component = component.decorate(TextDecoration.OBFUSCATED);
                    }
                }
            }

            sender.sendMessage(component);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send colored message: " + e.getMessage());
            sender.sendMessage(Component.text(text));
        }
    }

    private NamedTextColor getNamedTextColor(String colorName) {
        return switch (colorName) {
            case "BLACK" -> NamedTextColor.BLACK;
            case "DARK_BLUE" -> NamedTextColor.DARK_BLUE;
            case "DARK_GREEN" -> NamedTextColor.DARK_GREEN;
            case "DARK_RED" -> NamedTextColor.DARK_RED;
            case "DARK_PURPLE" -> NamedTextColor.DARK_PURPLE;
            case "GOLD" -> NamedTextColor.GOLD;
            case "GRAY" -> NamedTextColor.GRAY;
            case "DARK_GRAY" -> NamedTextColor.DARK_GRAY;
            case "BLUE" -> NamedTextColor.BLUE;
            case "GREEN" -> NamedTextColor.GREEN;
            case "RED" -> NamedTextColor.RED;
            case "LIGHT_PURPLE" -> NamedTextColor.LIGHT_PURPLE;
            case "YELLOW" -> NamedTextColor.YELLOW;
            case "WHITE" -> NamedTextColor.WHITE;
            default -> null;
        };
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
