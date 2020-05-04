package org.legitzxdevelopment.coinflip;

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
import org.legitzxdevelopment.coinflip.cooldown.CooldownManager;
import org.legitzxdevelopment.coinflip.database.DatabaseApi;
import org.legitzxdevelopment.coinflip.events.CoinflipEvents;
import org.legitzxdevelopment.coinflip.settings.Countdown;
import org.legitzxdevelopment.coinflip.settings.Utils;

import java.util.UUID;

public final class Coinflip extends JavaPlugin {
    // Converters
    CoinflipConverter coinflipConverter;

    // Managers
    CoinflipManager coinflipManager;
    CooldownManager cooldownManager;

    // Commands
    CoinflipCommands commands;

    // Utils
    Utils utils;

    // APIs
    private Economy econ = null;

    // Database
    private DatabaseApi databaseApi;

    @Override
    public void onEnable() {
        // Plugin startup logic

        // Load Config
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        utils = new Utils(this);

        // Setup VaultAPI
        if(!setupEconomy()) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + utils.CONSOLE_PREFIX + "Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Classes
        initInstances();

        // Events
        getServer().getPluginManager().registerEvents(new CoinflipEvents(this), this);

        // Commands
        this.getCommand("cf").setExecutor(new CoinflipCommands(this));
        this.getCommand("coinflip").setExecutor(new CoinflipCommands(this));

        // Refund old coinflips
        refundCooldown();

        // Finished!
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + utils.CONSOLE_PREFIX + "Enabled Coinflip " + utils.VERSION);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // When server disables, clean up hashmap
        this.getCoinflipManager().cleanUp();
    }

    public void initInstances() {
        //utils = new Utils(this);
        coinflipManager = new CoinflipManager(this);
        coinflipConverter = new CoinflipConverter();
        commands = new CoinflipCommands(this);
        cooldownManager = new CooldownManager(this);
        databaseApi = new DatabaseApi(this);
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

            if(economyResponse1.transactionSuccess()) {
                getServer().getConsoleSender().sendMessage(utils.CONSOLE_PREFIX + ChatColor.GREEN +  "Added $" + game.getPrize() / 2 + " to " + game.getPlayer1() + " since he was in an active CF when the server restarted!");
            } else {
                getServer().getConsoleSender().sendMessage(utils.CONSOLE_PREFIX + ChatColor.RED + "There was an issue adding " + game.getPrize() / 2 + " to " + game.getPlayer1() + "!");
            }

            try {
                EconomyResponse economyResponse2 = this.getEcon().depositPlayer(Bukkit.getOfflinePlayer(UUID.fromString(game.getPlayer2())), game.getPrize() / 2);
                if (economyResponse2.transactionSuccess()) {
                    getServer().getConsoleSender().sendMessage(utils.CONSOLE_PREFIX + ChatColor.GREEN + "Added $" + game.getPrize() / 2 + " to " + game.getPlayer2() + " since he was in an active CF when the server restarted!");
                } else {
                    getServer().getConsoleSender().sendMessage(utils.CONSOLE_PREFIX + ChatColor.RED + "There was an issue adding " + game.getPrize() / 2 + " to " + game.getPlayer2() + "!");
                }
            } catch(NullPointerException e) {
                return;
            }
        }
    }

    public Economy getEcon() {
        return econ;
    }

    public Utils getUtils() {
        return utils;
    }

    public DatabaseApi getDatabaseApi() {
        return databaseApi;
    }

    public CoinflipManager getCoinflipManager() {
        return coinflipManager;
    }

    public CoinflipCommands getCoinflipCommands() {
        return commands;
    }

    public CooldownManager getCooldownManager() { return cooldownManager; }

    public CoinflipConverter getCoinflipConverter() {
        return coinflipConverter;
    }
}

