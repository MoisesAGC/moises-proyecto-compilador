
/**
 * DfaMinimizer
 * -------------
 * This class provides an implementation of DFA minimization using the table-filling algorithm.
 * It identifies and merges equivalent states in a deterministic finite automaton (DFA),
 * resulting in a minimized DFA with the smallest number of states that recognizes the same language.
 *
 * Main steps:
 *   1. Initialization: Mark pairs of states as distinguishable if one is final and the other is not.
 *   2. Iterative marking: Mark pairs as distinguishable if their transitions lead to distinguishable states,
 *      or if only one state has a transition for a given symbol.
 *   3. Partitioning: Group equivalent states and build the minimized DFA.
 *
 * Helper methods are provided for partitioning, union-find operations, and pair representation.
 */
package com.compiler.lexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;


/**
 * Implements DFA minimization using the table-filling algorithm.
 */
/**
 * Utility class for minimizing DFAs using the table-filling algorithm.
 */
public class DfaMinimizer {
    /**
     * Default constructor for DfaMinimizer.
     */
        public DfaMinimizer() {
        }

    /**
     * Minimizes a given DFA using the table-filling algorithm.
     *
     * @param originalDfa The original DFA to be minimized.
     * @param alphabet The set of input symbols.
     * @return A minimized DFA equivalent to the original.
     */
    public static DFA minimizeDfa(DFA originalDfa, Set<Character> alphabet) {
    /*
     Pseudocode:
     1. Collect and sort all DFA states
     2. Initialize table of state pairs; mark pairs as distinguishable if one is final and the other is not
     3. Iteratively mark pairs as distinguishable if their transitions lead to distinguishable states or only one has a transition
     4. Partition states into equivalence classes (using union-find)
     5. Create new minimized states for each partition
     6. Reconstruct transitions for minimized states
     7. Set start state and return minimized DFA
    */
        // 1. Get and sort all states by id to ensure consistency
        List<DfaState> allStates = new ArrayList<>(originalDfa.allStates);
        allStates.sort(Comparator.comparingInt(s -> s.id));

        // 2. Initialize the pair table using auxiliar function
        Map<Pair, Boolean> table = initializeTable(allStates);

        // 3. Iteratively mark pairs as distinguishable until no changes occur
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < allStates.size(); i++) {
                for (int j = i + 1; j < allStates.size(); j++) {
                    Pair p = new Pair(allStates.get(i), allStates.get(j));

                    // Only check unmarked pairs 
                    if (!table.get(p)) {
                        for (char symbol : alphabet) {
                            DfaState t1 = allStates.get(i).getTransition(symbol);
                            DfaState t2 = allStates.get(j).getTransition(symbol);

                            if (t1 != null && t2 != null) {
                                // If their transitions lead to distinguishable states, mark as distinguishable
                                Pair tp = new Pair(t1, t2);
                                if (table.getOrDefault(tp, false)) {
                                    table.put(p, true);
                                    changed = true;
                                    break; 
                                }
                            } else if (t1 != null || t2 != null) {
                                // One state has a transition and the other does not -> distinguishable
                                table.put(p, true);
                                changed = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 4. Create partitions of equivalent states (using Union-Find)
        List<Set<DfaState>> partitions = createPartitions(allStates, table);

        // 5. Create new states for each partition
        Map<DfaState, DfaState> stateMap = new HashMap<>();
        List<DfaState> newStates = new ArrayList<>();
        for (Set<DfaState> part : partitions) {
            // The first state in the partition is chosen as representative
            DfaState rep = part.iterator().next();
            DfaState newState = new DfaState(rep.nfaStates);
            newState.setFinal(rep.isFinal());
            newStates.add(newState);

            // Map every old state in the partition to the new representative state
            for (DfaState old : part) {
                stateMap.put(old, newState);
            }
        }

        // 6. Reconstruct transitions for the minimized states
        for (DfaState old : allStates) {
            DfaState mapped = stateMap.get(old);
            for (Map.Entry<Character, DfaState> entry : old.getTransitions().entrySet()) {
                Character symbol = entry.getKey();
                DfaState target = entry.getValue();
                mapped.addTransition(symbol, stateMap.get(target));
            }
        }

        // 7. The new start state is the representative of the original start state
        DfaState newStart = stateMap.get(originalDfa.startState);
        return new DFA(newStart, newStates);
    }

    /**
     * Auxiliar function: initializes the pair table by marking as distinguishable
     * those pairs where one state is final and the other is not.
     */
    private static Map<Pair, Boolean> initializeTable(List<DfaState> allStates) {
        Map<Pair, Boolean> table = new HashMap<>();
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair p = new Pair(s1, s2);
                // Mark distinguishable if one is final and the other is not
                table.put(p, s1.isFinal() != s2.isFinal());
            }
        }
        return table;
    }

    /**
     * Groups equivalent states into partitions using union-find.
     *
     * @param allStates List of all DFA states.
     * @param table Table indicating which pairs are distinguishable.
     * @return List of partitions, each containing equivalent states.
     */
    private static List<Set<DfaState>> createPartitions(List<DfaState> allStates, Map<Pair, Boolean> table) {
    /*
     Pseudocode:
     1. Initialize each state as its own parent
     2. For each pair not marked as distinguishable, union the states
     3. Group states by their root parent
     4. Return list of partitions
    */
        Map<DfaState, DfaState> parent = new HashMap<>();
        for (DfaState s : allStates) {
            parent.put(s, s);
        }

        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                Pair p = new Pair(allStates.get(i), allStates.get(j));
                if (!table.get(p)) {
                    union(parent, allStates.get(i), allStates.get(j));
                }
            }
        }

        Map<DfaState, Set<DfaState>> groups = new HashMap<>();
        for (DfaState s : allStates) {
            DfaState root = find(parent, s);
            groups.computeIfAbsent(root, k -> new HashSet<>()).add(s);
        }

        return new ArrayList<>(groups.values());
    }

