package Processor;

public class QueryDocument {
    private String url;
    private String word;
    private Double score;

    public QueryDocument(String url, String word, Double score) {
        this.url = url;
        this.word = word;
        this.score = score;
    }
    public String getUrl() {
        return url;
    }
    public String getWord() {
        return word;
    }
    public Double getScore() {
        return score;
    }
}
