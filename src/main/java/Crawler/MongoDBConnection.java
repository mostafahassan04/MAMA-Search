package Crawler;

import DBClient.MongoDBClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;

public class MongoDBConnection {
    private MongoDatabase database;
    private MongoCollection<Document> collection1;
    private MongoCollection<Document> collection2;
    private static final String DB_NAME = "MAMA_Search";
    private static final String COLLECTION1_NAME = "crawled_data";
    private static final String COLLECTION2_NAME = "url_graph";


    public MongoDBConnection() {
        this.database = MongoDBClient.getDatabase();
        System.out.println("Connected to database " + this.database);
        this.collection1 = database.getCollection(COLLECTION1_NAME);
        this.collection2 = database.getCollection(COLLECTION2_NAME);
    }

    public void insertCrawledPage(String url, String title, String content) {
        Document document = new Document()
                .append("url", url)
                .append("title", title)
                .append("content", content)
                .append("crawledAt", new Date());

        collection1.insertOne(document);
    }


    public void insertUrlsGraph (String url, ArrayList<String> urls) {
            Document document = new Document()
                    .append("url", url)
                    .append("extractedUrls", urls);
            collection2.insertOne(document);
    }
//{ word: string, urls: [{url: string, positions:[int], score: double}]}
//
//    public void close() {
//        if (MongoDBClient != null) {
//            MongoDBClient.close();
//        }
//    }
}