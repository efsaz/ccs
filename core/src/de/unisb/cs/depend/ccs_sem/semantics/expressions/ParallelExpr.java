package de.unisb.cs.depend.ccs_sem.semantics.expressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unisb.cs.depend.ccs_sem.exceptions.ParseException;
import de.unisb.cs.depend.ccs_sem.semantics.types.Declaration;
import de.unisb.cs.depend.ccs_sem.semantics.types.Parameter;
import de.unisb.cs.depend.ccs_sem.semantics.types.Transition;
import de.unisb.cs.depend.ccs_sem.semantics.types.actions.TauAction;
import de.unisb.cs.depend.ccs_sem.semantics.types.value.Value;


public class ParallelExpr extends Expression {

    private final Expression left;
    private final Expression right;

    public ParallelExpr(Expression left, Expression right) {
        super();
        this.left = left;
        this.right = right;
    }

    @Override
    public Collection<Expression> getChildren() {
        final List<Expression> children = new ArrayList<Expression>(2);
        children.add(left);
        children.add(right);
        return children;
    }

    @Override
    protected List<Transition> evaluate0() {
        final List<Transition> leftTransitions = left.getTransitions();
        final List<Transition> rightTransitions = right.getTransitions();

        final List<Transition> transitions =
                new ArrayList<Transition>(
                        (leftTransitions.size() + rightTransitions.size()) * 3 / 2);

        // either left alone:
        for (final Transition trans: leftTransitions) {
            Expression newExpr = new ParallelExpr(trans.getTarget(), right);
            // search if this expression is already known
            newExpr = Expression.getExpression(newExpr);
            // search if this transition is already known (otherwise create it)
            final Transition newTrans =
                    Transition.getTransition(trans.getAction(), newExpr);
            transitions.add(newTrans);
        }

        // or right alone:
        for (final Transition trans: rightTransitions) {
            Expression newExpr = new ParallelExpr(left, trans.getTarget());
            // search if this expression is already known
            newExpr = Expression.getExpression(newExpr);
            // search if this transition is already known (otherwise create it)
            final Transition newTrans =
                    Transition.getTransition(trans.getAction(), newExpr);
            transitions.add(newTrans);
        }

        // or synchronized:
        // this is one of the hardest tasks, so it might be useful to use a more
        // clever way than just iterate nestedly through both transition lists
        // (however, on a small number of transitions, the naive way is mor efficient)

        // TODO try other values here
        final boolean useCleverWay =
                leftTransitions.size() > 3
                        && rightTransitions.size() > 3
                        && (leftTransitions.size() * rightTransitions.size()) > 20;
        if (useCleverWay) {
            combineUsingCleverWay(leftTransitions, rightTransitions, transitions);
        } else {
            combineUsingNaiveWay(leftTransitions, rightTransitions, transitions);
        }

        return transitions;
    }

    private void combineUsingNaiveWay(final List<Transition> leftTransitions,
            final List<Transition> rightTransitions,
            final List<Transition> transitions) {
        for (final Transition leftTrans: leftTransitions)
            for (final Transition rightTrans: rightTransitions) {
                Expression newFromLeft = null;
                Expression newFromRight = null;
                if (leftTrans.getAction().isInputAction()
                        && rightTrans.getAction().isOutputAction())
                    newFromLeft = leftTrans.synchronizeWith(rightTrans.getAction());
                if (rightTrans.getAction().isInputAction()
                        && leftTrans.getAction().isOutputAction())
                    newFromRight = rightTrans.synchronizeWith(leftTrans.getAction());

                if (newFromLeft != null) {
                    if (newFromRight != null) {
                        // take care that we don't add the same transition twice
                        if (!newFromLeft.equals(leftTrans.getTarget())
                                || !newFromRight.equals(rightTrans.getTarget())) {
                            // in this case, we have to add this new transition too
                            final Expression newTarget = Expression.getExpression(
                                new ParallelExpr(leftTrans.getTarget(), newFromRight));
                            final Transition newTransition = Transition.getTransition(TauAction.get(), newTarget);
                            transitions.add(newTransition);
                        }
                    }
                    final Expression newTarget = Expression.getExpression(
                        new ParallelExpr(newFromLeft, rightTrans.getTarget()));
                    final Transition newTransition = Transition.getTransition(TauAction.get(), newTarget);
                    transitions.add(newTransition);
                } else if (newFromRight != null) {
                    final Expression newTarget = Expression.getExpression(
                        new ParallelExpr(leftTrans.getTarget(), newFromRight));
                    final Transition newTransition = Transition.getTransition(TauAction.get(), newTarget);
                    transitions.add(newTransition);
                }
            }
    }

