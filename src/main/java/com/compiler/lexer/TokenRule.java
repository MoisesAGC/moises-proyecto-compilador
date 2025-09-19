package com.compiler.lexer;

import com.compiler.lexer.dfa.DFA;

/**
 * Represents a lexical analysis rule used by the Tokenizer.
 */
public class TokenRule {
    
    /** Deterministic Finite Automaton (DFA) used to recognize the token pattern. */
    private final DFA dfa;
    
    /** Type or category assigned to tokens recognized by this rule. */
    private final String tokenType;
    
    /** 
     * Priority of the rule. 
     * <p>Higher values indicate higher priority and are preferred in case of conflicts.</p>
     */
    private final int priority;

    /**
     * Constructs a newTokenRule.
     * @param dfa the DFA that recognizes the token pattern; must not be {@code null}.
     * @param tokenType the type or category assigned to tokens matched by the DFA; must not be {@code null}.
     * @param priority the priority of the rule, used to resolve conflicts between multiple matches.
     */
    public TokenRule(DFA dfa, String tokenType, int priority) {
        this.dfa = dfa;
        this.tokenType = tokenType;
        this.priority = priority;
    }

    /**
     * Returns the DFA associated with this token rule.
     *
     * @return the DFA that recognizes the token pattern.
     */
    public DFA getDfa() {
        return dfa;
    }

    /**
     * Returns the token type associated with this rule.
     *
     * @return the type or category assigned to matched tokens.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Returns the priority of this rule.
     *
     * @return the integer value representing rule priority.
     */
    public int getPriority() {
        return priority;
    }
}
