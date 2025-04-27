package com.mamasearch.Indexer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFCalculator {
    private static final Map<String, Double> tagWeights = new HashMap<>();

    static {
        tagWeights.put("title", 5.0);
        tagWeights.put("h1", 4.0);
        tagWeights.put("h2", 3.0);
        tagWeights.put("h3", 2.0);
        tagWeights.put("normal", 1.0);
    }

    public static Map<String, Double> calculateWeightedTF(List<ParsedWord> parsedWords) {
        Map<String, Double> weightedCount = new HashMap<>();
        double totalWeightedWords = 0;

        // Count weighted frequency
        for (ParsedWord word : parsedWords) {
            String term = word.getWord();
            String tag = word.getTag();
            double weight = tagWeights.getOrDefault(tag, 1.0);

            weightedCount.put(term, weightedCount.getOrDefault(term, 0.0) + weight);
            totalWeightedWords += weight;
        }

        // Normalize
        Map<String, Double> tfScores = new HashMap<>();
        for (Map.Entry<String, Double> entry : weightedCount.entrySet()) {
            tfScores.put(entry.getKey(), entry.getValue() / totalWeightedWords);
        }

        return tfScores;
    }
}
