package org.legitzxdevelopment.coinflip.commands;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.legitzxdevelopment.coinflip.Coinflip;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipGame;
import org.legitzxdevelopment.coinflip.settings.Countdown;
import org.legitzxdevelopment.coinflip.settings.FastCountdown;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class CoinflipCommands implements CommandExecutor {
    // Plugin this class belongs to
    Coinflip plugin = Coinflip.getPlugin(Coinflip.class);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("cf")) {
            if(sender instanceof Player) {
                Player player = (Player) sender;

                if(args.length > 0) {
                    if(args.length >= 1) {
                        if(args[0].equalsIgnoreCase("create")) {
                            if(args.length >= 2) {
                                if(args[1].matches("[0-9]+")) {
                                    if(Long.parseLong(args[1]) >= plugin.getConfig().getInt("coinflip.min")) {
                                        createGame(player, Long.parseLong(args[1]));
                                    } else {
                                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Minimum bet amount is $" + plugin.getConfig().getInt("coinflip.min") + "!");
                                    }
                                } else {
                                    player.sendMessage("create but false bcs only numbers");
                                }
                            } else {
                                player.sendMessage("Missing args for create");
                            }
                        }


                        if(args[0].equalsIgnoreCase("cancel")) {
                            CoinflipGame coinflipGame = plugin.getDatabaseApi().getCoinflipByUUID(player.getUniqueId().toString());

                            if(coinflipGame != null) {
                                if(!plugin.getCoinflipManager().isActive(coinflipGame.getPlayer1())) {
                                    EconomyResponse response = plugin.getEcon().depositPlayer(player, (coinflipGame.getPrize() / 2));

                                    if(response.transactionSuccess()) {
                                        deleteFromDatabase(coinflipGame);
                                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "Successfully canceled CF! Money has been refunded.");
                                    } else {
                                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "There was a problem removing you CF!");
                                    }
                                    return true;
                                } else {
                                    player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Seems that you are already in an active game.");
                                }
                            } else {
                                player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You do not have a CF up!");
                            }
                        }



                        if(args[0].equalsIgnoreCase("reload")) {

                            player.sendMessage("reload");
                        }

                        if(args[0].equalsIgnoreCase("help")) {
                            player.sendMessage("help");
                        }
                    }
                } else {
                    // TODO: Open cf inventory
                    player.sendMessage("open inventory cf");
                }
            }
            return true;
        }
        return true;
    }

    public void createGame(Player player, long betAmount) {
        // Checks if player already has a cf up
        CoinflipGame check = plugin.getDatabaseApi().getCoinflipByUUID(player.getUniqueId().toString());

        if(check != null) {
            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You already have a CF up! Do /cf cancel to cancel your current game.");
            return;
        }

        // Check if player has enough money - if so it will withdraw
        if(plugin.getEcon().has(player, betAmount)) {
            EconomyResponse response = plugin.getEcon().withdrawPlayer(player, betAmount);

            if(!response.transactionSuccess()) {
                player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Failed to withdraw money!");
                return;
            }
        } else {
            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You do not have enough money!");
            return;
        }

        // First it will check if a game already exists with the specified betAmount
        if(plugin.getDatabaseApi().getCoinflipGameByPrize(betAmount * 2) != null) {
            // Game exists
            CoinflipGame coinflipGame = plugin.getDatabaseApi().getCoinflipGameByPrize(betAmount * 2);

            // Double checking to make sure nobody has taken this game before this player has
            if(coinflipGame.getPlayer2() == null) {
                // Nobody has taken the game -> proceed
                coinflipGame.setPlayer2(player.getUniqueId().toString());
                updateToDatabase(coinflipGame);

                player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "Game has started");
                startGame(coinflipGame);

            } else { // Seems that someone has already taken this game - Deposit the betAmount back into the player and EXIT
                EconomyResponse response = plugin.getEcon().depositPlayer(player, betAmount);

                if(response.transactionSuccess()) {
                    player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Coinflip game was taken! $" + betAmount + " was deposited back into your account!");
                } else {
                    player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Coinflip game was taken [ERROR] Failed to deposit $" + betAmount + " back into your account! Contact an admin immediately.");
                }
                return;
            }
            coinflipGame.setPlayer2(player.getUniqueId().toString());
        } else {
            // Game does not exist - create one - wait for someone else to join
            CoinflipGame coinflipGame = new CoinflipGame(player.getUniqueId().toString(), null, betAmount * 2, System.currentTimeMillis());
            insertIntoDatabase(coinflipGame);

            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "Waiting for player to join");
        }
    }

    public void insertIntoDatabase(CoinflipGame coinflipGame) {
        plugin.getDatabaseApi().insertCoinflipGame(coinflipGame); // Remotely
        plugin.getCoinflipManager().addToList(coinflipGame.getPlayer1(), false); // Locally
    }

    public void updateToDatabase(CoinflipGame coinflipGame) {
        plugin.getDatabaseApi().updateCoinflipGame(coinflipGame);
        plugin.getCoinflipManager().updateList(coinflipGame.getPlayer1(), true);
    }

    public void deleteFromDatabase(CoinflipGame coinflipGame) {
        plugin.getDatabaseApi().deleteCoinflipGame(coinflipGame);
        plugin.getCoinflipManager().removeFromList(coinflipGame.getPlayer1());
    }

    public void startGame(CoinflipGame game) {
        Player player1 = Bukkit.getPlayer(UUID.fromString(game.getPlayer1()));
        Player player2 = Bukkit.getPlayer(UUID.fromString(game.getPlayer2()));

        new Countdown(3, plugin) {
            @Override
            public void count(int current) {
                player1.playSound(player1.getLocation(), Sound.NOTE_BASS, 40, 1);
                player2.playSound(player2.getLocation(), Sound.NOTE_BASS, 40, 1);
                if(current == 3) {
                    countDownScreen3(player1);
                    countDownScreen3(player2);
                } else if(current == 2) {
                    countDownScreen2(player1);
                    countDownScreen2(player2);
                } else if(current == 1) {
                    countDownScreen1(player1);
                    countDownScreen1(player2);
                } else if(current == 0) {
                    activeGameGUI(player1, player2);
                }
            }
        }.start();
    }


    public void countDownScreen3(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 45, "Coinflip");

        ItemStack main = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 14);
        ItemStack fill = new ItemStack(Material.STAINED_GLASS_PANE, 1);

        int[] mainArray = {3, 4, 5, 14, 22, 23, 32, 39, 40, 41};
        boolean check = false;

        for(int slot = 0; slot < inventory.getSize(); slot++) {
            check = false;

            for(int x = 0; x < mainArray.length; x++) {
                if(slot == mainArray[x]) {
                    inventory.setItem(slot, main);
                    check = true;
                    break;
                }
            }

            if(!check) {
                inventory.setItem(slot, fill);
            }
        }

        player.openInventory(inventory);
    }

    public void countDownScreen2(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 45, "Coinflip");

        ItemStack main = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 1);
        ItemStack fill = new ItemStack(Material.STAINED_GLASS_PANE, 1);

        int[] mainArray = {3, 4, 5, 14, 22, 30, 39, 40, 41};
        boolean check = false;

        for(int slot = 0; slot < inventory.getSize(); slot++) {
            check = false;

            for(int x = 0; x < mainArray.length; x++) {
                if(slot == mainArray[x]) {
                    inventory.setItem(slot, main);
                    check = true;
                    break;
                }
            }

            if(!check) {
                inventory.setItem(slot, fill);
            }
        }

        player.openInventory(inventory);
    }

    public void countDownScreen1(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 45, "Coinflip");

        ItemStack main = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 14);
        ItemStack fill = new ItemStack(Material.STAINED_GLASS_PANE, 1);

        int[] mainArray = {4, 12, 13, 22, 31, 39, 40, 41};
        boolean check = false;

        for(int slot = 0; slot < inventory.getSize(); slot++) {
            check = false;

            for(int x = 0; x < mainArray.length; x++) {
                if(slot == mainArray[x]) {
                    inventory.setItem(slot, main);
                    check = true;
                    break;
                }
            }

            if(!check) {
                inventory.setItem(slot, fill);
            }
        }

        player.openInventory(inventory);
    }

    public void activeGameGUI(Player player1, Player player2) {
        int limit = 15;

        Inventory inventory = Bukkit.createInventory(null, 45, "Coinflip");

        // Players head

        Random random = new Random();
        for(int slot = 0; slot < inventory.getSize(); slot++) {
            if(slot == 22) {
                if(random.nextInt(2) == 1) {
                    inventory.setItem(slot, getHead(player1));
                }
                inventory.setItem(slot, getHead(player2));
                continue;
            }
            ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE, 1, ((byte) random.nextInt(limit + 1)));
            inventory.setItem(slot, itemStack);
        }

        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }

    public void openMainCfGUI(Player player) {

    }

    public ItemStack getHead(Player player){
        ItemStack PlayerHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);

        SkullMeta meta = (SkullMeta) PlayerHead.getItemMeta();
        meta.setOwner(player.getName());
        meta.setDisplayName(player.getName());
        ArrayList<String> l = new ArrayList<String>();
        l.add(ChatColor.GREEN.toString() + "Du kan sælge Dette Head! /sellhead");
        meta.setLore(l);
        PlayerHead.setItemMeta(meta);

        return PlayerHead;
    }
}
