package Crawler;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.jsoup.nodes.Document;

public class VisitedSet {
    private static Set<String> visitedUrls = new HashSet<>();
    private static Set<String> visitedPages = new HashSet<>();
    // Adds a normalized URL to the visited set
    public static void addVisitedUrl(String url) {
        String normalizedUrl = URLNormalizer.normalize(url);
        synchronized (visitedUrls) {
            visitedUrls.add(normalizedUrl);
        }
    }

    // Adds a compact string representation of a page to the visited set
    public static void addVisitedPage(Document doc) {
        String page = URLNormalizer.getCompactString(doc);
        synchronized (visitedPages) {
            visitedPages.add(page);
        }
    }

    // Checks if a normalized URL is in the visited set
    public static boolean containsVisitedUrl(String url) {
        String normalizedUrl = URLNormalizer.normalize(url);
        return visitedUrls.contains(normalizedUrl);
    }

    // Checks if a compact string representation of a page is in the visited set
    public static boolean containsVisitedPage(Document doc) {
        String page = URLNormalizer.getCompactString(doc);
        return visitedPages.contains(page);
    }
}