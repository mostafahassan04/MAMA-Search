package Crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class main {
    // Configuration parameters
    private static final int NUM_THREADS = 32;
    private static final int CRAWL_DELAY = 200; // milliseconds
    private static final int MONITOR_INTERVAL = 5000; // milliseconds
    private static final int MAX_RUNTIME = 3600000; // 1 hour in milliseconds

    public static void main(String[] args) {
        // Initialize components
        MongoDBConnection mongoDBConnection = new MongoDBConnection();
        VisitedSet visitedSet = new VisitedSet(mongoDBConnection); // Fixed constructor
        RobotsTxtParser robotsParser = new RobotsTxtParser();
        URLFrontier frontier = new URLFrontier(visitedSet);

        try {
            // Add seed URLs
            addSeedUrls(frontier);

            // Create and start crawler threads
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < NUM_THREADS; i++) {
                CrawlerThread crawler = new CrawlerThread(visitedSet, robotsParser, frontier, mongoDBConnection, CRAWL_DELAY);
                Thread thread = new Thread(crawler, "Crawler-" + i);
                threads.add(thread);
                thread.start();
            }

            // Monitor crawler progress
            monitorCrawler(visitedSet, frontier, threads);

            // Wait for all threads to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while waiting for threads to finish");
                }
            }

            System.out.println("Crawl completed. Total pages crawled: " + visitedSet.getVisitedPagesCount());
            System.out.println("Pages remaining in frontier: " + frontier.size());

            // Upload URL graph data to MongoDB
            System.out.println("Uploading URL graph data to MongoDB...");
            visitedSet.filterAndUploadUrlExtractedUrls();
            System.out.println("URL graph data upload completed.");
        } finally {
            // Always close the MongoDB connection when done
            mongoDBConnection.close();
            System.out.println("MongoDB connection closed");
        }
    }

    private static void addSeedUrls(URLFrontier frontier) {
        String[] seeds = {

                // News sites
                "https://www.bbc.com",
                "https://www.cnn.com",
                "https://www.nytimes.com",

                // General websites
                "https://en.wikipedia.org",
                "https://github.com",
                "https://www.reddit.com",

                // Tech sites
                "https://techcrunch.com",
                "https://www.wired.com",
                "https://dev.to",

                // Educational sites
                "https://www.coursera.org",
                "https://www.khanacademy.org"
        };

        for (String url : seeds) {
            String normalizedUrl = URLNormalizer.normalize(url);
            if (normalizedUrl != null) {
                frontier.addURL(normalizedUrl);
                System.out.println("Added seed URL: " + normalizedUrl);
            } else {
                System.out.println("Failed to normalize seed URL: " + url);
            }
        }
    }

    private static void monitorCrawler(VisitedSet visitedSet, URLFrontier frontier, List<Thread> threads) {
        Thread monitorThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            int prevPageCount = 0;
            int stallCount = 0;

            while (true) {
                try {
                    Thread.sleep(MONITOR_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }

                int currentPageCount = visitedSet.getVisitedPagesCount();
                int crawlRate = (currentPageCount - prevPageCount) * 1000 / MONITOR_INTERVAL;
                prevPageCount = currentPageCount;

                System.out.println("\n--- Crawler Status ---");
                System.out.println("Pages crawled: " + currentPageCount);
                System.out.println("Queue size: " + frontier.size());
                System.out.println("Crawl rate: " + crawlRate + " pages/sec");

                // Check for termination conditions
                if (areAllThreadsDone(threads)) {
                    System.out.println("All threads have completed their work.");
                    break;
                }

                if (currentPageCount >= CrawlerThread.maxPages) {
                    System.out.println("Reached maximum page count. Stopping crawler.");
                    stopAllThreads(threads);
                    break;
                }

                if (System.currentTimeMillis() - startTime > MAX_RUNTIME) {
                    System.out.println("Reached maximum runtime. Stopping crawler.");
                    stopAllThreads(threads);
                    break;
                }

                // Detect stalled crawler
                if (crawlRate == 0 && !frontier.isEmpty()) {
                    stallCount++;
                    if (stallCount >= 3) { // Stalled for 3 consecutive intervals
                        System.out.println("Crawler appears stalled. Thread states:");
                        for (Thread thread : threads) {
                            System.out.println(thread.getName() + ": " + thread.getState());
                        }

                        if (areAllThreadsWaiting(threads)) {
                            System.out.println("All threads waiting but frontier not empty. Possible deadlock.");
                            stopAllThreads(threads);
                            break;
                        }
                    }
                } else {
                    stallCount = 0;
                }
            }
        }, "Monitor");

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private static boolean areAllThreadsDone(List<Thread> threads) {
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                return false;
            }
        }
        return true;
    }

    private static boolean areAllThreadsWaiting(List<Thread> threads) {
        for (Thread thread : threads) {
            if (thread.isAlive() && thread.getState() != Thread.State.WAITING &&
                    thread.getState() != Thread.State.TIMED_WAITING) {
                return false;
            }
        }
        return true;
    }

    private static void stopAllThreads(List<Thread> threads) {
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }
    }
}