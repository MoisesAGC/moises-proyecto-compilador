package com.compiler.lexer.regex;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 * <p>
 * Provides methods to preprocess regular expressions by inserting explicit
 * concatenation operators, and to convert infix regular expressions to postfix
 * notation for easier parsing and NFA construction.
 */
/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 */
public class ShuntingYard {

    /**
     * Default constructor for ShuntingYard.
     */
    public ShuntingYard() {
        // TODO: Implement constructor if needed
    }

    /**
     * Inserts the explicit concatenation operator ('·') into the regular
     * expression according to standard rules. This makes implicit
     * concatenations explicit, simplifying later parsing.
     *
     * @param regex Input regular expression (may have implicit concatenation).
     * @return Regular expression with explicit concatenation operators.
     */
    public static String insertConcatenationOperator(String regex) {
        /*
            Pseudocode:
            For each character in regex:
                - Append current character to output
                - If not at end of string:
                        - Check if current and next character form an implicit concatenation
                        - If so, append '·' to output
            Return output as string
         */
        if (regex.isEmpty()) return regex;
        
        StringBuilder output = new StringBuilder();
        
        for (int i = 0; i < regex.length(); i++) {
            char current = regex.charAt(i);
            output.append(current);
            
            if (i < regex.length() - 1) {
                char next = regex.charAt(i + 1);
                
                boolean shouldConcatenate = 
                    (isOperand(current) && isOperand(next)) ||
                    (isOperand(current) && next == '(') ||
                    (current == ')' && isOperand(next)) ||
                    (current == '*' && isOperand(next)) ||
                    (current == '*' && next == '(') ||
                    (current == '+' && isOperand(next)) ||
                    (current == '+' && next == '(') ||
                    (current == '?' && isOperand(next)) ||
                    (current == '?' && next == '(') ||
                    (current == ')' && next == '(');
                
                if (shouldConcatenate) {
                    output.append('·');
                }
            }
        }
        
        return output.toString();
    }

    /**
     * Determines if the given character is an operand (not an operator or
     * parenthesis).
     *
     * @param c Character to evaluate.
     * @return true if it is an operand, false otherwise.
     */
    private static boolean isOperand(char c) {
        /*
        Pseudocode:
        Return true if c is not one of: '|', '*', '?', '+', '(', ')', '·'
         */
        return !(c == '|' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')' || c == '·');
    }

    /**
     * Converts an infix regular expression to postfix notation using the
     * Shunting Yard algorithm. This is useful for constructing NFAs from
     * regular expressions.
     *
     * @param infixRegex Regular expression in infix notation.
     * @return Regular expression in postfix notation.
     */
    public static String toPostfix(String infixRegex) {
        /*
        Pseudocode:
        1. Define operator precedence map
        2. Preprocess regex to insert explicit concatenation operators
        3. For each character in regex:
            - If operand: append to output
            - If '(': push to stack
            - If ')': pop operators to output until '(' is found
            - If operator: pop operators with higher/equal precedence, then push current operator
        4. After loop, pop remaining operators to output
        5. Return output as string
         */
        // Definir precedencia
        Map<Character, Integer> precedence = new HashMap<>();
        precedence.put('|', 1);
        precedence.put('·', 2);
        precedence.put('*', 3);
        precedence.put('+', 3);
        precedence.put('?', 3);

        String regex = insertConcatenationOperator(infixRegex);
        StringBuilder output = new StringBuilder();
        Stack<Character> stack = new Stack<>();

        for (char c : regex.toCharArray()) {
            if (isOperand(c)) {
                output.append(c);
            } else if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    output.append(stack.pop());
                }
                stack.pop();
            } else { 
                while (!stack.isEmpty() && stack.peek() != '(' &&
                        precedence.get(stack.peek()) >= precedence.get(c)) {
                    output.append(stack.pop());
                }
                stack.push(c);
            }
        }

        while (!stack.isEmpty()) {
            output.append(stack.pop());
        }

        return output.toString();
    }
}
