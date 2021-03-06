package de.unisb.cs.depend.ltlchecker;

import java.util.HashMap;

import gov.nasa.ltl.graph.*;
import gov.nasa.ltl.trans.ParseErrorException;
import ltlcheck.Counterexample;
import ltlcheck.IModelCheckingMonitor;
import de.unisb.cs.depend.ccs_sem.semantics.expressions.Expression;
import de.unisb.cs.depend.ccs_sem.semantics.types.Transition;

public class ExpressionLTLChecker {
	
	// Evtl cache was schon gemacht wurde
	private static Graph lastGraph = null;
	private static Expression lastExp = null;
	private static HashMap<Expression,Node> nodes;
	
	/**
	 * Invariantly there is a node for <code>exp</code> in <code>nodes</code> before the call.
	 * @param g
	 * @param exp
	 */
	private static void addChildren(Graph g, Expression exp) {
		for( Transition trans : exp.getTransitions() ) {
			Node child = nodes.get(trans.getTarget() );
			
			if( child == null ) {
				child = new Node(g);
				nodes.put(trans.getTarget(), child);
				
				// add all children
				addChildren(g, trans.getTarget());
			}
			
			// add edge to this node 
			new Edge(nodes.get(exp), child, "-",trans.getAction().toString() ,null);
		}
	}
	
	/**
	 * Checks the formula after preprocessing it.
	 * So it's allowed to have things in it like WFAIR(a).
	 * 
	 * @param exp - The expression to check (it has to be evaluated before!)
	 * @param formula - the formula to check
	 * @return a counter example or <code>null</code> if the formula is satisfied
	 * @throws ParseErrorException
	 */
	public static Counterexample check(Expression exp, String formula,
				IModelCheckingMonitor monitor) throws ParseErrorException
	{
		assert exp!=null && exp.isEvaluated();
		
		formula = LTLFormulaPreprocessor.preprocessFormula(formula);
		
		if( monitor == null ) {
			monitor = new IModelCheckingMonitor() {
				public void subTask(String str) {
					System.out.println(str);
				}
			};
		}
		
		monitor.subTask("Prepare LTL formula...");
		formula = LTLFormula.prepare(formula);
		
		Graph graph = null;
		if( lastExp != null && lastExp.equals(exp) ) {
			graph = lastGraph;
		}
		
		monitor.subTask("Building CCS Graph...");
		if( graph == null ) {
			graph = new Graph();
		
			// build graph
			nodes = new HashMap<Expression,Node> ();
			Node n = new Node(graph);
			graph.setInit(n);
			nodes.put(exp, n);
		
			addChildren(graph, exp); // via getTransitions
			
			lastExp = exp;
			lastGraph = graph;
		}
		
		// run model-checker for this graph structure
		return ltlcheck.LtlModelChecker.check(graph, formula, monitor);
	}
}
