package Crawler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.Normalizer;
import java.util.*;

public class URLFrontier {
    private final Queue<String> queue;
    private final VisitedSet vs;
    private final int maxSize = 10000;



    public URLFrontier(VisitedSet vs) {

        queue = new LinkedList<>();
        this.vs = vs;

    }

    // Adds a URL to the queue in a thread-safe manner
    public void addURL(String url) {
        String normalizedUrl = URLNormalizer.normalize(url);

        synchronized (queue) {
            if (queue.size() >= maxSize) {
                return;
            }
            queue.add(normalizedUrl);
        }
    }

    // Checks if the queue is empty
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // Returns the size of the queue
    public int size() {
        return queue.size();
    }

    // Retrieves and removes the next URL from the queue in a thread-safe manner
    public String getNextURL() {
        synchronized (queue) {
            return queue.poll();
        }
    }

    // Saves the current state of the queue (to be implemented)
    public void serialize(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(queue);
            oos.flush();
        } catch (Exception e) {
            System.err.println("Error while saving state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Loads the saved state of the queue (to be implemented)
    public static URLFrontier deserialize(String filePath, VisitedSet vs) {
       try (FileInputStream fis = new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis)) {
            Queue<String> queue = (Queue<String>) ois.readObject();
            URLFrontier frontier = new URLFrontier(vs);
            frontier.queue.addAll(queue);
            return frontier;
        } catch (Exception e) {
            System.err.println("Error while loading state: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}