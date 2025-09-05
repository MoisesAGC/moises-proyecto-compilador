package com.compiler.lexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * NfaToDfaConverter
 * -----------------
 * This class provides a static method to convert a Non-deterministic Finite Automaton (NFA)
 * into a Deterministic Finite Automaton (DFA) using the standard subset construction algorithm.
 */
/**
 * Utility class for converting NFAs to DFAs using the subset construction algorithm.
 */
public class NfaToDfaConverter {
	/**
	 * Default constructor for NfaToDfaConverter.
	 */
		public NfaToDfaConverter() {
			// TODO: Implement constructor if needed
		}

	/**
	 * Converts an NFA to a DFA using the subset construction algorithm.
	 * Each DFA state represents a set of NFA states. Final states are marked if any NFA state in the set is final.
	 *
	 * @param nfa The input NFA
	 * @param alphabet The input alphabet (set of characters)
	 * @return The resulting DFA
	 */
	public static DFA convertNfaToDfa(NFA nfa, Set<Character> alphabet) {
		/*
		 Pseudocode:
		 1. Create initial DFA state from epsilon-closure of NFA start state
		 2. While there are unmarked DFA states:
			  - For each symbol in alphabet:
				  - Compute move and epsilon-closure for current DFA state
				  - If target set is new, create new DFA state and add to list/queue
				  - Add transition from current to target DFA state
		 3. Mark DFA states as final if any NFA state in their set is final
		 4. Return DFA with start state and all DFA states
		*/
		// Lista y cola de estados DFA
		List<DfaState> dfaStates = new ArrayList<>();
		Queue<DfaState> unmarked = new LinkedList<>();

		// 1. Crear estado inicial del DFA a partir de epsilon-closure del estado inicial del NFA
		Set<State> startClosure = epsilonClosure(Set.of(nfa.getStartState()));
		DfaState startDfaState = new DfaState(startClosure);
		dfaStates.add(startDfaState);
		unmarked.add(startDfaState);

		// 2. Procesar mientras existan estados DFA sin marcar
		while (!unmarked.isEmpty()) {
			DfaState current = unmarked.poll();

			// Para cada símbolo del alfabeto
			for (char symbol : alphabet) {
				// move y epsilonClosure
				Set<State> moveSet = move(current.getName(), symbol);
				Set<State> closure = epsilonClosure(moveSet);

				if (!closure.isEmpty()) {
					// Verificar si ya existe un estado DFA con ese conjunto
					DfaState target = findDfaState(dfaStates, closure);

					if (target == null) {
						target = new DfaState(closure);
						dfaStates.add(target);
						unmarked.add(target);
					}

					// Agregar transición
					current.addTransition(symbol, target);
				}
			}
		}

		// 3. Marcar estados finales
		markFinalStates(dfaStates);

		// 4. Devolver DFA completo
		return new DFA(startDfaState, dfaStates);
	}


	/**
	 * Marca los estados DFA como finales si contienen al menos un estado final del NFA.
	 */
	private static void markFinalStates(List<DfaState> dfaStates) {
		for (DfaState dfaState : dfaStates) {
			boolean hasFinal = dfaState.getName().stream().anyMatch(State::isFinal);
			dfaState.setFinal(hasFinal);
		}
	}

	/**
	 * Computes the epsilon-closure of a set of NFA states.
	 * The epsilon-closure is the set of states reachable by epsilon (null) transitions.
	 *
	 * @param states The set of NFA states.
	 * @return The epsilon-closure of the input states.
	 */
	private static Set<State> epsilonClosure(Set<State> states) {
		/*
		Pseudocode:
		1. Initialize closure with input states
		2. Use stack to process states
		3. For each state, add all reachable states via epsilon transitions
		4. Return closure set
		*/
		Set<State> closure = new HashSet<>(states);
		Stack<State> stack = new Stack<>();
		stack.addAll(states);

		while (!stack.isEmpty()) {
			State actual = stack.pop();
			// Revisar transiciones epsilon
			for (State next : actual.getEpsilonTransitions()) {
				if (!closure.contains(next)) {
					closure.add(next);
					stack.push(next);
				}
			}
		}

		return closure;
	}

	/**
	 * Returns the set of states reachable from a set of NFA states by a given symbol.
	 *
	 * @param states The set of NFA states.
	 * @param symbol The input symbol.
	 * @return The set of reachable states.
	 */
	private static Set<State> move(Set<State> states, char symbol) {
		/*
		 Pseudocode:
		 1. For each state in input set:
			  - For each transition with given symbol:
				  - Add destination state to result set
		 2. Return result set
		*/
		Set<State> result = new HashSet<>();
		for (State state : states) {
			for (State next : state.getTransitions(symbol)) {
				result.add(next);
			}
		}

		return result;
	}

	/**
	 * Finds an existing DFA state representing a given set of NFA states.
	 *
	 * @param dfaStates The list of DFA states.
	 * @param targetNfaStates The set of NFA states to search for.
	 * @return The matching DFA state, or null if not found.
	 */
	private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> targetNfaStates) {
	   /*
	    Pseudocode:
	    1. For each DFA state in list:
		    - If its NFA state set equals target set, return DFA state
	    2. If not found, return null
	   */
		for (DfaState dfaState : dfaStates) {
				if (dfaState.getName().equals(targetNfaStates)){
					return dfaState;
				} 
			}
			return null;
	}
}
