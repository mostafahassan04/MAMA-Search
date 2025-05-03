package com.mamasearch.Indexer;

import java.util.List;
import java.util.Map;

public class TestingMain {

    public static void main(String[] args) {
        Indexer indexer = new Indexer();
        IndexerMongoDBConnection mongo = new IndexerMongoDBConnection();
//        mongo.deleteAllInvertedIndex();
        indexer.processDocuments(mongo.getDocuments());
        mongo.insertInvertedIndex(indexer.getInvertedIndex());
//        indexer.printIndex();
    }
}
