package Processor;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.tartarus.snowball.ext.porterStemmer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

public class Processor {
    private final porterStemmer stemmer;
    private final TokenizerME tokenizer;
    private final String searchQuery;

    public Processor(String searchQuery) throws IOException {
        String prefix = "query=";
        if (searchQuery.startsWith(prefix))
            searchQuery = searchQuery.substring(prefix.length());
        this.searchQuery = URLDecoder.decode(searchQuery, StandardCharsets.UTF_8.name());
        this.stemmer = new porterStemmer();
        try (InputStream modelIn = getClass().getResourceAsStream("/models/en-token.bin")) {
            if (modelIn == null) {
                throw new IOException("Model file 'en-token.bin' not found in src/main/resources/models. " +
                        "Download from http://opennlp.sourceforge.net/models-1.5/en-token.bin");
            }
            TokenizerModel model = new TokenizerModel(modelIn);
            this.tokenizer = new TokenizerME(model);
        }
    }

    public String[] tokenizeAndStem() {
        String[] tokens = tokenizer.tokenize(searchQuery);
        String[] stemmedTokens = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            System.out.println("Before stemming: " + tokens[i]);
            stemmedTokens[i] = stem(tokens[i]);
            System.out.println("After stemming: " + stemmedTokens[i]);
        }
        return stemmedTokens;
    }

    private String stem(String text) {
        stemmer.setCurrent(text);
        stemmer.stem();
        return stemmer.getCurrent();
    }
}