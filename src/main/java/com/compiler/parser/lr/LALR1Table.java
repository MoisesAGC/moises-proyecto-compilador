package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;

/**
 * Builds the LALR(1) parsing table (ACTION/GOTO).
 * Main task for Practice 9.
 */
public class LALR1Table {
    private final LR1Automaton automaton;

    // merged LALR states and transitions
    private java.util.List<java.util.Set<LR1Item>> lalrStates = new java.util.ArrayList<>();
    private java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> lalrTransitions = new java.util.HashMap<>();
    
    // ACTION table: state -> terminal -> Action
    public static class Action {
        public enum Type { SHIFT, REDUCE, ACCEPT }
        public final Type type;
        public final Integer state; // for SHIFT
        public final com.compiler.parser.grammar.Production reduceProd; // for REDUCE

        private Action(Type type, Integer state, com.compiler.parser.grammar.Production prod) {
            this.type = type; this.state = state; this.reduceProd = prod;
        }

        public static Action shift(int s) { return new Action(Type.SHIFT, s, null); }
        public static Action reduce(com.compiler.parser.grammar.Production p) { return new Action(Type.REDUCE, null, p); }
        public static Action accept() { return new Action(Type.ACCEPT, null, null); }
    }

    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> action = new java.util.HashMap<>();
    private final java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> gotoTable = new java.util.HashMap<>();
    private final java.util.List<String> conflicts = new java.util.ArrayList<>();
    private int initialState = 0;

    private final Symbol dollar = new Symbol("$", SymbolType.TERMINAL);

    public LALR1Table(LR1Automaton automaton) {
        this.automaton = automaton;
    }

    /**
     * Builds the LALR(1) parsing table.
     */
    public void build() {
        // 1: Ensure the underlying LR(1) automaton is built.
        automaton.build(); 

        // 2: Merge LR(1) states to create LALR(1) states.
        
        // 2a. Group LR(1) states that have the same "kernel" (the set of LR(0) items).
        // Create a map from a kernel (Set<KernelEntry>) to a list of state IDs that share that kernel.
        Map<Set<KernelEntry>, List<Integer>> kernelToStates = new HashMap<>();
        
        List<Set<LR1Item>> lr1States = automaton.getStates();
        
        // Create a mapping from old LR(1) state IDs to new LALR(1) state IDs.
        Map<Integer, Integer> lr1ToLalr = new HashMap<>();

        for (int i = 0; i < lr1States.size(); i++) {
            Set<LR1Item> lr1State = lr1States.get(i);
            Set<KernelEntry> kernel = new HashSet<>();
            
            // Extract the kernel (LR(0) items) from the LR(1) state
            for (LR1Item item : lr1State) {
                // A kernel item is an LR(1) item without its lookahead.
                KernelEntry kernelItem = new KernelEntry(item.production, item.dotPosition);
                kernel.add(kernelItem);
            }
            
            // Group states by their kernel
            kernelToStates.computeIfAbsent(kernel, k -> new ArrayList<>()).add(i);
        }
        
        // 2b. For each group of states with the same kernel:
        int lalrStateId = 0;
        for (Set<KernelEntry> kernel : kernelToStates.keySet()) {
            List<Integer> lr1Ids = kernelToStates.get(kernel);
            
            // Create a single new LALR(1) state.
            Set<LR1Item> mergedState = new HashSet<>();
            
            // Create a temporary map to build the merged item set
            Map<KernelEntry, Set<Symbol>> lookaheadUnion = new HashMap<>();
            
            for (int lr1Id : lr1Ids) {
                // Create a mapping from old LR(1) state IDs to new LALR(1) state IDs.
                lr1ToLalr.put(lr1Id, lalrStateId);
                
                Set<LR1Item> currentLr1State = lr1States.get(lr1Id);
                
                // Merging means for each kernel item, the new lookahead set is the union of all lookaheads 
                // for that item across the group.
                for (LR1Item item : currentLr1State) {
                    KernelEntry k = new KernelEntry(item.production, item.dotPosition);
                    lookaheadUnion.computeIfAbsent(k, key -> new HashSet<>()).add(item.lookahead);
                }
            }
            
            // Rebuild the merged LR1Item set using the union of lookaheads
            // This new state is formed by merging the LR(1) items from all states in the group.
            for (KernelEntry k : lookaheadUnion.keySet()) {
                for (Symbol lookahead : lookaheadUnion.get(k)) {
                    mergedState.add(new LR1Item(k.production, k.dotPosition, lookahead));
                }
            }
            
            // Store these new LALR states in `lalrStates`.
            lalrStates.add(mergedState);
            
            // Find the initial state ID
            if (lr1Ids.contains(0)) {
                this.initialState = lalrStateId;
            }
            
            lalrStateId++;
        }


        // 3: Build the transitions for the new LALR(1) automaton.
        
        Map<Integer, Map<Symbol, Integer>> lr1Transitions = automaton.getTransitions();
        for (int s : lr1Transitions.keySet()) {
            // Use the mapping from step 2c to find the merged state IDs.
            int mergedS = lr1ToLalr.get(s);
            
            // Initialize transition map for the merged state 'mergedS'
            lalrTransitions.computeIfAbsent(mergedS, k -> new HashMap<>());
            
            for (Symbol X : lr1Transitions.get(s).keySet()) {
                int t = lr1Transitions.get(s).get(X);
                int mergedT = lr1ToLalr.get(t);
                
                // Add a new transition for the LALR automaton: `merged(s) -X-> merged(t)`.
                lalrTransitions.get(mergedS).put(X, mergedT);
            }
        }

        // 4: Fill the ACTION and GOTO tables based on the LALR automaton.
        fillActionGoto();
    }

