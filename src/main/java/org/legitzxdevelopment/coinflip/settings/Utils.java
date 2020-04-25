package org.legitzxdevelopment.coinflip.settings;

import org.bukkit.ChatColor;
import org.legitzxdevelopment.coinflip.Coinflip;

import java.util.Random;

public class Utils {
    Coinflip plugin = Coinflip.getPlugin(Coinflip.class);

    public final String CONSOLE_PREFIX = "[Coinflip] ";
    public final String INGAME_PREFIX = ChatColor.GOLD.BOLD + plugin.getConfig().getString("server.name") + ChatColor.DARK_GRAY.BOLD + " >> ";
    public final String VERSION = "v0.1";

    public int getRandNum() {
        Random random = new Random();

        return random.nextInt(2);
    }
}
