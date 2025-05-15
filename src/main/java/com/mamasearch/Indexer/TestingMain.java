package com.mamasearch.Indexer;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TestingMain {

    public static void main(String[] args) {

        Indexer indexer = new Indexer();
        IndexerMongoDBConnection mongo = new IndexerMongoDBConnection();
        boolean newIndex;

        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Indexer!");
        System.out.println("Do you want to continue on the previous state? (yes/no)");
        String loadState = scanner.nextLine().trim().toLowerCase();
        while (!loadState.equals("yes") && !loadState.equals("no")) {
            System.out.println("Invalid input. Please type 'yes' or 'no':");
            loadState = scanner.nextLine().trim().toLowerCase();
        }
        if (loadState.equals("yes")) {
            System.out.println("Loading on previous state...");
            newIndex = false;

        } else{
            System.out.println("Starting a new Indexer...");
            newIndex = true;
        }
            indexer.processDocuments(mongo.getDocuments(newIndex));
            mongo.insertInvertedIndex(indexer.getInvertedIndex());

        scanner.close();
        mongo.close();


    }
}
