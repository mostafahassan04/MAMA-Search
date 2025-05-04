package DBClient;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBClient {
    private static MongoClient mongoClient = null;
    private static final String CONNECTION_STRING = "mongodb://localhost:27017/";
    private static final String DATABASE_NAME = "MAMA_Search";

    private MongoDBClient() {}

    public static MongoClient getClient() {
        if (mongoClient == null) {
            synchronized (MongoDBClient.class) {
                if (mongoClient == null) {
                    ServerApi serverApi = ServerApi.builder()
                            .version(ServerApiVersion.V1)
                            .build();
                    MongoClientSettings settings = MongoClientSettings.builder()
                            .applyConnectionString(new ConnectionString(CONNECTION_STRING))
                            .serverApi(serverApi)
                            .build();
                    mongoClient = MongoClients.create(settings);
                }
            }
        }
        return mongoClient;
    }

    public static MongoDatabase getDatabase() {
        MongoClient m = getClient();
        MongoDatabase db = m.getDatabase(DATABASE_NAME);
        return db;
    }

    public static void closeClient() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            System.out.println("MongoDB client closed.");
        }
    }
}