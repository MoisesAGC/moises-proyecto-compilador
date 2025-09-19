package com.compiler.parser.syntax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;

/**
 * Calculates the FIRST and FOLLOW sets for a given grammar.
 * Main task of Practice 5.
 */
public class StaticAnalyzer {
    private final Grammar grammar;
    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;

    public StaticAnalyzer(Grammar grammar) {
        this.grammar = grammar;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    // Epsilon
    private static final String epsilonName = "ε";
    // End of input marker
    private static final Symbol endMarker = new Symbol("$", SymbolType.TERMINAL);

    /**
     * Checks whether a given symbol is epsilon (ε).
     * @param s The symbol to check
     * @return true if s represents ε, false otherwise
     */
    private boolean isEpsilon(Symbol s) {
        return epsilonName.equals(s.name);
    }

    /**
     * Calculates and returns the FIRST sets for all symbols.
     * @return A map from Symbol to its FIRST set.
     */
    public Map<Symbol, Set<Symbol>> getFirstSets() {        
        // 1: Initialization
        for (Symbol S : grammar.getNonTerminals()) {
            firstSets.put(S, new HashSet<>()); // empty set
        }
        for (Symbol S : grammar.getTerminals()) {
            Set<Symbol> set = new HashSet<>();
            set.add(S); // FIRST(S) = { S }
            firstSets.put(S, set);
        }

        boolean change;
        // 2: Iterative update until convergence
        do {
            change = false;

            for (Production P : grammar.getProductions()) {
                Symbol A = P.getLeft();
                Set<Symbol> firstA = firstSets.get(A);

                // Production A -> ε
                if (P.getRight().size() == 1 && isEpsilon(P.getRight().get(0))) {
                    Symbol epsilon = P.getRight().get(0);
                    if (firstA.add(epsilon)) {
                        change = true;
                    }
                    continue; // nothing else to do
                }

                boolean allDeriveEp = true;

                // For each Xi in the production A -> X1 X2 ... Xn
                for (Symbol Xi : P.getRight()) {
                    Set<Symbol> firstXi = firstSets.get(Xi);

                    if (firstXi != null) {
                        // a. Add FIRST(Xi) - {ε} to FIRST(A)
                        for (Symbol sym : firstXi) {
                            if (!isEpsilon(sym)) {
                                if (firstA.add(sym)) {
                                    change = true;
                                }
                            }
                        }
                    }

                    // b. If ε dont exist in FIRST(Xi), break
                    if (firstXi == null || firstXi.stream().noneMatch(this::isEpsilon)) {
                        allDeriveEp = false;
                        break;
                    }
                }

                // If ε is in FIRST(Xi) for all i, add ε to FIRST(A)
                if (allDeriveEp) {
                    Symbol epsilon = grammar.getTerminals().stream().filter(this::isEpsilon).findFirst().orElse(new Symbol(epsilonName, SymbolType.TERMINAL));
                    if (firstA.add(epsilon)) {
                        change = true;
                    }
                }
            }
        } while (change);

        //3. Return the map of FIRST sets for all symbols.
        return firstSets;
    }


    /**
     * Calculates and returns the FOLLOW sets for non-terminals.
     * @return A map from Symbol to its FOLLOW set.
     */
    public Map<Symbol, Set<Symbol>> getFollowSets() {
        
        Map<Symbol, Set<Symbol>> first = getFirstSets();

        // 1: Initialization FOLLOW(A) = {}
        for (Symbol A : grammar.getNonTerminals()) {
            followSets.put(A, new HashSet<>());
        }

        // 2: Add $ to FOLLOW(start symbol)
        followSets.get(grammar.getStartSymbol()).add(endMarker);

        boolean change;
        // Step 3: Iterative update until convergence
        do {
            change = false;

            for (Production P : grammar.getProductions()) {
                Symbol B = P.getLeft();
                Set<Symbol> followB = followSets.get(B);

                // For each Xi 
                for (int i = 0; i < P.getRight().size(); i++) {
                    Symbol Xi = P.getRight().get(i);

                    if (Xi.type != SymbolType.NON_TERMINAL) continue;

                    Set<Symbol> followXi = followSets.get(Xi);
                    boolean allDeriveEp = true;

                    // a. For each Xj after Xi
                    for (int j = i + 1; j < P.getRight().size(); j++) {
                        Symbol Xj = P.getRight().get(j);
                        Set<Symbol> firstXj = first.get(Xj);

                        if (firstXj != null) {
                            for (Symbol sym : firstXj) {
                                if (!isEpsilon(sym)) {
                                    if (followXi.add(sym)) {
                                        change = true;
                                    }
                                }
                            }
                        }

                        if (firstXj == null || firstXj.stream().noneMatch(this::isEpsilon)) {
                            allDeriveEp = false;
                            break;
                        }
                    }

                    //b. If ε is in FIRST(Xj) for all j > i, add FOLLOW(B) to FOLLOW(Xi)
                    if (allDeriveEp) {
                        if (followXi.addAll(followB)) {
                            change = true;
                        }
                    }
                }
            }
        } while (change);

        //Return the map of FOLLOW sets for all non-terminals.
        return followSets;
    }
}