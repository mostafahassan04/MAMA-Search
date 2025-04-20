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
    private static final Map<String, String> robotsCache = new ConcurrentHashMap<>();

    // Fetches and caches the robots.txt content for a given URL
    public static String fetchRobotsTxt(String url) {
        String baseUrl;
        baseUrl = URLNormalizer.getBaseURL(url);
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return null;
        }

        if (robotsCache.containsKey(baseUrl)) {
            return robotsCache.get(baseUrl);
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
                robotsCache.put(robotsUrl, "");
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
            robotsCache.put(robotsUrl, robotsContent.isEmpty() ? "" : robotsContent);
            return robotsContent;

        } catch (IOException e) {
            robotsCache.put(robotsUrl, "");
            System.err.println("Error fetching robots.txt: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // Checks if the robots.txt for a URL has already been parsed
    public static boolean isParsed(String url) {
        String robotsUrl = URLNormalizer.getBaseURL(url) + "/robots.txt";
        return robotsCache.containsKey(robotsUrl);
    }

    // Determines if a URL is allowed based on robots.txt rules
    public static boolean isAllowed(String url) {
        String normalizedUrl = URLNormalizer.normalize(url);
        String baseUrl = URLNormalizer.getBaseURL(normalizedUrl);
        String robotsUrl = baseUrl + "/robots.txt";

        if (!robotsCache.containsKey(robotsUrl)) {
            fetchRobotsTxt(url);
        }
        if (robotsCache.get(robotsUrl) == null) {
            return true;
        }
        String robotsContent = robotsCache.get(robotsUrl);
        if (robotsContent.isEmpty()) {
            return true;
        }

        String path = normalizedUrl.substring(baseUrl.length());
        boolean isAllowed = true;

        List<String> disallowRules = new ArrayList<>();
        List<String> allowRules = new ArrayList<>();
        boolean isUserAgentRelevant = false;
        String[] lines = robotsContent.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.toLowerCase().startsWith("user-agent:")) {
                String userAgent = line.substring("user-agent:".length()).trim();
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

        for (String allowRule : allowRules) {
            if (matchesRule(path, allowRule)) {
                return true;
            }
        }
        for (String disallowRule : disallowRules) {
            if (matchesRule(path, disallowRule)) {
                return false;
            }
        }

        return isAllowed;
    }

    // Matches a URL path against a robots.txt rule
    private static boolean matchesRule(String path, String rule) {
        if (rule.isEmpty()) {
            return false;
        }

        String regex = rule.replace("*", ".*");
        regex = regex.replaceAll("([\\[\\]{}()+?^$|])", "\\\\$1");
        regex = "^" + regex;

        return path.matches(regex);
    }
}