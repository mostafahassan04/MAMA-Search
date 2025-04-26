package com.mamasearch.Indexer;

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class DocumentParser {
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