    private void combineUsingCleverWay(final List<Transition> leftTransitions,
            final List<Transition> rightTransitions,
            final List<Transition> transitions) {
        final Map<String, List<Transition>> leftInput =
            new HashMap<String, List<Transition>>(leftTransitions.size());
        final Map<String, List<Transition>> rightInput =
            new HashMap<String, List<Transition>>(rightTransitions.size());

        for (final Transition leftTrans: leftTransitions) {
            if (leftTrans.getAction().isInputAction()) {
                final String channel = leftTrans.getAction().getChannel();
                List<Transition> list = leftInput.get(channel);
                if (list == null)
                    leftInput.put(channel, list = new ArrayList<Transition>(2));
                list.add(leftTrans);
            }
        }
        for (final Transition rightTrans: rightTransitions) {
            if (rightTrans.getAction().isInputAction()) {
                final String channel = rightTrans.getAction().getChannel();
                List<Transition> list = rightInput.get(channel);
                if (list == null)
                    rightInput.put(channel, list = new ArrayList<Transition>(2));
                list.add(rightTrans);
            }
            if (rightTrans.getAction().isOutputAction()) {
                // search for corresponding input action
                final String channel = rightTrans.getAction().getChannel();
                final List<Transition> inputTransitions = leftInput.get(channel);
                if (inputTransitions != null) {
                    for (final Transition inputTrans: inputTransitions) {
                        final Expression newLeftTarget = inputTrans.synchronizeWith(rightTrans.getAction());
                        if (newLeftTarget != null) {
                            // i.e. there was a match
                            final Expression newTarget = Expression.getExpression(
                                new ParallelExpr(newLeftTarget, rightTrans.getTarget()));
                            final Transition newTrans = Transition.getTransition(TauAction.get(), newTarget);
                            transitions.add(newTrans);
                        }
                    }
                }
            }
        }
        for (final Transition leftTrans: leftTransitions) {
            if (!leftTrans.getAction().isOutputAction())
                continue;
            // search for corresponding input action
            final String channel = leftTrans.getAction().getChannel();
            final List<Transition> inputTransitions = rightInput.get(channel);
            if (inputTransitions != null) {
                for (final Transition inputTrans: inputTransitions) {
                    final Expression newRightTarget = inputTrans.synchronizeWith(leftTrans.getAction());
                    if (newRightTarget != null) {
                        boolean seenBefore = inputTrans.getAction().isOutputAction()
                            && leftTrans.getAction().isInputAction();
                        if (seenBefore) {
                            final Expression newLeftTarget = leftTrans.synchronizeWith(inputTrans.getAction());
                            if (newLeftTarget == null
                                || !newLeftTarget.equals(leftTrans.getTarget())
                                || !newRightTarget.equals(inputTrans.getTarget()))
                                seenBefore = false;
                        }
                        if (seenBefore)
                            continue;

                        final Expression newTarget = Expression.getExpression(
                            new ParallelExpr(leftTrans.getTarget(), newRightTarget));
                        final Transition newTrans = Transition.getTransition(TauAction.get(), newTarget);
                        transitions.add(newTrans);
                    }
                }
            }
        }
    }

    @Override
    public Expression replaceRecursion(List<Declaration> declarations)
            throws ParseException {
        final Expression newLeft = left.replaceRecursion(declarations);
        final Expression newRight = right.replaceRecursion(declarations);

        if (newLeft.equals(left) && newRight.equals(right))
            return this;

        return Expression.getExpression(new ParallelExpr(newLeft, newRight));
    }

    @Override
    public Expression instantiate(List<Value> parameters) {
        final Expression newLeft = left.instantiate(parameters);
        final Expression newRight = right.instantiate(parameters);
        if (newLeft.equals(left) && newRight.equals(right))
            return this;
        return Expression.getExpression(new ParallelExpr(newLeft, newRight));
    }

    @Override
    public Expression insertParameters(List<Parameter> parameters) {
        final Expression newLeft = left.insertParameters(parameters);
        final Expression newRight = right.insertParameters(parameters);
        if (newLeft.equals(left) && newRight.equals(right))
            return this;
        return Expression.getExpression(new ParallelExpr(newLeft, newRight));
    }

    @Override
    public Expression instantiateInputValue(Value value) {
        final Expression newLeft = left.instantiateInputValue(value);
        final Expression newRight = right.instantiateInputValue(value);
        if (newLeft.equals(left) && newRight.equals(right))
            return this;
        return Expression.getExpression(new ParallelExpr(newLeft, newRight));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (left instanceof RestrictExpr) {
            sb.append('(').append(left).append(')');
        } else {
            sb.append(left);
        }
        sb.append(" | ");
        if (right instanceof RestrictExpr) {
            sb.append('(').append(right).append(')');
        } else {
            sb.append(right);
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((left == null) ? 0 : left.hashCode());
        result = PRIME * result + ((right == null) ? 0 : right.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ParallelExpr other = (ParallelExpr) obj;
        if (left == null) {
            if (other.left != null)
                return false;
        } else if (!left.equals(other.left))
            return false;
        if (right == null) {
            if (other.right != null)
                return false;
        } else if (!right.equals(other.right))
            return false;
        return true;
    }

}
