package com.mamasearch.Indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestingMain {

    public static void main(String[] args) {
        Indexer builder = new Indexer();
        IndexerMongoDBConnection mongo = new IndexerMongoDBConnection();

        List<DocumentData> documents = new ArrayList<DocumentData>();

        documents = mongo.getDocuments();

        for (DocumentData documentData : documents) {
            builder.processDocument(documentData.getContent(),documentData.getUrl());
        }

        builder.finalizeIndex();

        // Now display the inverted index
        InvertedIndex index = builder.getInvertedIndex();

        Map<String, Map<String, WordData>> invertedIndexMap = index.getInvertedIndex();

        for(String word : invertedIndexMap.keySet()) {
            Map<String, WordData> docs = invertedIndexMap.get(word);
            for (String url : docs.keySet()) {
                WordData data = docs.get(url);
                mongo.insertInvertedIndex(word, url, data.getPositions(), data.getScore());
            }
        }

        index.printIndex();
    }
}