    /**
     * Finds the root parent of a state in the union-find structure.
     * Implements path compression for efficiency.
     *
     * @param parent Parent map.
     * @param state State to find.
     * @return Root parent of the state.
     */
    private static DfaState find(Map<DfaState, DfaState> parent, DfaState state) {
    /*
     Pseudocode:
     If parent[state] == state, return state
     Else, recursively find parent and apply path compression
     Return parent[state]
    */
        if (parent.get(state) == state) {
            return state;
        }
        DfaState root = find(parent, parent.get(state));
        parent.put(state, root); // path compression
        return root;
    }

    /**
     * Unites two states in the union-find structure.
     *
     * @param parent Parent map.
     * @param s1 First state.
     * @param s2 Second state.
     */
    private static void union(Map<DfaState, DfaState> parent, DfaState s1, DfaState s2) {
    /*
     Pseudocode:
     Find roots of s1 and s2
     If roots are different, set parent of one to the other
    */
        DfaState root1 = find(parent, s1);
        DfaState root2 = find(parent, s2);
        if (root1 != root2) {
            parent.put(root2, root1);
        }
    }

    /**
     * Helper class to represent a pair of DFA states in canonical order.
     * Used for table indexing and comparison.
     */
    private static class Pair {
        final DfaState s1;
        final DfaState s2;

        /**
         * Constructs a pair in canonical order (lowest id first).
         * @param s1 First state.
         * @param s2 Second state.
         */
        public Pair(DfaState s1, DfaState s2) {
            /*
             Pseudocode:
             Assign s1 and s2 so that s1.id <= s2.id
            */
            if (s1.id <= s2.id) {
                this.s1 = s1;
                this.s2 = s2;
            } else {
                this.s1 = s2;
                this.s2 = s1;
            }
        }

        @Override
        public boolean equals(Object o) {
            /*
             Pseudocode:
             Return true if both s1 and s2 ids match
            */
            if (this == o) return true;          
            if (o == null || getClass() != o.getClass()) return false; 
            Pair other = (Pair) o;
            return s1.id == other.s1.id && s2.id == other.s2.id;
        }

        @Override
        public int hashCode() {
            /*
             Pseudocode:
             Return hash of s1.id and s2.id
            */
            return Objects.hash(s1.id, s2.id);
        }
    }
}
