package com.mamasearch.Ranker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import static java.util.Map.entry;


public class Ranker {
    private final int MAX_ITERATIONS = 50;
    private Map<String, Integer> documentFrequencies; //term -> no documents containing it
    private Integer totalNumberOfDocuments;

    Ranker(Integer totalNumberOfDocuments ,Map<String ,Integer> documentFrequencies ){
        this.totalNumberOfDocuments = totalNumberOfDocuments;
        this.documentFrequencies = documentFrequencies;
    }
    private Map<String , Page> pages = new HashMap<>();


    private Page getOrCreatePage(String url) {
        return pages.computeIfAbsent(url, key -> {
            Page newPage = new Page(key);
            return newPage;
        });
    }

    public void rankPages() throws FileNotFoundException {

        Map<String , ArrayList<String>> pagesGraph;
        try {
            pagesGraph = new Gson().fromJson(new FileReader("./src/main/resources/crawler-output.json"),
                    new TypeToken<Map<String, Object>>() {
                    }.getType());
        }catch (JsonSyntaxException e){
            throw new RuntimeException(e);
        }
        Map<Page,List<Page>>inboundLinks = new HashMap<>();
        Map<Page,Integer>outboundCount = new HashMap<>();

        for(Map.Entry<String ,  ArrayList<String>> entry : pagesGraph.entrySet()) {
            Page page1 = getOrCreatePage(entry.getKey());
            for(String url : entry.getValue()){
                Page page2 = getOrCreatePage(url);
            }
        }
        //construct the graph
        for(Map.Entry<String ,  ArrayList<String>> entry : pagesGraph.entrySet()){
            Page pageLinkFrom = pages.get(entry.getKey());
            outboundCount.put(pageLinkFrom,entry.getValue().size());

            for(String url : entry.getValue()){
                Page pageLinkTo = pages.get(url);
                inboundLinks.computeIfAbsent(pageLinkTo, k -> new ArrayList<>()).add(pageLinkFrom);
            }
        }

        Map<Page,Double> currentRanks = new HashMap<>();
        Map<Page,Double> nextRanks = new HashMap<>();
        double initialRank = 1.0/pages.size();
        for(Page page : pages.values()){
            currentRanks.put(page,initialRank);
        }
        double d = 0.85;

        for(int i = 0 ; i < MAX_ITERATIONS ;i++){
            for(Map.Entry<String ,Page> entry : pages.entrySet()){
                Page page = entry.getValue();
                double sum = 0.0;
                List<Page> inLinks = inboundLinks.get(page);
                if (inLinks != null) {
                    for (Page linker : inLinks) {
                        sum += currentRanks.get(linker) / outboundCount.get(linker);
                    }
                }
                double newRank = (1 - d)/ pages.size() + d * sum;
                nextRanks.put(page,newRank);
            }
            currentRanks.clear();
            currentRanks.putAll(nextRanks);
            nextRanks.clear();
        }
        for (Map.Entry<Page, Double> finalEntry : currentRanks.entrySet()) {
            finalEntry.getKey().setPageRank(finalEntry.getValue());
        }
    }

    public List<ScoredDocument> rankDocument(List<String> queryTerms, List<Document> documents) {

        double alpha = 0.7 , beta = 0.3;
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
        int urlFreq = document.getURLTermFreq(term)!=null ? document.getURLTermFreq(term):0;

        double weightedFreq = titleFreq * 10.0 + headerFreq * 5.0 +urlFreq* 5.0 + bodyFreq * 1.0;
        if (weightedFreq == 0) return 0.0;

        return (1 + Math.log10(weightedFreq)) / document.getTotalTermsCount();
    }

    public Double calculateIDF(String term) {
        long docsWithTerm = documentFrequencies.get(term);
        if (docsWithTerm == 0)
            return 0.0;
        return Math.log10((double) totalNumberOfDocuments / docsWithTerm);
    }
    public static void main(String[] args) {



        Document doc1 = new Document(
                "doc1",
                6,
                Map.of("hello", 2, "this", 1, "is", 1, "test", 1, "ranker", 1),
                Map.of(),
                Map.of(),
                Map.of()
        );

        Document doc2 = new Document(
                "doc2",
                9,
                Map.of("forest", 1, "of", 1, "the", 1, "koko", 1, "is", 1, "far", 1, "beyond", 1, "hills", 1),
                Map.of(),
                Map.of(),
                Map.of()
        );

        Document doc3 = new Document(
                "doc3",
                4,
                Map.of("hello", 1, "ranker", 1, "test", 1, "example", 1),
                Map.of(),
                Map.of(),
                Map.of()
        );
        // List of documents
        List<Document> documents = List.of(doc1, doc2, doc3);

        // Query terms
        List<String> queryTerms = List.of("hello", "ranker", "test");

        // Document frequencies (term -> number of documents containing the term)
        Map<String, Integer> documentFrequencies = Map.ofEntries(
                entry("hello", 2)
                , entry("this", 1)
                , entry("is", 2)
                , entry("test", 2)
                , entry("ranker", 2)
                , entry("forest", 1)
                , entry("of", 1)
                , entry("the", 1)
                , entry("koko", 1)
                , entry("far", 1)
                , entry("hills", 1)
                , entry("beyond", 1)
        );
//                "example", 1


        // Total number of documents
        int totalNumberOfDocuments = documents.size();

        // Initialize Ranker
        Ranker ranker = new Ranker(totalNumberOfDocuments,documentFrequencies);

        try {
            ranker.rankPages();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Rank documents
        List<ScoredDocument> rankedDocuments = ranker.rankDocument(queryTerms, documents);

        // Print results
        for (ScoredDocument scoredDocument : rankedDocuments) {
            System.out.println("Document ID: " + scoredDocument.getDocument().getId() + ", Score: " + scoredDocument.getScore());
        }



    }

}
