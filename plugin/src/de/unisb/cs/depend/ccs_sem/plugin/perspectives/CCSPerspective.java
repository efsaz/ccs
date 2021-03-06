package de.unisb.cs.depend.ccs_sem.plugin.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import de.unisb.cs.depend.ccs_sem.plugin.Global;


public class CCSPerspective implements IPerspectiveFactory {

    private IPageLayout factory;

    public CCSPerspective() {
        super();
    }

    public void createInitialLayout(IPageLayout factory) {
        this.factory = factory;
        factory.setFixed(false);
        addViews();
        addActionSets();
        addNewWizardShortcuts();
        addPerspectiveShortcuts();
        addViewShortcuts();
    }

    private void addViews() {
        // Creates the overall folder layout.
        // Note that each new Folder uses a percentage of the remaining EditorArea.

        final IFolderLayout leftTop =
            factory.createFolder(
                "leftTop",
                IPageLayout.LEFT,
                0.25f,
                factory.getEditorArea());

        final IFolderLayout leftBottom =
            factory.createFolder(
                "leftBottom",
                IPageLayout.BOTTOM,
                0.6f,
                "leftTop");

        final IFolderLayout bottom =
            factory.createFolder(
                "bottom",
                IPageLayout.BOTTOM,
                0.45f,
                factory.getEditorArea());

        final IFolderLayout center =
            factory.createFolder(
                "center",
                IPageLayout.TOP,
                0.75f,
                "bottom");

        leftTop.addView(IPageLayout.ID_RES_NAV);
        leftTop.addView( Global.getCounterExampleViewId() );
        leftBottom.addView(IPageLayout.ID_OUTLINE);
        bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
        
        center.addView(Global.getGraphViewId());
        center.addView(Global.getStepByStepTraverseViewId());
        center.addView(Global.getLTLCheckerViewId());
    }

    private void addActionSets() {
        //factory.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET); //NON-NLS-1
        //factory.addActionSet(Global.getActionSetId());
        //factory.addActionSet(Global.getResourceCreationActionSetId());
    }

    private void addPerspectiveShortcuts() {
        // nothing
    }

    private void addNewWizardShortcuts() {
        factory.addNewWizardShortcut(Global.getNewCCSProjectWizardId());
        factory.addNewWizardShortcut(Global.getNewCCSFileWizardId());
    }

    private void addViewShortcuts() {
        factory.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
    }

}
