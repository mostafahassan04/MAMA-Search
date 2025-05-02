package com.mamasearch.Indexer;

import DBClient.MongoDBClient;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexerMongoDBConnection {
    private MongoDatabase database;
    private static final String DB_NAME = "MAMA_Search";
    private MongoCollection<Document> crawledDataCollection;
    private MongoCollection<Document> invertedIndexCollection;
    private static final String crawledDataCollectionName = "crawled_data";
    private static final String invertedIndexCollectionName = "inverted_index";


    public IndexerMongoDBConnection() {
        String uri = "mongodb://localhost:27017/";
        MongoClient mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(DB_NAME);
        System.out.println("Connected to database " + this.database);
        this.crawledDataCollection = database.getCollection(crawledDataCollectionName);
        this.invertedIndexCollection = database.getCollection(invertedIndexCollectionName);
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



    public List<DocumentData> getDocuments() {
        List<DocumentData> documents = new ArrayList<>();
        for (Document doc : crawledDataCollection.find()) {
            Integer ID = doc.getInteger("id");
            String content = doc.getString("content");
            if (content != null) {
                documents.add(new DocumentData(ID, content));
            }
        }
        return documents;
    }

    public void deleteAllInvertedIndex() {
        invertedIndexCollection.deleteMany(new Document());
        System.out.println("Cleared all inverted index data from database");
    }
//
//    public void close() {
//        if (mongoClient != null) {
//            mongoClient.close();
//        }
//    }
}
