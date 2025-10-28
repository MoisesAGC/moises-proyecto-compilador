package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;

/**
 * Builds the canonical collection of LR(1) items (the DFA automaton).
 * Items contain a lookahead symbol.
 */
public class LR1Automaton {
    private final Grammar grammar;
    private final List<Set<LR1Item>> states = new ArrayList<>();
    private final Map<Integer, Map<Symbol, Integer>> transitions = new HashMap<>();
    private String augmentedLeftName = null;

    public LR1Automaton(Grammar grammar) {
        this.grammar = Objects.requireNonNull(grammar);
    }


    private final Symbol dollar = new Symbol("$", SymbolType.TERMINAL);
    private final Symbol epsilon = new Symbol("ε", SymbolType.TERMINAL);

    public List<Set<LR1Item>> getStates() { return states; }
    public Map<Integer, Map<Symbol, Integer>> getTransitions() { return transitions; }

    /**
     * CLOSURE for LR(1): standard algorithm using FIRST sets to compute lookaheads for new items.
     */
    private Set<LR1Item> closure(Set<LR1Item> items) {

        // 1. Initialize a new set `closure` with the given `items`.
        Set<LR1Item> closure = new HashSet<>(items);
        // 2. Create a worklist (like a Queue or List) and add all items from `items` to it.
        Queue<LR1Item> worklist = new LinkedList<>(items);
        
        // 3. Pre-calculate the FIRST sets for all symbols in the grammar.
        Map<Symbol, Set<Symbol>> firstSets = grammar.getFirstSets();

        // 4. While the worklist is not empty:
        while (!worklist.isEmpty()) {
            // 4a. Dequeue an item `[A -> α • B β, a]`.
            LR1Item currentItem = worklist.poll();
            Symbol symbolAfterDot = currentItem.getSymbolAfterDot();

            if (symbolAfterDot != null && symbolAfterDot.type == SymbolType.NON_TERMINAL) {
                Symbol B = symbolAfterDot;
                
                List<Symbol> beta = currentItem.production.right.subList(
                    currentItem.dotPosition + 1, 
                    currentItem.production.right.size()
                );
                
                List<Symbol> betaA = new ArrayList<>(beta);
                betaA.add(currentItem.lookahead);

                Set<Symbol> newLookaheads = computeFirstOfSequence(betaA, firstSets, epsilon);
                
                // 4bi.For each production of `B` (e.g., `B -> γ`):
                for (Production productionB : grammar.getProductionsByLHS(B)) {
                    
                    // 4bii.For each terminal `b` in FIRST(βa):
                    for (Symbol b : newLookaheads) {
                        if (b.equals(epsilon)) continue; 
                        
                        // Create a new item `[B -> • γ, b]`.
                        LR1Item newItem = new LR1Item(productionB, 0, b);
                        
                        // If this new item is not already in the `closure` set:
                        if (!closure.contains(newItem)) {
                            // Add it to `closure`.
                            closure.add(newItem);
                            worklist.add(newItem);
                        }
                    }
                }
            }
        }
        // 5.Return the `closure` set.
        return closure;
    }

    /**
     * Compute FIRST of a sequence of symbols.
     */
    private Set<Symbol> computeFirstOfSequence(List<Symbol> seq, Map<Symbol, Set<Symbol>> firstSets, Symbol epsilon) {
        // 1. Initialize an empty result set.
        Set<Symbol> result = new HashSet<>();
        
        // 2. If the sequence is empty, add epsilon to the result and return. (Handled by checking if result remains empty)
        if (seq.isEmpty()) {
            return result; 
        }
        
        // 3. Iterate through the symbols `X` in the sequence:
        for (Symbol X : seq) {
            // 3a. Get `FIRST(X)` from the pre-calculated `firstSets`.
            Set<Symbol> firstX = firstSets.get(X);
            
            if (firstX == null) {
                 // Fallback for terminal not in firstSets 
                 if (X.type == SymbolType.TERMINAL && !X.equals(epsilon)) {
                    result.add(X);
                    break;
                 }
                 continue; 
            }
            
            // 3b. Add all symbols from `FIRST(X)` to the result, except for epsilon.
            for (Symbol symbol : firstX) {
                if (!symbol.equals(epsilon)) {
                    result.add(symbol);
                }
            }
            
            // 3c. If `FIRST(X)` does not contain epsilon, stop and break the loop.
            if (!firstX.contains(epsilon)) {
                break;
            }
            
            // 3d. If it does contain epsilon and this is the last symbol in the sequence, add epsilon to the result set.
            // Note: For LR(1) closure (FIRST(beta a)), 'a' is a terminal (lookahead) and never contains epsilon, 
            // so epsilon is never added to FIRST(beta a).
        }

        // 4. Return the result set.
        return result;
    }

