package com.compiler.lexer;

import java.util.*;
import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;

/**
 * The Tokenizer class is responsible for scanning an input string and
 * breaking it into a sequence of Token objects based on a set of lexical rules.
 */
public class Tokenizer {

    /** A list of tokenization rules, sorted by descending priority. */
    private final List<TokenRule> rules = new ArrayList<>();

    /**
     * Adds a new tokenization rule to the tokenizer.
     *
     * @param dfa        the DFA that recognizes the token's pattern
     * @param tokenType  the symbolic name of the token (e.g., "IDENTIFIER", "NUMBER")
     * @param priority   the rule's priority; higher values take precedence when multiple rules match
     */
    public void addRule(DFA dfa, String tokenType, int priority) {
        rules.add(new TokenRule(dfa, tokenType, priority));
        rules.sort(Comparator.comparingInt(TokenRule::getPriority).reversed());
    }

    /**
     * Tokenizes the given input string into a list of Token objects.
     *
     * @param input the input string to tokenize
     * @return a list of tokens representing the input
     * @throws RuntimeException if no valid token is found at a given position
     */
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;

        while (position < input.length()) {
            TokenMatch match = findBestMatch(input, position);
            if (match == null) {
                throw new RuntimeException("No valid token found at position " + position +
                    " for character '" + input.charAt(position) + "'");
            }
            tokens.add(new Token(match.tokenType(), match.value(), position));
            position += match.length();
        }

        return tokens;
    }

    /**
     * Finds the best matching token rule at the specified input position.
     * The best match is defined as the longest valid match, with priority used to break ties.
     *
     * @param input    the input string
     * @param startPos the starting position in the input
     * @return the best matching {@link TokenMatch}, or {@code null} if no match is found
     */
    private TokenMatch findBestMatch(String input, int startPos) {
        return rules.stream()
            .map(rule -> tryMatch(rule, input, startPos))
            .filter(Objects::nonNull)
            .max(Comparator.comparingInt(TokenMatch::length)
                .thenComparingInt(TokenMatch::priority))
            .orElse(null);
    }

    /**
     * Attempts to match a given rule against the input starting at a specific position.
     * The method walks through the DFA and tracks the longest match that ends in a final state.
     *
     * @param rule     the token rule to apply
     * @param input    the input string
     * @param startPos the starting position in the input
     * @return a {@link TokenMatch} if the rule matches, or {@code null} otherwise
     */
    private TokenMatch tryMatch(TokenRule rule, String input, int startPos) {
        int maxLength = 0;
        int currentPos = startPos;
        DfaState state = rule.getDfa().startState;

        while (currentPos < input.length() && state != null) {
            char c = input.charAt(currentPos);
            state = state.transitions.get(c);
            if (state != null && state.isFinal) {
                maxLength = currentPos - startPos + 1;
            }
            currentPos++;
        }

        if (maxLength > 0) {
            String value = input.substring(startPos, startPos + maxLength);
            return new TokenMatch(rule.getTokenType(), value, maxLength, rule.getPriority());
        }

        return null;
    }

    /**
     * Represents a candidate match for a token rule.
     * This record encapsulates the token type, matched value, match length, and rule priority.
     *
     * @param tokenType the symbolic type of the token
     * @param value     the matched substring from the input
     * @param length    the length of the matched substring
     * @param priority  the priority of the rule that produced the match
     */
    private record TokenMatch(String tokenType, String value, int length, int priority) {}
}


