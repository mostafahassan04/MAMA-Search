package Crawler;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MongoDBConnection {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private static final String DB_NAME = "MAMA_Search";
    private static final String COLLECTION_NAME = "crawled_data";

    public MongoDBConnection() {
        String uri = "mongodb://localhost:27017";
        this.mongoClient = MongoClients.create(uri);
        this.database = mongoClient.getDatabase(DB_NAME);
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    public void insertCrawledPage(String url, String title, String content) {
        Document document = new Document()
                .append("url", url)
                .append("title", title)
                .append("content", content)
                .append("crawledAt", new Date());

        collection.insertOne(document);
    }


    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}