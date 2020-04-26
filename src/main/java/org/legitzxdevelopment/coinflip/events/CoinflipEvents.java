package org.legitzxdevelopment.coinflip.events;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
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

        // Check if player is in the cf menu, if they are and if they exit we can remove them
        if(!plugin.getCoinflipManager().refreshing) {
            if(plugin.getCoinflipManager().isPlayerInGUI(event.getPlayer().getUniqueId().toString())) {
                plugin.getCoinflipManager().removePlayer(event.getPlayer().getUniqueId().toString());
                //plugin.getServer().broadcastMessage("CLOSE");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if (plugin.getCoinflipManager().isActive(event.getWhoClicked().getUniqueId().toString())) {
            event.setCancelled(true);
        }

        try {
            if(plugin.getCoinflipManager().isPlayerInGUI(event.getWhoClicked().getUniqueId().toString())) {
                event.setCancelled(true);
            }
        } catch (NullPointerException e) { }

        try {
            if(plugin.getCoinflipManager().isPlayerInGUI(event.getWhoClicked().getUniqueId().toString())) {
                if(event.getSlot() >= 0 && event.getSlot() <= 54) {
                    // Cooldown stuff
                    if(plugin.getCooldownManager().hasCooldown(player)) {
                        if(plugin.getCooldownManager().getCooldownTime(player) > 0) {
                            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You still have " + plugin.getCooldownManager().getCooldownTime(player) + " seconds left until you can join a game.");
                            event.setCancelled(true);
                            return;
                        }
                        if(plugin.getCooldownManager().getCooldownTime(player) <= 0) {
                            plugin.getCooldownManager().removeFromHashMap(player);
                        }
                    }
                    plugin.getCooldownManager().addCooldown(player);

                    // Gets prize from item meta
                    String v = event.getClickedInventory().getItem(event.getSlot()).getItemMeta().getLore().get(0);
                    String[] args = v.split(" ");

                    // Removes all random shit
                    String v1 = args[1].replaceAll("[^\\d.]", "");

                    // Removes $ and ,
                    long betAmount = Long.parseLong(v1.replaceAll("[$,]", ""));



                    // Gets game from prize
                    CoinflipGame check;
                    try {
                        check = plugin.getDatabaseApi().getCoinflipGameByPrize(betAmount * 2);
                    } catch (Exception e) { e.printStackTrace(); event.setCancelled(true); return;}

                    CoinflipGame check1 = plugin.getDatabaseApi().getCoinflipByUUID(player.getUniqueId().toString());

                    if(check1 != null) {
                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You already have a coinflip up! Do /cf cancel to cancel your current game.");
                        event.setCancelled(true);
                        return;
                    }

                    // Check if player has enough money - if so it will withdraw
                    if(plugin.getEcon().has(player, betAmount)) {
                        EconomyResponse response = plugin.getEcon().withdrawPlayer(player, betAmount);

                        if(!response.transactionSuccess()) {
                            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Failed to withdraw money!");
                            event.setCancelled(true);
                            return;
                        }
                    } else {
                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You do not have enough money!");
                        event.setCancelled(true);
                        return;
                    }

                    // Double checking to make sure nobody has taken this game before this player has
                    if(check.getPlayer2() == null) {
                        // Nobody has taken the game -> proceed
                        check.setPlayer2(player.getUniqueId().toString());
                        plugin.getCoinflipCommands().updateToDatabase(check);

                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "Coinflip wager has started!");
                        plugin.getCoinflipCommands().startGame(check);

                    } else { // Seems that someone has already taken this game - Deposit the betAmount back into the player and EXIT
                        EconomyResponse response = plugin.getEcon().depositPlayer(player, betAmount);

                        if(response.transactionSuccess()) {
                            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Coinflip game was taken! $" + betAmount + " was deposited back into your account!");
                        } else {
                            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Coinflip game was taken [ERROR] Failed to deposit $" + betAmount + " back into your account! Contact an admin immediately.");
                        }
                        event.setCancelled(true);
                        return;
                    }
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {  }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Check if player has active CF's, if so add them to the queue
        CoinflipGame check = plugin.getDatabaseApi().getCoinflipByUUID(event.getPlayer().getUniqueId().toString());
        if(check != null) {
            // has CF
            plugin.getCoinflipManager().addToList(check.getPlayer1(), false);
            return;
        }

        // head caching
        plugin.getCoinflipManager().cacheHead(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if(plugin.getCoinflipManager().getHead(event.getPlayer().getUniqueId().toString()) != null) {
            plugin.getCoinflipManager().removeHead(event.getPlayer().getUniqueId().toString());
        }

        // Check if player has any CF's, if so, remove them from queue(only if inactive)
        CoinflipGame check = plugin.getDatabaseApi().getCoinflipByUUID(event.getPlayer().getUniqueId().toString());

        if(check != null) {
            // has cf
            if(plugin.getCoinflipManager().isActive(check.getPlayer1())) {
                // Well its active so let it play out
            } else {
                // Its not active, lets remove it from the queue
                plugin.getCoinflipCommands().deleteFromDatabase(check);
                EconomyResponse response = plugin.getEcon().depositPlayer(event.getPlayer(), check.getPrize() / 2);

                if(response.transactionSuccess()) {
                    // COOL!
                } else {
                    plugin.getServer().getConsoleSender().sendMessage(plugin.getUtils().CONSOLE_PREFIX + ChatColor.RED + "Unable to deposit $" + check.getPrize() / 2 + " to " + check.getPlayer1());
                }
            }
            return;
        }

        // Delete cf gui record from database
        if(plugin.getCoinflipManager().isPlayerInGUI(event.getPlayer().getUniqueId().toString())) {
            plugin.getCoinflipManager().removePlayer(event.getPlayer().getUniqueId().toString());
        }
    }

//    @EventHandler
//    public void movement(PlayerMoveEvent event) {
//        event.getPlayer().getServer().broadcastMessage("CF: " + plugin.getCoinflipManager().getCoinflipGames());
//        event.getPlayer().getServer().broadcastMessage("PLAYERS: " + plugin.getCoinflipManager().getPlayersInCfGUI());
//    }
}
