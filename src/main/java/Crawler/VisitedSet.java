package Crawler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;

public class VisitedSet {
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> visitedPages = ConcurrentHashMap.newKeySet();
    private final Map<String, ArrayList<String>> UrlExtractedUrls = new ConcurrentHashMap<>();
    MongoDBConnection mongoDBConnection;

    VisitedSet(MongoDBConnection mongoDBConnection) {
        this.mongoDBConnection = mongoDBConnection;
    }
    // Adds a normalized URL to the visited set
    public void addVisitedUrl(String url) {
        if (url == null) {
            return;
        }

        String normalizedUrl = URLNormalizer.normalize(url);
        if (normalizedUrl != null) {
            visitedUrls.add(normalizedUrl);
        }
    }

    // Adds a compact string representation of a page to the visited set
    public  void addVisitedPage(Document doc) {
        String page = URLNormalizer.getCompactString(doc);
        visitedPages.add(page);
    }

    // Checks if a normalized URL is in the visited set
    public boolean containsVisitedUrl(String url) {
        if (url == null) {
            return false;
        }

        String normalizedUrl = URLNormalizer.normalize(url);
        if (normalizedUrl == null) {
            return false; // Can't check a null URL
        }

        return visitedUrls.contains(normalizedUrl);
    }

    // Checks if a compact string representation of a page is in the visited set
    public  boolean containsVisitedPage(Document doc) {
        String page = URLNormalizer.getCompactString(doc);
        return visitedPages.contains(page);
    }

    public  int getVisitedPagesCount() {
        return visitedPages.size();
    }

    public void addUrlExtractedUrls(String url, ArrayList<String> ExtractedUrls) {
        if (url == null ) {
            return;
        }
        String normalizedUrl = URLNormalizer.normalize(url);
        if (normalizedUrl == null) {
            return;
        }
        if (ExtractedUrls == null) {
            UrlExtractedUrls.put(normalizedUrl, new ArrayList<>());
            return;
        }
        UrlExtractedUrls.put(normalizedUrl, ExtractedUrls);
    }

    public void filterAndUploadUrlExtractedUrls() {
        try {
            for (Map.Entry<String, ArrayList<String>> entry : UrlExtractedUrls.entrySet()) {
                String url = entry.getKey();
                ArrayList<String> extractedUrls = entry.getValue();

                // Filter URLs without modifying the original map
                ArrayList<String> filteredUrls = extractedUrls.stream()
                        .filter(visitedUrls::contains)
                        .collect(Collectors.toCollection(ArrayList::new));

                // Insert directly to MongoDB without updating the map
                mongoDBConnection.insertUrlsGraph(url, filteredUrls);
            }
        } catch (Exception e) {
            System.err.println("Error uploading URL graph data: " + e.getMessage());
        }
    }
}