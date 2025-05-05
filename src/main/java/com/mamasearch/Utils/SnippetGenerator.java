package com.mamasearch.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SnippetGenerator {
    // Cache for frequently accessed files
    private static final Map<Integer, String> FILE_CACHE = new ConcurrentHashMap<>();

    // Reusable patterns
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+|\\p{N}+");
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[.!?][\"\']?\\s+|[.!?][\"\']?$");

    // Constants for snippet generation
    private static final int SENTENCE_EXTENSION_LIMIT = 100;
    private static final double COVERAGE_WEIGHT = 100.0;
    private static final double PROXIMITY_WEIGHT = 10.0;
    private static final double SINGLE_TERM_BONUS = 5.0;

    // Inner classes for data structure
    private static class TokenInfo {
        final String normalizedText;
        final int startCharOffset;
        final int endCharOffset;

        TokenInfo(String normalizedText, int startCharOffset, int endCharOffset) {
            this.normalizedText = normalizedText;
            this.startCharOffset = startCharOffset;
            this.endCharOffset = endCharOffset;
        }
    }

    private static class CandidateSnippet {
        final String text;
        final int startOffset;

        CandidateSnippet(String text, int startOffset) {
            this.text = text;
            this.startOffset = startOffset;
        }
    }

    /**
     * Generates a snippet from a file containing the query terms.
     *
     * @param fileId The ID of the file to read
     * @param queryTerms The search terms to highlight
     * @param targetLength The target length of the snippet
     * @return A highlighted snippet containing the query terms
     */
    public String generateSnippet(int fileId, String[] queryTerms, int targetLength) {
        if (queryTerms == null || queryTerms.length == 0) {
            return getDefaultSnippet(readFileWithCache(fileId), targetLength);
        }

        // Read file content
        String plainText = readFileWithCache(fileId);
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }

        // Normalize query terms
        Set<String> normalizedQueryTerms = normalizeQueryTerms(queryTerms);
        if (normalizedQueryTerms.isEmpty()) {
            return getDefaultSnippet(plainText, targetLength);
        }

        // Compile pattern once for reuse
        Pattern termPattern = compileTermPattern(normalizedQueryTerms);

        // Generate first candidate snippet based on first occurrence of query term
        CandidateSnippet firstCandidate = generateFirstCandidateSnippet(
                plainText, normalizedQueryTerms, termPattern, targetLength);

        // If first candidate generation strategy didn't yield results, use default
        if (firstCandidate.text.isEmpty()) {
            return getDefaultSnippet(plainText, targetLength);
        }

        // Add highlighting to query terms and return
        return addHighlighting(firstCandidate.text, termPattern);
    }

    /**
     * Reads file content with caching for performance
     */
    private String readFileWithCache(int fileId) {
        return FILE_CACHE.computeIfAbsent(fileId, id -> {
            try {
                Path path = Paths.get("./src/main/resources/PlainText/" + id + ".txt");
                return Files.readString(path);
            } catch (IOException e) {
                System.err.println("Error reading file " + fileId + ": " + e.getMessage());
                return "";
            }
        });
    }

    /**
     * Normalizes a set of query terms
     */
    private Set<String> normalizeQueryTerms(String[] queryTerms) {
        Set<String> normalizedTerms = new HashSet<>();
        for (String term : queryTerms) {
            String normalized = normalizeToken(term);
            if (!normalized.isEmpty()) {
                normalizedTerms.add(normalized);
            }
        }
        return normalizedTerms;
    }

    /**
     * Compiles a regex pattern for the query terms
     */
    private Pattern compileTermPattern(Set<String> normalizedQueryTerms) {
        if (normalizedQueryTerms.isEmpty()) {
            return null;
        }

        String regex = "\\b(" +
                normalizedQueryTerms.stream()
                        .map(Pattern::quote)
                        .collect(Collectors.joining("|")) +
                ")\\b";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Generates candidate snippets around term occurrences
     */
    private List<CandidateSnippet> generateCandidates(String plainText,
                                                      Map<String, List<Integer>> termTokenIndices,
                                                      List<TokenInfo> allTokens,
                                                      int targetLength) {
        List<CandidateSnippet> candidates = new ArrayList<>();

        // Process each term's occurrences
        for (List<Integer> indices : termTokenIndices.values()) {
            for (int tokenIndex : indices) {
                if (tokenIndex < 0 || tokenIndex >= allTokens.size()) continue;

                int charOffset = allTokens.get(tokenIndex).startCharOffset;

                // Window centered on the term
                int halfLength = targetLength / 2;
                int startChar = Math.max(0, charOffset - halfLength);
                int endChar = Math.min(plainText.length(), charOffset + halfLength);

                // Expand to include the full term if it's at the edge
                if (tokenIndex + 1 < allTokens.size() &&
                        allTokens.get(tokenIndex + 1).startCharOffset < endChar) {
                    endChar = Math.min(plainText.length(),
                            allTokens.get(tokenIndex + 1).endCharOffset);
                }

                String windowText = plainText.substring(startChar, endChar);
                candidates.add(new CandidateSnippet(windowText, startChar));
            }
        }

        // If we have too many candidates, sample them to avoid excessive processing
        if (candidates.size() > 100) {
            Collections.shuffle(candidates);
            return candidates.subList(0, 100);
        }

        return candidates;
    }

    private static class ScoreInfo {
        int uniqueTermsCount = 0;
        int firstTermPos = -1;
        int lastTermPos = -1;
    }

    // OPTIMIZED version - called by findBestCandidate
    private ScoreInfo calculateScoreInfo(String windowText, Set<String> normalizedQueryTerms, Pattern termPattern) {
        ScoreInfo info = new ScoreInfo();
        if (termPattern == null || windowText == null || windowText.isEmpty()) {
            return info;
        }

        Set<String> foundTerms = new HashSet<>();
        Matcher matcher = termPattern.matcher(windowText);
        int currentFirst = -1;
        int currentLast = -1;

        while (matcher.find()) {
            int matchStart = matcher.start();
            if (currentFirst == -1) {
                currentFirst = matchStart; // Found the first one
            }
            currentLast = matchStart; // Keep track of the latest one found

            String matchedTerm = matcher.group(1);
            String normalizedMatch = normalizeToken(matchedTerm);
            if (normalizedQueryTerms.contains(normalizedMatch)) {
                foundTerms.add(normalizedMatch);
            }
        }

        info.uniqueTermsCount = foundTerms.size();
        info.firstTermPos = currentFirst;
        info.lastTermPos = currentLast;
        return info;
    }

    // Modified findBestCandidate to use ScoreInfo
    private CandidateSnippet findBestCandidate(List<CandidateSnippet> candidates,
                                               Set<String> normalizedQueryTerms,
                                               Pattern termPattern,
                                               int targetLength) {
        if (candidates.isEmpty()) {
            return null; // Or return a default empty candidate
        }

        CandidateSnippet bestCandidate = candidates.get(0); // Start with the first
        double maxScore = -1.0;

        for (CandidateSnippet candidate : candidates) {
            ScoreInfo scoreInfo = calculateScoreInfo(candidate.text, normalizedQueryTerms, termPattern);

            // Calculate final score based on ScoreInfo
            double coverageScore = scoreInfo.uniqueTermsCount * COVERAGE_WEIGHT;
            double proximityScore = 0.0;
            if (scoreInfo.uniqueTermsCount > 1 && scoreInfo.firstTermPos != -1 && scoreInfo.lastTermPos != -1) {
                int span = scoreInfo.lastTermPos - scoreInfo.firstTermPos;
                proximityScore = Math.max(0.0, (double)targetLength - span) * PROXIMITY_WEIGHT;
            } else if (scoreInfo.uniqueTermsCount == 1) {
                proximityScore = SINGLE_TERM_BONUS;
            }

            // Add simple readability check from previous code if desired
            double readabilityScore = 0.0;
            // ... (calculate readabilityScore based on candidate.text) ...

            double currentScore = coverageScore + proximityScore + readabilityScore;

            if (currentScore > maxScore) {
                maxScore = currentScore;
                bestCandidate = candidate;
            }
        }
        // Handle case where no candidate scored > -1.0 (e.g., if all had 0 terms)
        if (bestCandidate == null && !candidates.isEmpty()){
            return candidates.get(0); // Or some other default
        }
        return bestCandidate;
    }

    /**
     * Calculates a score for a candidate snippet
     */
    private double calculateScore(String windowText, Set<String> normalizedQueryTerms,
                                  Pattern termPattern, int targetLength) {
        // Coverage score - how many unique terms are found
        int uniqueTerms = countUniqueTerms(windowText, normalizedQueryTerms, termPattern);
        double coverageScore = uniqueTerms * COVERAGE_WEIGHT;

        // Proximity score - how close the terms are to each other
        double proximityScore = 0.0;
        if (uniqueTerms > 1) {
            int firstPos = findFirstTermCharPos(windowText, termPattern);
            int lastPos = findLastTermCharPos(windowText, termPattern);

            if (firstPos != -1 && lastPos != -1) {
                int span = lastPos - firstPos;
                proximityScore = Math.max(0.0, targetLength - span) * PROXIMITY_WEIGHT;
            }
        } else if (uniqueTerms == 1) {
            proximityScore = SINGLE_TERM_BONUS;
        }

        // Readability score - bonus for complete sentences
        double readabilityScore = 0.0;
        boolean startsWithCapital = !windowText.isEmpty() &&
                Character.isUpperCase(windowText.charAt(0));
        boolean endsWithPunctuation = !windowText.isEmpty() &&
                ".!?".indexOf(windowText.charAt(windowText.length()-1)) >= 0;

        if (startsWithCapital) readabilityScore += 5.0;
        if (endsWithPunctuation) readabilityScore += 5.0;

        return coverageScore + proximityScore + readabilityScore;
    }

    /**
     * Refines snippet boundaries to align with sentence boundaries
     */
    private String refineBoundaries(String plainText, int windowStart, int windowLength, int targetLength) {
        int bestStart = windowStart;
        int bestEnd = Math.min(plainText.length(), windowStart + windowLength);

        // Look backward for sentence start
        int lookBackLimit = Math.max(0, bestStart - SENTENCE_EXTENSION_LIMIT);
        for (int i = bestStart; i > lookBackLimit; i--) {
            if (i == 0 || (i > 0 &&
                    (plainText.charAt(i-1) == '.' ||
                            plainText.charAt(i-1) == '!' ||
                            plainText.charAt(i-1) == '?'))) {
                if (i < plainText.length() && Character.isWhitespace(plainText.charAt(i))) {
                    bestStart = i+1;  // Skip whitespace after punctuation
                } else {
                    bestStart = i;
                }
                break;
            }
        }

        // Look forward for sentence end
        int lookForwardLimit = Math.min(plainText.length(), bestEnd + SENTENCE_EXTENSION_LIMIT);
        for (int i = bestEnd; i < lookForwardLimit; i++) {
            if (i < plainText.length() &&
                    (plainText.charAt(i) == '.' ||
                            plainText.charAt(i) == '!' ||
                            plainText.charAt(i) == '?')) {
                bestEnd = i + 1;  // Include the punctuation
                break;
            }
        }

        // Apply ellipsis as needed
        StringBuilder sb = new StringBuilder();
        boolean ellipseStart = bestStart > 0 && bestStart > windowStart - SENTENCE_EXTENSION_LIMIT;
        boolean ellipseEnd = bestEnd < plainText.length() &&
                bestEnd < windowStart + windowLength + SENTENCE_EXTENSION_LIMIT;

        if (ellipseStart) sb.append("...");
        sb.append(plainText.substring(bestStart, bestEnd));
        if (ellipseEnd) sb.append("...");

        return sb.toString();
    }

    /**
     * Tokenizes text with character offsets
     */
    private List<TokenInfo> tokenizeWithOffsets(String text) {
        List<TokenInfo> tokens = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String word = matcher.group();
            String normalized = normalizeToken(word);
            if (!normalized.isEmpty()) {
                tokens.add(new TokenInfo(normalized, matcher.start(), matcher.end()));
            }
        }

        return tokens;
    }

    /**
     * Finds indices of tokens that match query terms
     */
    private Map<String, List<Integer>> findTermTokenIndices(List<TokenInfo> allTokens,
                                                            Set<String> normalizedQueryTerms) {
        Map<String, List<Integer>> termTokenIndices = new HashMap<>();

        for (int i = 0; i < allTokens.size(); i++) {
            String currentNormalizedToken = allTokens.get(i).normalizedText;
            if (normalizedQueryTerms.contains(currentNormalizedToken)) {
                termTokenIndices.computeIfAbsent(currentNormalizedToken, k -> new ArrayList<>()).add(i);
            }
        }

        return termTokenIndices;
    }

    /**
     * Counts unique terms in the window text
     */
    private int countUniqueTerms(String windowText, Set<String> normalizedQueryTerms, Pattern termPattern) {
        if (termPattern == null || windowText == null || windowText.isEmpty()) {
            return 0;
        }

        Set<String> foundTerms = new HashSet<>();
        Matcher matcher = termPattern.matcher(windowText);

        while (matcher.find()) {
            String matchedTerm = matcher.group(1);
            String normalizedMatch = normalizeToken(matchedTerm);

            if (normalizedQueryTerms.contains(normalizedMatch)) {
                foundTerms.add(normalizedMatch);
            }
        }

        return foundTerms.size();
    }

    /**
     * Finds the position of the first term in the window
     */
    private int findFirstTermCharPos(String windowText, Pattern termPattern) {
        if (termPattern == null || windowText == null || windowText.isEmpty()) {
            return -1;
        }

        Matcher matcher = termPattern.matcher(windowText);
        return matcher.find() ? matcher.start() : -1;
    }

    /**
     * Finds the position of the last term in the window
     */
    private int findLastTermCharPos(String windowText, Pattern termPattern) {
        if (termPattern == null || windowText == null || windowText.isEmpty()) {
            return -1;
        }

        Matcher matcher = termPattern.matcher(windowText);
        int lastPos = -1;

        while (matcher.find()) {
            lastPos = matcher.start();
        }

        return lastPos;
    }

    /**
     * Normalizes a token by converting to lowercase and removing non-alphanumeric characters
     */
    private String normalizeToken(String token) {
        return token.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Gets a default snippet from the beginning of the text
     */
    private String getDefaultSnippet(String plainText, int targetLength) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }

        int endLimit = Math.min(plainText.length(), targetLength);
        StringBuilder windowText = new StringBuilder(plainText.substring(0, endLimit));

        // Try to extend to sentence boundary
        for (int i = endLimit; i < plainText.length() && windowText.length() < targetLength * 1.2; i++) {
            char ch = plainText.charAt(i);
            windowText.append(ch);

            if (ch == '.' || ch == '?' || ch == '!') {
                break;
            }
        }

        if (windowText.length() < plainText.length()) {
            windowText.append("...");
        }

        return windowText.toString();
    }

    /**
     * Generates the first candidate snippet by finding the first query term
     * and extending targetLength/2 before and after it, trying to align with paragraph boundaries.
     *
     * @param plainText The text to search in
     * @param normalizedQueryTerms The set of normalized query terms to find
     * @param termPattern The pattern to match query terms
     * @param targetLength The target length of the snippet
     * @return A CandidateSnippet object centered on the first found query term
     */
    private CandidateSnippet generateFirstCandidateSnippet(
            String plainText,
            Set<String> normalizedQueryTerms,
            Pattern termPattern,
            int targetLength) {

        if (plainText == null || plainText.isEmpty() || termPattern == null) {
            return new CandidateSnippet("", 0);
        }

        // Find the first occurrence of any query term
        Matcher matcher = termPattern.matcher(plainText);
        if (!matcher.find()) {
            // If no terms found, return a snippet from the beginning
            return new CandidateSnippet(
                    plainText.substring(0, Math.min(plainText.length(), targetLength)),
                    0
            );
        }

        // Found a match - get the center position
        int matchStart = matcher.start();
        int matchEnd = matcher.end();
        int centerPos = (matchStart + matchEnd) / 2;

        // Calculate initial window boundaries
        int halfLength = targetLength / 2;
        int initialStart = Math.max(0, centerPos - halfLength);
        int initialEnd = Math.min(plainText.length(), centerPos + halfLength);

        // Find paragraph start (looking backward for double newline or start of text)
        int paragraphStart = initialStart;
        for (int i = initialStart; i > Math.max(0, initialStart - 500); i--) {
            // Check for paragraph break patterns:
            // 1. Double newline
            if (i > 0 &&
                    plainText.charAt(i) == '\n' &&
                    plainText.charAt(i-1) == '\n') {
                paragraphStart = i + 1; // Start after the double newline
                break;
            }
            // 2. Newline followed by space/tab (indented paragraph)
            if (i > 0 &&
                    plainText.charAt(i-1) == '\n' &&
                    (plainText.charAt(i) == ' ' || plainText.charAt(i) == '\t')) {
                paragraphStart = i;
                break;
            }
            // 3. Beginning of text
            if (i == 0) {
                paragraphStart = 0;
                break;
            }
        }

        // Find paragraph end (looking forward for double newline or end of text)
        int paragraphEnd = initialEnd;
        for (int i = initialEnd; i < Math.min(plainText.length(), initialEnd + 500); i++) {
            // Check for paragraph end patterns
            if (i < plainText.length() - 1 &&
                    plainText.charAt(i) == '\n' &&
                    plainText.charAt(i+1) == '\n') {
                paragraphEnd = i;
                break;
            }
            // End of text
            if (i == plainText.length() - 1) {
                paragraphEnd = plainText.length();
                break;
            }
        }

        // If the paragraph is too long, constrain it closer to the original window
        // but ensure we don't cut in the middle of a word
        if (paragraphEnd - paragraphStart > targetLength * 2) {
            // Adjust paragraph start if needed
            if (paragraphStart < initialStart) {
                // Look for sentence beginning near initialStart
                for (int i = initialStart; i > Math.max(paragraphStart, initialStart - 200); i--) {
                    if (i > 0 &&
                            (plainText.charAt(i-1) == '.' ||
                                    plainText.charAt(i-1) == '!' ||
                                    plainText.charAt(i-1) == '?') &&
                            Character.isWhitespace(plainText.charAt(i))) {
                        paragraphStart = i;
                        break;
                    }
                }
            }

            // Adjust paragraph end if needed
            if (paragraphEnd > initialEnd) {
                // Look for sentence ending near initialEnd
                for (int i = initialEnd; i < Math.min(paragraphEnd, initialEnd + 200); i++) {
                    if (i < plainText.length() &&
                            (plainText.charAt(i) == '.' ||
                                    plainText.charAt(i) == '!' ||
                                    plainText.charAt(i) == '?')) {
                        paragraphEnd = i + 1; // Include the punctuation
                        break;
                    }
                }
            }
        }

        // Create the candidate snippet
        String snippetText = plainText.substring(paragraphStart, paragraphEnd);
        return new CandidateSnippet(snippetText, paragraphStart);
    }

    /**
     * Adds HTML highlighting to query terms
     */
    private String addHighlighting(String snippet, Pattern termPattern) {
        if (termPattern == null || snippet == null || snippet.isEmpty()) {
            return snippet;
        }

        Matcher matcher = termPattern.matcher(snippet);
        return matcher.replaceAll("<b>$1</b>");
    }
}