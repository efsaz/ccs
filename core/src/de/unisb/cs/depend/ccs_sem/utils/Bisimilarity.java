package de.unisb.cs.depend.ccs_sem.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import de.unisb.cs.depend.ccs_sem.semantics.expressions.Expression;
import de.unisb.cs.depend.ccs_sem.semantics.types.Transition;
import de.unisb.cs.depend.ccs_sem.semantics.types.actions.Action;
import de.unisb.cs.depend.ccs_sem.semantics.types.actions.TauAction;


/**
 * This class provides some methods to compute the quotient of the LTS w.r.t
 * weak bisimulation (i.e. it computes the smalles weak bisimilar LTS).
 *
 * It uses my very own algorithm and has a lot of strange optimisations to get
 * a very fast runtime.
 */
public abstract class Bisimilarity {

    private Bisimilarity() {
        // forbid instantiation
    }

    public static Map<Expression, Partition> computePartitions(Expression expression) {
        return computePartitions(Collections.singleton(expression));
    }

    public static Map<Expression, Partition> computePartitions(Collection<Expression> expressions) {
        final Queue<Partition> partitions = new PriorityQueue<Partition>(128,
            new Comparator<Partition>() {
                public int compare(Partition o1, Partition o2) {
                    if (o1.isNew() && !o2.isNew())
                        return -1;
                    if (o2.isNew() && !o1.isNew())
                        return 1;
                    return o2.getExprWrappers().size() - o1.getExprWrappers().size();
                }
            });

        // first, fill the partitions list
        final Map<Expression, ExprWrapper> exprMap = new HashMap<Expression, ExprWrapper>();
        {
            final Set<Expression> seen = new HashSet<Expression>();
            seen.addAll(expressions);

            final Queue<Expression> queue = new LinkedList<Expression>();
            queue.addAll(expressions);

            Expression expr2;
            while ((expr2 = queue.poll()) != null) {
                exprMap.put(expr2, new ExprWrapper(expr2, null));

                if (!expr2.isEvaluated())
                    throw new IllegalArgumentException("Expression or one of it's successors is not evaluated.");

                for (final Transition trans: expr2.getTransitions()) {
                    final Expression succ = trans.getTarget();
                    if (seen.add(succ))
                        queue.add(succ);
                }
            }

            // now, add all transitions to the expression wrappers
            final Map<Transition, TransWrapper> transMap = new HashMap<Transition, TransWrapper>();
            for (final Entry<Expression, ExprWrapper> entry: exprMap.entrySet()) {
                final List<TransWrapper> newTransitions = new ArrayList<TransWrapper>();
                for (final Transition trans: entry.getKey().getTransitions()) {
                    TransWrapper tw = transMap.get(trans);
                    if (tw == null)
                        transMap.put(trans, tw = new TransWrapper(trans.getAction(),
                            exprMap.get(trans.getTarget())));
                    newTransitions.add(tw);
                }
                entry.getValue().transitions = newTransitions;
            }

            final Partition partition = new Partition(new ArrayList<ExprWrapper>(exprMap.values()));
            partitions.add(partition);
        }


        // now, divide the partitions into new partitions
        final List<Partition> readyPartitions = new ArrayList<Partition>();
        final List<Partition> unChangedPartitions = new ArrayList<Partition>();
        Partition partition;
        int changed = 0;
        //final int i = 0;
        while (true) {
            while ((partition = partitions.poll()) != null) {
                /*
                if (++i % 10000 == 0)
                    Main.log(i + ": Ready: " + readyPartitions.size() + "; unchanged: " + unChangedPartitions.size() + "; changed: " + changed + "; queue: " + partitions.size());
                */
                if (partition.getExprWrappers().size() < 2) {
                    readyPartitions.add(partition);
                } else if (partition.divide(partitions)) {
                    if (++changed * 10 >= unChangedPartitions.size())
                        break;
                } else {
                    unChangedPartitions.add(partition);
                }
            }
            if (changed == 0)
                break;

            changed = 0;
            partitions.addAll(unChangedPartitions);
            unChangedPartitions.clear();
        }

        final Map<Expression, Partition> partitionMap = new HashMap<Expression, Partition>(1+exprMap.size()*4/3);

        for (final Entry<Expression, ExprWrapper> entry: exprMap.entrySet())
            partitionMap.put(entry.getKey(), entry.getValue().part);

        return partitionMap;
    }

    public static class Partition {
        private final List<ExprWrapper> expressionWrappers;
        private boolean isNew = true;

        public Partition(List<ExprWrapper> expressionWrappers) {
            this.expressionWrappers = expressionWrappers;
            for (final ExprWrapper ew: expressionWrappers)
                ew.part = this;
        }

