package com.compiler.lexer.regex;

import java.util.Stack;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.Transition;
import com.compiler.lexer.nfa.State;

/**
 * RegexParser
 * -----------
 * This class provides functionality to convert infix regular expressions into nondeterministic finite automata (NFA)
 * using Thompson's construction algorithm. It supports standard regex operators: concatenation (·), union (|),
 * Kleene star (*), optional (?), and plus (+). The conversion process uses the Shunting Yard algorithm to transform
 * infix regex into postfix notation, then builds the corresponding NFA.
 *
 * Features:
 * - Parses infix regular expressions and converts them to NFA.
 * - Supports regex operators: concatenation, union, Kleene star, optional, plus.
 * - Implements Thompson's construction rules for NFA generation.
 *
 * Example usage:
 * <pre>
 *     RegexParser parser = new RegexParser();
 *     NFA nfa = parser.parse("a(b|c)*");
 * </pre>
 */
/**
 * Parses regular expressions and constructs NFAs using Thompson's construction.
 */
public class RegexParser {
    /**
     * Default constructor for RegexParser.
     */
        public RegexParser() {
            // TODO: Implement constructor if needed
        }

    /**
     * Converts an infix regular expression to an NFA.
     *
     * @param infixRegex The regular expression in infix notation.
     * @return The constructed NFA.
     */
    public NFA parse(String infixRegex) {
    // Pseudocode: Convert infix to postfix, then build NFA from postfix
        String postfix = ShuntingYard.toPostfix(infixRegex);
        return buildNfaFromPostfix(postfix);
    }

    /**
     * Builds an NFA from a postfix regular expression.
     *
     * @param postfixRegex The regular expression in postfix notation.
     * @return The constructed NFA.
     */
    private NFA buildNfaFromPostfix(String postfixRegex) {
    // Pseudocode: For each char in postfix, handle operators and operands using a stack
        Stack<NFA> stack = new Stack<>();

        for (char c : postfixRegex.toCharArray()) {
            if (isOperand(c)) {
                stack.push(createNfaForCharacter(c));
            } else {
                switch (c) {
                    case '·':
                        handleConcatenation(stack);
                        break;
                    case '|':
                        handleUnion(stack);
                        break;
                    case '*':
                        handleKleeneStar(stack);
                        break;
                    case '+':
                        handlePlus(stack);
                        break;
                    case '?':
                        handleOptional(stack);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown operator: " + c);
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Invalid regex");
        }
        return stack.pop();
    }

    /**
     * Handles the '?' operator (zero or one occurrence).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or one occurrence.
     * @param stack The NFA stack.
     */
    private void handleOptional(Stack<NFA> stack) {
    // Pseudocode: Pop NFA, create new start/end, add epsilon transitions for zero/one occurrence
        NFA nfa = stack.pop();

        State start = new State();
        State end = new State();
        end.isFinal = true;

        start.transitions.add(new Transition(null, nfa.startState));
        start.transitions.add(new Transition(null, end));

        nfa.endState.isFinal = false;
        nfa.endState.transitions.add(new Transition(null, end));

        stack.push(new NFA(start, end));
    }

    /**
     * Handles the '+' operator (one or more occurrences).
     * Pops an NFA from the stack and creates a new NFA that accepts one or more occurrences.
     * @param stack The NFA stack.
     */
    private void handlePlus(Stack<NFA> stack) {
    // Pseudocode: Pop NFA, create new start/end, add transitions for one or more occurrence
        NFA nfa = stack.pop();

        State start = new State();
        State end = new State();
        end.isFinal = true;

        start.transitions.add(new Transition(null, nfa.startState));

        nfa.endState.isFinal = false;
        nfa.endState.transitions.add(new Transition(null, nfa.startState));
        nfa.endState.transitions.add(new Transition(null, end));

        stack.push(new NFA(start, end));
    }
    
    /**
     * Creates an NFA for a single character.
     * @param c The character to create an NFA for.
     * @return The constructed NFA.
     */
    private NFA createNfaForCharacter(char c) {
    // Pseudocode: Create start/end state, add transition for character
        State start = new State();
        State end = new State();
        end.isFinal = true;

        start.transitions.add(new Transition(c, end));

        return new NFA(start, end);
    }

    /**
     * Handles the concatenation operator (·).
     * Pops two NFAs from the stack and connects them in sequence.
     * @param stack The NFA stack.
     */
    private void handleConcatenation(Stack<NFA> stack) {
    // Pseudocode: Pop two NFAs, connect end of first to start of second
        NFA nfa2 = stack.pop();
        NFA nfa1 = stack.pop();

        nfa1.endState.isFinal = false;
        nfa1.endState.transitions.add(new Transition(null, nfa2.startState));

        stack.push(new NFA(nfa1.startState, nfa2.endState));
    }

    /**
     * Handles the union operator (|).
     * Pops two NFAs from the stack and creates a new NFA that accepts either.
     * @param stack The NFA stack.
     */
    private void handleUnion(Stack<NFA> stack) {
    // Pseudocode: Pop two NFAs, create new start/end, add epsilon transitions for union
        NFA nfa2 = stack.pop();
        NFA nfa1 = stack.pop();

        State start = new State();
        State end = new State();
        end.isFinal = true;

        start.transitions.add(new Transition(null, nfa1.startState));
        start.transitions.add(new Transition(null, nfa2.startState));

        nfa1.endState.isFinal = false;
        nfa2.endState.isFinal = false;

        nfa1.endState.transitions.add(new Transition(null, end));
        nfa2.endState.transitions.add(new Transition(null, end));

        stack.push(new NFA(start, end));
    }

    /**
     * Handles the Kleene star operator (*).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or more repetitions.
     * @param stack The NFA stack.
     */
    private void handleKleeneStar(Stack<NFA> stack) {
    // Pseudocode: Pop NFA, create new start/end, add transitions for zero or more repetitions
        NFA nfa = stack.pop();

        State start = new State();
        State end = new State();
        end.isFinal = true;

        start.transitions.add(new Transition(null, nfa.startState));
        start.transitions.add(new Transition(null, end));

        nfa.endState.isFinal = false;
        nfa.endState.transitions.add(new Transition(null, nfa.startState));
        nfa.endState.transitions.add(new Transition(null, end));

        stack.push(new NFA(start, end));
    }

    /**
     * Checks if a character is an operand (not an operator).
     * @param c The character to check.
     * @return True if the character is an operand, false if it is an operator.
     */
    private boolean isOperand(char c) {
    // Pseudocode: Return true if c is not an operator
        return !(c == '|' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')' || c == '·');
    }
}