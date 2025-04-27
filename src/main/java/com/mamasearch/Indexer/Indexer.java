package com.mamasearch.Indexer;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Indexer {
    private InvertedIndex invertedIndex = new InvertedIndex();
    private List<List<ParsedWord>> allDocumentsFilteredWords = new ArrayList<>();
    private List<String> allDocumentUrls = new ArrayList<>();

    public void processDocument(String htmlContent, String url) {
        DocumentParser parser = new DocumentParser();
        List<ParsedWord> parsedWords = parser.parseFile(htmlContent);

        List<ParsedWord> filteredWords = Tokenizer.filter(parsedWords);
        allDocumentsFilteredWords.add(filteredWords);
        allDocumentUrls.add(url);

        for (ParsedWord pw : filteredWords) {
            invertedIndex.addWord(pw.getWord(), url, pw.getPosition());
        }
    }

    public void finalizeIndex() {
        // Step 1: calculate IDF
        Map<String, Double> idfScores = IDFCalculator.calculateIDF(allDocumentsFilteredWords);

        // Step 2: calculate TF-IDF for each document
        for (int i = 0; i < allDocumentsFilteredWords.size(); i++) {
            List<ParsedWord> docWords = allDocumentsFilteredWords.get(i);
            String url = allDocumentUrls.get(i);

            Map<String, Double> tfScores = TFCalculator.calculateWeightedTF(docWords);
            Map<String, Double> tfidfScores = TFIDFScorer.calculateTFIDF(tfScores, idfScores);

            // Step 3: update inverted index with TF-IDF score
            for (Map.Entry<String, Double> entry : tfidfScores.entrySet()) {
                String word = entry.getKey();
                double score = entry.getValue();
                invertedIndex.addScore(word, url, score);
            }
        }
    }

    public InvertedIndex getInvertedIndex() {
        return invertedIndex;
    }
}
