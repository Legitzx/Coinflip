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
import org.legitzxdevelopment.coinflip.Coinflip;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipGame;
import org.legitzxdevelopment.coinflip.settings.Countdown;
import org.legitzxdevelopment.coinflip.settings.FastCountdown;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class CoinflipCommands implements CommandExecutor {
    // Plugin this class belongs to
    Coinflip plugin = Coinflip.getPlugin(Coinflip.class);

    // TODO: ADD COOLDOWNS TO COMMANDS, ALSO ADD COOLDOWNS FOR CLICKINVENTORYEVENT - refer to simplestats

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("cf") || command.getName().equalsIgnoreCase("coinflip")) {
            if(sender instanceof Player) {
                Player player = (Player) sender;

                if(args.length > 0) {
                    if(args.length >= 1) {
                        if(args[0].equalsIgnoreCase("create")) {
                            if(args.length >= 2) {
                                if(args[1].matches("[0-9]+")) {
                                    // Cooldown stuff
                                    if(plugin.getCooldownManager().hasCooldown(player)) {
                                        if(plugin.getCooldownManager().getCooldownTime(player) > 0) {
                                            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You still have " + plugin.getCooldownManager().getCooldownTime(player) + " seconds left untill you can execute /cf create");
                                            return true;
                                        }
                                        if(plugin.getCooldownManager().getCooldownTime(player) <= 0) {
                                            plugin.getCooldownManager().removeFromHashMap(player);
                                        }
                                    }
                                    plugin.getCooldownManager().addCooldown(player);

                                    if(Long.parseLong(args[1]) >= plugin.getConfig().getInt("coinflip.min") && Long.parseLong(args[1]) <= plugin.getConfig().getInt("coinflip.max")) {
                                        createGame(player, Long.parseLong(args[1]));
                                    } else {
                                        DecimalFormat df = new DecimalFormat("#,###");
                                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Minimum bet amount is $" + df.format(plugin.getConfig().getInt("coinflip.min")) + " and max bet amount is $" + df.format(plugin.getConfig().getInt("coinflip.max")) + "!");
                                    }
                                } else {
                                    player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Wrong arguments! Visit /cf for more help.");
                                }
                            } else {
                                player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Missing bet amount! Visit /cf for more help.");
                            }
                        }


                        if(args[0].equalsIgnoreCase("cancel")) {
                            CoinflipGame coinflipGame = plugin.getDatabaseApi().getCoinflipByUUID(player.getUniqueId().toString());

                            if(coinflipGame != null) {
                                if(!plugin.getCoinflipManager().isActive(coinflipGame.getPlayer1())) {
                                    EconomyResponse response = plugin.getEcon().depositPlayer(player, (coinflipGame.getPrize() / 2));

                                    if(response.transactionSuccess()) {
                                        deleteFromDatabase(coinflipGame);
                                        DecimalFormat df = new DecimalFormat("#,###");
                                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "Successfully cancelled coinflip wager for $" + df.format(coinflipGame.getPrize() / 2));
                                    } else {
                                        player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "There was a problem removing you coinflip!");
                                    }
                                    return true;
                                } else {
                                    player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "Seems that you are already in an active game. Visit /cf for more help.");
                                }
                            } else {
                                player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You do not have a CF up! Visit /cf for more help.");
                            }
                        }



                        if(args[0].equalsIgnoreCase("reload")) {
                            if(player.hasPermission("coinflip.reload")) {
                                plugin.reloadConfig();
                                player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "Reloaded!");
                                return true;
                            }
                        }

                        if(args[0].equalsIgnoreCase("help")) {
                            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.AQUA + "Visit /cf for help.");
                        }
                    }
                } else {
                    // Open cf inventory
                    openMainCfGUI(player);
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
            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + "You already have a Coinflip up! Do /cf cancel to cancel your current wager.");
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

                player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "Coinflip wager has started!");
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

            DecimalFormat df = new DecimalFormat("#,###");
            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GREEN + "New coinflip proposal created for $" + df.format(betAmount));
            player.sendMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.GRAY + "Use " + ChatColor.UNDERLINE + "/cf cancel" + ChatColor.RESET + ChatColor.GRAY + " to cancel your proposal");
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

        Player player2 = Bukkit.getPlayer(UUID.fromString(game.getPlayer2()));

        //plugin.getCoinflipManager().refreshInventory();

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

        plugin.getCoinflipManager().refreshInventory();

        if(getRandNum() == 1) {
            // player1 wins
            player1Wins = true;
        } else {
            // player2 wins
            player1Wins = false;
        }

        try { // Player is online
            ItemStack player1Head = plugin.getCoinflipManager().getHead(player1.getUniqueId().toString());
            ItemMeta player1Meta = player1Head.getItemMeta();
            player1Meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "HEADS");
            ArrayList<String> player1Lore = new ArrayList<>();
            player1Lore.add(ChatColor.YELLOW + "" + ChatColor.YELLOW + "Player: " + ChatColor.WHITE + player1.getName());
            player1Meta.setLore(player1Lore);
            player1Head.setItemMeta(player1Meta);

            ItemStack player2Head = plugin.getCoinflipManager().getHead(player2.getUniqueId().toString());
            ItemMeta player2Meta = player2Head.getItemMeta();
            player2Meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "TAILS");
            ArrayList<String> player2Lore = new ArrayList<>();
            player2Lore.add(ChatColor.YELLOW + "" + ChatColor.YELLOW + "Player: " + ChatColor.WHITE + player2.getName());
            player2Meta.setLore(player2Lore);
            player2Head.setItemMeta(player2Meta);



            new FastCountdown(40, plugin) {
                @Override
                public void count(int current) {
                    player1.playSound(player1.getLocation(), Sound.LEVEL_UP, 100, 2);
                    player2.playSound(player1.getLocation(), Sound.LEVEL_UP, 100, 2);
                    if(current % 2 == 0) {

                        if(player1Wins) {
                            // Display one player
                            //player1.getServer().broadcastMessage("player1: " + current);
                            activeGameGUI(player1, player2, player1Head);
                            if(current == 0) {
                                rewardWinner(player1, player2, game);
                            }
                        } else {
                            // Display other player
                            //player2.getServer().broadcastMessage("player2: " + current);
                            activeGameGUI(player1, player2, player2Head);
                            if(current == 0) {
                                rewardWinner(player2, player1, game);
                            }
                        }

                    } else {

                        if(player1Wins) {
                            // Display other player
                            //player2.getServer().broadcastMessage("player2: " + current);
                            activeGameGUI(player1, player2, player2Head);
                            if(current == 0) {
                                rewardWinner(player1, player2, game);
                            }
                        } else {
                            // Display one player
                            //player1.getServer().broadcastMessage("player1: " + current);
                            activeGameGUI(player1, player2, player1Head);
                            if(current == 0) {
                                rewardWinner(player2, player1, game);
                            }
                        }
                    }
                }
            }.start();
        } catch (NullPointerException e) { // Player is offline
            new FastCountdown(40, plugin) {
                @Override
                public void count(int current) {
                    if(current % 2 == 0) {

                        if(player1Wins) {
                            // Display one player
                            //player1.getServer().broadcastMessage("player1: " + current);
                            activeGameGUI(player1, player2, plugin.getCoinflipManager().getHead(player1.getUniqueId().toString()));
                            if(current == 0) {
                                rewardWinner(player1, player2, game);
                            }
                        } else {
                            // Display other player
                            //player2.getServer().broadcastMessage("player2: " + current);
                            activeGameGUI(player1, player2, plugin.getCoinflipManager().getHead(player2.getUniqueId().toString()));
                            if(current == 0) {
                                rewardWinner(player2, player1, game);
                            }
                        }

                    } else {

                        if(player1Wins) {
                            // Display other player
                            //player2.getServer().broadcastMessage("player2: " + current);
                            activeGameGUI(player1, player2, plugin.getCoinflipManager().getHead(player2.getUniqueId().toString()));
                            if(current == 0) {
                                rewardWinner(player1, player2, game);
                            }
                        } else {
                            // Display one player
                            //player1.getServer().broadcastMessage("player1: " + current);
                            activeGameGUI(player1, player2, plugin.getCoinflipManager().getHead(player1.getUniqueId().toString()));
                            if(current == 0) {
                                rewardWinner(player2, player1, game);
                            }
                        }
                    }
                }
            }.start();
        }
    }

    public void rewardWinner(Player winner, Player loser, CoinflipGame game) {
        DecimalFormat df = new DecimalFormat("#,###");
        winner.getServer().broadcastMessage(plugin.getUtils().INGAME_PREFIX + ChatColor.RED + winner.getName() + ChatColor.YELLOW + " has defeated " + ChatColor.RED + loser.getName() + ChatColor.YELLOW + " in a $" + ChatColor.RED + df.format(game.getPrize()) + ChatColor.YELLOW + " coinflip!");

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

    public int getRandNum() {
        Random random = new Random();
        return random.nextInt(2);
    }

    public void closeInventory(Player player) {
        player.closeInventory();
    }

    public void activeGameGUI(Player player1, Player player2, ItemStack playerSkull) {
        int limit = 15;

        Inventory inventory = Bukkit.createInventory(null, 45, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + plugin.getConfig().getString("server.name"));

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
        Inventory inventory = Bukkit.createInventory(player, 45, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + plugin.getConfig().getString("server.name"));

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
        Inventory inventory = Bukkit.createInventory(player, 45, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + plugin.getConfig().getString("server.name"));

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
        Inventory inventory = Bukkit.createInventory(player, 45, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + plugin.getConfig().getString("server.name"));

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

    public void openMainCfGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, ChatColor.DARK_AQUA + " " + ChatColor.BOLD + "GAMES AVAILABLE");

        DecimalFormat df = new DecimalFormat("#,###");

        // <-- BASE CF GUI -->
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1);

        ItemStack echest = new ItemStack(Material.ENDER_CHEST, 1);
        ItemMeta echestMeta = echest.getItemMeta();
        echestMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Information");
        ArrayList<String> echestLore = new ArrayList<>();
        echestLore.add(ChatColor.WHITE + "Click on someone's head to accept");
        echestLore.add(ChatColor.WHITE + "their" + ChatColor.GREEN + " wager " + ChatColor.WHITE + "and challenge them");
        echestLore.add(ChatColor.WHITE +"to a" + ChatColor.GREEN + " 50/50 " + ChatColor.WHITE + "coin toss - afterwards");
        echestLore.add(ChatColor.WHITE + "the" + ChatColor.GREEN + " winner takes all " + ChatColor.WHITE + "(both bets combined)");
        echestLore.add("");
        echestLore.add(ChatColor.WHITE + "Start a" + ChatColor.GREEN + " new wager " + ChatColor.WHITE + "of your own");
        echestLore.add(ChatColor.WHITE + "with" + ChatColor.GOLD + " /cf create <bet amount> " + ChatColor.WHITE + "where");
        echestLore.add(ChatColor.WHITE + "the bet amount is between " + ChatColor.GREEN + "$" + df.format(plugin.getConfig().getInt("coinflip.min")));
        echestLore.add(ChatColor.WHITE + "and " + ChatColor.GREEN + "$" + df.format(plugin.getConfig().getInt("coinflip.max")));
        echestLore.add("");
        echestLore.add(ChatColor.WHITE + "You can" + ChatColor.GREEN + " cancel an existing Coinflip");
        echestLore.add(ChatColor.WHITE + "proposal with" + ChatColor.GOLD + " /cf cancel");
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