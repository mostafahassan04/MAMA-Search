package PhraseSearcher;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhraseSearcher {
    public static class QuoteResult {
        private final String[] quotedParts;
        private final String remainingString;
        private final String[] operators;

        public QuoteResult(String[] quotedParts, String remainingString, String[] operators) {
            this.quotedParts = quotedParts;
            this.remainingString = remainingString;
            this.operators = operators;
        }

        public String[] getQuotedParts() {
            return quotedParts;
        }

        public String getRemainingString() {
            return remainingString;
        }

        public String[] getOperators() {
            return operators;
        }
    }

    public static QuoteResult extractQuotedParts(String input) {
        if (input == null || input.isEmpty()) {
            return new QuoteResult(new String[0], input == null ? "" : input, new String[0]);
        }

        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(input);
        ArrayList<String> quotedParts = new ArrayList<>();
        ArrayList<String> operators = new ArrayList<>();
        int lastEnd = 0;
        boolean fullyConsumed = true;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            String between = input.substring(lastEnd, start).trim();
            System.out.println("Between: " + between);
            if (between.toLowerCase().contains("or")) {
                operators.add("OR");
            } else if (between.toLowerCase().contains("and")) {
                operators.add("AND");
            } else if (between.toLowerCase().contains("not")) {
                operators.add("NOT");
            } else {
                operators.add("");
            }

            quotedParts.add(matcher.group(1));
            lastEnd = end;
        }

        String[] quotedArray = quotedParts.toArray(new String[0]);
        String[] operatorsArray = operators.toArray(new String[0]);
        String remaining = quotedArray.length > 0 ? "" : input;
        System.out.println("Remaining: " + remaining);
        System.out.println("Quoted: " + quotedParts);
        System.out.println("Operators: " + operators);
        return new QuoteResult(quotedArray, remaining, operatorsArray);
    }
}