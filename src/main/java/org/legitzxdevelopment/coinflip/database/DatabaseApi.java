package org.legitzxdevelopment.coinflip.database;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.legitzxdevelopment.coinflip.Coinflip;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipConverter;
import org.legitzxdevelopment.coinflip.coinflip.CoinflipGame;

import java.util.ArrayList;
import java.util.List;

public class DatabaseApi extends DatabaseConnection {
    // Plugin this class belongs to
    Coinflip coinflip = Coinflip.getPlugin(Coinflip.class);

    // Gets main collection
    MongoCollection collection = getDatabase().getCollection(plugin.getConfig().getString("collection"));

    // Converter for serializing/deserializing
    CoinflipConverter coinflipConverter = new CoinflipConverter();

    /**
     * Description: Gets coinflip game by prize amount
     * @param prize
     * @return              CoinflipGame
     */
    @Nullable
    public CoinflipGame getCoinflipGameByPrize(long prize) {
        try {
            Document found = (Document) collection.find(new Document("prize", prize)).first();

            return coinflipConverter.deserialize(found);
        } catch (NullPointerException e) {
            return null;
        }
    }

    // Only checks player 1
    @Nullable
    public CoinflipGame getCoinflipByUUID(String uuid) {
        try {
            Document found = (Document) collection.find(new Document("player1", uuid)).first();

            return coinflipConverter.deserialize(found);
        } catch (NullPointerException e) {
            return null;
        }
    }

    // Checks for either player 1 || player 2
    public CoinflipGame getCoinflipByAnyUUID(String uuid) {
        try {
            Document found = (Document) collection.find(new Document("player1", uuid)).first();
            Document found2 = (Document) collection.find(new Document("player2", uuid)).first();

            return coinflipConverter.deserialize(found) == null ? null : coinflipConverter.deserialize(found2);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public CoinflipGame getCoinflipBySecondUUID(String uuid) {
        try {
            Document found = (Document) collection.find(new Document("player2", uuid)).first();

            return coinflipConverter.deserialize(found);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void insertCoinflipGame(CoinflipGame coinflipGame) {
        collection.insertOne(coinflipConverter.serialize(coinflipGame));
    }

    // Basically just updates player2 since thats the only thing that needs to be updated
    public void updateCoinflipGame(CoinflipGame coinflipGame) {
        Document found = (Document) collection.find(new Document("player1", coinflipGame.getPlayer1())).first();
        Bson newValue = new Document("player2", coinflipGame.getPlayer2());

        Bson updateOperationDocument = new Document("$set", newValue);
        collection.updateOne(found, updateOperationDocument);
    }

    public void deleteCoinflipGame(CoinflipGame coinflipGame) {
        collection.deleteOne(new Document("player1", coinflipGame.getPlayer1()));
    }

    public List<CoinflipGame> getAllCoinflips() {
        List<CoinflipGame> coinflipGames = new ArrayList<>();

        List<Document> documents = (List<Document>) collection.find().into(
                new ArrayList<Document>());

        for(Document document : documents){
            coinflipGames.add(coinflipConverter.deserialize(document));

            plugin.getServer().broadcastMessage(coinflipConverter.deserialize(document).getPlayer1());
        }

        return coinflipGames;
    }
}
