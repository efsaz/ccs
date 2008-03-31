package de.unisb.cs.depend.ccs_sem.plugin.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import de.unisb.cs.depend.ccs_sem.plugin.views.CCSGraphView;


public class Evaluate implements IViewActionDelegate {

    private IViewPart view;

    public void init(IViewPart view) {
        this.view = view;
    }

    public void run(IAction action) {
        if (view instanceof CCSGraphView)
            ((CCSGraphView)view).update();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // ignore
    }

}