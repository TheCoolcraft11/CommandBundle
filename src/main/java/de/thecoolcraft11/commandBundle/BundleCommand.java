package de.thecoolcraft11.commandBundle;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("SameReturnValue")
public class BundleCommand implements CommandExecutor, TabCompleter {
    private final CustomCommandManager commandManager;

    public BundleCommand(CustomCommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "add", "create" -> handleAdd(sender, args);
            case "remove", "delete" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "edit" -> handleEdit(sender, args);
            case "subcommand", "sub" -> handleSubCommand(sender, args);
            case "permission", "perm" -> handlePermission(sender, args);
            case "help" -> handleHelp(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandbundle.add")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /bundle add <name> <command1> [command2] [command3] ...", NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /bundle add heal heal %player% | give %player% diamond 1", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Use | to separate multiple commands", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Use ! prefix for console commands (e.g., !give %player% diamond)", NamedTextColor.YELLOW));
            return true;
        }

        String commandName = args[1].toLowerCase();


        String actionString = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        List<String> actions = Arrays.stream(actionString.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (actions.isEmpty()) {
            sender.sendMessage(Component.text("You must specify at least one command to execute.", NamedTextColor.RED));
            return true;
        }

        if (commandManager.addCommand(commandName, actions)) {
            sender.sendMessage(Component.text("Successfully created command bundle: /" + commandName, NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Actions (" + actions.size() + "):", NamedTextColor.GRAY));
            for (int i = 0; i < actions.size(); i++) {
                sender.sendMessage(Component.text("  " + (i + 1) + ". ", NamedTextColor.GRAY)
                        .append(Component.text(actions.get(i), NamedTextColor.WHITE)));
            }
        } else {
            sender.sendMessage(Component.text("A command bundle with that name already exists.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandbundle.remove")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /bundle remove <name>", NamedTextColor.RED));
            return true;
        }

        String commandName = args[1].toLowerCase();

        if (commandManager.removeCommand(commandName)) {
            sender.sendMessage(Component.text("Successfully removed command bundle: /" + commandName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("No command bundle found with that name.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("commandbundle.list")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        var commands = commandManager.getCommandNames();

        if (commands.isEmpty()) {
            sender.sendMessage(Component.text("No command bundles have been created yet.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("Command Bundles (" + commands.size() + "):", NamedTextColor.GREEN));
        for (String cmd : commands) {
            List<String> actions = commandManager.getCommandActions(cmd);
            sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("/" + cmd, NamedTextColor.WHITE))
                    .append(Component.text(" (" + actions.size() + " action" + (actions.size() != 1 ? "s" : "") + ")", NamedTextColor.GRAY)));
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandbundle.info")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /bundle info <name>", NamedTextColor.RED));
            return true;
        }

        String commandName = args[1].toLowerCase();
        List<String> actions = commandManager.getCommandActions(commandName);

        if (actions == null) {
            sender.sendMessage(Component.text("No command bundle found with that name.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Command Bundle: ", NamedTextColor.GREEN)
                .append(Component.text("/" + commandName, NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Actions (" + actions.size() + "):", NamedTextColor.GRAY));
        for (int i = 0; i < actions.size(); i++) {
            sender.sendMessage(Component.text("  " + (i + 1) + ". ", NamedTextColor.GRAY)
                    .append(Component.text(actions.get(i), NamedTextColor.WHITE)));
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("commandbundle.reload")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        commandManager.loadCommands();
        sender.sendMessage(Component.text("Command bundles reloaded successfully.", NamedTextColor.GREEN));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== CommandBundle Help ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bundle add <name> <cmd1> | <cmd2> | ...", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a command bundle", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle remove <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove a command bundle", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle list", NamedTextColor.YELLOW)
                .append(Component.text(" - List all command bundles", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle info <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - View bundle details", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle edit <name> <operation>", NamedTextColor.YELLOW)
                .append(Component.text(" - Edit a bundle", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle subcommand <name> <sub> <actions>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add sub-command", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle permission <name> [perm]", NamedTextColor.YELLOW)
                .append(Component.text(" - Set/view permission", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload command bundles", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/bundle help [topic]", NamedTextColor.YELLOW)
                .append(Component.text(" - Detailed help", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("Topics: placeholders, conditions, delays, variables, random", NamedTextColor.GRAY));
    }

    private boolean handleHelp(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendHelp(sender);
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("Quick Examples:", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("  /bundle add heal heal %player% | tell %player% Healed!", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /bundle add kit [delay:3] !give %player% diamond 1", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /bundle add staff [if:permission:admin] gamemode creative", NamedTextColor.GRAY));
            return true;
        }

        String topic = args[1].toLowerCase();

        switch (topic) {
            case "placeholders":
            case "placeholder":
                sender.sendMessage(Component.text("=== Placeholders Help ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Basic Placeholders:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  %player% ", NamedTextColor.GRAY)
                        .append(Component.text("- Player name", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %uuid% ", NamedTextColor.GRAY)
                        .append(Component.text("- Player UUID", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %world% ", NamedTextColor.GRAY)
                        .append(Component.text("- World name", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %x%, %y%, %z% ", NamedTextColor.GRAY)
                        .append(Component.text("- Coordinates", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %health% ", NamedTextColor.GRAY)
                        .append(Component.text("- Player health", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %level% ", NamedTextColor.GRAY)
                        .append(Component.text("- Player level", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %gamemode% ", NamedTextColor.GRAY)
                        .append(Component.text("- Current gamemode", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("\nArgument Placeholders:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  %arg1%, %arg2%, ... ", NamedTextColor.GRAY)
                        .append(Component.text("- Individual arguments", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %args% ", NamedTextColor.GRAY)
                        .append(Component.text("- All arguments", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  %arg1|default% ", NamedTextColor.GRAY)
                        .append(Component.text("- With default value", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("\nVariable Placeholders:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  %var:name% ", NamedTextColor.GRAY)
                        .append(Component.text("- Custom variable", NamedTextColor.WHITE)));
                break;

            case "conditions":
            case "condition":
            case "if":
                sender.sendMessage(Component.text("=== Conditions Help ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Syntax: ", NamedTextColor.YELLOW)
                        .append(Component.text("[if:type:value] command", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("\nCondition Types:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  permission:node ", NamedTextColor.GRAY)
                        .append(Component.text("- Check permission", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  item:material ", NamedTextColor.GRAY)
                        .append(Component.text("- Check for item", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  item:diamond:5 ", NamedTextColor.GRAY)
                        .append(Component.text("- Check for 5 diamonds", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  world:world_nether ", NamedTextColor.GRAY)
                        .append(Component.text("- Check world", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  gamemode:creative ", NamedTextColor.GRAY)
                        .append(Component.text("- Check gamemode", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  health:>10 ", NamedTextColor.GRAY)
                        .append(Component.text("- Health greater than 10", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  level:>=5 ", NamedTextColor.GRAY)
                        .append(Component.text("- Level 5 or higher", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  flying:true ", NamedTextColor.GRAY)
                        .append(Component.text("- Check if flying", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("  sneaking:true ", NamedTextColor.GRAY)
                        .append(Component.text("- Check if sneaking", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("\nExamples:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  [if:permission:admin] gamemode creative", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("  [if:item:diamond:10] tell %player% You have diamonds!", NamedTextColor.WHITE));
                break;

            case "delays":
            case "delay":
                sender.sendMessage(Component.text("=== Delays Help ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Syntax: ", NamedTextColor.YELLOW)
                        .append(Component.text("[delay:seconds] command", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("\nExamples:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  heal %player% | [delay:5] tell %player% Cooldown over!", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("  [delay:3] !give %player% diamond 1", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("  tp %player% 0 100 0 | [delay:1] playsound ...", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("\nDelays stack: First command runs immediately,", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("second runs after its delay, etc.", NamedTextColor.GRAY));
                break;

            case "variables":
            case "variable":
            case "var":
                sender.sendMessage(Component.text("=== Variables Help ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Set Variable: ", NamedTextColor.YELLOW)
                        .append(Component.text("[var:set:name:value]", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("Use Variable: ", NamedTextColor.YELLOW)
                        .append(Component.text("%var:name%", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("\nExamples:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  Store location: setvar lastloc %x%,%y%,%z%", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("  Use variable: tell %player% Last: %var:lastloc%", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("\nVariables are per-player by default.", NamedTextColor.GRAY));
                break;

            case "random":
                sender.sendMessage(Component.text("=== Random Selection Help ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Syntax: ", NamedTextColor.YELLOW)
                        .append(Component.text("[random] command", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("Weighted: ", NamedTextColor.YELLOW)
                        .append(Component.text("[random:weight] command", NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("\nExample:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  [random:70] give %player% diamond 1", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("  [random:20] give %player% gold_ingot 5", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("  [random:10] give %player% dirt 64", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("\n70% chance diamond, 20% gold, 10% dirt", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Only ONE random action executes per bundle.", NamedTextColor.GRAY));
                break;

            case "subcommands":
            case "sub":
                sender.sendMessage(Component.text("=== Sub-Commands Help ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Create sub-command:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  /bundle subcommand <cmd> <sub> <actions>", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("\nExample:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  /bundle add kit heal %player%", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("  /bundle sub kit food !give %player% bread 32", NamedTextColor.WHITE));
                sender.sendMessage(Component.text("Now you have: /kit (heals) and /kit food (gives bread)", NamedTextColor.GRAY));
                break;

            case "edit":
                sender.sendMessage(Component.text("=== Edit Commands Help ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Operations:", NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("  /bundle edit <cmd> add <action> ", NamedTextColor.WHITE)
                        .append(Component.text("- Add to end", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("  /bundle edit <cmd> insert <index> <action> ", NamedTextColor.WHITE)
                        .append(Component.text("- Insert at position", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("  /bundle edit <cmd> remove <index> ", NamedTextColor.WHITE)
                        .append(Component.text("- Remove action", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("  /bundle edit <cmd> replace <index> <action> ", NamedTextColor.WHITE)
                        .append(Component.text("- Replace action", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("\nIndexes start at 0. Use /bundle info to see indexes.", NamedTextColor.GRAY));
                break;

            default:
                sender.sendMessage(Component.text("Unknown help topic: " + topic, NamedTextColor.RED));
                sender.sendMessage(Component.text("Available topics: placeholders, conditions, delays,", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("variables, random, subcommands, edit", NamedTextColor.GRAY));
                break;
        }

        return true;
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandbundle.edit")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /bundle edit <name> <operation> [index] [action]", NamedTextColor.RED));
            sender.sendMessage(Component.text("Operations: add, insert, remove, replace", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Examples:", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /bundle edit heal add tell %player% Done!", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /bundle edit heal insert 0 heal %player%", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /bundle edit heal remove 2", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /bundle edit heal replace 1 give %player% diamond", NamedTextColor.GRAY));
            return true;
        }

        String commandName = args[1].toLowerCase();
        String operation = args[2].toLowerCase();

        if (!commandManager.getCommandNames().contains(commandName)) {
            sender.sendMessage(Component.text("Command bundle not found: " + commandName, NamedTextColor.RED));
            return true;
        }

        boolean success = false;

        switch (operation) {
            case "add":
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /bundle edit <name> add <action>", NamedTextColor.RED));
                    return true;
                }
                String addAction = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                success = commandManager.editCommand(commandName, "add", -1, addAction);
                if (success) {
                    sender.sendMessage(Component.text("Added action to " + commandName, NamedTextColor.GREEN));
                }
                break;

            case "insert":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /bundle edit <name> insert <index> <action>", NamedTextColor.RED));
                    return true;
                }
                try {
                    int insertIndex = Integer.parseInt(args[3]);
                    String insertAction = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    success = commandManager.editCommand(commandName, "insert", insertIndex, insertAction);
                    if (success) {
                        sender.sendMessage(Component.text("Inserted action at index " + insertIndex, NamedTextColor.GREEN));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid index: " + args[3], NamedTextColor.RED));
                }
                break;

            case "remove":
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage: /bundle edit <name> remove <index>", NamedTextColor.RED));
                    return true;
                }
                try {
                    int removeIndex = Integer.parseInt(args[3]);
                    success = commandManager.editCommand(commandName, "remove", removeIndex, null);
                    if (success) {
                        sender.sendMessage(Component.text("Removed action at index " + removeIndex, NamedTextColor.GREEN));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid index: " + args[3], NamedTextColor.RED));
                }
                break;

            case "replace":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("Usage: /bundle edit <name> replace <index> <action>", NamedTextColor.RED));
                    return true;
                }
                try {
                    int replaceIndex = Integer.parseInt(args[3]);
                    String replaceAction = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    success = commandManager.editCommand(commandName, "replace", replaceIndex, replaceAction);
                    if (success) {
                        sender.sendMessage(Component.text("Replaced action at index " + replaceIndex, NamedTextColor.GREEN));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid index: " + args[3], NamedTextColor.RED));
                }
                break;

            default:
                sender.sendMessage(Component.text("Unknown operation: " + operation, NamedTextColor.RED));
                sender.sendMessage(Component.text("Valid operations: add, insert, remove, replace", NamedTextColor.YELLOW));
                return true;
        }

        if (!success) {
            sender.sendMessage(Component.text("Failed to edit command. Check the index or operation.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleSubCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandbundle.subcommand")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /bundle subcommand <name> <subname> <actions>", NamedTextColor.RED));
            sender.sendMessage(Component.text("Example: /bundle sub kit food !give %player% bread 32", NamedTextColor.YELLOW));
            return true;
        }

        String commandName = args[1].toLowerCase();
        String subName = args[2].toLowerCase();

        if (!commandManager.getCommandNames().contains(commandName)) {
            sender.sendMessage(Component.text("Command bundle not found: " + commandName, NamedTextColor.RED));
            return true;
        }

        String actionString = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        List<String> actions = Arrays.stream(actionString.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (commandManager.addSubCommand(commandName, subName, actions)) {
            sender.sendMessage(Component.text("Added sub-command: /" + commandName + " " + subName, NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Actions (" + actions.size() + "):", NamedTextColor.GRAY));
            for (int i = 0; i < actions.size(); i++) {
                sender.sendMessage(Component.text("  " + (i + 1) + ". ", NamedTextColor.GRAY)
                        .append(Component.text(actions.get(i), NamedTextColor.WHITE)));
            }
        } else {
            sender.sendMessage(Component.text("Failed to add sub-command.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handlePermission(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandbundle.permission")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /bundle permission <name> [permission]", NamedTextColor.RED));
            sender.sendMessage(Component.text("Set permission: /bundle perm heal commandbundle.heal", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("View permission: /bundle perm heal", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Remove permission: /bundle perm heal none", NamedTextColor.YELLOW));
            return true;
        }

        String commandName = args[1].toLowerCase();

        if (!commandManager.getCommandNames().contains(commandName)) {
            sender.sendMessage(Component.text("Command bundle not found: " + commandName, NamedTextColor.RED));
            return true;
        }

        if (args.length == 2) {
            
            String permission = commandManager.getCommandPermission(commandName);
            if (permission == null || permission.isEmpty()) {
                sender.sendMessage(Component.text("Command /" + commandName + " has no permission requirement.", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("Command /" + commandName + " requires: ", NamedTextColor.GREEN)
                        .append(Component.text(permission, NamedTextColor.WHITE)));
            }
        } else {
            
            String permission = args[2];

            if (permission.equalsIgnoreCase("none") || permission.equalsIgnoreCase("remove")) {
                commandManager.setCommandPermission(commandName, null);
                sender.sendMessage(Component.text("Removed permission requirement from /" + commandName, NamedTextColor.GREEN));
            } else {
                commandManager.setCommandPermission(commandName, permission);
                sender.sendMessage(Component.text("Set permission for /" + commandName + ": ", NamedTextColor.GREEN)
                        .append(Component.text(permission, NamedTextColor.WHITE)));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("add", "remove", "list", "info", "reload",
                    "edit", "subcommand", "permission", "help");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("remove") || subCmd.equals("info") || subCmd.equals("delete") ||
                    subCmd.equals("edit") || subCmd.equals("permission") || subCmd.equals("perm") ||
                    subCmd.equals("subcommand") || subCmd.equals("sub")) {
                return commandManager.getCommandNames().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (subCmd.equals("help")) {
                return Stream.of("placeholders", "conditions", "delays", "variables", "random", "subcommands", "edit")
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            return Stream.of("add", "insert", "remove", "replace")
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}