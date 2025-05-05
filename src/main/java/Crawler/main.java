package Crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class main {
    private static final int CRAWL_DELAY = 100; // milliseconds

    public static void main(String[] args) {
        // Ask for number of threads
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Crawler!");
        System.out.println("Do you want to load the previous state? (yes/no)");
        String loadState = scanner.nextLine().trim().toLowerCase();
        if (loadState.equals("yes")) {
            System.out.println("Loading previous state...");
        } else if (loadState.equals("no")) {
            System.out.println("Starting a new crawl...");
        } else {
            System.out.println("Invalid input. Starting a new crawl...");
        }
        System.out.print("Enter the number of threads: ");
        int numThreads = scanner.nextInt();
        scanner.close();

        // Initialize components
        MongoDBConnection mongoDBConnection = new MongoDBConnection();
        VisitedSet visitedSet = null;
        RobotsTxtParser robotsParser = null;
        URLFrontier frontier = null;

        String currentDir = System.getProperty("user.dir");
        String statesDir = currentDir + "/States/";
        // Create states directory if it doesn't exist
        File statesDirFile = new File(statesDir);
        if (!statesDirFile.exists()) {
            statesDirFile.mkdirs();
        }

        // Try to initialize from states directory
        boolean loadedFromStates = false;
        if (loadState.equals("yes"))
        {
            mongoDBConnection.deleteAllUrlGraph();
            File visitedSetFile = new File(statesDir + "visited_set.ser");
            File frontierFile = new File(statesDir + "frontier.ser");
            File robotsCacheFile = new File(statesDir + "robots_cache.ser");

            if (visitedSetFile.exists() && frontierFile.exists() && robotsCacheFile.exists()) {
                // Try to load the states
                visitedSet = VisitedSet.deserialize(statesDir + "visited_set.ser", mongoDBConnection);
                robotsParser = RobotsTxtParser.deserialize(statesDir + "robots_cache.ser");

                if (visitedSet != null && robotsParser != null) {
                    frontier = URLFrontier.deserialize(statesDir + "frontier.ser", visitedSet);
                    if (frontier != null) {
                        loadedFromStates = true;
                        System.out.println("Loaded previous crawler state from files.");
                    }
                }
            }
        }

        // If not loaded from states, initialize with default values
        if (!loadedFromStates) {
            visitedSet = new VisitedSet(mongoDBConnection);
            robotsParser = new RobotsTxtParser();
            frontier = new URLFrontier(visitedSet);
            addSeedUrls(frontier);
            mongoDBConnection.deleteAllCrawledPages();
            mongoDBConnection.deleteAllUrlGraph();
            System.out.println("Created new crawler with seed URLs.");
        }

        int crawledPagesCount = visitedSet.getVisitedPagesCount();

        CrawlerThread.setPageCount(crawledPagesCount);
        CrawlerThread.setId(crawledPagesCount - 1);

        // Print URL frontier size
        System.out.println("URL Frontier size: " + frontier.size());

        // Print visited set size
        System.out.println("Visited Set size: " + visitedSet.getVisitedPagesCount());

        // Start crawling
        System.out.println("Starting crawler with " + numThreads + " threads...");

        // Create and start crawler threads
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            CrawlerThread crawler = new CrawlerThread(visitedSet, robotsParser, frontier, mongoDBConnection, CRAWL_DELAY);
            Thread thread = new Thread(crawler, "Crawler-" + i);
            threads.add(thread);
            thread.start();
        }

        // Add shutdown hook to save states on exit
        VisitedSet finalVisitedSet = visitedSet;
        URLFrontier finalFrontier = frontier;
        RobotsTxtParser finalRobotsParser = robotsParser;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Signal all threads to stop processing
            CrawlerThread.setShuttingDown(true);

            // Give threads a moment to notice the flag and stop
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }

            synchronized (CrawlerThread.class) {
                finalVisitedSet.serialize(statesDir + "visited_set.ser");
                finalFrontier.serialize(statesDir + "frontier.ser");
                finalRobotsParser.serialize(statesDir + "robots_cache.ser");
            }
            System.out.println("States saved successfully. Exiting.");
        }));


        while(visitedSet.getVisitedPagesCount() < CrawlerThread.maxPages)
        {
            try {
                System.out.println("number of pages crawled: " + visitedSet.getVisitedPagesCount());
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting for threads to finish");
            }
        }


        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting for threads to finish");
            }
        }


        // Save states
        System.out.println("Saving crawler states...");
        visitedSet.serialize(statesDir + "visited_set.ser");
        frontier.serialize(statesDir + "frontier.ser");
        robotsParser.serialize(statesDir + "robots_cache.ser");

        // Print final count
        System.out.println("Crawling complete. Number of crawled pages: " + visitedSet.getVisitedPagesCount());

        // Upload URL graph data to MongoDB
        System.out.println("Uploading URL graph data to MongoDB...");
        visitedSet.filterAndUploadUrlExtractedUrls();
        System.out.println("URL graph data upload completed.");
    }

    private static void addSeedUrls(URLFrontier frontier) {
        String[] seeds = {
                // General websites
                "https://en.wikipedia.org",
                "https://github.com",
                "https://www.reddit.com",
                "https://www.quora.com",
                "https://www.medium.com",

                // News sites
                "https://www.bbc.com",
                "https://www.cnn.com",
                // Tech sites
                "https://techcrunch.com",
                "https://dev.to",

                // social media sites
                "https://www.twitter.com",
                "https://www.facebook.com",

        };

        for (String url : seeds) {
            String normalizedUrl = URLNormalizer.normalize(url);
            if (normalizedUrl != null) {
                frontier.addURL(normalizedUrl);
            }
        }
    }
}