package com.mamasearch.Indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentData {
    private final String url;
    private final String content;
    private List<ParsedWord> filteredWords = new ArrayList<ParsedWord>();

    public DocumentData(String url, String content) {
        this.url = url;
        this.content = content;
        this.filteredWords = DocumentParser.ParesingandFilteringDocuments(this.content);
    }

    public String getUrl() { return url; }
    public String getContent() { return content; }
    public List<ParsedWord> getFilteredWords() {return filteredWords;}
    public Map<String,Double> getTF() {return TFCalculator.calculateWeightedTF(filteredWords);}

}
