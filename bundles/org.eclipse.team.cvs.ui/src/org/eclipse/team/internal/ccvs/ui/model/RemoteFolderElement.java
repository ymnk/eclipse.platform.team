/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.model;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

public class RemoteFolderElement extends RemoteResourceElement {

	public class RemoteFolderElement_SerializeMeRule implements ISchedulingRule {		
		public boolean isConflicting(ISchedulingRule rule) {
			if(rule instanceof RemoteFolderElement_SerializeMeRule) {
				return true;
			}
			return false;
		}
	}
	private static RemoteFolderElement_SerializeMeRule instance = null;
	public RemoteFolderElement_SerializeMeRule getInstance() {
		if(instance == null) {
			instance =   new RemoteFolderElement_SerializeMeRule();
		}
		return instance;
	}
	
	/**
	 * Overridden to append the version name to remote folders which
	 * have version tags and are top-level folders.
	 */
	public String getLabel(Object o) {
		if (!(o instanceof ICVSRemoteFolder)) return null;
		ICVSRemoteFolder folder = (ICVSRemoteFolder)o;
		CVSTag tag = folder.getTag();
		if (tag != null && tag.getType() != CVSTag.HEAD) {
			if (folder.getRemoteParent() == null) {
				return Policy.bind("RemoteFolderElement.nameAndTag", folder.getName(), tag.getName()); //$NON-NLS-1$
			}
		}
		return folder.getName();
	}
	public ImageDescriptor getImageDescriptor(Object object) {
		if (!(object instanceof ICVSRemoteFolder)) return null;
		ICVSRemoteFolder folder = (ICVSRemoteFolder) object;
		if (folder.isDefinedModule()) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_MODULE);
		}
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#getChildrenDeferred(org.eclipse.team.internal.ccvs.ui.model.RemoteContentProvider, java.lang.Object, org.eclipse.ui.IWorkingSet)
	 */
	public void getChildrenDeferred(final RemoteContentProvider provider, final Object parent, IWorkingSet workingSet) {
		final String familyName = "org.eclipse.team.cvs.ui.remotefolderelement";
		// Create a job
		Job job = new Job() {
			public IStatus run(IProgressMonitor monitor) {
				ICVSRemoteResource[] children = new ICVSRemoteResource[0];
				try {
					children = (ICVSRemoteResource[])internalGetChildren(parent, monitor);
				} catch (TeamException e) {
					// Concurrency: what to do about an exception?
				}
				provider.addChildren(parent, children, monitor);
				return Status.OK_STATUS;
			}
			
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.String)
			 */
			public boolean isMatching(String family) {
				return family.equals(familyName);
			}
		};
		job.setRule(getInstance());		
		job.schedule();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#isDeferred()
	 */
	public boolean isDeferred() {
		return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#internalGetChildren(java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object[] internalGetChildren(Object o, IProgressMonitor monitor) throws TeamException {
		if (!(o instanceof ICVSRemoteFolder)) return new Object[0];
		return ((ICVSRemoteFolder)o).members(monitor);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.model.CVSModelElement#isRemoteElement()
	 */
	public boolean isRemoteElement() {
		return true;
	}
}
