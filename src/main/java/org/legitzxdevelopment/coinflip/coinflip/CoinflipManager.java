package org.legitzxdevelopment.coinflip.coinflip;

import org.legitzxdevelopment.coinflip.Coinflip;

import java.util.HashMap;

public class CoinflipManager {
    private Coinflip plugin;
    private HashMap<String, Boolean> coinflipGameList;

    public CoinflipManager(Coinflip plugin) {
        this.plugin = plugin;
        coinflipGameList = new HashMap<>();
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

    public boolean isActive(String uuid) {
        return coinflipGameList.get(uuid
        );
    }

    public HashMap<String, Boolean> getCoinflipGames() {
        return coinflipGameList;
    }

    public void cleanUp() {
        // Cleans up memory by wiping hashmap
        coinflipGameList.clear();
    }
}
