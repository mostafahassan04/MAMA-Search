package Processor;

import DBClient.MongoDBClient;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class Processor {
    private final englishStemmer stemmer;
    private final TokenizerME tokenizer;
    private final String searchQuery;
    private final MongoCollection<Document> collection1;
    private static final String COLLECTION1_NAME = "inverted_index";
    private MongoDatabase database;


    private static final Set<String> stopwords = new HashSet<>(Arrays.asList(
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

    public Processor(String searchQuery) throws IOException {
        String prefix = "query=";
        if (searchQuery.startsWith(prefix))
            searchQuery = searchQuery.substring(prefix.length());
        this.searchQuery = URLDecoder.decode(searchQuery, StandardCharsets.UTF_8.name());
        this.stemmer = new englishStemmer();
        try (InputStream modelIn = getClass().getResourceAsStream("/models/en-token.bin")) {
            if (modelIn == null) {
                throw new IOException("Model file 'en-token.bin' not found in src/main/resources/models. " +
                        "Download from http://opennlp.sourceforge.net/models-1.5/en-token.bin");
            }
            TokenizerModel model = new TokenizerModel(modelIn);
            this.tokenizer = new TokenizerME(model);
        }
        this.database = MongoDBClient.getDatabase();
        this.collection1 = database.getCollection(COLLECTION1_NAME);
    }

    private static boolean isStopword(String word) {
        return stopwords.contains(word.toLowerCase());
    }

    public String[] tokenizeAndStem() {
        String[] tokens = tokenizer.tokenize(searchQuery);
        ArrayList<String> filteredTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!isStopword(token)) {
                filteredTokens.add(token);
            }
        }
        String[] stemmedTokens = new String[filteredTokens.size()];
        for (int i = 0; i < filteredTokens.size(); i++) {
            String token = filteredTokens.get(i);
            System.out.println("Before stemming: " + token);
            stemmedTokens[i] = stem(token);
            System.out.println("After stemming: " + stemmedTokens[i]);
        }
        return stemmedTokens;
    }

    public ArrayList<Document> getRelevantDocuments(String[] words) {
        ArrayList<Document> relevantDocuments = new ArrayList<>();
        for(String word : words) {
            System.out.println("getting rel docs for word: " + word);
            Document query = new Document("word", word);
            Document doc = collection1.find(query).first();
            System.out.println("got rel docs " + doc);
            if (doc != null) {
                relevantDocuments.add(doc);
            }
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
        return relevantDocuments;
    }

    private String stem(String text) {
        stemmer.setCurrent(text);
        stemmer.stem();
        return stemmer.getCurrent();
    }
}