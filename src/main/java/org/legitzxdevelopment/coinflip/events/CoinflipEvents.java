package org.legitzxdevelopment.coinflip.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.legitzxdevelopment.coinflip.Coinflip;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipGame;

public class CoinflipEvents implements Listener {

    Coinflip plugin = Coinflip.getPlugin(Coinflip.class);

    // If player is in a CF game, it will make sure they cannot exit out of it.
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Make it so if a player is in the active queue -> they cannot exit their inventory
        CoinflipGame check = plugin.getDatabaseApi().getCoinflipByAnyUUID(event.getPlayer().getUniqueId().toString());

        if(check != null) {
            // has CF - So make sure they cannot exit their inventory
            if(plugin.getCoinflipManager().isActive(check.getPlayer1())) {
                event.getPlayer().openInventory(event.getInventory());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (plugin.getCoinflipManager().isActive(event.getWhoClicked().getUniqueId().toString())) {
                event.setCancelled(true);
            }
        } catch (NullPointerException e) {
            return;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Check if player has active CF's, if so add them to the queue
        CoinflipGame check = plugin.getDatabaseApi().getCoinflipByUUID(event.getPlayer().getUniqueId().toString());
        // TODO: REMOVE SECOND PLAYER FROM GAME
        if(check != null) {
            // has CF
            plugin.getCoinflipManager().addToList(check.getPlayer1(), false);
            return;
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        // Check if player has any CF's, if so, remove them from queue(only if inactive)
        CoinflipGame check = plugin.getDatabaseApi().getCoinflipByUUID(event.getPlayer().getUniqueId().toString());

        if(check != null) {
            // has cf
            if(plugin.getCoinflipManager().isActive(check.getPlayer1())) {
                // Well its active so let it play out
            } else {
                // Its not active, lets remove it from the queue
                plugin.getCoinflipManager().removeFromList(check.getPlayer1());
            }
            return;
        }
    }
}
