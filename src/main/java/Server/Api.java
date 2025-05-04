package Server;

import Processor.Processor;
import PhraseSearcher.PhraseSearcher;
import PhraseSearcher.PhraseSearcher.QuoteResult;

import com.mamasearch.Ranker.Ranker;
import com.mamasearch.Ranker.ScoredDocument;
import com.mamasearch.Utils.ProcessorData;

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
        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                String queryString = exchange.getRequestURI().getQuery();

                if (queryString != null) {
                    // Measure start time
                    long startTime = System.currentTimeMillis();

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

                    String [] allTokens = processor.getAllTokens();
                    System.out.println("Relevant documents: " + relevantDocuments.size());
                    System.out.println("all tokens: " + allTokens.length);

                    Ranker ranker = new Ranker();
                    ProcessorData processorData = new ProcessorData(relevantDocuments, allTokens);
                    System.out.println("ProcessorData: " + processorData.relevantDocuments.size());
                    System.out.println("ProcessorData: " + processorData.words.length);
                    // PROCESSOR_DATA IS FINE, THE NEXT HAVE PROBLEMS
                    List<ScoredDocument> sortedDocuments = ranker.rankDocument(processorData);
                    System.out.println("Ranked documents: \n" + sortedDocuments.size());

                    for(ScoredDocument doc : sortedDocuments)
                        System.out.println(doc.getSnippet());

                    // Measure end time and calculate duration in milliseconds
                    long endTime = System.currentTimeMillis();
                    double timeMs = (endTime - startTime);

                    System.out.println(timeMs+"ms");

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
        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
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
                List<String> suggestions = processor.getSuggestions(query);
                System.out.println("Suggestions: " + suggestions.size());

                // Build JSON response for suggestions
                StringBuilder responseBuilder = new StringBuilder("{\"suggestions\":[");
                for (int i = 0; i < suggestions.size(); i++) {
                    responseBuilder.append("\"").append(escapeJson(suggestions.get(i))).append("\"");

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