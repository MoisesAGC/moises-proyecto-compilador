package com.compiler.parser.lr;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import com.compiler.lexer.Token;
import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;
import com.compiler.parser.lr.LALR1Table.Action;
import com.compiler.parser.lr.LALR1Table.Action.Type;

/**
 * Implements the LALR(1) parsing engine.
 * Uses a stack and the LALR(1) table to process a sequence of tokens.
 * Complementary task for Practice 9.
 */
public class LALR1Parser {
   private final LALR1Table table;
   private final Grammar grammar;
      
    // Cache map for terminal name (String) to its corresponding Symbol object.
    private final Map<String, Symbol> terminalSymbolCache;
    
    // End-of-input symbol.
    private final Symbol dollar = new Symbol("$", SymbolType.TERMINAL);

   
    public LALR1Parser(LALR1Table table) {
        this.table = Objects.requireNonNull(table);
        this.grammar = null; // Indicates that the Grammar object was not passed.
        this.terminalSymbolCache = new HashMap<>(); 
        // The '$' symbol is handled "on the fly" in getTokenSymbol if the full constructor wasn't used.
    }
   

    public LALR1Parser(LALR1Table table, Grammar grammar) {
        this.table = Objects.requireNonNull(table);
        this.grammar = Objects.requireNonNull(grammar);
        this.terminalSymbolCache = buildTerminalCache(grammar.getTerminals());
        
        // Add the '$' symbol to the cache, as it is fundamental.
        terminalSymbolCache.put(dollar.name, dollar);
    }

   

    // package-private accessor for tests
    LALR1Table getTable() {
        return table;
    }

    /**
    * Builds a quick lookup map from terminal name (String) to Symbol object.
    */
    private Map<String, Symbol> buildTerminalCache(Set<Symbol> terminals) {
        Map<String, Symbol> cache = new HashMap<>();
        for (Symbol s : terminals) {
            if (s.type == SymbolType.TERMINAL) {
               cache.put(s.name, s);
            }
        }
        return cache;
    }

    /**
    * Maps a token type string to its corresponding Symbol object.
    */
    private Symbol getTokenSymbol(Token token) {
      // Case 1: The full constructor was used. We use the pre-built cache.
      if (grammar != null) {
         Symbol symbol = terminalSymbolCache.get(token.getType());
         return symbol;
      }
      
      // Case 2: The simple constructor was used (grammar == null).
      // We must create the Symbol "on the fly" to match the table's Symbol objects.
      
      // Step 2a: Check the cache if we already created it (to maintain Symbol object consistency).
      if (terminalSymbolCache.containsKey(token.getType())) {
         return terminalSymbolCache.get(token.getType());
      }
      
      // Step 2b: If not in the cache, create it, cache it, and return it.
      // This ensures that all tokens of the same type use the SAME Symbol object, 
      // which is CRITICAL for ACTION table lookups.
      Symbol newSymbol = new Symbol(token.getType(), SymbolType.TERMINAL);
      terminalSymbolCache.put(newSymbol.name, newSymbol);
      
      return newSymbol;
    }

    /**
    * Parses a sequence of tokens using the LALR(1) parsing algorithm.
    * @param tokens The list of tokens from the lexer.
    * @return true if the sequence is accepted, false if a syntax error is found.
    */
    public boolean parse(List<Token> tokens) {
        // 1. Initialize a stack for states and push the initial state (from table.getInitialState()).
        Stack<Integer> stateStack = new Stack<>();
        stateStack.push(table.getInitialState());

        // 2. Create a mutable list of input tokens from the parameter and add the end-of-input token ("$").
        List<Symbol> inputSymbols = new LinkedList<>();
        for (Token t : tokens) {
            Symbol tokenSymbol = getTokenSymbol(t);
            if (tokenSymbol == null) {
                System.err.println("Lexical Error/Undefined Symbol: Token type '" + t.getType() + "' not found in grammar terminals.");
                return false;
            }
            inputSymbols.add(tokenSymbol);
        }
        inputSymbols.add(dollar);

        // 3. Initialize an instruction pointer `ip` to 0, pointing to the first token.
        int ip = 0;

        // 4. Start a loop that runs until an ACCEPT or ERROR condition is met.
        while (true) {
            // 4a. Get the current state from the top of the stack.
            int currentState = stateStack.peek();
            
            if (ip >= inputSymbols.size()) {
                 return false; 
            }
            // 4b. Get the current token `a` from the input list at index `ip`.
            Symbol currentSymbol = inputSymbols.get(ip);
            
            // 4c. Look up the action in the ACTION table: action = table.getActionTable()[state][a.type].
            Action action = table.getActionTable()
                                 .getOrDefault(currentState, new HashMap<>())
                                 .get(currentSymbol);

            // 4d. If no action is found (it's null), it's a syntax error. Return false.
            if (action == null) {
                System.err.println("Syntax Error: No action found in state " + currentState + " for symbol " + currentSymbol.name);
                return false;
            }

            // 4e. If the action is SHIFT(s'):
            if (action.type == Type.SHIFT) {
                // i. Push the new state s' onto the stack.
                stateStack.push(action.state);
                // ii. Advance the input pointer: ip++.
                ip++;
            } 
            // 4f. If the action is REDUCE(A -> β):
            else if (action.type == Type.REDUCE) {
                Symbol A = action.reduceProd.left;
                int betaLength = action.reduceProd.right.size();
                
                // Handle epsilon productions (where |β|=0).
                if (betaLength == 1 && action.reduceProd.right.get(0).name.equals("ε")) {
                    betaLength = 0;
                }
                
                // i. Pop |β| symbols (and states) from the stack.
                for (int i = 0; i < betaLength; i++) {
                    stateStack.pop();
                }

                // ii. Get the new state `s` from the top of the stack.
                int newStateAfterPop = stateStack.peek();

                // iii. Look up the GOTO state: goto_state = table.getGotoTable()[s][A].
                Integer gotoState = table.getGotoTable()
                                         .getOrDefault(newStateAfterPop, new HashMap<>())
                                         .get(A);

                // iv. If no GOTO state is found, it's an error. Return false.
                if (gotoState == null) {
                    System.err.println("Syntax Error: No GOTO found from state " + newStateAfterPop + " on non-terminal " + A.name + " after reduction.");
                    return false;
                }
                
                // v. Push the goto_state onto the stack.
                stateStack.push(gotoState);

            } 
            // 4g. If the action is ACCEPT:
            else if (action.type == Type.ACCEPT) {
                // i. The input has been parsed successfully. Return true.
                return true;
            } 
            // 4h. If the action is none of the above, it's an unhandled case or error. Return false.
            else {
                System.err.println("Internal Parser Error: Unhandled action type.");
                return false;
            }
        }
    }
}