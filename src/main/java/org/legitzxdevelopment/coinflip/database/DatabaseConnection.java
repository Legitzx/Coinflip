package org.legitzxdevelopment.coinflip.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.legitzxdevelopment.coinflip.Coinflip;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private MongoDatabase database;
    Coinflip plugin = Coinflip.getPlugin(Coinflip.class);

    public DatabaseConnection() {
        MongoClient mongoClient = MongoClients.create(
                plugin.getConfig().getString("uri"));
        database = mongoClient.getDatabase(plugin.getConfig().getString("database"));

        muteLogger();
    }

    /**
     * Description: Prevents console spam due to MongoDB Logging.
     */
    public void muteLogger() {
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
