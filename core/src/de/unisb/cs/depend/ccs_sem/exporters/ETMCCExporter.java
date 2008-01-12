package de.unisb.cs.depend.ccs_sem.exporters;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import de.unisb.cs.depend.ccs_sem.exceptions.ExportException;
import de.unisb.cs.depend.ccs_sem.exporters.helpers.StateNumberComparator;
import de.unisb.cs.depend.ccs_sem.exporters.helpers.StateNumerator;
import de.unisb.cs.depend.ccs_sem.exporters.helpers.TransitionCounter;
import de.unisb.cs.depend.ccs_sem.exporters.helpers.TransitionsTargetNumberComparator;
import de.unisb.cs.depend.ccs_sem.semantics.expressions.Expression;
import de.unisb.cs.depend.ccs_sem.semantics.types.Transition;


public class ETMCCExporter implements Exporter {

    final File traFile;

    public ETMCCExporter(File traFile) {
        super();
        this.traFile = traFile;
    }

    public void export(Expression expr) throws ExportException {
        final PrintWriter traWriter;
        try {
            traWriter = new PrintWriter(traFile);
        } catch (final IOException e) {
            throw new ExportException("Error opening .tra-File: "
                    + e.getMessage(), e);
        }

        final Map<Expression, Integer> stateNumbers =
                StateNumerator.numerateStates(expr, 1);
        final int transitionCount = TransitionCounter.countTransitions(expr);

        // write tra header
        traWriter.print("STATES ");
        traWriter.println(stateNumbers.size());
        traWriter.print("TRANSITIONS ");
        traWriter.println(transitionCount);

        // write the transitions
        final PriorityQueue<Expression> queue =
                new PriorityQueue<Expression>(11, new StateNumberComparator(
                        stateNumbers));
        queue.add(expr);

        final Set<Expression> written = new HashSet<Expression>(stateNumbers.size()*3/2);
        written.add(expr);

        while (!queue.isEmpty()) {
            final Expression e = queue.poll();
            final Collection<Transition> transitions = e.getTransitions();
            final PriorityQueue<Transition> transQueue =
                    new PriorityQueue<Transition>(transitions.size(),
                            new TransitionsTargetNumberComparator(stateNumbers));
            transQueue.addAll(transitions);
            final int sourceStateNr = stateNumbers.get(e);
            while (!transQueue.isEmpty()) {
                final Transition trans = transQueue.poll();
                final Expression targetExpr = trans.getTarget();
                final int targetStateNr = stateNumbers.get(targetExpr);
                traWriter.println("d " + sourceStateNr + " " + targetStateNr
                        + " 0.0 I");
                if (written.add(targetExpr))
                    queue.add(targetExpr);
            }
        }

        // close the tra file
        traWriter.close();

    }

}