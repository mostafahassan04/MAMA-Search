package com.mamasearch;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;

class ParsedWord {
    String word;
    String tag;
    int position;


    public ParsedWord(String word, String tag, int position) {
        this.word = Objects.requireNonNull(word, "Word cannot be null");
        this.tag = Objects.requireNonNull(tag, "Tag cannot be null");
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public String getWord() {
        return word;
    }

    public String getTag() {
        return tag;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setWord(String word) {
        this.word = word;
    }
}


class DocumentParser {

    public List<ParsedWord> parseFile(String htmlContent) {
        List<ParsedWord> parsedWords = new ArrayList<>();
        int position = 0;

        Document doc = Jsoup.parse(htmlContent);

        // Title
        String title = doc.title();
        position = addWords(parsedWords, title, "title", position);

        // h1
        Elements h1Tags = doc.select("h1");
        position = extractAndAdd(parsedWords, h1Tags, "h1", position);

        // h2
        Elements h2Tags = doc.select("h2");
        position = extractAndAdd(parsedWords, h2Tags, "h2", position);

        // h3
        Elements h3Tags = doc.select("h3");
        position = extractAndAdd(parsedWords, h3Tags, "h3", position);

        // Normal (rest of body)
        Elements allTextElements = doc.body().getAllElements();
        Elements normalTags = new Elements();
        for (Element element : allTextElements) {
            if (!h1Tags.contains(element) && !h2Tags.contains(element) && !h3Tags.contains(element)) {
                normalTags.add(element);
            }
        }
        position = extractAndAdd(parsedWords, normalTags, "normal", position);

        return parsedWords;
    }

    private int extractAndAdd(List<ParsedWord> list, Elements elements, String tag, int startPosition) {
        for (Element element : elements) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                startPosition = addWords(list, text, tag, startPosition);
            }
        }
        return startPosition;
    }

    private int addWords(List<ParsedWord> list, String text, String tag, int startPosition) {
        String[] words = text.split("\\s+");
        for (String word : words) {
            list.add(new ParsedWord(word, tag, startPosition++));
        }
        return startPosition;
    }
}


class Tokenizer {
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

    private static String stemWord(String word) {
        englishStemmer stemmer = new englishStemmer();
        stemmer.setCurrent(word);
        if (stemmer.stem()) {
            return stemmer.getCurrent();
        }
        return word;  // return the original word if stemming doesn't change it
    }

    public static List<ParsedWord> filter(List<ParsedWord> parsedWords) {
        List<ParsedWord> filtered = new ArrayList<>();

        for (ParsedWord pw : parsedWords) {
            String cleaned = pw.getWord().replaceAll("[^a-zA-Z]", "").toLowerCase();
            if (!cleaned.isEmpty() && !isStopword(cleaned)) {
                String stemmedWord = stemWord(cleaned);
                pw.setWord(stemmedWord);
                filtered.add(pw);
            }
        }

        return filtered;
    }
}

public class Indexer {
}