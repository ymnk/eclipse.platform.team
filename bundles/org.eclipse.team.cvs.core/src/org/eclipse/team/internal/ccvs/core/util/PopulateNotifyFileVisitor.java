package org.eclipse.team.internal.ccvs.core.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.NotifyInfo;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PopulateNotifyFileVisitor implements ICVSResourceVisitor {

	private IProgressMonitor monitor;
	private int depth;

	private char notificationType;
	private Date timestamp;
	private char[] watches;
	
	/**
	 * @see org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFile(ICVSFile)
	 */
	public void visitFile(ICVSFile file) throws CVSException {
		if ( ! file.isManaged()) return;
		NotifyInfo info = new NotifyInfo(file, notificationType, timestamp, watches);
		file.setNotifyInfo(info);
		monitor.worked(1);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor#visitFolder(ICVSFolder)
	 */
	public void visitFolder(ICVSFolder folder) throws CVSException {
		if (folder.isCVSFolder()) {
			// Visit the children of the folder as appropriate
			if (depth == IResource.DEPTH_INFINITE) {
				folder.acceptChildren(this);
			} else if (depth == IResource.DEPTH_ONE) {
				ICVSResource[] files = folder.members(ICVSFolder.FILE_MEMBERS);
				for (int i = 0; i < files.length; i++) {
					files[i].accept(this);
				}
			}
		}
		monitor.worked(1);
	}

	public void visitResources(IProject project, final IResource[] resources, int depth, char notificationType, char[] watches, IProgressMonitor pm) throws CVSException {
		this.depth = depth;
		this.notificationType = notificationType;
		this.watches = watches;
		this.timestamp = new Date();
		CVSWorkspaceRoot.getCVSFolderFor(project).run(new ICVSRunnable() {
			public void run(IProgressMonitor pm) throws CVSException {
				monitor = Policy.infiniteSubMonitorFor(pm, 100);
				monitor.beginTask(null, 512);
				monitor.subTask(Policy.bind("PopulateNotifyFileVisitor.settingUpNotifications")); //$NON-NLS-1$
				for (int i = 0; i < resources.length; i++) {
					CVSWorkspaceRoot.getCVSResourceFor(resources[i]).accept(PopulateNotifyFileVisitor.this);
				}
				monitor.done();
			}
		}, pm);
	}
	
}
