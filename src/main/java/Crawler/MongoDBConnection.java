package Crawler;

import DBClient.MongoDBClient;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;

public class MongoDBConnection {
    private MongoDatabase database;
    private MongoCollection<Document> crawledData;
    private MongoCollection<Document> urlGraph;
    private static final String DB_NAME = "MAMA_Search";
    private static final String crawledDataName = "crawled_data";
    private static final String urlGraphName = "url_graph";


    public MongoDBConnection() {
        String uri = "mongodb://localhost:27017/";
        MongoClient mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(DB_NAME);
        System.out.println("Connected to database " + this.database);
        this.crawledData = database.getCollection(crawledDataName);
        this.urlGraph = database.getCollection(urlGraphName);
    }

    public void insertCrawledPage(int id, String url, String title, String content) {
        Document document = new Document()
                .append("id", id)
                .append("url", url)
                .append("title", title)
                .append("content", content)
                .append("crawledAt", new Date());

        crawledData.insertOne(document);
    }


    public void insertUrlsGraph (Integer id, ArrayList<Integer> urlsIds) {
            Document document = new Document()
                    .append("id", id)
                    .append("extractedUrlsIds", urlsIds);
            urlGraph.insertOne(document);
    }
    public void deleteAllCrawledPages() {
        crawledData.deleteMany(new Document());
        System.out.println("Cleared all crawled pages from database");
    }

    public void deleteAllUrlGraph() {
        urlGraph.deleteMany(new Document());
        System.out.println("Cleared all URL graph data from database");
    }
}