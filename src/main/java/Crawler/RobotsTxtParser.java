package Crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class RobotsTxtParser {
    private static final String UserAgent = "MAMA_Search";
    // Cache to store robots.txt rules for each base URL to avoid redundant fetching
    private static final Map<String, List<List<String>>> robotsCache = new ConcurrentHashMap<>();

    // Fetches and caches the robots.txt content for a given URL
    public static List<List<String>> fetchRobotsTxt(String url) {
        if (isParsed(url)) {
            return robotsCache.get(URLNormalizer.getBaseURL(url));
        }

        String baseUrl;
        baseUrl = URLNormalizer.getBaseURL(url);
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            // Return null if the base URL is invalid or cannot be extracted
            return null;
        }

        String robotsUrl = baseUrl + "/robots.txt";
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new java.net.URL(robotsUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", UserAgent);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // Cache empty list for non-200 responses to avoid repeated requests
                List<List<String>> empty = new ArrayList<>();
                robotsCache.put(baseUrl, empty);
                return null;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            String robotsContent = content.toString();

            // Parse robots.txt content to extract allow and disallow rules
            List<List<String>> robotsList = extractRules(robotsContent);

            // Cache parsed rules, or empty list if no rules are found
            robotsCache.put(baseUrl, robotsContent.isEmpty() ? new ArrayList<>() : robotsList);
            return robotsList;

        } catch (IOException e) {
            // Cache empty list on error to prevent repeated failed attempts
            robotsCache.put(baseUrl, new ArrayList<>());

            System.err.println("Error fetching robots.txt: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                // Ensure connection is closed to prevent resource leaks
                connection.disconnect();
            }
        }
    }

    // Checks if the robots.txt for a URL has already been parsed
    public static boolean isParsed(String url) {
        String baseUrl = URLNormalizer.getBaseURL(url);
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return false;
        }
        return robotsCache.containsKey(baseUrl);
    }

    // Determines if a URL is allowed based on robots.txt rules
    public static boolean isAllowed(String url) {
        String normalizedUrl = URLNormalizer.normalize(url);
        String baseUrl = URLNormalizer.getBaseURL(normalizedUrl);

        if (!isParsed(url)) {
            fetchRobotsTxt(url);
        }

        List<List<String>> robotsList = robotsCache.get(baseUrl);

        if (robotsList == null || robotsList.isEmpty()) {
            // Allow access if no rules are defined
            return true;
        }

        String path = normalizedUrl.substring(baseUrl.length());

        // Check allow rules first, as they take precedence
        for (String allowRule : robotsList.get(0)) {
            if (matchesRule(path, allowRule)) {
                return true;
            }
        }
        // Check disallow rules if no allow rule matches
        for (String disallowRule : robotsList.get(1)) {
            if (matchesRule(path, disallowRule)) {
                return false;
            }
        }

        // Allow access by default if no rules match
        return true;
    }

    // Extracts allow and disallow rules from robots.txt content
    private static List<List<String>> extractRules(String content) {
        List<String> disallowRules = new ArrayList<>();
        List<String> allowRules = new ArrayList<>();
        boolean isUserAgentRelevant = false;
        String[] lines = content.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                // Skip empty lines and comments
                continue;
            }

            if (line.toLowerCase().startsWith("user-agent:")) {
                String userAgent = line.substring("user-agent:".length()).trim();
                // Apply rules for our UserAgent or wildcard
                isUserAgentRelevant = userAgent.equals("*") || userAgent.equalsIgnoreCase(UserAgent);
                continue;
            }

            if (isUserAgentRelevant) {
                if (line.toLowerCase().startsWith("disallow:")) {
                    String rule = line.substring("disallow:".length()).trim();
                    if (!rule.isEmpty()) {
                        disallowRules.add(rule);
                    }
                } else if (line.toLowerCase().startsWith("allow:")) {
                    String rule = line.substring("allow:".length()).trim();
                    if (!rule.isEmpty()) {
                        allowRules.add(rule);
                    }
                }
            }
        }

        List<List<String>> res = new ArrayList<>();
        res.add(allowRules);
        res.add(disallowRules);
        return res;
    }

    // Matches a URL path against a robots.txt rule
    private static boolean matchesRule(String path, String rule) {
        if (rule.isEmpty()) {
            return false;
        }

        // Convert rule to regex, escaping special characters
        String regex = rule.replace("*", ".*");
        regex = regex.replaceAll("([\\[\\]{}()+?^$|])", "\\\\$1");
        regex = "^" + regex;

        return path.matches(regex);
    }

}