package com.mamasearch.Ranker;

import DBClient.MongoDBClient;
import com.mamasearch.Utils.ProcessorData;
import com.mamasearch.Utils.SnippetGenerator;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;


import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.*;


public class Ranker {


    public List<ScoredDocument> rankDocument(ProcessorData processorData) throws InterruptedException {


        Map<Integer, Double> id_scoreMap = new HashMap<>();
        ArrayList<Document> documents = processorData.relevantDocuments;
        for (Document document : documents) {
            int id = document.getInteger("id");
            double score = document.getDouble("score");
            id_scoreMap.put(id, id_scoreMap.getOrDefault(id, 0.0) + score);
        }

        Set<Integer> relevantDocIds = id_scoreMap.keySet();
        Map<Integer, Document> docDetailsMap = new HashMap<>();
        MongoDatabase database = MongoDBClient.getDatabase();
        String COLLECTION_NAME1 = "id_data";
        if (!relevantDocIds.isEmpty()) { // Avoid empty query if map is empty
            MongoCollection<Document> collection1 = database.getCollection(COLLECTION_NAME1); // Get your collection

            // Define which fields to retrieve
            Bson projection = fields(include("id", "popularityScore", "url", "title"), excludeId()); // Exclude MongoDB's default _id if not needed

            // Execute the single query
            try (MongoCursor<Document> cursor = collection1.find(in("id", relevantDocIds))
                    .projection(projection)
                    .iterator()) {
                // Load results into an in-memory map for fast lookup
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    docDetailsMap.put(doc.getInteger("id"), doc);
                }
            }
        }



        AtomicLong totalsnippet = new AtomicLong(0);
        long startr = System.currentTimeMillis();

        List<ScoredDocument> scoredDocumentsList = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        SnippetGenerator snippetGenerator = new SnippetGenerator(); // Reuse

        for (Map.Entry<Integer, Double> entry : id_scoreMap.entrySet()) {
            int docId = entry.getKey();
            double relevanceScore = entry.getValue();

            executor.submit(() -> {
                Document doc = docDetailsMap.get(docId);
                if (doc == null) {
                    System.err.println("Warning: Details not found for doc ID: " + docId);
                    return;
                }

                Double popularityScore = doc.getDouble("popularityScore");
                double pageRankScore = (popularityScore != null) ? popularityScore : 0.0;
                String url = doc.getString("url");
                String title = doc.getString("title");

                double alpha = 0.7, beta = 0.3;
                double finalScore = alpha * relevanceScore + beta * pageRankScore;

                int TARGET_LENGTH = 250;

                long start = System.currentTimeMillis();
                String snippet = snippetGenerator.generateSnippet(docId, processorData.words, TARGET_LENGTH);
                long end = System.currentTimeMillis();
                totalsnippet.addAndGet(end - start);

                ScoredDocument scoredDocument = new ScoredDocument(url, title, snippet, finalScore);
                scoredDocumentsList.add(scoredDocument);
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endr = System.currentTimeMillis();
        System.out.println("Total ranking loop time: " + (endr - startr) + " ms");
        System.out.println("Total snippet generation time (within loop): " + totalsnippet + " ms");

        Collections.sort(scoredDocumentsList);
        return scoredDocumentsList;
    }




        public static void main (String[]args){

//        SnippetGenerator snippetGenerator = new SnippetGenerator();
//
//        String[] arr = {"field","method","iron"};
//
//        long start = System.currentTimeMillis();
//        String s = snippetGenerator.generateSnippet(1,arr,250);
//        long end = System.currentTimeMillis();

//            System.out.println(s);
//            System.out.println("time taken" + (end-start));
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
            for(Map.Entry<Integer,ArrayList<Integer>> entry : pagesGraph.entrySet()){
                System.out.print(entry.getKey()+": [");
                for(Integer i : entry.getValue()){
                    System.out.print(i+", ");

                }
                System.out.println("]");
            }
//
//
            PageRanker pageRanker = new PageRanker();
//        long start = System.nanoTime();
            long start = System.currentTimeMillis();
            pageRanker.rank(pagesGraph);
//        long end = System.nanoTime();
            pageRanker.insertPages(collection2);
            long end = System.currentTimeMillis();

pageRanker.printPages();
            System.out.println("Time taken to rank: " + (end - start) + " ms");


        }
    }
