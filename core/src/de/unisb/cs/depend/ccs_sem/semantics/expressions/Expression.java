package de.unisb.cs.depend.ccs_sem.semantics.expressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.unisb.cs.depend.ccs_sem.exceptions.ParseException;
import de.unisb.cs.depend.ccs_sem.semantics.expressions.RecursiveExpression.RecursiveExpressionAlphabetWrapper;
import de.unisb.cs.depend.ccs_sem.semantics.expressions.adapters.TopMostExpression;
import de.unisb.cs.depend.ccs_sem.semantics.types.Parameter;
import de.unisb.cs.depend.ccs_sem.semantics.types.ParameterOrProcessEqualsWrapper;
import de.unisb.cs.depend.ccs_sem.semantics.types.ProcessVariable;
import de.unisb.cs.depend.ccs_sem.semantics.types.Transition;
import de.unisb.cs.depend.ccs_sem.semantics.types.actions.Action;
import de.unisb.cs.depend.ccs_sem.semantics.types.values.ParameterReference;
import de.unisb.cs.depend.ccs_sem.semantics.types.values.Value;
import de.unisb.cs.depend.ccs_sem.utils.LazyCreatedMap;

public abstract class Expression {
	
	private static final int START_NUMBERING = 0; 

	private static HashMap<String,Integer> leftRightMap;
	private static boolean isVisibleTau = true; // TODO TAU PreferenceStore
	
	public static boolean getVisibleTau() {
		return isVisibleTau;
	}
	
	public static void setVisibleTau( boolean tau ) {
		isVisibleTau = tau;
	}
	
    private volatile List<Transition> transitions = null;

    // stores the hashcode of this expression
    protected int hash = 0;

    // cache for isError()
    private Boolean isError = null;

    protected Expression() {
        // nothing to do
    }

    /**
     * Evaluates this expression, i.e. creates a List of all outgoing {@link Transition}s.
     *
     * Before calling this method, all children (see {@link #getChildren()})
     * must have been evaluated.
     */
    public void evaluate() {
        if (isEvaluated())
            return;

        List<Transition> transitions0;
        if (isError())
            transitions0 = Collections.emptyList();
        else
            transitions0 = evaluate0();

        assert transitions0 != null;

        // some optimisations to save memory
        if (transitions0 instanceof ArrayList) {
            if (transitions0.size() == 0)
                transitions0 = Collections.emptyList();
            else if (transitions0.size() == 1)
                transitions0 = Collections.singletonList(transitions0.get(0));
            else
                ((ArrayList<Transition>)transitions0).trimToSize();
        }

        // volatile write
        transitions = transitions0;
    }

    public boolean isEvaluated() {
        return transitions != null;
    }
    
    public void resetEval() {
    	if( transitions != null ) {
    		// first copy the list to avoid divergence
    		List<Transition> list = transitions;
    		transitions = null;
    		
    		for(Transition trans : list ) {
    			trans.getTarget().resetEval();
    			trans.getAction().resetLRTrace();
    		}
    		for( Expression e : getChildren() ) {
    			e.resetEval();
    		}
    	}
    }

    // precondition: children have been evaluated
    protected abstract List<Transition> evaluate0();

    /**
     * @return the children that have to be evaluated before calling evaluate()
     */
    public abstract Collection<Expression> getChildren();

    /**
     * @return all subterms occuring in this expression. In general, it is the
     *         same as the children, but in some Expressions, it must be overwritten.
     */
    public Collection<Expression> getSubTerms() {
        return getChildren();
    }

    /**
     * Precondition: evaluate() has been called before.
     *
     * @return all outgoing transitions of this expression.
     */
    public List<Transition> getTransitions() {
        assert transitions != null;

        return transitions;
    }

    /**
     * Replaces every {@link UnknownRecursiveExpression} by a
     * {@link RecursiveExpression}, if a corresponding {@link ProcessVariable}
     * has been found.
     * Typically delegates to its subterms.
     * @return either itself or a new created Expression, if something changed
     */
    public abstract Expression replaceRecursion(List<ProcessVariable> processVariables) throws ParseException;

