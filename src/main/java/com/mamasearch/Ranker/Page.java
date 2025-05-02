package com.mamasearch.Ranker;

public class Page {
    private final Integer id;
    private Double pageRank;

    Page(Integer id) {
        this.id = id;
        this.pageRank = 0.0;
    }


    public Integer getId() {
        return id;
    }

    public Double getPageRank() {
        return pageRank;
    }

    public void setPageRank(Double pageRank) {
        this.pageRank = pageRank;
    }
}
