package com.mamasearch.Indexer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IDFCalculator {
    public static Map<String, Double> calculateIDF(List<List<ParsedWord>> allDocsFilteredWords) {
        Map<String, Integer> docFreq = new HashMap<>();
        int totalDocs = allDocsFilteredWords.size();

        for (List<ParsedWord> docWords : allDocsFilteredWords) {
            Set<String> uniqueWordsInDoc = new HashSet<>();
            for (ParsedWord pw : docWords) {
                uniqueWordsInDoc.add(pw.getWord());
            }
            for (String word : uniqueWordsInDoc) {
                docFreq.put(word, docFreq.getOrDefault(word, 0) + 1);
            }
        }

        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            String word = entry.getKey();
            int df = entry.getValue();
            double value = Math.log(1+((double) totalDocs / df));
            idf.put(word, value);
        }

        return idf;
    }
}
