package com.mamasearch.Utils;

import com.mamasearch.Utils.CandidateSnippet;
import com.mamasearch.Utils.TokenInfo;

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
    public SnippetGenerator(){

    }

    private static final Map<Integer, String> FILE_CACHE = new ConcurrentHashMap<>();

    // Reusable patterns
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+|\\p{N}+");
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("[.!?][\"\']?\\s+|[.!?][\"\']?$");

    // Constants for snippet generation
    private static final int SENTENCE_EXTENSION_LIMIT = 100;
    private static final double COVERAGE_WEIGHT = 100.0;
    private static final double PROXIMITY_WEIGHT = 10.0;
    private static final double SINGLE_TERM_BONUS = 5.0;


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

        // Tokenize text with character offsets
        List<TokenInfo> allTokens = tokenizeWithOffsets(plainText);

        // Find positions of query terms
        Map<String, List<Integer>> termTokenIndices = findTermTokenIndices(allTokens, normalizedQueryTerms);

        // No terms found
        if (termTokenIndices.isEmpty()) {
            return getDefaultSnippet(plainText, targetLength);
        }

        // Generate and score candidate snippets
        List<CandidateSnippet> candidates = generateCandidates(plainText, termTokenIndices, allTokens, targetLength);
        CandidateSnippet bestCandidate = findBestCandidate(candidates, normalizedQueryTerms, termPattern, targetLength);

        // Refine snippet boundaries to align with sentences
        String refinedSnippet = refineBoundaries(plainText, bestCandidate.startOffset,
                bestCandidate.text.length(), targetLength);

        // Add highlighting to query terms
        return addHighlighting(refinedSnippet, termPattern);
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

    /**
     * Finds the best candidate snippet based on scoring
     */
    private CandidateSnippet findBestCandidate(List<CandidateSnippet> candidates,
                                               Set<String> normalizedQueryTerms,
                                               Pattern termPattern,
                                               int targetLength) {
        if (candidates.isEmpty()) {
            return null;
        }

        CandidateSnippet bestCandidate = null;
        double maxScore = -1.0;

        for (CandidateSnippet candidate : candidates) {
            double score = calculateScore(candidate.text, normalizedQueryTerms, termPattern, targetLength);
            if (score > maxScore) {
                maxScore = score;
                bestCandidate = candidate;
            }
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