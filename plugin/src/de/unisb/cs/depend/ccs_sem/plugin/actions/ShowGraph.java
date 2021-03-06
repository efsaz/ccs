package de.unisb.cs.depend.ccs_sem.plugin.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import de.unisb.cs.depend.ccs_sem.plugin.Global;
import de.unisb.cs.depend.ccs_sem.plugin.views.CCSGraphView;


public class ShowGraph extends Action implements IWorkbenchWindowActionDelegate, IEditorActionDelegate {

    public ShowGraph() {
        super("Evaluate");
        setImageDescriptor(ImageDescriptor.createFromURL(getClass().getResource("/resources/icons/refresh.gif")));
        setDisabledImageDescriptor(ImageDescriptor.createFromURL(getClass().getResource("/resources/icons/refresh_dis.gif")));
        setToolTipText("Evaluate (opens the Graph view)");
    }

    public void dispose() {
        // nothing to do
    }

    public void init(IWorkbenchWindow window) {
        // nothing to do
    }

    @Override
    public void run() {
        try {
            final IWorkbench workbench = PlatformUI.getWorkbench();
            final IWorkbenchWindow activeWorkbenchWindow = workbench == null ? null
                : workbench.getActiveWorkbenchWindow();
            final IWorkbenchPage activePage = activeWorkbenchWindow == null ? null
                : activeWorkbenchWindow.getActivePage();
            if (activePage != null) {
                final IViewPart view = activePage.showView(Global.getGraphViewId());
                final IEditorPart activeEditor = activePage.getActiveEditor();
                if (activeEditor != null && view instanceof CCSGraphView) {
                    ((CCSGraphView)view).showGraphFor(activeEditor, true);
                }
            }
        } catch (final PartInitException e) {
            e.printStackTrace();
        }
    }

    public void run(IAction action) {
        run();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // nothing to do
    }

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        // nothing to do
    }

}
