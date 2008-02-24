package de.unisb.cs.depend.ccs_sem.semantics.types;

import java.util.List;

import de.unisb.cs.depend.ccs_sem.evaluators.EvaluationMonitor;
import de.unisb.cs.depend.ccs_sem.evaluators.Evaluator;
import de.unisb.cs.depend.ccs_sem.exceptions.ParseException;
import de.unisb.cs.depend.ccs_sem.semantics.expressions.Expression;
import de.unisb.cs.depend.ccs_sem.semantics.expressions.adapters.MinimisingExpression;
import de.unisb.cs.depend.ccs_sem.utils.Globals;


/**
 * This class represents a whole program, containing all Declarations and
 * the main expression that describes the program.
 *
 * @author Clemens Hammacher
 */
public class Program {

    private final List<Declaration> declarations;
    private boolean isMinimized = false;
    private Expression mainExpression = null;
    private Expression minimizedExpression = null;

    public Program(List<Declaration> declarations, Expression expr) throws ParseException {
        this.mainExpression = expr.replaceRecursion(declarations);
        this.declarations = declarations;
        for (final Declaration decl: declarations)
            decl.replaceRecursion(declarations);
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean useUnminimizedExpression) {
        final String newLine = Globals.getNewline();

        final StringBuilder sb = new StringBuilder();
        for (final Declaration decl: declarations) {
            sb.append(decl).append(';').append(newLine);
        }
        if (declarations.size() > 0)
            sb.append(newLine);

        sb.append(isMinimized && !useUnminimizedExpression
            ? minimizedExpression : mainExpression);

        return sb.toString();
    }

    public Expression getMainExpression() {
        return mainExpression;
    }

    public Expression getMinimizedExpression() {
        return minimizedExpression;
    }

    public Expression getExpression() {
        return isMinimized ? minimizedExpression : mainExpression;
    }

    /**
     * A program is regular iff every recursive definition is regular.
     * See {@link Declaration#isRegular(List)}.
     */
    public boolean isRegular() {
        for (final Declaration decl: declarations)
            if (!decl.isRegular())
                return false;

        return true;
    }

    /**
     * A program is guarded iff every recursive definition is guarded.
     * See {@link Declaration#isGuarded(List)}.
     */
    public boolean isGuarded() {
        for (final Declaration decl: declarations)
            if (!decl.isGuarded())
                return false;

        return true;
    }

    public void evaluate(Evaluator eval) throws InterruptedException {
        evaluate(eval, null);
    }

    public boolean evaluate(Evaluator eval, EvaluationMonitor monitor)
            throws InterruptedException {
        return eval.evaluateAll(mainExpression, monitor);
    }

    public List<Transition> getTransitions() {
        return mainExpression.getTransitions();
    }

    public boolean isEvaluated() {
        return mainExpression.isEvaluated();
    }

    public boolean isMinimized() {
        return isMinimized;
    }

    /**
     * Before calling this method, the program must be evaluated.
     * @param minimizationMonitor an EvaluationMonitor that is informed about the progress
     * @param evaluator the preferred evaluator to use
     * @param strong if <code>true</code>, the lts is minimized w.r.t. strong
     *               bisimulation instead of weak bisimulation
     * @return <code>true</code> if minimization was successfull
     * @throws InterruptedException
     */
    public boolean minimizeTransitions(Evaluator evaluator, EvaluationMonitor minimizationMonitor, boolean strong) throws InterruptedException {
        assert isEvaluated();

        minimizedExpression = MinimisingExpression.create(mainExpression, strong);
        //minimizedExpression = new FastMinimisingExpression(mainExpression);

        if (minimizedExpression == null)
            return false;

        if (!evaluator.evaluateAll(minimizedExpression, minimizationMonitor))
            return false;

        isMinimized = true;
        return true;
    }

    public void minimizeTransitions() throws InterruptedException {
        minimizeTransitions(Globals.getDefaultEvaluator(), null, false);
    }

}
