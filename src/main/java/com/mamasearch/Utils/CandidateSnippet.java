package com.mamasearch.Utils;

public class CandidateSnippet {
    public String text;
    public int startOffset;
    public  CandidateSnippet(String text , int startOffset){
        this.startOffset = startOffset;
        this.text = text;
    }
}