        public Set<TransitionToPartition> computeTransitions() {
            final Set<TransitionToPartition> transitions = new HashSet<TransitionToPartition>();
            for (final ExprWrapper ew: expressionWrappers)
                for (final TransWrapper tw: ew.transitions)
                    transitions.add(new TransitionToPartition(tw));
            return transitions;
        }

        public List<ExprWrapper> getExprWrappers() {
            return expressionWrappers;
        }

        protected boolean isNew() {
            return isNew;
        }

        public boolean divide(Queue<Partition> partitions) {
            isNew = false;

            final Collection<TransitionToPartition> transitions = computeTransitions();
            for (final TransitionToPartition trans: transitions) {
                List<ExprWrapper> fulfills = null;
                List<ExprWrapper> fulfillsNot = null;
                for (final ExprWrapper otherExpr: expressionWrappers) {
                    if (fulfills(otherExpr, trans)) {
                        if (fulfills != null)
                            fulfills.add(otherExpr);
                    } else {
                        if (fulfills == null) {
                            fulfills = new ArrayList<ExprWrapper>(expressionWrappers.size());
                            fulfillsNot = new ArrayList<ExprWrapper>(expressionWrappers.size());
                            for (final ExprWrapper e2: expressionWrappers) {
                                if (e2.equals(otherExpr))
                                    break;
                                fulfills.add(e2);
                            }
                        }
                        fulfillsNot.add(otherExpr);
                    }
                }
                if (fulfills != null) {
                    final Partition part1 = new Partition(fulfills);
                    final Partition part2 = new Partition(fulfillsNot);
                    partitions.add(part1);
                    partitions.add(part2);
                    return true;
                }
            }
            return false;
        }

        private static boolean fulfills(ExprWrapper otherExpr, TransitionToPartition trans) {
            if (trans.act instanceof TauAction) {
                final Queue<ExprWrapper> tauReachable = new LinkedList<ExprWrapper>();
                tauReachable.add(otherExpr);
                final Set<ExprWrapper> seen = new HashSet<ExprWrapper>();
                seen.add(otherExpr);
                ExprWrapper e;
                while ((e = tauReachable.poll()) != null) {
                    if (e.part.equals(trans.targetPart))
                        return true;

                    for (final TransWrapper t: e.transitions)
                        if (t.act instanceof TauAction && seen.add(t.target))
                            tauReachable.add(t.target);
                }
                return false;
            } else {
                final Queue<ExprWrapper> tauReachable = new LinkedList<ExprWrapper>();
                tauReachable.add(otherExpr);
                final Set<ExprWrapper> seen = new HashSet<ExprWrapper>();
                seen.add(otherExpr);
                ExprWrapper e;
                while ((e = tauReachable.poll()) != null) {
                    for (final TransWrapper t: e.transitions) {
                        if (t.act instanceof TauAction) {
                            if (seen.add(t.target))
                                tauReachable.add(t.target);
                        } else {
                            if (t.act.equals(trans.act)) {
                                // now check all tau-reachable successor states for a match
                                final Queue<ExprWrapper> reachableAfter = new LinkedList<ExprWrapper>();
                                reachableAfter.add(t.target);
                                final Set<ExprWrapper> seenAfter = new HashSet<ExprWrapper>();
                                seenAfter.add(t.target);
                                ExprWrapper e2;
                                while ((e2 = reachableAfter.poll()) != null) {
                                    if (e2.part.equals(trans.targetPart))
                                        return true;

                                    for (final TransWrapper t2: e2.transitions)
                                        if (t2.act instanceof TauAction && seenAfter.add(t2.target))
                                                reachableAfter.add(t2.target);
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }

    }

    private static class ExprWrapper {
        public Expression expr;
        public Partition part;
        public List<TransWrapper> transitions;

        public ExprWrapper(Expression e, Partition partition) {
            this.expr = e;
            this.part = partition;
            this.transitions = null;
        }

        // no need for hashCode or equals because these objects are unique
        // for an expression

    }

    private static class TransWrapper {
        public Action act;
        public ExprWrapper target;

        public TransWrapper(Action act, ExprWrapper target) {
            this.act = act;
            this.target = target;
        }

        // no need for hashCode or equals because these objects are unique
        // for any combination of action and expression

    }

    public static class TransitionToPartition {
        public Action act;
        public Partition targetPart;

        public TransitionToPartition(Action act, Partition targetPart) {
            this.act = act;
            this.targetPart = targetPart;
        }

        public TransitionToPartition(TransWrapper tw) {
            this(tw.act, tw.target.part);
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + act.hashCode();
            result = PRIME * result + targetPart.hashCode();
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
            final TransitionToPartition other = (TransitionToPartition) obj;
            if (!act.equals(other.act))
                return false;
            if (!targetPart.equals(other.targetPart))
                return false;
            return true;
        }



    }

}
