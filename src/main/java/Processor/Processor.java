package Processor;

import DBClient.MongoDBClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.bson.Document;
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
    public ArrayList<String> allTokens;
    private final MongoCollection<Document> collection1;
    private final MongoCollection<Document> collection2;
    private static final String COLLECTION1_NAME = "inverted_index";
    private static final String COLLECTION2_NAME = "search_queries";

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
        this.collection2 = database.getCollection(COLLECTION2_NAME);
        allTokens = new ArrayList<>();
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

    public List<String> getSuggestions(String query) {
        List<String> allQueries = Objects.requireNonNull(collection2.find().first()).getList("queries", String.class);

        return allQueries.stream()
                .filter(q -> q != null && q.toLowerCase().startsWith(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void insertSearchQuery(String query) {
        if(query.startsWith("query="))
            query = query.substring(6);
        Document filter = new Document();
        Document update = new Document("$addToSet", new Document("queries", query));

        collection2.updateOne(filter, update);
    }

    private static boolean isStopWord(String word) {
        return stopWords.contains(word.toLowerCase());
    }

    public String[] getAllTokens() {
        return Arrays.copyOf(allTokens.toArray(), allTokens.size(), String[].class);
    }

    public ArrayList<String> tokenize(String input) {
        String[] tokens = tokenizer.tokenize(input);
        ArrayList<String> filteredTokens = new ArrayList<>();
        for (String token : tokens) {
            token = token.toLowerCase();
            if (!isStopWord(token)) {
                filteredTokens.add(token);
            }
        }
        allTokens.addAll(filteredTokens);
        return filteredTokens;
    }

    public String[] stemAll(ArrayList<String> tokens) {
        String[] stemmedTokens = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            stemmedTokens[i] = stem(token);
        }
        return stemmedTokens;
    }

    public ArrayList<Document> getRelevantDocuments() {
        allTokens.clear();
        String[] words = stemAll(tokenize(searchQuery));
        ArrayList<Document> relevantDocuments = new ArrayList<>();
        for(String word : words) {
            Document query = new Document("word", word);
            Document doc = collection1.find(query).first();
            if (doc != null) {
                List<Document> occurrences = doc.containsKey("ids") ?
                        doc.getList("ids", Document.class) :
                        new ArrayList<>();
                relevantDocuments.addAll(occurrences);
            }
        }
        return relevantDocuments;
    }

    public ArrayList<Document> getPhraseDocuments() {
        allTokens.clear();
        ArrayList<Document> phraseDocuments = new ArrayList<>();

        for (int i = 0; i < quotedParts.length; i++) {
            // Tokenize and stem the current quoted phrase
            String[] words = stemAll(tokenize(quotedParts[i]));
            ArrayList<Document> currentPhraseDocs = new ArrayList<>();
            Map<Integer, List<Document>> docOccurrences = new HashMap<>();

            // Find documents containing all words in the phrase
            for (String word : words) {
                Document query = new Document("word", word);
                Document doc = collection1.find(query).first();
                if (doc != null) {
                    List<Document> occurrences = doc.containsKey("ids") ?
                            doc.getList("ids", Document.class) :
                            new ArrayList<>();
                    for (Document occurrence : occurrences) {
                        Integer id = occurrence.getInteger("id");
                        if (id != null) {
                            docOccurrences.computeIfAbsent(id, k -> new ArrayList<>()).add(occurrence);
                        }
                    }
                }
            }

            // Check for phrase matches (sequential word positions)
            for (Map.Entry<Integer, List<Document>> entry : docOccurrences.entrySet()) {
                Integer id = entry.getKey();
                List<Document> occurrences = entry.getValue();
                if (occurrences.size() == words.length) {
                    List<Integer> firstWordPos = occurrences.get(0).getList("positions", Integer.class);
                    for (Integer pos : firstWordPos) {
                        if (checkPosition(occurrences, pos, words.length, 1)) {
                            currentPhraseDocs.addAll(occurrences);
                            break;
                        }
                    }
                }
            }

            // Apply operator to combine with previous results
            if (i == 0) {
                // First phrase: initialize phraseDocuments
                phraseDocuments.addAll(currentPhraseDocs);
            } else {
                String operator = operators[i]; // Operator between phrase i-1 and i
                ArrayList<Document> newPhraseDocuments = new ArrayList<>();
                Set<Integer> currentIds = currentPhraseDocs.stream()
                        .map(doc -> doc.getInteger("id"))
                        .collect(Collectors.toSet());
                Set<Integer> previousIds = phraseDocuments.stream()
                        .map(doc -> doc.getInteger("id"))
                        .collect(Collectors.toSet());

                switch (operator) {
                    case "AND":
                        // Keep documents present in both sets
                        for (Document doc : phraseDocuments) {
                            if (currentIds.contains(doc.getInteger("id"))) {
                                newPhraseDocuments.add(doc);
                            }
                        }
                        break;
                    case "OR":
                        // Combine documents, avoiding duplicates
                        newPhraseDocuments.addAll(phraseDocuments);
                        for (Document doc : currentPhraseDocs) {
                            if (!previousIds.contains(doc.getInteger("id"))) {
                                newPhraseDocuments.add(doc);
                            }
                        }
                        break;
                    case "NOT":
                        // Keep documents from phraseDocuments not in currentPhraseDocs
                        for (Document doc : phraseDocuments) {
                            if (!currentIds.contains(doc.getInteger("id"))) {
                                newPhraseDocuments.add(doc);
                            }
                        }
                        break;
                    case null:
                    default:
                        // Default: union (like OR)
                        newPhraseDocuments.addAll(phraseDocuments);
                        for (Document doc : currentPhraseDocs) {
                            if (!previousIds.contains(doc.getInteger("id"))) {
                                newPhraseDocuments.add(doc);
                            }
                        }
                        break;
                }
                phraseDocuments = newPhraseDocuments;
            }
        }

        return phraseDocuments;
    }
    private boolean checkPosition(List<Document> positions, Integer position, int length, int i) {
        if(i == length) {
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