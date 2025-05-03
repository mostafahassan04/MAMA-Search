package Processor;

import DBClient.MongoDBClient;
import com.mamasearch.Utils.ProcessorData;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.bson.Document;
//import org.tartarus.snowball.ext.porterStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

public class Processor {
    private final englishStemmer stemmer;
    private final TokenizerME tokenizer;
    private String searchQuery;
    private String [] quotedParts;
    private String [] operators;
    private final MongoCollection<Document> collection1;
    private static final String COLLECTION1_NAME = "inverted_index";

    private static final Set<String> stopWords = new HashSet<>(Arrays.asList(
            "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
            "you", "your", "yours", "yourself", "yourselves",
            "he", "him", "his", "himself", "she", "her", "hers", "herself",
            "it", "its", "itself", "they", "them", "their", "theirs", "themselves",
            "what", "which", "who", "whom", "this", "that", "these", "those",
            "am", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "having",
            "do", "does", "did", "doing",
            "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while",
            "of", "at", "by", "for", "with", "about", "against", "between", "into", "through",
            "during", "before", "after", "above", "below", "to", "from", "up", "down", "in",
            "out", "on", "off", "over", "under", "again", "further", "then", "once", "here",
            "there", "when", "where", "why", "how", "all", "any", "both", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own",
            "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don",
            "should", "now"
    ));

    public Processor() throws IOException {
        this.stemmer = new englishStemmer();
        try (InputStream modelIn = getClass().getResourceAsStream("/models/en-token.bin")) {
            if (modelIn == null) {
                throw new IOException("Model file 'en-token.bin' not found in src/main/resources/models. " +
                        "Download from http://opennlp.sourceforge.net/models-1.5/en-token.bin");
            }
            TokenizerModel model = new TokenizerModel(modelIn);
            this.tokenizer = new TokenizerME(model);
        }
        MongoDatabase database = MongoDBClient.getDatabase();
        this.collection1 = database.getCollection(COLLECTION1_NAME);

        // Pre-warm MongoDB connection with ping operation
        try {
            database.runCommand(new Document("ping", 1));
        } catch (Exception e) {
            System.err.println("Failed to pre-warm MongoDB connection: " + e.getMessage());
            System.err.flush();
            throw new IOException("MongoDB ping failed: " + e.getMessage(), e);
        }
    }

    public void setSearchQuery(String searchQuery) {
        String prefix = "query=";
        if (searchQuery.startsWith(prefix))
            searchQuery = searchQuery.substring(prefix.length());
        this.searchQuery = URLDecoder.decode(searchQuery, StandardCharsets.UTF_8).trim();
    }

    public void setQuotedParts(String[] quotedParts) {
        for(int i = 0; i < quotedParts.length; i++) {
            quotedParts[i] = URLDecoder.decode(quotedParts[i], StandardCharsets.UTF_8).trim();
        }
        this.quotedParts = quotedParts;
    }

    public void setOperators(String[] operators) {
        for(int i = 0; i < operators.length; i++) {
            operators[i] = URLDecoder.decode(operators[i], StandardCharsets.UTF_8).trim();
        }
        this.operators = operators;
    }

    private static boolean isStopWord(String word) {
        return stopWords.contains(word.toLowerCase());
    }

    public String[] tokenizeAndStem(String input) {
        String[] tokens = tokenizer.tokenize(input);
        ArrayList<String> filteredTokens = new ArrayList<>();
        for (String token : tokens) {
            token = token.toLowerCase();
            if (!isStopWord(token)) {
                filteredTokens.add(token);
            }
        }
        String[] stemmedTokens = new String[filteredTokens.size()];
        for (int i = 0; i < filteredTokens.size(); i++) {
            String token = filteredTokens.get(i);
            stemmedTokens[i] = stem(token);
//            System.out.println("After stemming: " + stemmedTokens[i]);
        }
        return stemmedTokens;
    }

    public ProcessorData getRelevantDocuments() {
        String[] words = tokenizeAndStem(searchQuery);
        ArrayList<Document> relevantDocuments = new ArrayList<>();
        for(String word : words) {
            System.out.println("getting rel docs for word: " + word);
            Document query = new Document("word", word);
            Document doc = collection1.find(query).first();
            System.out.println("got rel docs " + doc);
            List<Document> occurrences = (List<Document>) doc.get("occurrences");
            if (occurrences != null) {
                relevantDocuments.addAll(occurrences);
            }
//            if (doc != null) {
//                relevantDocuments.add(doc);
//            }

//            if (doc != null) {
//                String url = doc.containsKey("url") ? doc.getString("url") : null;
//                Double score = doc.containsKey("score") ? doc.getDouble("score") : null;
//
//                if (url != null && score != null) {
//                    QueryDocument queryDoc = new QueryDocument(url, word, score);
//                    relevantDocuments.add(queryDoc);
//                } else {
//                    System.out.println("Skipping document for word '" + word + "': missing or null fields (url: " + url + ", score: " + score + ")");
//                }
//            }
        }
        return new ProcessorData(relevantDocuments,words);
    }

