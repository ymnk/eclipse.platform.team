package org.eclipse.team.internal.ccvs.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
 
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.ui.CVSLocalCompareEditorInput;
import org.eclipse.team.ui.actions.TeamAction;

/**
 * Action for container compare with base.
 */
public class CompareWithBaseAction extends CompareWithTagAction {
	/*
	 * Method declared on IActionDelegate.
	 */
	public void run(IAction action) {
		CompareUI.openCompareEditor(new CVSLocalCompareEditorInput(getSelectedResources(), CVSTag.BASE));
	}
}
