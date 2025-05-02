package Crawler;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;

public class VisitedSet implements Serializable {
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Set<String> visitedPages = ConcurrentHashMap.newKeySet();
    private final Map<String, ArrayList<String>> UrlExtractedUrls = new ConcurrentHashMap<>();
    private final Map<String, Integer> urlsIdMap = new ConcurrentHashMap<>();
    private transient MongoDBConnection  mongoDBConnection;

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
    public boolean checkAndAddVisitedUrl(String url) {
        if (url == null) {
            return false;
        }

        String normalizedUrl = URLNormalizer.normalize(url);
        if (normalizedUrl == null) {
            return false; // Can't check a null URL
        }

        synchronized(this) {
            if (visitedUrls.contains(normalizedUrl)) {
                return true; // Already visited
            }
            visitedUrls.add(normalizedUrl);
            return false; // Newly added
        }
    }

    public boolean checkVisitedUrl(String url) {
        if (url == null) {
            return false;
        }

        String normalizedUrl = URLNormalizer.normalize(url);
        if (normalizedUrl == null) {
            return false; // Can't check a null URL
        }

        synchronized(this) {
            return visitedPages.contains(normalizedUrl); // Check if the page is visited
        }

    }

    // Checks if a compact string representation of a page is in the visited set
    public  boolean checkAndAddVisitedPage(Document doc) {
        String page = URLNormalizer.getCompactString(doc);
        if (page == null || page.trim().isEmpty()) {
            return false; // Can't check a null page
        }

        synchronized(this) {
            if (visitedPages.contains(page)) {
                return true; // Already visited
            }
            visitedPages.add(page);
            return false; // Newly added
        }
    }

    public  int getVisitedPagesCount() {
        return visitedPages.size();
    }

    public  int getVisitedUrlsCount() {
        return visitedUrls.size();
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
                Integer id = urlsIdMap.get(url);
                ArrayList<String> extractedUrls = entry.getValue();

                // Filter URLs without modifying the original map
                ArrayList<Integer> filteredUrlIds = extractedUrls.stream()
                        .filter(extractedUrl -> extractedUrl != null && visitedUrls.contains(extractedUrl))
                        .map(urlsIdMap::get)
                        .filter(Objects::nonNull)  // Filter out null IDs
                        .collect(Collectors.toCollection(ArrayList::new));

                // Insert directly to MongoDB without updating the map
                mongoDBConnection.insertUrlsGraph(id, filteredUrlIds);
            }
        } catch (Exception e) {
            System.err.println("Error uploading URL graph data: " + e.getMessage());
        }
    }

    public void serialize(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Error serializing VisitedSet: " + e.getMessage());
        }
    }

    public static VisitedSet deserialize(String filePath, MongoDBConnection connection) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            VisitedSet visitedSet = (VisitedSet) ois.readObject();
            // Restore the MongoDB connection after deserialization
            visitedSet.mongoDBConnection = connection;
            return visitedSet;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error deserializing VisitedSet: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void mapUrlToId(String url, Integer id) {
        if (url == null || id == null) {
            return;
        }
        String normalizedUrl = URLNormalizer.normalize(url);
        if (normalizedUrl == null) {
            return;
        }
        urlsIdMap.put(normalizedUrl, id);
    }
}