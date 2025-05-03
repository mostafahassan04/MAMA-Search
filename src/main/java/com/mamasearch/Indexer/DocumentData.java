package com.mamasearch.Indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocumentData {
    private final Integer ID;
    private final String content;
    private List<ParsedWord> filteredWords = new ArrayList<ParsedWord>();

    public DocumentData(Integer ID, String content) {
        this.ID = ID;
        this.content = content;
        this.filteredWords = DocumentParser.ParesingandFilteringDocuments(ID, this.content);
    }

    public Integer getID() { return ID; }
    public String getContent() { return content; }
    public List<ParsedWord> getFilteredWords() {return filteredWords;}
    public Map<String,Double> getTF() {return TFCalculator.calculateWeightedTF(filteredWords);}

}
