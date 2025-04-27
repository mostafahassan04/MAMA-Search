package Server;

import Processor.Processor;
import Processor.QueryDocument;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bson.Document;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Api {
    public static void main(String[] args) throws IOException {
        // Create HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Define routes
        server.createContext("/api/search", new SearchHandler());

        // Start server
        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port 8080...");
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                // Extract query from URL query parameter (e.g., /api/search?query=best+running+shoes)
                String queryString = exchange.getRequestURI().getQuery(); // e.g., "query=best+running+shoes"

                if (queryString != null) {
                    Processor processor = new Processor(queryString);
                    String[] result = processor.tokenizeAndStem();
                    System.out.println("Final stemmed tokens:");
                    for (String token : result) {
                        System.out.println(token);
                    }
                    ArrayList<Document> relevantDocuments = processor.getRelevantDocuments(result);

                }

                // For now, echo the query back as a response
                String response = String.format("{\"query\":\"%s\"}", queryString);
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response)
            throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}