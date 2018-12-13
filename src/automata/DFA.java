
package automata;

import java.util.*;
import java.io.File;

import util.Pair;


/**
 * Deterministic Finite-States Automaton class.
 * Each transition has a label of type LabelT.
 */
public class DFA<LabelT>
		extends AbstractGraph<LabelT, DFA.DNode<LabelT>>
		implements Automaton<LabelT>, LatexPrintableGraph {
	
	// >>> Fields

	/* Auxiliary variable, used for printing a legend of the labels.
	 * It's used with (short_label, long_label) couples. */
	private Map<Integer,String> legendMap = null;


	// >>> Private functions
	
	/**
	 * Creates a new node instance.
	 * Just the override is important.
	 * @param id the id
	 * @return A new DNode
	 */
	@Override
	DNode<LabelT> newNodeObj(int id) {
		return new DNode<LabelT>(id);
	}
	

	/**
	 * Create a new DFA with merged arcs for each pair of nodes.
	 * This is useful for printing in Latex the graph. Arcs won't be overlapped.
	 * NOTE: LabelT should have a good string representation.
	 * @return A new DFA representing this object. The two DFA are not
	 * equivalent.
	 */
	private DFA<String> simpleArcsRepresentation() {

		DFA<String> simpleDFA = new DFA<>();  // Create a new DFA
		Map<DNode<LabelT>, DNode<String>> nodesMap =  // Old -> new map
				new HashMap<>();

		// Optional: using ints instead of true labels
		int nextIntLabel = 0;
		boolean usingLegend = false;
		if (legendMap != null) {
			legendMap.clear();
			usingLegend = true;
		}
	
		// Create all nodes in advance
		for (DNode<LabelT> node: this) {
			DNode<String> newNode = new DNode<String>(node.id, node.getFinalFlag());
			nodesMap.put(node, newNode);
		}

		// Set initial state
		simpleDFA.firstNode = nodesMap.get(this.firstNode);

		// Traverse this graph for the arcs
		for (DNode<LabelT> node: this) {

			// Map child -> merged arcs
			Map<DNode<String>, String> mergedArcs = new HashMap<>();

			// List and merge all arcs
			for (LabelT arc: node.getLabels()) {
				DNode<String> newChild = nodesMap.get(node.followArc(arc));
				String mergedArc = mergedArcs.get(newChild);
				if (mergedArc == null) { // Add new
					mergedArc = arc.toString();
				} else {                 // Merge new
					mergedArc = mergedArc + " | " + arc.toString();
				}
				mergedArcs.put(newChild, mergedArc);
			}
			
			// Add the merged arcs to the new node
			for (DNode<String> newChild: mergedArcs.keySet()) {
				String finalLabel = mergedArcs.get(newChild);

				// Abbreviation?
				if (usingLegend) {
					legendMap.put(nextIntLabel, finalLabel);
					finalLabel = Integer.toString(nextIntLabel);
					++nextIntLabel;
				}

				// Add the arc
				DNode<String> newNode = nodesMap.get(node);
				newNode.addArc(finalLabel, newChild);
			}
		}

		return simpleDFA;
	}


	/**
	 * Build LaTex tree representation.
	 * Depth first visit of the graph. First part of the helper function:
	 * just a spanning tree is printed.
	 * NOTE: assuming the labels are not Latex special codes.
	 * @param stringB The string representation, modified in place,initally empty
	 * @param parent The parent node, initially null.
	 * @param outLabel The label to go through, initially null.
	 * @param visited Set of visited states, initially empty.
	 * @param loops Arcs not printed because they form loops, initially empty.
	 * @see DFA#getLatexGraphRepresentation
	 */
	private void buildLatexRepresentation1(StringBuilder stringB,
			DNode<LabelT> parent, LabelT outLabel, Set<DNode<LabelT>> visited,
			Set<Pair<DNode<LabelT>, LabelT>> loops) {

		// Get the current node
		DNode<LabelT> node;
		if (parent != null) {
			node = parent.followArc(outLabel);
		} else {
			node = firstNode;
		}

		// If this is already drawn, save for later
		if (visited.contains(node)) {
			Pair<DNode<LabelT>, LabelT> arc = new Pair<>(parent, outLabel);
			loops.add(arc);
			return;
		}

		// Read this node
		visited.add(node);
		Set<LabelT> labels = node.getLabels();

		// Add the node id
		stringB.append("\n\t\t");
		stringB.append(node.id).append(' ');

		// Add final, if that is the case
		boolean openBracket = false;
		if (node.getFinalFlag()) {
			stringB.append("[accept");
			openBracket = true;
		}

		// Add the incoming label
		if (outLabel != null) {
			char c = (openBracket) ? ',' : '[';
			openBracket = true;
			stringB.append(c).append(">\"").append(outLabel.toString()).append('"');
		}

		if (openBracket) { stringB.append("] "); }

		// Base case: no children
		if (labels.isEmpty()) { return; }

		// Recursion
		stringB.append("-> {");

		int i = labels.size();
		for (LabelT l: labels) {
			buildLatexRepresentation1(stringB, node, l, visited, loops);
			--i;
			char sep = (i > 0) ? ',' : '}';
			stringB.append(sep);
		}
	}


	/**
	 * Build LaTex tree representation.
	 * Second part of the helper function: the loops are printed.
	 * NOTE: assuming the labels are not Latex special codes.
	 * @param stringB The string representation, result is appended.
	 * @param loops Arcs to print.
	 * @see DFA#getLatexGraphRepresentation
	 */
	private void buildLatexRepresentation2(StringBuilder stringB,
			Set<Pair<DNode<LabelT>, LabelT>> loops) {

		stringB.append(",\n");

		// Writing edges one by one.
		for (Pair<DNode<LabelT>, LabelT> arc: loops) {
			DNode<LabelT> node = arc.left;
			LabelT l = arc.right;

			// If this is a self loop
			if (node.followArc(l) == node) {
				stringB.append("\t\t").
						append(node.id).
						append(" -> [clear >, \"" + l + "\", self loop] ").
						append(node.id).
						append(",\n");
			}
			// Else, we go to some previous node
			else {
				stringB.append("\t\t").
						append(node.id).
						append(" -> [clear >, \"" + l + "\", backward] ").
						append(node.followArc(l).id).
						append(",\n");
			}
		}
		stringB.delete(stringB.length()-2, stringB.length());
	}


	// >>> Public functions
	
	/**
	 * Parse this sequence and return the result.
	 * If the sequence has some nonexistent transitions, false is returned.
	 * @param sequence A list of labels
	 * @param strict If true, for any sequence leading to impossible transitions,
	 * a RuntimeException is thrown; if false, the sequence is just rejected.
	 * @return true if the sequence is accepted, false otherwise
	 */
	@Override
	public boolean parseSequence(List<LabelT> sequence, boolean strict) {

		// Traverse the automaton
		DNode<LabelT> node = followPath(sequence);

		if (node == null) {
			if (strict) {
				throw new RuntimeException("Can't parse " + sequence +
						" : impossible transitions.");
			} else {
				return false;
			}
		}
		return node.getFinalFlag();
	}


	/**
	 * Returns the body of a tikzpicture in Latex that represents this graph.
	 * @return The string for this graph
	 */
	@Override
	public String getLatexGraphRepresentation() {

		// Simplify the graph
		legendMap = new HashMap<>(); // Shortening labels
		DFA<String> simpleDFA = simpleArcsRepresentation();

		// Data structures
		StringBuilder stringB = new StringBuilder();
		HashSet<DNode<String>> visited = new HashSet<>();
		Set<Pair<DNode<String>,String>> loops = new HashSet<>();

		// Recursive call
		simpleDFA.buildLatexRepresentation1(stringB, null, null, visited, loops);

		// Adding remaining edges
		simpleDFA.buildLatexRepresentation2(stringB, loops);

		return stringB.toString();
	}


	/**
	 * Extra latex code used for printing the legend.
	 * A new environment in which the legend of the labels is printed
	 * @return The legend as Latex code
	 */
	@Override
	public String extraLatexEnv() {

		// Open
		StringBuilder stringB = new StringBuilder();
		stringB.append("\n");
		stringB.append("\\textbf{Transitions}\n");
		stringB.append("\\begin{flushleft}\n");
		stringB.append("\\begin{description}\n");
		
		// Sorted legend
		List<Integer> shortLabels = new ArrayList<Integer>(legendMap.keySet());
		Collections.sort(shortLabels);

		for (Integer shortLabel: shortLabels) {
			String longLabel = legendMap.get(shortLabel);
			stringB.append("\t\\item [" + shortLabel + "] " + longLabel + "\n");
		}
		
		// Close
		stringB.append("\\end{description}\n");
		stringB.append("\\end{flushleft}\n");

		return stringB.toString();
	}


	/**
	 * This function is used to pass options to standalone document class.
	 * Return the empty string to pass nothing. Otherwise return a
	 * list of options like: [varwidth]
	 * @return Latex options for standalone document class
	 */
	@Override
	public String standaloneClassLatexOptions() {
		return "[varwidth=40em]"; // NOTE: assuming the DFA is small enough
	}


	/**
	 * Debugging
	 */
	public static void test() {
		
		DNode.test();

		System.out.println("Testing DFA");
		
		// Define a graph
		DFA<Character> dfa = new DFA<>();
		DNode<Character> n1 = dfa.newChild(dfa.firstNode, 'a');
		DNode<Character> n2 = dfa.newChild(n1, 'b');
		DNode<Character> n3 = dfa.newChild(n1, 'c');
		n3.addArc('a', n1);
		n3.addArc('g', n3);
		n2.setFinalFlag(true);
		dfa.firstNode.setFinalFlag(true);
		DNode<Character> n4 = dfa.newChild(n2, 'c');
		n4.setFinalFlag(true);

		// Test parsing
		List<Character> l;
		l = new ArrayList<Character>();
		System.out.println(dfa.parseSequence(l, false));
		l = Arrays.asList('a','c');
		System.out.println(dfa.parseSequence(l, false));
		l = Arrays.asList('a','h');
		System.out.println(dfa.parseSequence(l, false));
		l = Arrays.asList('a','c','a','b');
		System.out.println(dfa.parseSequence(l, false));


		// Test Latex
		LatexPrintableGraph printableGraph = dfa;
		LatexSaver.saveLatexFile(printableGraph, new File("latex/dfa.tex"), 1);

		System.out.println();
	}


	// >>> Nested classes

	/**
	 * Class for each node of the DFA.
	 * Each node can be final (i.e. accepting), or not.
	 */
	public static class DNode<LabelT> 
			extends AbstractNode<LabelT,DNode<LabelT>> {

		// >>> Fields
		
		/* Each state can be final or not */
		private boolean isFinal = false;


		// >>> Public functions
		
		/**
		 * Constructor: just set the id
		 * @param id Any identifier
		 */
		public DNode(int id) {
			super(id);
		}


		/**
		 * Constructor: id and final
		 * @param id Any identifier
		 * @param isFinal Whether this is a final state
		 */
		public DNode(int id, boolean isFinal) {
			super(id);
			this.isFinal = isFinal;
		}


		/**
		 * Set if this state is accepting or not.
		 * @param isFinal Final flag
		 */
		public void setFinalFlag(boolean isFinal) {
			this.isFinal = isFinal;
		}


		/**
		 * Returns whether this is a final state
		 * @return Wether this is a final state
		 */
		public boolean getFinalFlag() {
			return this.isFinal;
		}


		/**
		 * String representation
		 * @return A string with the id and '_final' if that is the case
		 */
		@Override
		public String toString() {
			if (isFinal) {
				return id + "_Final";
			} else {
				return Integer.toString(id);
			}
		}


		/**
		 * Debugging
		 */
		public static void test() {

			System.out.println("Testing DFA.DNode");

			// Testing basic methods
			DNode<Character> n1 = new DNode<>(1, true);
			DNode<Character> n2 = new DNode<>(2);
			DNode<Character> n3 = new DNode<>(3, true);

			n1.addArc('a', n2);
			n2.addArc('b', n3);
			n2.addArc('c', n2);
			n2.addArc('d', n1);

			System.out.println(n2.getLabels()); // ['b', 'c', 'd']
			System.out.println(n3.getLabels()); // []

			System.out.println(n1.followArc('a').followArc('c')); // 2

			n2.removeArc('h');
			n2.removeArc('c');
			System.out.println(n1.followArc('a').followArc('c')); // null
			
			System.out.println();
		}
	}
}
