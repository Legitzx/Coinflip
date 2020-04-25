package org.legitzxdevelopment.coinflip;

import com.mongodb.client.MongoDatabase;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipConverter;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipManager;
import org.legitzxdevelopment.coinflip.commands.CoinflipCommands;
import org.legitzxdevelopment.coinflip.database.DatabaseApi;
import org.legitzxdevelopment.coinflip.database.DatabaseConnection;
import org.legitzxdevelopment.coinflip.events.CoinflipEvents;
import org.legitzxdevelopment.coinflip.settings.Utils;

public final class Coinflip extends JavaPlugin {
    // Converters
    CoinflipConverter coinflipConverter;

    // Managers
    CoinflipManager coinflipManager;

    // Utils
    Utils utils;

    // APIs
    private static Economy econ = null;

    // Database
    private MongoDatabase database;
    private DatabaseApi databaseApi;

    @Override
    public void onEnable() {
        // Plugin startup logic

        // Initialize Classes
        initInstances();

        // Setup VaultAPI
        if(!setupEconomy()) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + utils.CONSOLE_PREFIX + "Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Load Config
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        // Connect to database
        DatabaseConnection connection = new DatabaseConnection();
        database = connection.getDatabase();
        databaseApi = new DatabaseApi();

        // Events
        getServer().getPluginManager().registerEvents(new CoinflipEvents(), this);

        // Commands
        this.getCommand("cf").setExecutor(new CoinflipCommands());

        // Finished!
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + utils.CONSOLE_PREFIX + "Enabled Coinflip " + utils.VERSION);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // TODO: Cancel ALL active games - umm idk
        // When server disables, clean up hashmap
        this.getCoinflipManager().cleanUp();
    }

    public void initInstances() {
        coinflipManager = new CoinflipManager(this);
        utils = new Utils();
        coinflipConverter = new CoinflipConverter();
    }

    // <-- VaultAPI -->
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEcon() {
        return econ;
    }

    public Utils getUtils() {
        return utils;
    }

    public MongoDatabase getMongoDatabase() {
        return database;
    }

    public DatabaseApi getDatabaseApi() {
        return databaseApi;
    }

    public CoinflipManager getCoinflipManager() {
        return coinflipManager;
    }
}
