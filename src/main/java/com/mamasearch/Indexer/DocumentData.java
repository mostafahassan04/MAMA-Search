package com.mamasearch.Indexer;

public class DocumentData {
    private String url;
    private String content;

    public DocumentData(String url, String content) {
        this.url = url;
        this.content = content;
    }

    public String getUrl() { return url; }
    public String getContent() { return content; }
}
