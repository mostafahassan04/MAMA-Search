package com.mamasearch.Indexer;

public class TestingMain {

    public static void main(String[] args) {
        Indexer builder = new Indexer();

        // Example HTML documents
        String html1 = "<html><head><title>Fast Fox</title></head><body><h1>The quick brown fox</h1><p>jumps over the lazy dog</p></body></html>";
        String html2 = "<html><head><title>Lazy Dog</title></head><body><h1>Lazy Dog Sleeps</h1><p>while the quick brown fox jumps</p></body></html>";

        builder.processDocument(html1, "https://site1.com");
        builder.processDocument(html2, "https://site2.com");

        builder.finalizeIndex();

        // Now display the inverted index
        InvertedIndex index = builder.getInvertedIndex();

        index.printIndex();
    }
}
