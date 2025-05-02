package com.mamasearch.Ranker;

import DBClient.MongoDBClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.*;


public class Ranker {
    private final int MAX_ITERATIONS = 50;
    private Map<String, Integer> documentFrequencies; //term -> no documents containing it
    private Integer totalNumberOfDocuments;
    private Map<String, Page> pages = new HashMap<>();

    Ranker(Integer totalNumberOfDocuments, Map<String, Integer> documentFrequencies) {
        this.totalNumberOfDocuments = totalNumberOfDocuments;
        this.documentFrequencies = documentFrequencies;
    }

    public static void main(String[] args) {

        MongoDatabase database = MongoDBClient.getDatabase();
        String COLLECTION_NAME = "url_graph";
        MongoCollection<org.bson.Document> collection = database.getCollection(COLLECTION_NAME);

        Map<Integer, ArrayList<Integer>> pagesGraph = new HashMap<>();

        FindIterable<org.bson.Document> documents = collection.find();
        for (org.bson.Document doc : documents) {
            Integer id = Integer.valueOf(doc.getString("id"));
            List<Integer> links = doc.getList("extractedUrls", Integer.class);
            pagesGraph.put(id, new ArrayList<>(links));
        }


        PageRanker pageRanker = new PageRanker();
//        long start = System.nanoTime();
        long start = System.currentTimeMillis();
        pageRanker.rank(pagesGraph);
//        long end = System.nanoTime();
        long end = System.currentTimeMillis();

        pageRanker.print(pagesGraph);

        System.out.println("Time taken to rank: " + (end - start) + " ms");


    }

    public List<ScoredDocument> rankDocument(List<String> queryTerms, List<Document> documents) {

        double alpha = 0.7, beta = 0.3;
        List<ScoredDocument> scoredDocumentsList = new ArrayList<ScoredDocument>();
        for (Document document : documents) {
            double score = 0.0;
            for (String queryTerm : queryTerms) {
                score += calculateTF(document, queryTerm) * calculateIDF(queryTerm);
            }
            ScoredDocument s = new ScoredDocument(document);
            Page page = pages.get(document.getId());
            double pageRankScore = (page != null) ? page.getPageRank() : 0.0; // Assign default score if not found
            double finalScore = alpha * score + beta * pageRankScore;
            s.setScore(finalScore);
            scoredDocumentsList.add(s);
        }
        Collections.sort(scoredDocumentsList);
        return scoredDocumentsList;
    }

    public Double calculateTF(Document document, String term) {
        int titleFreq = document.getTitleTermFreq(term) != null ? document.getTitleTermFreq(term) : 0;
        int headerFreq = document.getHeaderTermFreq(term) != null ? document.getHeaderTermFreq(term) : 0;
        int bodyFreq = document.getBodyTermFreq(term) != null ? document.getBodyTermFreq(term) : 0;
        int urlFreq = document.getURLTermFreq(term) != null ? document.getURLTermFreq(term) : 0;

        double weightedFreq = titleFreq * 10.0 + headerFreq * 5.0 + urlFreq * 5.0 + bodyFreq * 1.0;
        if (weightedFreq == 0) return 0.0;

        return (1 + Math.log10(weightedFreq)) / document.getTotalTermsCount();
    }

    public Double calculateIDF(String term) {
        long docsWithTerm = documentFrequencies.get(term);
        if (docsWithTerm == 0)
            return 0.0;
        return Math.log10((double) totalNumberOfDocuments / docsWithTerm);
    }

}
