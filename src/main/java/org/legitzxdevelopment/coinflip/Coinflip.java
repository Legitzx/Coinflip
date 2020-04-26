package org.legitzxdevelopment.coinflip;

import com.mongodb.client.MongoDatabase;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipConverter;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipGame;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipManager;
import org.legitzxdevelopment.coinflip.commands.CoinflipCommands;
import org.legitzxdevelopment.coinflip.database.DatabaseApi;
import org.legitzxdevelopment.coinflip.database.DatabaseConnection;
import org.legitzxdevelopment.coinflip.events.CoinflipEvents;
import org.legitzxdevelopment.coinflip.settings.Countdown;
import org.legitzxdevelopment.coinflip.settings.Utils;

import java.util.UUID;

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

        // Refund old coinflips
        refundCooldown();

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

    public void refundCooldown() {
        new Countdown(3, this) {
            @Override
            public void count(int current) {
                refundOverloadedCoinflips();
            }
        }.start();
    }

    public void refundOverloadedCoinflips() {
        for (CoinflipGame game : this.getDatabaseApi().getAllCoinflips()) {
            this.getDatabaseApi().deleteCoinflipGame(game);

            EconomyResponse economyResponse1 = this.getEcon().depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(game.getPlayer1())), game.getPrize() / 2);
            EconomyResponse economyResponse2 = this.getEcon().depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(game.getPlayer2())), game.getPrize() / 2);

            if(economyResponse1.transactionSuccess()) {
                getServer().getConsoleSender().sendMessage(utils.INGAME_PREFIX + ChatColor.GREEN +  "Added " + game.getPrize() / 2 + " to " + game.getPlayer1() + " since he was in an active CF when the server restarted!");
            } else {
                getServer().getConsoleSender().sendMessage(utils.INGAME_PREFIX + ChatColor.RED + "There was an issue adding " + game.getPrize() / 2 + " to " + game.getPlayer1() + "!");
            }

            if(economyResponse2.transactionSuccess()) {
                getServer().getConsoleSender().sendMessage(utils.INGAME_PREFIX + ChatColor.GREEN +  "Added " + game.getPrize() / 2 + " to " + game.getPlayer2() + " since he was in an active CF when the server restarted!");
            } else {
                getServer().getConsoleSender().sendMessage(utils.INGAME_PREFIX + ChatColor.RED + "There was an issue adding " + game.getPrize() / 2 + " to " + game.getPlayer2() + "!");
            }
        }
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
