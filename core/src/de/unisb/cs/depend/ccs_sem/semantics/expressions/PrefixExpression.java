package de.unisb.cs.depend.ccs_sem.semantics.expressions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.unisb.cs.depend.ccs_sem.exceptions.ParseException;
import de.unisb.cs.depend.ccs_sem.semantics.types.Declaration;
import de.unisb.cs.depend.ccs_sem.semantics.types.Parameter;
import de.unisb.cs.depend.ccs_sem.semantics.types.Transition;
import de.unisb.cs.depend.ccs_sem.semantics.types.actions.Action;
import de.unisb.cs.depend.ccs_sem.semantics.types.values.Value;


public class PrefixExpression extends Expression {

    private final Action prefix;
    private final Expression target;

    public PrefixExpression(Action prefix, Expression target) {
        super();
        this.prefix = prefix;
        this.target = target;
    }

    @Override
    public Collection<Expression> getChildren() {
        // nothing has to be evaluated before we can evaluate, so: empty set
        return Collections.emptySet();
    }

    @Override
    public Collection<Expression> getSubTerms() {
        return Collections.singleton(target);
    }

    @Override
    protected List<Transition> evaluate0() {
        return Collections.singletonList(new Transition(prefix, target));
    }

    @Override
    public Expression replaceRecursion(List<Declaration> declarations) throws ParseException {
        final Expression newTarget = target.replaceRecursion(declarations);
        if (newTarget.equals(target))
            return this;
        return ExpressionRepository.getExpression(new PrefixExpression(prefix, newTarget));
    }

    @Override
    public Expression instantiate(Map<Parameter, Value> parameters) {
        final Action newPrefix = prefix.instantiate(parameters);
        final Expression newTarget = target.instantiate(parameters);
        if (newPrefix.equals(prefix) && newTarget.equals(target))
            return this;
        return ExpressionRepository.getExpression(new PrefixExpression(newPrefix, newTarget));
    }

    @Override
    protected boolean isError0() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(prefix).append('.');
        if (target instanceof ChoiceExpression || target instanceof ParallelExpression
                || target instanceof RestrictExpression)
            sb.append('(').append(target).append(')');
        else
            sb.append(target);

        return sb.toString();
    }

    @Override
    protected int hashCode0() {
        final int PRIME = 31;
        int result = 4;
        result = PRIME * result + target.hashCode();
        result = PRIME * result + prefix.hashCode();
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
        final PrefixExpression other = (PrefixExpression) obj;
        // hashCode is cached, so we compare it first (it's cheap)
        if (hashCode() != other.hashCode())
            return false;
        if (!prefix.equals(other.prefix))
            return false;
        if (!target.equals(other.target))
            return false;
        return true;
    }

}