package com.mamasearch.Utils;

public class TokenInfo {
    public final String normalizedText;
    public final int startCharOffset;
    public final int endCharOffset;
    public TokenInfo(String normalizedText , int startCharOffset , int endCharOffset){
        this.endCharOffset=endCharOffset;
        this.normalizedText=normalizedText;
        this.startCharOffset=startCharOffset;
    }
}