    /**
     * Is called i.e. by a {@link RecursiveExpression} to get the instantiated
     * Expression from a {@link ProcessVariable}.
     * Replaces all {@link ParameterReference}s that occure in the expression by
     * the corresponding {@link Value} from the parameter list.
     * Typically delegates to its subterms.
     *
     * @param parameters the parameters to replace by concrete values
     * @return either <code>this</code> or a new created Expression
     */
    public abstract Expression instantiate(Map<Parameter, Value> parameters);

    /**
     * @return whether this Expression represents an "error" expression
     */
    public boolean isError() {
        if (isError == null)
            isError = Boolean.valueOf(isError0());
        return isError;
    }

    /**
     * @see #isError()
     */
    protected abstract boolean isError0();

    /**
     * Computes the alphabet of this Expression.
     *
     * @return the alphabet of this Expression, as map from actual action to
     *         the (possibly parameterized) action that generated it.
     */
    public final Map<Action, Action> getAlphabet() {
        return getAlphabet(new HashSet<RecursiveExpressionAlphabetWrapper>(4));
    }

    /**
     * Only for internal use. Always call {@link #getAlphabet()}.
     *
     * @param alreadyIncluded a set of {@link RecursiveExpression}s with channels
     *                        that have already been taken into account
     * @return (a part of) the alphabet of this Expression
     */
    public abstract Map<Action, Action> getAlphabet(Set<RecursiveExpressionAlphabetWrapper> alreadyIncluded);

    // we store the hashCode so that we only compute it once
    @Override
    public final int hashCode() {
        int h = this.hash;
        if (h == 0) {
            h = hashCode(new LazyCreatedMap<ParameterOrProcessEqualsWrapper, Integer>(4));
            this.hash = h;
        }

        return h;
    }

    public abstract int hashCode(Map<ParameterOrProcessEqualsWrapper, Integer> parameterOccurences);

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        // hashCode is cached, so we compare it first (it's cheap)
        if (hashCode() != ((Expression)obj).hashCode())
            return false;
        
        return equals(obj, new LazyCreatedMap<ParameterOrProcessEqualsWrapper, Integer>(4));
    }

    public abstract boolean equals(Object obj,
            Map<ParameterOrProcessEqualsWrapper, Integer> parameterOccurences);

    public synchronized static void genereateLeftRightMap(Expression exp) {
    	if( exp == null )
    		throw new IllegalArgumentException("Expression to generate l,r-Map is null!");
    	if( !(exp instanceof TopMostExpression)) {
    		throw new IllegalArgumentException("l,r-Map has to be initialized with a TopMostExpression");
    	}
    	exp = ((TopMostExpression) exp).getInnerExpression();
    	
    	if( exp instanceof RestrictExpression) {
    		exp = ((RestrictExpression) exp).getChildren().iterator().next();
    	}
    	
    	leftRightMap = new HashMap<String, Integer> ();
    	
    	SortedSet<String> set = new TreeSet<String> ();
    	generateLRList(set, exp, "");

    	int counter = START_NUMBERING;
    	String tmp;
    	while( !set.isEmpty() ) {
    		tmp = set.first();
    		set.remove(tmp);
    		leftRightMap.put(tmp, counter);
    		counter++;
    	}
    }
    
    private static void generateLRList(SortedSet<String> set, Expression exp, String state) {
    	if( exp instanceof ParallelExpression) {
    		generateLRList(set, ((ParallelExpression) exp).getLeft(), state+"l");
    		generateLRList(set, ((ParallelExpression) exp).getRight(), state+"r");
    	} else {
    		set.add(state);
    	}
    }
    
    public static synchronized void removeLeftRightMap() {
    	leftRightMap = null;
    }
    
    public static synchronized boolean isLeftRightMapGenerated() {
    	return !(leftRightMap == null);
    }
    
    public static int getProcessNumber(Action act) {
    	return getProcessNumber(act.getLRTrace());
    }
    
    public static int getProcessNumber(String lrTrace) {
    	assert isLeftRightMapGenerated();
    	
    	for(int i=lrTrace.length(); i>=0; i--) {
    		if( leftRightMap.containsKey(
    					lrTrace.substring(0, i)
    				) ) {
    			return leftRightMap.get(
    					lrTrace.substring(0,i)
    					);
    		}
    	}
    	return -1; // Error
    }
}
