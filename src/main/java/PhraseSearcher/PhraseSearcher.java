package PhraseSearcher;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhraseSearcher {
    public static class QuoteResult {
        private final String[] quotedParts;
        private final String remainingString;

        public QuoteResult(String[] quotedParts, String remainingString) {
            this.quotedParts = quotedParts;
            this.remainingString = remainingString;
        }

        public String[] getQuotedParts() {
            return quotedParts;
        }

        public String getRemainingString() {
            return remainingString;
        }
    }

    public static QuoteResult extractQuotedParts(String input) {
        if (input == null) {
            return new QuoteResult(new String[0], "");
        }

        ArrayList<String> quotedParts = new ArrayList<>();

        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(input);

        StringBuilder remaining = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            remaining.append(input.substring(lastEnd, matcher.start()));
            quotedParts.add(matcher.group(1));
            lastEnd = matcher.end();
        }

        remaining.append(input.substring(lastEnd));

        String[] quotedArray = quotedParts.toArray(new String[0]);

        return new QuoteResult(quotedArray, remaining.toString());
    }
}
