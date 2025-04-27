package com.mamasearch.Indexer;
import java.util.ArrayList;
import java.util.List;

public class WordData {
    private List<Integer> positions;
    private double score;

    public WordData() {
        this.positions = new ArrayList<>();
        this.score = 0.0;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public double getScore() {
        return score;
    }

    public void addPosition(int position) {
        this.positions.add(position);
    }

    public void setScore(double score) {
        this.score = score;
    }
}
