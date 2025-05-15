package com.mamasearch.Indexer;

import DBClient.MongoDBClient;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexerMongoDBConnection {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private static final String DB_NAME = "MAMA_Search";
    private MongoCollection<Document> crawledDataCollection;
    private MongoCollection<Document> invertedIndexCollection;
    private MongoCollection<Document> lastCrawledDataCollection;
    private static final String crawledDataCollectionName = "crawled_data";
    private static final String invertedIndexCollectionName = "inverted_index";
    private static final String lastCrawledDataCollectionName = "last_crawled_data";


    public IndexerMongoDBConnection() {
        String uri = "mongodb://localhost:27017/";
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(DB_NAME);
        System.out.println("Connected to database " + this.database);
        this.crawledDataCollection = database.getCollection(crawledDataCollectionName);
        this.invertedIndexCollection = database.getCollection(invertedIndexCollectionName);
        this.lastCrawledDataCollection = database.getCollection(lastCrawledDataCollectionName);
    }

    public void insertInvertedIndex(Map<String, Map<Integer, WordData>> invertedIndex) {
        System.out.println("Total number of words: " + invertedIndex.size());
        for (String word : invertedIndex.keySet()) {
            Map<Integer, WordData> docs = invertedIndex.get(word);

            List<Document> IDsList = new ArrayList<>();

            for (Map.Entry<Integer, WordData> entry : docs.entrySet()) {
                Integer ID = entry.getKey();
                WordData wordData = entry.getValue();

                Document DocID = new Document("id", ID)
                        .append("positions", wordData.getPositions())
                        .append("score", wordData.getScore());

                IDsList.add(DocID);
            }

            Document document = new Document("word", word)
                    .append("ids", IDsList);

            invertedIndexCollection.insertOne(document);
        }
    }



    public List<DocumentData> getDocuments(boolean newIndex) {
        List<DocumentData> documents = new ArrayList<>();
        int maxID = 0;
        if (newIndex) {
            deleteAllInvertedIndex();
            for (Document doc : crawledDataCollection.find()) {
                Integer ID = doc.getInteger("id");
                String content = doc.getString("content");
                if (content != null) {
                    documents.add(new DocumentData(ID, content));
                }
                if (ID > maxID) {
                    maxID = ID;
                }
            }
        }
        else {
           for (Document doc : crawledDataCollection.find()) {
               Document lastCrawledDoc = lastCrawledDataCollection.find().first();
               if (lastCrawledDoc != null) {
                   maxID = lastCrawledDoc.getInteger("maxID");
               }
               Integer ID = doc.getInteger("id");
               String content = doc.getString("content");
               if (content != null && ID > maxID) {
                   documents.add(new DocumentData(ID, content));
               }
               if (ID > maxID) {
                   maxID = ID;
               }
           }
        }
        lastCrawledDataCollection.updateOne(
                new Document(), // Match all (since only one doc exists)
                new Document("$set", new Document("maxID", maxID)),
                new UpdateOptions().upsert(true)
        );
        return documents;
    }


    public void deleteAllInvertedIndex() {
        invertedIndexCollection.drop();
        database.createCollection(invertedIndexCollectionName);
        this.invertedIndexCollection = database.getCollection(invertedIndexCollectionName);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
