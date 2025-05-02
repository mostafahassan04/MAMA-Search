package com.mamasearch.Indexer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFIDFScorer {
//    public static Map<String, Double> calculateTFIDF(Map<String, Double> tf, Map<String, Double> idf) {
//        Map<String, Double> tfidf = new HashMap<>();
//
//        for (String word : tf.keySet()) {
//            double tfScore = tf.getOrDefault(word, 0.0);
//            double idfScore = idf.getOrDefault(word, 0.0);  // if IDF missing, assume 0
//            tfidf.put(word, tfScore * idfScore);
//        }
//
//        return tfidf;
//    }

    public static Map<String, Map<Integer, Double>> calculateTFIDF(List<DocumentData> documents) {
        Map<String, Map<Integer, Double>> tfidfScores = new HashMap<>();
        Map<String, Double> idf = IDFCalculator.calculateIDF(documents);

        for (DocumentData document : documents) {
            Map<String, Double> tf = document.getTF();
            Integer ID = document.getID();

            for (String word : tf.keySet()) {
                double tfScore = tf.getOrDefault(word, 0.0);
                double idfScore = idf.getOrDefault(word, 0.0);  // if IDF missing, assume 0
                tfidfScores.computeIfAbsent(word, k -> new HashMap<>())
                        .put(ID, tfScore* idfScore);
            }
        }

        return tfidfScores;
    }


}
