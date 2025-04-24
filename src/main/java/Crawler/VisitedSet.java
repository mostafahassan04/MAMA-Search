package Crawler;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.nodes.Document;

public class VisitedSet {
    private  Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private  Set<String> visitedPages = ConcurrentHashMap.newKeySet();
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
}