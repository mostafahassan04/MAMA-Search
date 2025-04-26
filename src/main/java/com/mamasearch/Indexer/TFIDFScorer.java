package com.mamasearch.Indexer;
import java.util.HashMap;
import java.util.Map;

public class TFIDFScorer {
    public static Map<String, Double> calculateTFIDF(Map<String, Double> tf, Map<String, Double> idf) {
        Map<String, Double> tfidf = new HashMap<>();

        for (String word : tf.keySet()) {
            double tfScore = tf.getOrDefault(word, 0.0);
            double idfScore = idf.getOrDefault(word, 0.0);  // if IDF missing, assume 0
            tfidf.put(word, tfScore * idfScore);
        }

        return tfidf;
    }
}
