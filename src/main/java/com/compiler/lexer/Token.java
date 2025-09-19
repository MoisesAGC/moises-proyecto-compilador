package com.compiler.lexer;

/**
 * Represents a lexical token produced by the tokenizer.
 */
public class Token {
    /** The type of the token. */
    private final String type;

    /** The exact string value matched in the input. */
    private final String value;

    /** The position (index) in the input string where the token begins. */
    private final int position;

    /**
     * Constructs a new Token.
     *
     * @param type     the type of the token
     * @param value    the actual lexeme matched from the input
     * @param position the starting position of the token in the input string
     */
    public Token(String type, String value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }

    /**
     * Returns the type of the token.
     *
     * @return the token type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the exact string value (lexeme) matched in the input.
     *
     * @return the token value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the starting position of the token in the input string.
     *
     * @return the token position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Returns a string representation of the token for debugging purposes.
     *
     * @return a string containing the type, value, and position
     */
    @Override
    public String toString() {
        return String.format("The Token{type='%s', value='%s', pos=%d}", type, value, position);
    }
}


