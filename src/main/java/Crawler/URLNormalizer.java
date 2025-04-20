package Crawler;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.*;
import org.jsoup.nodes.Document;

public class URLNormalizer {

    // Normalizes a given URL by standardizing its components
    public static String normalize(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return null;
            }

            URI uri = new URI(url).normalize();

            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : null;
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                return null;
            }

            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : null;
            if (host == null) {
                return null;
            }
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            String path = uri.getPath();
            if (path == null || path.trim().isEmpty()) {
                path = "/";
            }
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            int port = uri.getPort();
            if (port != -1) {
                if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
                    port = -1;
                }
            }

            String query = uri.getQuery();
            String normalizedQuery = null;

            if (query != null && !query.isEmpty()) {
                String[] params = query.split("&");
                params = Arrays.stream(params)
                        .filter(param -> !param.isEmpty() && !param.startsWith("sessionid=") && !param.startsWith("utm_"))
                        .sorted()
                        .toArray(String[]::new);
                if (params.length > 0) {
                    normalizedQuery = String.join("&", params);
                }
            }

            return scheme + "://" + host + (port == -1 ? "" : ":" + port) + path + (normalizedQuery == null ? "" : "?" + normalizedQuery);
        } catch (URISyntaxException e) {
            System.err.println("Error normalizing URL: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during URL normalization: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Extracts the base URL from a given URL
    public static String getBaseURL(String url) {
        try {
            String normalizedUrl = normalize(url);
            if (normalizedUrl == null) {
                return null;
            }
            URI uri = new URI(normalizedUrl);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (URISyntaxException e) {
            System.err.println("Error extracting base URL: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error during base URL extraction: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Generates a compact string representation of a document using SHA-256
    public static String getCompactString(Document doc) {
        try {
            if (doc == null) {
                return null;
            }

            String content = doc.html();
            return sha256(content);
        } catch (Exception e) {
            System.err.println("Unexpected error during compact string generation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Computes the SHA-256 hash of a given input string
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }


}