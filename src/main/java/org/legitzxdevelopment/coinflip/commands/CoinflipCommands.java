package org.legitzxdevelopment.coinflip.commands;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    // TODO: ADD COOLDOWNS TO COMMANDS, ALSO ADD COOLDOWNS FOR CLICKINVENTORYEVENT

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
                            player.getInventory().addItem(cacheHead(player.getName()));
                            player.sendMessage("reload");
                        }

                        if(args[0].equalsIgnoreCase("help")) {
                            player.sendMessage("help");
                        }
                    }
                } else {
                    // Open cf inventory
                    openMainCfGUI(player);
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
            // coinflipGame.setPlayer2(player.getUniqueId().toString());
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

        plugin.getCoinflipManager().refreshInventory();
    }

    public void updateToDatabase(CoinflipGame coinflipGame) {
        plugin.getDatabaseApi().updateCoinflipGame(coinflipGame);
        plugin.getCoinflipManager().updateList(coinflipGame.getPlayer1(), true);

        //plugin.getCoinflipManager().refreshInventory();
    }

    public void deleteFromDatabase(CoinflipGame coinflipGame) {
        plugin.getDatabaseApi().deleteCoinflipGame(coinflipGame);
        plugin.getCoinflipManager().removeFromList(coinflipGame.getPlayer1());

        plugin.getCoinflipManager().refreshInventory();
    }

    public void startGame(CoinflipGame game) {
        Player player1 = Bukkit.getPlayer(UUID.fromString(game.getPlayer1()));

//        try {
//            if(plugin.getCoinflipManager().isPlayerInGUI(player1.getUniqueId().toString())) {
//                plugin.getCoinflipManager().removePlayer(player1.getUniqueId().toString());
//            }
//        } catch (NullPointerException e) { }
//
//        player1.closeInventory();

        Player player2 = Bukkit.getPlayer(UUID.fromString(game.getPlayer2()));

//        try {
//            if(plugin.getCoinflipManager().isPlayerInGUI(player2.getUniqueId().toString())) {
//                plugin.getCoinflipManager().removePlayer(player2.getUniqueId().toString());
//            }
//        } catch (NullPointerException e) { }
//
//        player2.closeInventory();

        ItemStack player1Stack = cacheHead(player1.getName());
        ItemStack player2Stack = cacheHead(player2.getName());


        new Countdown(3, plugin) {
            @Override
            public void count(int current) {
                player1.playSound(player1.getLocation(), Sound.NOTE_BASS, 50, 1);
                player2.playSound(player2.getLocation(), Sound.NOTE_BASS, 50, 1);
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
                    spinCoinflip(player1, player2, game);
                }
            }
        }.start();
    }

    public void spinCoinflip(Player player1, Player player2, CoinflipGame game) {
        final boolean player1Wins;

        if(getRandNum() == 1) {
            // player1 wins
            player1Wins = true;
        } else {
            // player2 wins
            player1Wins = false;
        }

        plugin.getServer().broadcastMessage("Player1wins: " + player1Wins);

        new FastCountdown(40, plugin) {
            @Override
            public void count(int current) {
                // TODO: ADD SOUNDS
                if(current % 2 == 0) {

                    if(player1Wins) {
                        // Display one player
                        player1.getServer().broadcastMessage("player1: " + current);
                        activeGameGUI(player1, player2, cacheHead(player1.getName()));
                        if(current == 0) {
                            rewardWinner(player1, player2, game);
                        }
                    } else {
                        // Display other player
                        player2.getServer().broadcastMessage("player2: " + current);
                        activeGameGUI(player1, player2, cacheHead(player2.getName()));
                        if(current == 0) {
                            rewardWinner(player2, player1, game);
                        }
                    }

                } else {

                    if(player1Wins) {
                        // Display other player
                        player2.getServer().broadcastMessage("player2: " + current);
                        activeGameGUI(player1, player2, cacheHead(player2.getName()));
                        if(current == 0) {
                            rewardWinner(player1, player2, game);
                        }
                    } else {
                        // Display one player
                        player1.getServer().broadcastMessage("player1: " + current);
                        activeGameGUI(player1, player2, cacheHead(player1.getName()));
                        if(current == 0) {
                            rewardWinner(player2, player1, game);
                        }
                    }
                }
            }
        }.start();
    }

    public int getRandNum() {
        Random random = new Random();
        return random.nextInt(2);
    }

    public void rewardWinner(Player winner, Player loser, CoinflipGame game) {
        winner.getServer().broadcastMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + winner.getName() + ChatColor.YELLOW + " has defeated " + ChatColor.RED + loser.getName() + ChatColor.YELLOW + " in a $" + ChatColor.RED + game.getPrize() + ChatColor.YELLOW + " coinflip!");

        // Waits 3 seconds, then deletes from database && closes game
        new Countdown(3, plugin) {
            @Override
            public void count(int current) {
                if(current == 0) {
                    deleteFromDatabase(game);
                    closeInventory(winner);
                    closeInventory(loser);
                }

            }
        }.start();

        EconomyResponse economyResponse = plugin.getEcon().depositPlayer(winner, game.getPrize());

        if(economyResponse.transactionSuccess()) {
            return;
        } else {
            winner.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "There was a problem depositing $" + game.getPrize() + " into your account!");
        }
    }

    public void closeInventory(Player player) {
        player.closeInventory();
    }

    public void activeGameGUI(Player player1, Player player2, ItemStack playerSkull) {
        int limit = 15;

        Inventory inventory = Bukkit.createInventory(null, 45, "Coinflip");

        // Players head
        Random random = new Random();
        for(int slot = 0; slot < inventory.getSize(); slot++) {
            if(slot == 22) {
                inventory.setItem(slot, playerSkull);
                continue;
            }
            ItemStack itemStack = new ItemStack(Material.STAINED_GLASS_PANE, 1, ((byte) random.nextInt(limit + 1)));
            inventory.setItem(slot, itemStack);
        }

        player1.openInventory(inventory);
        player2.openInventory(inventory);
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

    public ItemStack cacheHead(String player) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(player);
        item.setItemMeta(meta);

        return item;
    }

    public void openMainCfGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, ChatColor.DARK_AQUA + " " + ChatColor.BOLD + "GAMES AVAILABLE");

        // <-- BASE CF GUI -->
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1);

        ItemStack echest = new ItemStack(Material.ENDER_CHEST, 1);
        ItemMeta echestMeta = glass.getItemMeta();
        echestMeta.setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Coin Flip Help");
        ArrayList<String> echestLore = new ArrayList<>();
        echestLore.add("Want to make a Coin Flip?");
        echestLore.add("Type " + ChatColor.YELLOW + "/cf create <bet>");
        echestLore.add("");
        echestLore.add("Need more help? Type " + ChatColor.YELLOW + "/cf help");
        echestMeta.setLore(echestLore);
        echest.setItemMeta(echestMeta);

        inventory.setItem(45, glass);
        inventory.setItem(46, glass);
        inventory.setItem(47, glass);
        inventory.setItem(48, glass);
        inventory.setItem(49, echest);
        inventory.setItem(50, glass);
        inventory.setItem(51, glass);
        inventory.setItem(52, glass);
        inventory.setItem(53, glass);
        // <!-- BASE CF GUI -->
        plugin.getCoinflipManager().updateMainCfGUI(player, inventory);
        //player.openInventory(inventory);
    }


}

// https://www.spigotmc.org/threads/loading-inventory-with-player-head-crash-the-server.348970/