    private void fillActionGoto() {
        // 1. Clear the action, gotoTable, and conflicts lists.
        action.clear();
        gotoTable.clear();
        conflicts.clear();

        // 2. Iterate through each LALR state `s` from 0 to lalrStates.size() - 1.
        for (int s = 0; s < lalrStates.size(); s++) {
            Set<LR1Item> lalrState = lalrStates.get(s);
            
            // Initialize action/goto maps for state s
            action.computeIfAbsent(s, k -> new HashMap<>());
            gotoTable.computeIfAbsent(s, k -> new HashMap<>());

            // 3. For each state `s`, iterate through its LR1Item `it`.
            for (LR1Item it : lalrState) {
                // 3a. Get the symbol after the dot, `X = it.getSymbolAfterDot()`.
                Symbol X = it.getSymbolAfterDot();

                if (X != null) {
                    // Item is [A -> alpha • X beta, a]
                    // 3b. If `X` is a terminal (SHIFT action):
                    if (X.type == SymbolType.TERMINAL) {
                        // Find the destination state `t` from `lalrTransitions.get(s).get(X)`.
                        Integer t = lalrTransitions.get(s).get(X);
                        if (t != null) {
                            // Otherwise, set `action[s][X] = SHIFT(t)`.
                            Action newAction = Action.shift(t);
                            // Check for conflicts: if action table already has an entry for `[s, X]`, it's a conflict.
                            checkAndSetAction(s, X, newAction, "Shift/Reduce");
                        }
                    }
                    // For non-terminals, handle GOTO later
                } else {
                    // Item is [A -> alpha •, a] (Dot is at the end)
                    
                    // 3c. If the dot is at the end of the production (`X` is null) (REDUCE or ACCEPT action):
                    Production p = it.production;
                    Symbol lookahead = it.lookahead;
                    
                    // If it's the augmented start production (`S' -> S •`) and lookahead is `$`, this is an ACCEPT action.
                    if (p.left.name.equals(automaton.getAugmentedLeftName()) && lookahead.equals(dollar)) {
                        // Set `action[s][$] = ACCEPT`.
                        Action newAction = Action.accept();
                        checkAndSetAction(s, dollar, newAction, "Shift/Accept or Reduce/Accept");
                    } else {
                        // Otherwise, it's a REDUCE action.
                        // Otherwise, set `action[s][a] = REDUCE(A -> α)`.
                        Action newAction = Action.reduce(p);
                        // For the lookahead symbol `a` in the item:
                        // Check for conflicts: if `action[s][a]` is already filled, report a Shift/Reduce or Reduce/Reduce conflict.
                        checkAndSetAction(s, lookahead, newAction, "Shift/Reduce or Reduce/Reduce");
                    }
                }
            }
            
            // 4. Populate the GOTO table.
            Map<Symbol, Integer> sTransitions = lalrTransitions.get(s);
            if (sTransitions != null) {
                 // For each state `s`, look at its transitions in `lalrTransitions`.
                 for (Symbol B : sTransitions.keySet()) {
                    // For each transition on a NON-TERMINAL symbol `B` to state `t`:
                    if (B.type == SymbolType.NON_TERMINAL) {
                        // Set `gotoTable[s][B] = t`.
                        gotoTable.get(s).put(B, sTransitions.get(B));
                    }
                 }
            }
        }
    }

    /**
     * Helper method to check for conflicts before setting an action.
     */
    private void checkAndSetAction(int s, Symbol a, Action newAction, String conflictType) {
        Map<Symbol, Action> sAction = action.get(s);
        if (sAction == null) {
            // Should not happen if initialized above, but for safety:
            sAction = action.computeIfAbsent(s, k -> new HashMap<>());
        }
        
        Action existingAction = sAction.get(a);
        
        if (existingAction == null) {
            sAction.put(a, newAction);
        } else {
            // Conflict detected!
            String conflictDetails = String.format(
                "State %d: %s Conflict on terminal '%s'. Existing: %s, New: %s", 
                s, conflictType, a.name, existingAction.toString(), newAction.toString());
            conflicts.add(conflictDetails);
            
            // Standard resolution (often favors shift over reduce, but here we just report)
            // For the practice, we just report and typically keep the existing one or choose a priority.
            // Since the problem doesn't specify resolution, we just report and don't overwrite
            // to maintain determinism (or you could prioritize one for testing). 
            // We'll prioritize the existing action if it's not a SHIFT vs REDUCE conflict.
        }
    }
    
    // ... (Getters and KernelEntry class can remain as is)
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Action>> getActionTable() { return action; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getGotoTable() { return gotoTable; }
    public java.util.List<String> getConflicts() { return conflicts; }
    private static class KernelEntry {
        public final com.compiler.parser.grammar.Production production;
        public final int dotPosition;
        KernelEntry(com.compiler.parser.grammar.Production production, int dotPosition) {
            this.production = production;
            this.dotPosition = dotPosition;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof KernelEntry)) return false;
            KernelEntry o = (KernelEntry) obj;
            return dotPosition == o.dotPosition && production.equals(o.production);
        }
        @Override
        public int hashCode() {
            int r = production.hashCode();
            r = 31 * r + dotPosition;
            return r;
        }
    }
    public java.util.List<java.util.Set<LR1Item>> getLALRStates() { return lalrStates; }
    public java.util.Map<Integer, java.util.Map<com.compiler.parser.grammar.Symbol, Integer>> getLALRTransitions() { return lalrTransitions; }
    public int getInitialState() { return initialState; }
}
