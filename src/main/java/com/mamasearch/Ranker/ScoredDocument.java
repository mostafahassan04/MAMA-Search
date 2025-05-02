package com.mamasearch.Ranker;

public class ScoredDocument implements Comparable<ScoredDocument> {
    private final Document document;
    private Double score;

    ScoredDocument(Document document) {
        this.document = document;
        this.score = 0.0;
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
