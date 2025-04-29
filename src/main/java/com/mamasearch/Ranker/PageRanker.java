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

public class PageRanker {

    private final Map<String, Page> pages = new HashMap<>();
    private final MongoCollection<Document> collection;


    PageRanker(){
        MongoDatabase database = MongoDBClient.getDatabase();
        String COLLECTION_NAME = "url_graph";
        this.collection = database.getCollection(COLLECTION_NAME);
    }


    public void rank(){

        Map<String, ArrayList<String>> pagesGraph = new HashMap<>();
        FindIterable<Document> documents = collection.find();
        for(Document doc :documents){
            String url = doc.getString("url");
            List<String >links = doc.getList("extractedUrls",String.class);
            pagesGraph.put(url,new ArrayList<>(links));
        }
        Map<Page, List<Page>> inboundLinks = new HashMap<>();
        Map<Page, Integer> outboundCount = new HashMap<>();

        for (Map.Entry<String, ArrayList<String>> entry : pagesGraph.entrySet()) {
            pages.computeIfAbsent(entry.getKey(), Page::new);
            for (String url : entry.getValue()) {
                pages.computeIfAbsent(url, Page::new);
            }
        }
        //construct the graph
        for (Map.Entry<String, ArrayList<String>> entry : pagesGraph.entrySet()) {
            Page pageLinkFrom = pages.get(entry.getKey());
            outboundCount.put(pageLinkFrom, entry.getValue().size());

            for (String url : entry.getValue()) {
                Page pageLinkTo = pages.get(url);
                inboundLinks.computeIfAbsent(pageLinkTo, _ -> new ArrayList<>()).add(pageLinkFrom);
            }
        }

        Map<Page, Double> currentRanks = new HashMap<>();
        Map<Page, Double> nextRanks = new HashMap<>();
        double initialRank = 1.0 / pages.size();
        for (Page page : pages.values()) {
            currentRanks.put(page, initialRank);
        }
        double d = 0.85;

        double currentMaxErrorThreshold = 1.0;
        int MAX_ITERATIONS = 50;
        for (int i = 0; i < MAX_ITERATIONS; i++) {

            double sumDanglingRank = 0.0;
            for (Page p : pages.values()) {
                if(outboundCount.get(p)==0){
                    sumDanglingRank+=currentRanks.get(p);
                }
            }

            for (Map.Entry<String, Page> entry : pages.entrySet()) {
                Page page = entry.getValue();
                double sumFromLinks = 0.0;
                List<Page> inLinks = inboundLinks.get(page);
                if (inLinks != null) {
                    for (Page linker : inLinks) {
                        int outLinks = outboundCount.get(linker);
                        if(outLinks>0)
                            sumFromLinks += currentRanks.get(linker) / outboundCount.get(linker);
                    }
                }
                double newRank = (1.0 - d) / pages.size()           // Base teleportation
                        + d * (sumDanglingRank / pages.size()) // Redistributed dangling rank (scaled by d)
                        + d * sumFromLinks;                  // Rank from actual links (scaled by d)

                nextRanks.put(page, newRank);
                currentMaxErrorThreshold = Math.max(currentMaxErrorThreshold, Math.abs(newRank - currentRanks.get(page)));
            }
            currentRanks.clear();
            currentRanks.putAll(nextRanks);
            nextRanks.clear();

            double ERROR_THRESHOLD = 0.000001;
            if (currentMaxErrorThreshold <= ERROR_THRESHOLD)
                break;
        }
        for (Map.Entry<Page, Double> finalEntry : currentRanks.entrySet()) {
            finalEntry.getKey().setPageRank(finalEntry.getValue());
        }
    }


}
