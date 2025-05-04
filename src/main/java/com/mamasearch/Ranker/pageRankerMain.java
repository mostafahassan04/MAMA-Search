package com.mamasearch.Ranker;

import DBClient.MongoDBClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class pageRankerMain {
    public static void main(String[] args) {
        MongoDatabase database = MongoDBClient.getDatabase();
        String COLLECTION_NAME = "url_graph";
        String COLLECTION_NAME2 = "crawled_data";
        MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
        MongoCollection<Document> collection2 = database.getCollection(COLLECTION_NAME2);

        Map<Integer, ArrayList<Integer>> pagesGraph = new HashMap<>();

        FindIterable<Document> documents = collection.find();
        for (Document doc : documents) {
            Integer id = doc.getInteger("id");
            List<Integer> links = doc.getList("extractedUrlsIds", Integer.class);
            pagesGraph.put(id, new ArrayList<>(links));
        }
        PageRanker pageRanker = new PageRanker();
        long start = System.currentTimeMillis();
        pageRanker.rank(pagesGraph);
        pageRanker.insertPages(collection2);
        long end = System.currentTimeMillis();

    }
}
