package com.compiler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import com.compiler.lexer.*;

/**
 * Unit tests for the Tokenizer class.
 */
public class TokenizerTest {

    /**
     * Creates a basic alphabet
     *
     * @return a {@link Set} of characters representing the alphabet.
     */
    private Set<Character> createBasicAlphabet() {
        Set<Character> alphabet = new HashSet<>();
        for (char c = 'a'; c <= 'z'; c++) alphabet.add(c);
        for (char c = 'A'; c <= 'Z'; c++) alphabet.add(c);
        for (char c = '0'; c <= '9'; c++) alphabet.add(c);
        alphabet.add('+');
        alphabet.add('-');
        alphabet.add('*');
        alphabet.add('/');
        alphabet.add('=');
        alphabet.add('(');
        alphabet.add(')');
        alphabet.add(';');
        alphabet.add(' ');
        alphabet.add('\t');
        alphabet.add('\n');
        return alphabet;
    }

    /**
     * Ensures that tokenizing an empty input string produces no tokens.
     */
    @Test
    public void testEmptyInput() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("A", "a");
        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize("");
        assertTrue(tokens.isEmpty());
    }

    /**
     * Verifies that encountering an unknown character in the input
     * results in a {@link RuntimeException}.
     */
    @Test
    public void testUnknownCharacterThrows() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("DIGIT", "1");
        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        Exception ex = assertThrows(RuntimeException.class, () -> tokenizer.tokenize("z"));
        assertTrue(ex.getMessage().contains("No valid token"));
    }

    /**
     * Tests recognition of multiple operator tokens in sequence.
     */
    @Test
    public void testOperatorTokens() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("PLUS_OP", "p");
        rules.put("MINUS_OP", "r");
        rules.put("MULT_OP", "m");
        rules.put("DIV_OP", "d");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize("prmd");
        assertEquals(4, tokens.size());
    }

    /**
     * Tests correct recognition of parentheses and semicolon tokens.
     */
    @Test
    public void testParenthesesAndSemicolon() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("LPAREN", "l");
        rules.put("RPAREN", "r");
        rules.put("SEMICOLON", ";");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize("lr;");
        assertEquals(3, tokens.size());
    }

    /**
     * Ensures that the tokenizer respects the longest-match preference rule.
     */
    @Test
    public void testLongestMatchPreference() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("DOUBLE_PLUS_OP", "pp");
        rules.put("PLUS_OP", "p");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize("ppp");
        assertEquals(2, tokens.size());
        assertEquals("DOUBLE_PLUS_OP", tokens.get(0).getType());
        assertEquals("PLUS_OP", tokens.get(1).getType());
    }

    /**
     * Verifies that the tokenizer can correctly process multiple lines
     * by handling newline characters.
     */
    @Test
    public void testMultipleLines() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("LETTER_A", "a");
        rules.put("NEWLINE", "\n");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize("a\na");
        assertEquals(3, tokens.size());
    }

    /**
     * Tests recognition of a more complex expression with variables,
     * assignment, digits, and operators.
     */
    @Test
    public void testComplexExpression() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("VAR_X", "x");
        rules.put("ASSIGN", "=");
        rules.put("DIGIT_1", "1");
        rules.put("DIGIT_2", "2");
        rules.put("PLUS_OP", "p");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize("x=1p2");
        assertEquals(5, tokens.size());
        assertEquals("VAR_X", tokens.get(0).getType());
        assertEquals("ASSIGN", tokens.get(1).getType());
        assertEquals("DIGIT_1", tokens.get(2).getType());
        assertEquals("PLUS_OP", tokens.get(3).getType());
        assertEquals("DIGIT_2", tokens.get(4).getType());
    }

    /**
     * Ensures that the tokenizer can handle a mix of tabs and spaces as tokens.
     */
    @Test
    public void testTabsAndSpacesMix() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("TAB", "\t");
        rules.put("SPACE", " ");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize(" \t ");
        assertEquals(3, tokens.size());
    }

    /**
     * Tests distinction between identifiers and keywords
     * by ensuring that keywords take precedence.
     */
    @Test
    public void testIdentifiersVsKeywords() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("IF", "if");
        rules.put("ID_X", "x");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        List<Token> tokens = tokenizer.tokenize("ifx");
        assertEquals(2, tokens.size());
        assertEquals("IF", tokens.get(0).getType());
        assertEquals("ID_X", tokens.get(1).getType());
    }

    /**
     * Stress test: verifies tokenizer performance and correctness
     * with very long input strings.
     */
    @Test
    public void testLongInputString() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("LETTER_A", "a");

        Tokenizer tokenizer = new TokenizerBuilder().buildTokenizer(rules, createBasicAlphabet());
        String input = "a".repeat(1000);
        List<Token> tokens = tokenizer.tokenize(input);
        assertEquals(1000, tokens.size());
    }
}
