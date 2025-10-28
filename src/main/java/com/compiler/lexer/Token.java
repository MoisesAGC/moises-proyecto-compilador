package com.compiler.lexer;

/**
 * Represents a lexical token produced by the tokenizer.
 * Combines structural, textual, and positional information.
 */
public class Token {

    /** The type of the token (e.g., IDENTIFIER, NUMBER, KEYWORD, etc.). */
    public final String type;

    /** The exact string value (lexeme) matched in the input. */
    private final String lexeme;

    /** The starting position of the token in the input string. */
    private final int start;

    /** The ending position (exclusive) of the token in the input string. */
    private final int end;

    /** The position alias for compatibility (same as start). */
    private final int position;

    /**
     * Constructs a Token with full positional information.
     *
     * @param type   the type of the token
     * @param lexeme the matched text
     * @param start  the start position in the input
     * @param end    the end position (exclusive)
     */
    public Token(String type, String lexeme, int start, int end) {
        this.type = type;
        this.lexeme = lexeme;
        this.start = start;
        this.end = end;
        this.position = start;
    }

    /**
     * Constructs a Token when only type and lexeme are known.
     *
     * @param type   the token type
     * @param lexeme the matched value
     */
    public Token(String type, String lexeme) {
        this(type, lexeme, -1, -1);
    }

    /**
     * Constructs a Token when only a single position is known.
     * (Alternative constructor for compatibility with other tokenizers.)
     *
     * @param type      the token type
     * @param value     the actual lexeme matched from the input
     * @param position  the starting position of the token in the input string
     */
    public Token(String type, String value, int position) {
        this.type = type;
        this.lexeme = value;
        this.start = position;
        this.end = -1;
        this.position = position;
    }

    /** Returns the type of the token. */
    public String getType() {
        return type;
    }

    /** Returns the matched lexeme (exact string value). */
    public String getLexeme() {
        return lexeme;
    }

    /** Returns the start position of the token. */
    public int getStart() {
        return start;
    }

    /** Returns the end position (exclusive) of the token. */
    public int getEnd() {
        return end;
    }

    /** Returns the token’s position (alias of start). */
    public int getPosition() {
        return position;
    }

    /** Returns a readable representation for debugging. */
    @Override
    public String toString() {
        return String.format(
            "Token{type='%s', lexeme='%s', start=%d, end=%d, position=%d}",
            type, lexeme, start, end, position
        );
    }

    /** Checks equality based on type and lexeme. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Token)) return false;
        Token t = (Token) o;
        return (type == null ? t.type == null : type.equals(t.type)) &&
               (lexeme == null ? t.lexeme == null : lexeme.equals(t.lexeme));
    }

    /** Hash code based on type and lexeme. */
    @Override
    public int hashCode() {
        int r = type == null ? 0 : type.hashCode();
        r = 31 * r + (lexeme == null ? 0 : lexeme.hashCode());
        return r;
    }
}
