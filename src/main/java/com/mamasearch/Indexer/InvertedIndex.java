package com.mamasearch.Indexer;
import java.util.*;

public class InvertedIndex {
    private Map<String, Map<String, WordData>> index = new HashMap<>();

    public void addWord(String word, String url, int position) {
        index
                .computeIfAbsent(word, k -> new HashMap<>())
                .computeIfAbsent(url, k -> new WordData())
                .addPosition(position);
    }

    public void addScore(String word, String url, double score) {
        if (index.containsKey(word) && index.get(word).containsKey(url)) {
            index.get(word).get(url).setScore(score);
        }
    }

    public Map<String, Map<String, WordData>> getIndex() {
        return index;
    }
    public void printIndex() {
        for (String word : index.keySet()) {
            System.out.println("Word: " + word);
            Map<String, WordData> docs = index.get(word);
            for (String url : docs.keySet()) {
                WordData data = docs.get(url);
                System.out.println("  URL: " + url);
                System.out.println("    Positions: " + data.getPositions());
                System.out.println("    TF-IDF Score: " + data.getScore());
            }
        }
    }
}

