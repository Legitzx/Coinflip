package org.legitzxdevelopment.coinflip.cooldown;
import org.bukkit.entity.Player;
import org.legitzxdevelopment.coinflip.Coinflip;

import java.util.HashMap;

public class CooldownManager {
    private HashMap<String, Long> cooldowns;
    private Coinflip plugin;

    public CooldownManager(Coinflip plugin) {
        this.plugin = plugin;

        cooldowns = new HashMap<>();
    }

    /**
     * Description: Adds cooldown to specified player.
     * @param player    Player the cooldown is being applied to.
     */
    public void addCooldown(Player player) {
        cooldowns.put(player.getUniqueId().toString(), System.currentTimeMillis());
    }

    /**
     * Description: Removes a player from the HashMap.
     * @param player    Player the removal is being applied to.
     */
    public void removeFromHashMap(Player player) {
        cooldowns.remove(player.getUniqueId().toString());
    }

    /**
     * Description: Gets the cooldown timer from a specified player.
     * @param player    Player with the cooldown.
     * @return          Returns an integer, this integer will be the players cool down.
     */
    public int getCooldownTime(Player player) {
        if(cooldowns.containsKey(player.getUniqueId().toString())) {
            long remainingTime = cooldowns.get(player.getUniqueId().toString());
            int oldTime = (int) Math.floor(remainingTime / 1000);
            int newTime = (int) Math.floor(System.currentTimeMillis() / 1000);
            return plugin.getConfig().getInt("cooldown") - (newTime - oldTime);
        } else {
            return 0;
        }
    }

    /**
     * Description: Checks if the player has an active cool down.
     * @param player    Player that is being checked.
     * @return          Returns true if the person has a cooldown, returns false if a person does not have a cool down.
     */
    public boolean hasCooldown(Player player) {
        if(cooldowns.containsKey(player.getUniqueId().toString())) {
            return true;
        } else {
            return false;
        }
    }
}
