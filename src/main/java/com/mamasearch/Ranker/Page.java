package com.mamasearch.Ranker;

public class Page {
    private final String url;
    private Double pageRank;

    Page(String url) {
        this.url = url;
        this.pageRank = 0.0;
    }

    public String getUrl() {
        return url;
    }

    public Double getPageRank() {
        return pageRank;
    }

    public void setPageRank(Double pageRank) {
        this.pageRank = pageRank;
    }
}
