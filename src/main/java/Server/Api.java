package Server;

import Processor.Processor;
import PhraseSearcher.PhraseSearcher;
import PhraseSearcher.PhraseSearcher.QuoteResult;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bson.Document;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Api {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        Processor processor = new Processor();

        // Define routes
        server.createContext("/api/search", new SearchHandler(processor));
        server.createContext("/api/suggest", new SuggestHandler(processor));

        server.setExecutor(null);
        server.start();
        System.out.println("Server running on port 8080...");
    }

    static class SearchHandler implements HttpHandler {
        Processor processor;

        SearchHandler(Processor processor) {
            this.processor = processor;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                String queryString = exchange.getRequestURI().getQuery();

                if (queryString != null) {
                    // Measure start time
                    long startTime = System.nanoTime();

                    // Process the query
                    processor.insertSearchQuery(queryString);
                    QuoteResult res = PhraseSearcher.extractQuotedParts(queryString);
                    ArrayList<Document> relevantDocuments;
                    if (!res.getRemainingString().isEmpty()) {
                        processor.setSearchQuery(res.getRemainingString());
                        relevantDocuments = processor.getRelevantDocuments();
                    } else {
                        processor.setQuotedParts(res.getQuotedParts());
                        processor.setOperators(res.getOperators());
                        relevantDocuments = processor.getPhraseDocuments();
                    }
                    ArrayList<String> allTokens = processor.getAllTokens();
                    
                    System.out.println("All tokens: " + allTokens);
                    System.out.println("Relevant documents: " + relevantDocuments);

                    // Measure end time and calculate duration in milliseconds
                    long endTime = System.nanoTime();
                    double timeMs = (endTime - startTime) / 1_000_000.0;

                    System.out.println("Found " + relevantDocuments);
                    System.out.println("Found " + relevantDocuments.size());

                    // Convert relevantDocuments to JSON array
                    StringBuilder docsJson = new StringBuilder("[");
                    for (int i = 0; i < relevantDocuments.size(); i++) {
                        docsJson.append(relevantDocuments.get(i).toJson());
                        if (i < relevantDocuments.size() - 1) {
                            docsJson.append(",");
                        }
                    }
                    docsJson.append("]");

                    // Build response with documents and time
                    String response = String.format(
                            "{\"documents\":%s, \"time_ms\":%.2f}",
                            docsJson, timeMs
                    );

                    sendResponse(exchange, 200, response);
                } else {
                    sendResponse(exchange, 400, "{\"error\":\"Query parameter is required\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    static class SuggestHandler implements HttpHandler {
        Processor processor;

        SuggestHandler(Processor processor) {
            this.processor = processor;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle OPTIONS preflight request
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }

            String method = exchange.getRequestMethod();
            if ("POST".equals(method)) {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                JSONObject jsonBody = new JSONObject(requestBody);
                String query = jsonBody.optString("query", ""); // Use optString to avoid exceptions if "query" is missing
                System.out.println("Parsed query: " + query);
                List<String> suggestions = processor.getSuggestions();
                // get docs
                StringBuilder responseBuilder = new StringBuilder("{\"suggestions\":[");
                for (int i = 0; i < suggestions.size(); i++) {
                    responseBuilder.append("\"").append(suggestions.get(i)).append("\"");
                    if (i < suggestions.size() - 1) {
                        responseBuilder.append(",");
                    }
                }
                responseBuilder.append("]}");
                String response = responseBuilder.toString();
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