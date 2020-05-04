package org.legitzxdevelopment.coinflip.settings;

import org.bukkit.ChatColor;
import org.legitzxdevelopment.coinflip.Coinflip;

import java.text.DecimalFormat;
import java.util.Random;

public class Utils {
    Coinflip plugin;

    public final String CONSOLE_PREFIX = "[Coinflip] ";
    public final String INGAME_PREFIX;
    public final String VERSION = "v0.1";
    public Random random = new Random();
    public DecimalFormat df = new DecimalFormat("#,###");

    public Utils(Coinflip plugin) {
        this.plugin = plugin;

        INGAME_PREFIX = ChatColor.GOLD + "" + ChatColor.BOLD + plugin.getConfig().getString("server.name") + ChatColor.GOLD + "" + ChatColor.BOLD + " >> ";
    }

    public Random getRandom() {
        return random;
    }

    public DecimalFormat getDecimalFormat() {
        return df;
    }
}
