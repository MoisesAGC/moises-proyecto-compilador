package com.compiler.lexer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.regex.RegexParser;

/**
 * The TokenizerBuilder class is responsible for constructing a Tokenizer
 * from a set of token rules defined using regular expressions.
 */
public class TokenizerBuilder {

    /** Utility for parsing regular expressions into NFAs. */
    private final RegexParser regexParser = new RegexParser();

    /**
     * Builds a Tokenizer from a map of token types to regular expressions.
     * @param tokenRules a map where keys are token types and values are regex patterns
     * @param alphabet   the set of valid input characters for the DFA
     * @return a fully constructed Tokenizer capable of recognizing the specified rules
     * @throws RuntimeException if any regex fails to compile or convert
     */
    public Tokenizer buildTokenizer(Map<String, String> tokenRules, Set<Character> alphabet) {
        Tokenizer tokenizer = new Tokenizer();
        AtomicInteger priorityCounter = new AtomicInteger(1000); // Start with high priority

        tokenRules.forEach((tokenType, regex) -> {
            try {
                DFA dfa = compileRegexToDfa(regex, alphabet);
                tokenizer.addRule(dfa, tokenType, priorityCounter.getAndDecrement());
            } catch (Exception e) {
                throw new RuntimeException("Error compiling rule for token: " + tokenType +
                    " with regex: " + regex, e);
            }
        });

        return tokenizer;
    }

    /**
     * Compiles a regular expression into a minimized DFA.
     * @param regex    the regular expression to compile
     * @param alphabet the set of valid input characters
     * @return a minimized DFA that recognizes the given regex
     * @throws Exception if parsing or conversion fails
     */
    private DFA compileRegexToDfa(String regex, Set<Character> alphabet) throws Exception {
        NFA nfa = regexParser.parse(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        return DfaMinimizer.minimizeDfa(dfa, alphabet);
    }
}



