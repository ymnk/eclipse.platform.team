package org.eclipse.team.internal.ccvs.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.ccvs.core.CVSTeamProvider;
import org.eclipse.team.core.ITeamManager;
import org.eclipse.team.core.ITeamProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.ui.actions.TeamAction;

/**
 * TagAction tags the selected resources with a version tag specified by the user.
 */
public class TagAction extends TeamAction {
	// The previously remembered tag
	private static String previousTag = "";
	
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					final Shell s = getShell();
					final String[] result = new String[1];
					s.getDisplay().syncExec(new Runnable() {
						public void run() {
							// Prompt for the tag
							IInputValidator validator = new IInputValidator() {
								public String isValid(String tagName) {
									IStatus status = CVSTag.validateTagName(tagName);
									if(status.isOK()) {
										return null;
									} else {
										return status.getMessage();
									}
								}
							};
							
							InputDialog dialog = new InputDialog(s, Policy.bind("TagAction.tagResources"), Policy.bind("TagAction.enterTag"), previousTag, validator);
							if (dialog.open() != InputDialog.OK) return;
							result[0] = dialog.getValue();
						}
					});
					if (result[0] == null) return;
					Hashtable table = getProviderMapping();
					Set keySet = table.keySet();
					monitor.beginTask("", keySet.size() * 1000);
					Iterator iterator = keySet.iterator();
					while (iterator.hasNext()) {
						IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
						CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
						List list = (List)table.get(provider);
						IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
						provider.tag(providerResources, IResource.DEPTH_INFINITE, result[0], false, subMonitor);
					}	
					previousTag = result[0];							
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			}
		}, Policy.bind("TagAction.tag"), this.PROGRESS_DIALOG);

	}
	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		IResource[] resources = getSelectedResources();
		try {
			if (resources.length == 0) return false;
			ITeamManager manager = TeamPlugin.getManager();
			for (int i = 0; i < resources.length; i++) {
				ITeamProvider provider = manager.getProvider(resources[i].getProject());
				if (provider == null) return false;
				if (!((CVSTeamProvider)provider).isManaged(resources[i])) return false;
			}
			return true;
		} catch (TeamException e) {
			return false;
		}
	}
}

