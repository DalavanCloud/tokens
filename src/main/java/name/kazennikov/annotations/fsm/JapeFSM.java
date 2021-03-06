package name.kazennikov.annotations.fsm;


import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.kazennikov.annotations.patterns.AnnotationMatcher;
import name.kazennikov.annotations.patterns.AnnotationMatcherPatternElement;
import name.kazennikov.annotations.patterns.BasePatternElement;
import name.kazennikov.annotations.patterns.PatternElement;
import name.kazennikov.annotations.patterns.PatternElement.Operator;
import name.kazennikov.annotations.patterns.RangePatternElement;
import name.kazennikov.annotations.patterns.Rule;

import com.google.common.base.Objects;


public class JapeFSM {
	
	public static interface StateVisitor {
		public void visit(State s);
	}
	
	public static class State {
		int number;
		List<Transition> transitions = new ArrayList<>();
		Set<Rule> actions = new HashSet<>();


		public boolean isFinal() {
			return !actions.isEmpty();
		}

		public void addTransition(State to, AnnotationMatcher matcher, List<String> bindings) {
			Transition t = new Transition();
			t.bindings = new ArrayList<>(bindings);
			t.matcher = matcher;
			t.target = to;
			transitions.add(t);
		}


		private String escape(String s) {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < s.length(); i++) {
				char ch = s.charAt(i);
				if(ch == '"')
					sb.append("\\");
				sb.append(ch);
			}

			return sb.toString();
		}


