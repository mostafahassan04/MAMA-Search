package com.mamasearch.Indexer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IndexerMongoDBConnection {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection1;
    private MongoCollection<Document> collection2;
    private static final String DB_NAME = "MAMA_Search";
    private static final String COLLECTION1_NAME = "crawled_data";
    private static final String COLLECTION2_NAME = "inverted_index";


    public IndexerMongoDBConnection() {
        String uri = "mongodb://localhost:27017";
        this.mongoClient = MongoClients.create(uri);
        this.database = mongoClient.getDatabase(DB_NAME);
        this.collection1 = database.getCollection(COLLECTION1_NAME);
        this.collection2 = database.getCollection(COLLECTION2_NAME);
    }

    public void insertInvertedIndex(String Word, String url, List<Integer> positions, double score) {
        Document document = new Document();
        document.append("word", Word)
                .append("url", url)
                .append("positions", positions)
                .append("score", score);
        collection2.insertOne(document);
    }


    public List<DocumentData> getDocuments() {
        List<DocumentData> documents = new ArrayList<>();
        for (Document doc : collection1.find()) {
            String url = doc.getString("url");
            String content = doc.getString("content");
            if (url != null && content != null) {
                documents.add(new DocumentData(url, content));
            }
        }
        return documents;
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
