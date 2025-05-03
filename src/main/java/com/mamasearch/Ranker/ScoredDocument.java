package com.mamasearch.Ranker;

public class ScoredDocument implements Comparable<ScoredDocument> {
    private final String url;
    private final String title;
    private final String snippet;
    private final Double score;

    ScoredDocument(String url , String title,String snippet , Double score ) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.score = score;
    }


    public Double getScore() {
        return score;
    }

    @Override
    public int compareTo(ScoredDocument other) {
        return Double.compare(other.score, this.score);
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

}
