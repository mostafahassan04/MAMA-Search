package com.mamasearch.Ranker;

import DBClient.MongoDBClient;
import com.mamasearch.Utils.ProcessorData;
import com.mamasearch.Utils.SnippetGenerator;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;


import java.util.*;


public class Ranker {


    public List<ScoredDocument> rankDocument(ProcessorData processorData) {


        Map<Integer, Double> id_scoreMap = new HashMap<>();
        ArrayList<Document> documents = processorData.relevantDocuments;
        List<ScoredDocument> scoredDocumentsList = new ArrayList<ScoredDocument>();
        for (Document document : documents) {
            int id = document.getInteger("id");
            double score = document.getDouble("score");
            id_scoreMap.put(id, id_scoreMap.getOrDefault(id, 0.0) + score);
        }


        MongoDatabase database = MongoDBClient.getDatabase();
        String COLLECTION_NAME1 = "id_data";

        MongoCollection<Document> collection1 = database.getCollection(COLLECTION_NAME1);


        for (Map.Entry<Integer, Double> entry : id_scoreMap.entrySet()) {
            Document query = new Document("id", entry.getKey());
            Document doc = collection1.find(query).first();
            Double popularityScore = doc.getDouble("popularityScore");
            double pageRankScore = (popularityScore != null) ? popularityScore : 0.0; // Assign default score if not found
            double alpha = 0.7, beta = 0.3;
            double finalScore = alpha * entry.getValue() + beta * pageRankScore;
            // generating snippets
            int TARGET_LENGTH = 250;
            SnippetGenerator snippetGenerator = new SnippetGenerator();

            String snippet = snippetGenerator.generateSnippet(entry.getKey(), processorData.words, TARGET_LENGTH);

            ScoredDocument scoredDocument = new ScoredDocument(doc.getString("url"), doc.getString("title"), snippet, finalScore);
            scoredDocumentsList.add(scoredDocument);
        }


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
