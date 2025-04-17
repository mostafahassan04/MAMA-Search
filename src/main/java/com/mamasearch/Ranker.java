package com.mamasearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class Document {
    private final String id;
    private final Map<String, Integer> termsFreq;
    private final Integer totalTermsCount;

    Document(String id, Map<String, Integer> termsFreq, Integer totalTermsCount) {
        this.id = id;
        this.totalTermsCount = totalTermsCount;
        this.termsFreq = termsFreq;
    }

    public Integer getTermFreq(String term) {
        return termsFreq.get(term);
    }

    public Integer getTotalTermsCount() {
        return totalTermsCount;
    }

    public String getId() {
        return id;
    }
}

class ScoredDocument implements Comparable<ScoredDocument> {
    private final Document document;
    private Double score;

    ScoredDocument(Document document, Double score) {
        this.document = document;
        this.score = score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getScore() {
        return score;
    }

    @Override
    public int compareTo(ScoredDocument other) {
        return Double.compare(other.score, this.score);
    }

    public Document getDocument() {
        return document;
    }
}

public class Ranker {

    Map<String, Integer> documentFrequencies; //term -> no documents containing it
    Integer totalNumberOfDocuments;

    public List<ScoredDocument> rankDocument(List<String> queryTerms, List<Document> documents) {

        List<ScoredDocument> scoredDocumentsList = new ArrayList<ScoredDocument>();
        for (Document document : documents) {
            double score = 0.0;
            for (String queryTerm : queryTerms) {
                score += calculateTF(document, queryTerm) * calculateIDF(queryTerm);
            }
            ScoredDocument s = new ScoredDocument(document, score);
            scoredDocumentsList.add(s);
        }
        Collections.sort(scoredDocumentsList);
        return scoredDocumentsList;
    }

    public Double calculateTF(Document document, String term) {
        double tf;
        if (document.getTermFreq(term) == null || document.getTermFreq(term) <= 0 || document.getTotalTermsCount() == 0) {
            return 0.0;
        }
        tf = 1 + Math.log10(document.getTermFreq(term));
        return tf / document.getTotalTermsCount();
    }

    public Double calculateIDF(String term) {
        long docsWithTerm = documentFrequencies.get(term);
        if (docsWithTerm == 0)
            return 0.0;
        return Math.log10((double) totalNumberOfDocuments / docsWithTerm);
    }


}
