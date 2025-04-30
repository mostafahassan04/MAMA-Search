package com.mamasearch.Indexer;

import DBClient.MongoDBClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexerMongoDBConnection {
    private MongoDatabase database;
    private MongoCollection<Document> collection1;
    private MongoCollection<Document> collection2;
    private static final String COLLECTION1_NAME = "crawled_data";
    private static final String COLLECTION2_NAME = "inverted_index";


    public IndexerMongoDBConnection() {
        this.database = MongoDBClient.getDatabase();
//        System.out.println("Connected to database" + this.database);
        this.collection1 = database.getCollection(COLLECTION1_NAME);
        this.collection2 = database.getCollection(COLLECTION2_NAME);
    }

    public void insertInvertedIndex(Map<String, Map<String,WordData>> invertedIndex) {
        for (String word : invertedIndex.keySet()) {
            Map<String, WordData> docs = invertedIndex.get(word);

            Document document = new Document();
            document.append("word", word);

            for (String url : docs.keySet()) {
                document.append("url", url);
                document.append("positions", docs.get(url).getPositions());
                document.append("score", docs.get(url).getScore());
            }
            collection2.insertOne(document);
        }
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
//
//    public void close() {
//        if (mongoClient != null) {
//            mongoClient.close();
//        }
//    }
}
