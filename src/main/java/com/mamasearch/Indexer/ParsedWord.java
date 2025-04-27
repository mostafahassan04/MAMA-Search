package com.mamasearch.Indexer;

import java.util.Objects;

public class ParsedWord {
    String word;
    String tag;
    int position;


    public ParsedWord(String word, String tag, int position) {
        this.word = Objects.requireNonNull(word, "Word cannot be null");
        this.tag = Objects.requireNonNull(tag, "Tag cannot be null");
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public String getWord() {
        return word;
    }

    public String getTag() {
        return tag;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setWord(String word) {
        this.word = word;
    }
}

