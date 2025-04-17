package com.mamasearch;

import java.util.*;

import static java.util.Map.entry;

public class Main {
    public static void main(String[] args) {
        // Sample documents
        Document doc1 = new Document("doc1", Map.of("hello", 2, "this", 1, "is", 1, "test", 1, "ranker", 1), 6);
        Document doc2 = new Document("doc2", Map.of("forest", 1, "of", 1, "the", 1, "koko", 1, "is", 1, "far", 1, "beyond", 1, "hills", 1), 9);
        Document doc3 = new Document("doc3", Map.of("hello", 1, "ranker", 1, "test", 1, "example", 1), 4);

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
        Ranker ranker = new Ranker();
        ranker.documentFrequencies = documentFrequencies;
        ranker.totalNumberOfDocuments = totalNumberOfDocuments;

        // Rank documents
        List<ScoredDocument> rankedDocuments = ranker.rankDocument(queryTerms, documents);

        // Print results
        for (ScoredDocument scoredDocument : rankedDocuments) {
            System.out.println("Document ID: " + scoredDocument.getDocument().getId() + ", Score: " + scoredDocument.getScore());
        }
    }
}