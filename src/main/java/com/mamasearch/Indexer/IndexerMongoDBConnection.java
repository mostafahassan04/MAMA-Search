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

    public void insertInvertedIndex(Map<String, Map<String, WordData>> invertedIndex) {
        System.out.println("Total number of words: " + invertedIndex.size());
        for (String word : invertedIndex.keySet()) {
            Map<String, WordData> docs = invertedIndex.get(word);

            List<Document> urlsList = new ArrayList<>();

            for (Map.Entry<String, WordData> entry : docs.entrySet()) {
                String url = entry.getKey();
                WordData wordData = entry.getValue();

                Document urlDoc = new Document("url", url)
                        .append("positions", wordData.getPositions())
                        .append("score", wordData.getScore());

                urlsList.add(urlDoc);
            }

            Document document = new Document("word", word)
                    .append("urls", urlsList);

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
