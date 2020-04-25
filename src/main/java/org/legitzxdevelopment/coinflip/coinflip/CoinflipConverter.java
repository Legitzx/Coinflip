package org.legitzxdevelopment.coinflip.coinflip;

import org.bson.Document;

public class CoinflipConverter {

    public CoinflipGame deserialize(Document document) {
        // Collect fields
        String player1UUID = document.getString("player1");
        String player2UUID = document.getString("player2");
        long prize = document.getLong("prize");
        long timeCreated = document.getLong("timeCreated");

        // Create Object
        CoinflipGame coinflipGame = new CoinflipGame(player1UUID, player2UUID, prize, timeCreated);

        return coinflipGame;
    }

    public Document serialize(CoinflipGame coinflipGame) {
        Document doc = new Document();

        doc.put("player1", coinflipGame.getPlayer1());
        doc.put("player2", coinflipGame.getPlayer2());
        doc.put("prize", coinflipGame.getPrize());
        doc.put("timeCreated", coinflipGame.getTimeCreated());

        return doc;
    }
}
