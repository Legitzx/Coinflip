package org.legitzxdevelopment.coinflip.settings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.legitzxdevelopment.coinflip.Coinflip;

import java.text.DecimalFormat;
import java.util.Random;

public class Utils {
    Coinflip plugin;

    public final String CONSOLE_PREFIX = "[Coinflip] ";
    public final String INGAME_PREFIX;
    public final String VERSION = "v0.1";
    public Random random;
    public DecimalFormat df;
    private Inventory inventory;

    public Utils(Coinflip plugin) {
        this.plugin = plugin;

        INGAME_PREFIX = ChatColor.GOLD + "" + ChatColor.BOLD + plugin.getConfig().getString("server.name") + ChatColor.GOLD + "" + ChatColor.BOLD + " >> ";
        inventory = Bukkit.createInventory(null, 45, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + plugin.getConfig().getString("server.name"));
        random = new Random();
        df = new DecimalFormat("#,###");

    }

    public Random getRandom() {
        return random;
    }

    public DecimalFormat getDecimalFormat() {
        return df;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
