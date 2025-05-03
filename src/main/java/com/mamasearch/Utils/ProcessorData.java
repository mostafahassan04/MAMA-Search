package com.mamasearch.Utils;


import org.bson.Document;

import java.util.ArrayList;

public class ProcessorData {
    public ArrayList<Document> relevantDocuments;
    public String[] words;
    public ProcessorData(ArrayList<Document> relevantDocuments , String[] words){
        this.words=words;
        this.relevantDocuments = relevantDocuments;
    }
}
