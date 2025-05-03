package Server;

import Processor.Processor;
import PhraseSearcher.PhraseSearcher;
import PhraseSearcher.PhraseSearcher.QuoteResult;

import com.mamasearch.Utils.ProcessorData;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bson.Document;

import java.io.*;
import java.net.InetSocketAddress;
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
        Processor processor;
        SearchHandler() {
            try {
                processor = new Processor();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                String queryString = exchange.getRequestURI().getQuery();
                System.out.println("Query: " + queryString);
                if (queryString != null) {
                    QuoteResult res = PhraseSearcher.extractQuotedParts(queryString);
                    if(!res.getRemainingString().isEmpty()) {
                        processor.setSearchQuery(res.getRemainingString());
                        ProcessorData pdata1 = processor.getRelevantDocuments();
                    } else {
                        processor.setQuotedParts(res.getQuotedParts());
                        processor.setOperators(res.getOperators());
                        ProcessorData pdata2 = processor.getRelevantDocuments();
                    }
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