package com.mamasearch;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

class Document {
    private final String id;
    private final Map<String, Integer> bodyTermsFreq;
    private final Map<String, Integer> titleTermsFreq;
    private final Map<String, Integer> headerTermsFreq;
    private final Map<String, Integer> urlTermsFreq;
    private final Integer totalTermsCount;

    Document(String id,Integer totalTermsCount,Map<String , Integer> bodyTermsFreq , Map<String , Integer>titleTermsFreq,
    Map<String,Integer> headerTermsFreq , Map<String,Integer> urlTermsFreq) {
        this.id = id;
        this.totalTermsCount = totalTermsCount;
        this.bodyTermsFreq = bodyTermsFreq;
        this.titleTermsFreq = titleTermsFreq;
        this.headerTermsFreq = headerTermsFreq;
        this.urlTermsFreq = urlTermsFreq;
    }

    public Integer getBodyTermFreq(String term) {
        return bodyTermsFreq.get(term);
    }
    public Integer getTitleTermFreq(String term) {
        return titleTermsFreq.get(term);
    }
    public Integer getHeaderTermFreq(String term) {
        return headerTermsFreq.get(term);
    }
    public Integer getURLTermFreq(String term) {
        return urlTermsFreq.get(term);
    }

    public Integer getTotalTermsCount() {
        return totalTermsCount;
    }

    public String getId() {
        return id;
    }
}

class ScoredDocument implements Comparable<ScoredDocument> {
    private final Document document;
    private Double score;

    ScoredDocument(Document document) {
        this.document = document;
        this.score = 0.0;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getScore() {
        return score;
    }

    @Override
    public int compareTo(ScoredDocument other) {
        return Double.compare(other.score, this.score);
    }

    public Document getDocument() {
        return document;
    }
}

class Page{
    private final String url;
    private Double pageRank;
    Page(String url ){
        this.url = url;
        this.pageRank = 0.0 ;
    }

    public String getUrl() {
        return url;
    }

    public Double getPageRank() {
        return pageRank;
    }

    public void setPageRank(Double pageRank) {
        this.pageRank = pageRank;
    }
}


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


}