    /**
     * GOTO for LR(1): moves dot over symbol and takes closure.
     */
    private Set<LR1Item> goTo(Set<LR1Item> state, Symbol symbol) {

        // 1. Initialize an empty set `movedItems`.
        Set<LR1Item> movedItems = new HashSet<>();
        
        // 2. For each item `[A -> α • X β, a]` in the input `state`:
        for (LR1Item item : state) {
            Symbol symbolAfterDot = item.getSymbolAfterDot();

            // If `X` is equal to the input `symbol`:
            if (symbolAfterDot != null && symbolAfterDot.equals(symbol)) {
                // Add the new item `[A -> α X • β, a]` to `movedItems`.
                LR1Item newItem = new LR1Item(item.production, item.dotPosition + 1, item.lookahead);
                movedItems.add(newItem);
            }
        }
        
        // 3.Return the `closure` of `movedItems`.
        return closure(movedItems);
    }

    /**
     * Build the LR(1) canonical collection: states and transitions.
     */
    public void build() {
        // 1. Clear any existing states and transitions.
        states.clear();
        transitions.clear();

        // 2. Create the augmented grammar: Add a new start symbol S' and production S' -> S.
        Symbol originalStartSymbol = grammar.getStartSymbol();
        augmentedLeftName = originalStartSymbol.name + "'"; 
        Symbol augmentedStartSymbol = new Symbol(augmentedLeftName, SymbolType.NON_TERMINAL);

        Production augmentedProduction = new Production(augmentedStartSymbol, List.of(originalStartSymbol));

        // 3. Create the initial item: `[S' -> • S, $]`.
        LR1Item initialItem = new LR1Item(augmentedProduction, 0, dollar);
        Set<LR1Item> initialItemSet = new HashSet<>();
        initialItemSet.add(initialItem);
        
        // 4. The first state, `I0`, is the `closure` of this initial item set. Add `I0` to the list of states.
        Set<LR1Item> I0 = closure(initialItemSet);
        states.add(I0);
        
        // 5. Create a worklist (queue) and add `I0` to it.
        Queue<Set<LR1Item>> worklist = new LinkedList<>();
        worklist.add(I0);

        // Create a map to track state indices.
        Map<Set<LR1Item>, Integer> stateToIndex = new HashMap<>();
        stateToIndex.put(I0, 0);

        // Get all grammar symbols (Terminals + Non-terminals) for transitions.
        Set<Symbol> allSymbols = new HashSet<>();
        allSymbols.addAll(grammar.getTerminals());
        allSymbols.addAll(grammar.getNonTerminals());
        allSymbols.remove(dollar);

        int current_I_index = -1;
        // 6. While the worklist is not empty:
        while (!worklist.isEmpty()) {
            // 6a. Dequeue a state `I`.
            Set<LR1Item> I = worklist.poll();
            current_I_index++; 

            transitions.put(current_I_index, new HashMap<>());
            
            for (Symbol X : allSymbols) {
                // 6b. For each grammar symbol `X`: i. Calculate `J = goTo(I, X)`.
                Set<LR1Item> J = goTo(I, X);

                if (!J.isEmpty()) {
                    int J_index;
                    
                    // 6b. ii. If `J` is not empty and not already in the list of states:
                    if (stateToIndex.containsKey(J)) {
                        J_index = stateToIndex.get(J);
                    } else {
                        // Add `J` to the list of states.
                        J_index = states.size();
                        states.add(J);
                        // Enqueue `J` to the worklist.
                        worklist.add(J);
                        stateToIndex.put(J, J_index);
                    }
                    
                    // 6b. iii. Create a transition from the index of state `I` to the index of state `J` on symbol `X`.
                    transitions.get(current_I_index).put(X, J_index);
                }
            }
        }
    }

    public String getAugmentedLeftName() { return augmentedLeftName; }
}
