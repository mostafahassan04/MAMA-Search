import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBClient {
    private static MongoClient mongoClient = null;
    private static final String CONNECTION_STRING = System.getenv("MONGO_CONNECTION_STRING");
    static {
        if (CONNECTION_STRING == null || CONNECTION_STRING.isEmpty()) {
            throw new IllegalStateException("Environment variable MONGO_CONNECTION_STRING is not set or is empty.");
        }
    }
    private static final String DATABASE_NAME = "test";

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
        return getClient().getDatabase(DATABASE_NAME);
    }

    public static void closeClient() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            System.out.println("MongoDB client closed.");
        }
    }
}