    public ProcessorData getPhraseDocuments() {
        ArrayList<Document> phraseDocuments = new ArrayList<>();
        for (int i = 0; i < quotedParts.length; i++) {
            String[] words = tokenizeAndStem(quotedParts[i]);
            ArrayList<Document> quoteDocuments = new ArrayList<>();
            Map<String, List<Document>> mp = new HashMap<>();
            for (String word : words) {
                Document query = new Document("word", word);
                Document doc = collection1.find(query).first();
//                System.out.println("got rel docs " + doc);
                if (doc != null) {
                    List<Document> occurrences = doc.containsKey("urls") ?
                            doc.getList("urls", Document.class) :
                            new ArrayList<>();
                    for (Document occurrence : occurrences) {
                        String url = occurrence.getString("url");
                        if (url != null && !mp.containsKey(url)) {
                            mp.put(url, new ArrayList<>());
                        }
                        mp.get(url).add(occurrence);
                    }
                }
            }
//            System.out.println("mp size: " + mp.size());
            for (String url : mp.keySet()) {
                List<Document> occurrences = mp.get(url);
                if (occurrences.size() == words.length) {
                    List<Integer> firstWordPos = occurrences.getFirst().getList("positions", Integer.class);
                    for (Integer pos : firstWordPos) {
                        if (checkPosition(occurrences, pos, words.length, 1)) {
                            Double scr = 0.0;
                            for (Document occurrence : occurrences) {
                                scr += occurrence.getDouble("score");
                            }
                            scr /= occurrences.size();
                            quoteDocuments.add(new Document("url", url).append("score", scr));
//                            System.out.println("url: " + url);
                            break;
                        }
                    }
                }
            }
            switch (operators[i]) {
                case "AND" -> {
                    // Get URLs from both lists
                    Set<String> phraseUrls = phraseDocuments.stream()
                            .map(doc -> doc.getString("url"))
                            .collect(Collectors.toSet());

                    // Keep only quoteDocuments whose url is in phraseUrls
                    ArrayList<Document> intersection = new ArrayList<>();
                    for (Document quoteDoc : quoteDocuments) {
                        if (phraseUrls.contains(quoteDoc.getString("url"))) {
                            intersection.add(quoteDoc);
                        }
                    }

                    // Update phraseDocuments: clear and add intersection
                    phraseDocuments.clear();
                    phraseDocuments.addAll(intersection);
                }
                case "OR" -> {
                    // Union: Add quoteDocuments to phraseDocuments, avoiding duplicates by url
                    Set<String> phraseUrls = phraseDocuments.stream()
                            .map(doc -> doc.getString("url"))
                            .collect(Collectors.toSet());
                    for (Document quoteDoc : quoteDocuments) {
                        if (!phraseUrls.contains(quoteDoc.getString("url"))) {
                            phraseDocuments.add(quoteDoc);
                        }
                    }
                }
                case "NOT" -> {
                    // Difference: Remove quoteDocuments' urls from phraseDocuments
                    Set<String> quoteUrls = quoteDocuments.stream()
                            .map(doc -> doc.getString("url"))
                            .collect(Collectors.toSet());
                    phraseDocuments.removeIf(doc -> quoteUrls.contains(doc.getString("url")));
                }
                case null, default -> phraseDocuments.addAll(quoteDocuments);
            }
        }
        for(Document doc : phraseDocuments) {
            System.out.println("phrase doc: " + doc);
        }
        return new ProcessorData(phraseDocuments,quotedParts); // for now to test needs to be updated with the right  values
    }

    private boolean checkPosition(List<Document> positions, Integer position, int length, int i) {
        if(i == length - 1) {
            return true;
        }
        Document doc = positions.get(i);
        List<Integer> pos = doc.getList("positions", Integer.class);
        if(pos.contains(position + 1)) {
            return checkPosition(positions, position + 1, length, i+1);
        } else {
            return false;
        }
    }

    private String stem(String text) {
        stemmer.setCurrent(text);
        stemmer.stem();
        return stemmer.getCurrent();
    }
}