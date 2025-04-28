package com.mamasearch.Ranker;

import java.util.Map;

public class Document {
    private final String id;
    private final Map<String, Integer> bodyTermsFreq;
    private final Map<String, Integer> titleTermsFreq;
    private final Map<String, Integer> headerTermsFreq;
    private final Map<String, Integer> urlTermsFreq;
    private final Integer totalTermsCount;

    Document(String id, Integer totalTermsCount, Map<String, Integer> bodyTermsFreq, Map<String, Integer> titleTermsFreq,
             Map<String, Integer> headerTermsFreq, Map<String, Integer> urlTermsFreq) {
        this.id = id;
        this.totalTermsCount = totalTermsCount;
        this.bodyTermsFreq = bodyTermsFreq;
        this.titleTermsFreq = titleTermsFreq;
        this.headerTermsFreq = headerTermsFreq;
        this.urlTermsFreq = urlTermsFreq;
    }

    public Integer getBodyTermFreq(String term) {
        return bodyTermsFreq.get(term);
    }

    public Integer getTitleTermFreq(String term) {
        return titleTermsFreq.get(term);
    }

    public Integer getHeaderTermFreq(String term) {
        return headerTermsFreq.get(term);
    }

    public Integer getURLTermFreq(String term) {
        return urlTermsFreq.get(term);
    }

    public Integer getTotalTermsCount() {
        return totalTermsCount;
    }

    public String getId() {
        return id;
    }
}
