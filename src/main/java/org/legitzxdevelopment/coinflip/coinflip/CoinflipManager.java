package org.legitzxdevelopment.coinflip.coinflip;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.legitzxdevelopment.coinflip.Coinflip;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class CoinflipManager {
    private Coinflip plugin;
    private HashMap<String, Boolean> coinflipGameList;
    private HashMap<String, Inventory> playersInCfGUI; // CF Inventory

    public CoinflipManager(Coinflip plugin) {
        this.plugin = plugin;
        coinflipGameList = new HashMap<>();
        playersInCfGUI = new HashMap<>();
    }

    public void addToList(String uuid, boolean isActive) {
        coinflipGameList.put(uuid, isActive);
    }

    public void updateList(String uuid, boolean isActive) {
        coinflipGameList.replace(uuid, coinflipGameList.get(uuid), isActive);
    }

    public void removeFromList(String uuid) {
        coinflipGameList.remove(uuid);
    }

    public boolean isActive(String uuid) { // stupid code -> but works
        try {
            if(coinflipGameList.get(uuid) == true) {
                return true;
            }
        } catch (Exception e) {
            CoinflipGame game = plugin.getDatabaseApi().getCoinflipBySecondUUID(uuid);
            if(game != null) {
                return true;
            }
        }

        return false;
    }

    public HashMap<String, Boolean> getCoinflipGames() {
        return coinflipGameList;
    }

    public void cleanUp() {
        // Cleans up memory by wiping lists
        coinflipGameList.clear();
        playersInCfGUI.clear();
    }

    // CF Inventory stuff
    public void addPlayer(String uuid, Inventory inventory) {
        playersInCfGUI.put(uuid, inventory);
    }

    public void removePlayer(String uuid) {
        playersInCfGUI.remove(uuid);
    }

    public boolean isPlayerInGUI(String uuid) {
        if(playersInCfGUI.containsKey(uuid)) {
            return true;
        }
        return false;
    }

    public HashMap<String, Inventory> getPlayersInCfGUI() {
        return playersInCfGUI;
    }

    public Inventory getInventoryByUUID(String uuid) {
        if(isPlayerInGUI(uuid)) {
            return playersInCfGUI.get(uuid);
        }
        return null;
    }

    public void refreshInventory() {
        for(String uuid : playersInCfGUI.keySet()) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));

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

            updateMainCfGUI(player, inventory);
        }
    }

    public void updateMainCfGUI(Player player, Inventory inventory) {
        int count = 0;
        if(plugin.getCoinflipManager().getCoinflipGames().keySet() != null) {
            for(String uuid : plugin.getCoinflipManager().getCoinflipGames().keySet()) {
                if(!plugin.getCoinflipManager().isActive(uuid)) {
                    // Format numbers properly
                    DecimalFormat df = new DecimalFormat("#,###");

                    String playerName = Bukkit.getPlayer(UUID.fromString(uuid)).getName();
                    CoinflipGame game = plugin.getDatabaseApi().getCoinflipByUUID(uuid);

                    ItemStack head = cacheHead(playerName);
                    ItemMeta meta = head.getItemMeta();
                    meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + playerName + "'s Game");
                    ArrayList<String> lore = new ArrayList<>();
                    lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "Bet: $" + ChatColor.WHITE + df.format(game.getPrize() / 2));
                    lore.add(ChatColor.GRAY + "\"Click to play against " + playerName + ".\"");
                    meta.setLore(lore);
                    head.setItemMeta(meta);

                    inventory.setItem(count, head);
                    count++;
                }
            }
        }

        player.openInventory(inventory);

        if(!isPlayerInGUI(player.getUniqueId().toString()))
            plugin.getCoinflipManager().addPlayer(player.getUniqueId().toString(), inventory);
    }

    public ItemStack cacheHead(String player) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(player);
        item.setItemMeta(meta);

        return item;
    }
}
