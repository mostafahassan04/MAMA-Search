package com.mamasearch.Ranker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageRanker {

    private final int MAX_ITERATIONS = 50;
    private final double ERROR_THRESHOLD = 0.000001;
    private Map<String , Page> pages = new HashMap<>();



    public void run() throws FileNotFoundException {

        Map<String , ArrayList<String>> pagesGraph;
        try {
            pagesGraph = new Gson().fromJson(new FileReader("./src/main/resources/crawler-output.json"),
                    new TypeToken<Map<String, Object>>() {
                    }.getType());
        }catch (JsonSyntaxException e){
            throw new RuntimeException(e);
        }
        Map<Page, List<Page>> inboundLinks = new HashMap<>();
        Map<Page, Integer> outboundCount = new HashMap<>();

        for (Map.Entry<String, ArrayList<String>> entry : pagesGraph.entrySet()) {
            Page page1 = getOrCreatePage(entry.getKey());
            for (String url : entry.getValue()) {
                Page page2 = getOrCreatePage(url);
            }
        }
        //construct the graph
        for (Map.Entry<String, ArrayList<String>> entry : pagesGraph.entrySet()) {
            Page pageLinkFrom = pages.get(entry.getKey());
            outboundCount.put(pageLinkFrom, entry.getValue().size());

            for (String url : entry.getValue()) {
                Page pageLinkTo = pages.get(url);
                inboundLinks.computeIfAbsent(pageLinkTo, k -> new ArrayList<>()).add(pageLinkFrom);
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
        for (int i = 0; i < MAX_ITERATIONS; i++) {

            for (Map.Entry<String, Page> entry : pages.entrySet()) {
                Page page = entry.getValue();
                double sum = 0.0;
                List<Page> inLinks = inboundLinks.get(page);
                if (inLinks != null) {
                    for (Page linker : inLinks) {
                        sum += currentRanks.get(linker) / outboundCount.get(linker);
                    }
                }
                double newRank = (1 - d) / pages.size() + d * sum;
                nextRanks.put(page, newRank);
                currentMaxErrorThreshold = Math.max(currentMaxErrorThreshold,Math.abs(newRank-currentRanks.get(page)));y
            }
            currentRanks.clear();
            currentRanks.putAll(nextRanks);
            nextRanks.clear();

            if(currentMaxErrorThreshold<=ERROR_THRESHOLD)
                break;
        }
        for (Map.Entry<Page, Double> finalEntry : currentRanks.entrySet()) {
            finalEntry.getKey().setPageRank(finalEntry.getValue());
        }
    }

    private Page getOrCreatePage(String url) {
        return pages.computeIfAbsent(url, key -> {
            Page newPage = new Page(key);
            return newPage;
        });
    }

}
