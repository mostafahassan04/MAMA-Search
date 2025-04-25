package Crawler;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerThread implements Runnable {
    private final VisitedSet visitedSet;
    private final RobotsTxtParser robotsTxtParser;
    private final URLFrontier frontier;
    private final MongoDBConnection mongoDBConnection;
    private final int crawlDelay; // Delay in milliseconds
    private final String userAgent;
    private static final AtomicInteger pageCount = new AtomicInteger(0);
    public static final int maxPages = 1000;

    CrawlerThread(VisitedSet vs, RobotsTxtParser robotsTxtParser, URLFrontier frontier, MongoDBConnection mongoDBConnection, int crawlDelay) {
        this.visitedSet = vs;
        this.robotsTxtParser = robotsTxtParser;
        this.frontier = frontier;
        this.crawlDelay = crawlDelay;
        userAgent = "MAMA_Search";
        this.mongoDBConnection = mongoDBConnection;
    }

    private void processUrl(String url) {
        if (pageCount.incrementAndGet() > maxPages) {
            pageCount.decrementAndGet();
            Thread.currentThread().interrupt();
            return;
        }

        if (url == null) {
            pageCount.decrementAndGet();
            return;
        }

        // Normalize URL
        String normalizedUrl = URLNormalizer.normalize(url);
        if (normalizedUrl == null || visitedSet.containsVisitedUrl(normalizedUrl)) {
            pageCount.decrementAndGet();
            return;
        }

        // Check robots.txt rules
        if (!robotsTxtParser.isAllowed(normalizedUrl)) {
            pageCount.decrementAndGet();
            return;
        }

        Document doc = getDocument(normalizedUrl);
        if (doc == null) {
            pageCount.decrementAndGet();
            return;
        }

        // Check if the page has already been visited
        if (visitedSet.containsVisitedPage(doc)) {
            pageCount.decrementAndGet();
            return;
        }

        // Mark the page as visited
        visitedSet.addVisitedUrl(normalizedUrl);
        visitedSet.addVisitedPage(doc);

        // Add the document to the database
        mongoDBConnection.insertCrawledPage(normalizedUrl, doc.title(), doc.body().text());
        // Extract and add new URLs to the frontier
        ArrayList<String> urls = extractUrls(doc);
        for (String extractedUrl : urls) {
            String normalizedExtractedUrl = URLNormalizer.normalize(extractedUrl);
            if (normalizedExtractedUrl != null && !visitedSet.containsVisitedUrl(normalizedExtractedUrl)) {
                frontier.addURL(normalizedExtractedUrl);
            }
        }
    }

    private ArrayList<String> extractUrls(Document doc) {
        ArrayList<String> urls = new ArrayList<>();
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            urls.add(link.attr("abs:href"));
        }
        return urls;
    }

    private Document getDocument(String url) {
        try {
            // Increase timeout values (in milliseconds)
            return Jsoup.connect(url)
                    .timeout(10000)          // Connect timeout
                    .maxBodySize(1024 * 1024) // Max body size (1MB)
                    .followRedirects(true)
                    .userAgent(userAgent)
                    .ignoreHttpErrors(true)  // Don't throw exceptions for 4xx/5xx errors
                    .get();
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                System.out.println("Timeout while crawling: " + url + " - will retry later");
                return null;
            }
            System.out.println("Error fetching document: " + url + " - " + e.getMessage());
            return null;
        } catch (IOException e) {
            System.out.println("Error fetching document: " + url + " - " + e.getMessage());
            return null;
        }
    }

    @Override
    public void run() {
        while (true) {
            String url = frontier.getNextURL();
            if (url == null) {
                try {
                    Thread.sleep(50); // Prevent tight busy loop
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // Exit if the thread is interrupted
                }
                continue; // Retry after delay
            }

            processUrl(url);

            // Apply crawl delay
            try {
                Thread.sleep(crawlDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // Exit if the thread is interrupted
            }
        }
    }


}