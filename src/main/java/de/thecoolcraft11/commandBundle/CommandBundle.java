package de.thecoolcraft11.commandBundle;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CommandBundle extends JavaPlugin {
    private CustomCommandManager commandManager;

    @Override
    public void onEnable() {

        getLogger().info("CommandBundle is starting...");


        commandManager = new CustomCommandManager(this);


        BundleCommand bundleCommand = new BundleCommand(commandManager);
        Objects.requireNonNull(getCommand("bundle")).setExecutor(bundleCommand);
        Objects.requireNonNull(getCommand("bundle")).setTabCompleter(bundleCommand);

        getLogger().info("CommandBundle has been enabled! Loaded " +
                commandManager.getCommandNames().size() + " command bundle(s).");
    }

    @Override
    public void onDisable() {

        getLogger().info("CommandBundle has been disabled.");
    }

    public CustomCommandManager getCommandManager() {
        return commandManager;
    }
}