		public void toDot(PrintWriter pw, Set<State> visited) {
			if(visited.contains(this))
				return;

			visited.add(this);

			for(Transition t : transitions) {
				pw.printf("%d -> %d [label=\"%s%s\"];%n", number, t.target.number, escape(t.matcher != null? t.matcher.toString() : "null"), t.bindings);
			}

			for(Transition t : transitions) {
				t.target.toDot(pw, visited);
			}



			if(isFinal()) {
				pw.printf("%d [shape=doublecircle];%n", number);
			}
		}
		
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.add("number", number)
					.add("isFinal", isFinal())
					.toString();
		}
		
		public List<Transition> getTransitions() {
			return transitions;
		}
		
		public Set<Rule> getActions() {
			return actions;
		}
		
		public int getNumber() {
			return number;
		}
		
		public void visit(StateVisitor v, Set<State> visited) {
			if(visited.contains(this))
				return;
			
			visited.add(this);
			v.visit(this);

			for(Transition t : transitions) {
				t.target.visit(v, visited);
			}
		}
		
		public void setFinalFrom(Set<State> states) {
			for(State currentInnerState : states) {
				if(currentInnerState.isFinal()) {
					actions.addAll(currentInnerState.actions);
				}
			}
		}
	}
	
	public static class Transition {
		AnnotationMatcher matcher;
		List<String> bindings;
		State target;
		
		public List<String> getBindings() {
			return bindings;
		}
		
		public State getTarget() {
			return target;
		}
		
		public AnnotationMatcher getMatcher() {
			return matcher;
		}

	}


	List<State> states = new ArrayList<>();	
	State start;
	
	public JapeFSM() {
		this.start = addState();
	}
	
	public void addRule(Rule r) {
		State start = this.start;
		for(PatternElement e : r.lhs()) {
			start = addPE(e, start, new ArrayList<String>());
		}
		
		start.actions.add(r);
	}
	
	public State addState() {
		State state = new State();
		state.number = states.size();
		states.add(state);
		return state;
	}
	
	public void addTransition(State src, State dest, AnnotationMatcher label, List<String> bindings) {
		src.addTransition(dest, label, bindings);
	}
	
	/**
	 * Add NFA states to start that represent pattern element
	 * @param e pattern element
	 * @param start current start state
	 * @return end state of the pattern element
	 */
	private State addPE(PatternElement e, State start, List<String> bindings) {
		State end = null;
		
		if(e instanceof AnnotationMatcherPatternElement) {
			end = addState();
			AnnotationMatcher matcher = ((AnnotationMatcherPatternElement) e).matcher();
			
			addTransition(start, end, matcher, bindings);
		} else if(e instanceof BasePatternElement) {
			// OR or SEQ
			if(e.op() == Operator.OR) {
				end = addOR((BasePatternElement)e, start, bindings);
			} else if(e.op() == Operator.SEQ) {
				end = addSeq((BasePatternElement)e, start, bindings);
			}
			
		} else if(e instanceof RangePatternElement) {
			end = addRange((RangePatternElement)e, start, bindings);
		}

		return end;
	}

	private State addRange(RangePatternElement e, State start, List<String> bindings) {
		State end = addState();
		
		for(int i = 0; i < e.min(); i++) {
			start = addPE(e.get(0), start, bindings);
		}
		addTransition(start, end, null, bindings); // skip optional parrts
		
		if(e.max() == RangePatternElement.INFINITE) {
			State mEnd = addPE(e.get(0), start, bindings);
			addTransition(mEnd, end, null, bindings);
			addTransition(mEnd, start, null, bindings);
		} else {
			for(int i = e.min(); i < e.max(); i++) {
				start = addPE(e.get(0), start, bindings);
				addTransition(start, end, null, bindings);
			}
		} 
		
		return end;
	}

	private State addSeq(BasePatternElement e, State start, List<String> bindings) {
		if(e.getName() != null)
			bindings.add(e.getName());
		
		for(int i = 0; i < e.size(); i++) {
			start = addPE(e.get(i), start, bindings);
		}
		
		if(e.getName() != null)
			bindings.remove(bindings.size() - 1);
		
		return start;
		
	}

	private State addOR(BasePatternElement e, State start, List<String> bindings) {
		State end = addState();

		for(int i = 0; i < e.size(); i++) {
			State mStart = addState();
			addTransition(start, mStart, null, bindings);
			State mEnd = addPE(e.get(i), mStart, bindings);
			addTransition(mEnd, end, null, bindings);
		}

		return end;
	}


	public void toDot(PrintWriter pw) {
		pw.println("digraph finite_state_machine {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle]");
		start.toDot(pw, new HashSet<State>());
		pw.println("}");

	}
	
	public void toDot(String fileName) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fileName);
		toDot(pw);
		pw.close();
	}
	
	public Set<State> lambdaClosure(Set<State> states) {
		LinkedList<State> list = new LinkedList<State>(states);
		Set<State> closure = new HashSet<State>(states);

		while(!list.isEmpty()) {
			State current = list.removeFirst();
			for(Transition t : current.transitions) {
				if(t.matcher == null) {
					State target = t.target;
					if(!closure.contains(target)) {
						closure.add(target);
						list.addFirst(target);
					}
				}
			}
		}
		return closure;
	}

	
	/**
	 * Converts this epsilon-NFA to epsilon-free NFA. Non-destructive procedure
	 * 
	 * @return fresh epsilon free NFA
	 */
	public JapeFSM epsilonFreeFSM() {

		Map<Set<State>, State> newStates = new HashMap<>();
		Set<Set<State>> dStates = new HashSet<>();
		LinkedList<Set<State>> unmarkedDStates = new LinkedList<Set<State>>();
		Set<State> currentDState = new HashSet<State>();


		currentDState.add(start);
		currentDState = lambdaClosure(currentDState);
		dStates.add(currentDState);
		unmarkedDStates.add(currentDState);
		JapeFSM fsm = new JapeFSM();

		newStates.put(currentDState, fsm.start);
		fsm.start.setFinalFrom(currentDState);

		while(!unmarkedDStates.isEmpty()) {
			currentDState = unmarkedDStates.removeFirst();

			for(State state: currentDState) {
				for(Transition t : state.transitions) {

					// skip epsilon transitions
					if(t.matcher == null)
						continue;


					State target = t.target;
					Set<State> newDState = new HashSet<State>();
					newDState.add(target);
					newDState = lambdaClosure(newDState);

					// add new state to epsilon-free automaton
					if(!dStates.contains(newDState)) {
						dStates.add(newDState);
						unmarkedDStates.add(newDState);
						State newState = fsm.addState();
						newStates.put(newDState, newState);

						newState.setFinalFrom(newDState);
					}

					State currentState = newStates.get(currentDState);
					State newState = newStates.get(newDState);
					fsm.addTransition(currentState, newState, t.matcher, t.bindings);

				}
			}

		}

		return fsm;
	}
	
	public State getStart() {
		return start;
	}
	
	public int size() {
		return states.size();
	}
}
