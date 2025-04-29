package com.mamasearch.Indexer;

import java.util.Set;
import org.tartarus.snowball.ext.englishStemmer;
import java.util.*;

public class Tokenizer {
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

    private static boolean isStopword(String word) {
        return stopwords.contains(word.toLowerCase());
    }

    private static boolean isValidWord(String word) {

        if (word == null || word.isEmpty()) return false;
        if (word.length() < 2 || word.length() > 25) {
            return false;
        }
        if (!word.matches("[a-zA-Z]+")) {
            return false;
        }
        return true;
    }


    private static String stemWord(String word) {
        englishStemmer stemmer = new englishStemmer();
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        }
        return word;  // return the original word if stemming doesn't change it
    }

    public static List<String> filter(List<String> Words) {
        List<String> filtered = new ArrayList<>();

        for (String word : Words) {
            word = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (!isStopword(word) && isValidWord(word)) {
                word = stemWord(word);
                filtered.add(word);
            }
        }

        return filtered;
    }
}
