/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;


import java.text.SimpleDateFormat;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.*;
import org.eclipse.team.internal.core.ExceptionCollector;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.PlatformUI;

public class CVSLightweightDecorator extends LabelProvider implements ILightweightLabelDecorator, IResourceStateChangeListener, IPropertyChangeListener {

	// Decorator id as defined in the decorator extension point
	public final static String ID = "org.eclipse.team.cvs.ui.decorator"; //$NON-NLS-1$
	
	// Images cached for better performance
	private static ImageDescriptor dirty;
	private static ImageDescriptor checkedIn;
	private static ImageDescriptor noRemoteDir;
	private static ImageDescriptor added;
	private static ImageDescriptor merged;
	private static ImageDescriptor newResource;
	private static ImageDescriptor edited;

	private static ExceptionCollector exceptions = new ExceptionCollector(Policy.bind("CVSDecorator.exceptionMessage"), CVSUIPlugin.ID, IStatus.ERROR, CVSUIPlugin.getPlugin().getLog()); //$NON-NLS-1$;
	
	private static String DECORATOR_FORMAT = "yyyy/MM/dd HH:mm:ss"; //$NON-NLS-1$
	private static SimpleDateFormat decorateFormatter = new SimpleDateFormat(DECORATOR_FORMAT, Locale.getDefault());

	/*
	 * Define a cached image descriptor which only creates the image data once
	 */
	public static class CachedImageDescriptor extends ImageDescriptor {
		ImageDescriptor descriptor;
		ImageData data;
		public CachedImageDescriptor(ImageDescriptor descriptor) {
			Assert.isNotNull(descriptor);
			this.descriptor = descriptor;
		}
		public ImageData getImageData() {
			if (data == null) {
				data = descriptor.getImageData();
			}
			return data;
		}
	}
	
	static {
		dirty = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR));
		checkedIn = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		added = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		merged = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_MERGED));
		newResource = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_QUESTIONABLE));
		edited = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_EDITED));
		noRemoteDir = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_NO_REMOTEDIR));
	}

	public CVSLightweightDecorator() {
		ResourceStateChangeListeners.getListener().addResourceStateChangeListener(this);
		TeamUI.addPropertyChangeListener(this);
		CVSUIPlugin.addPropertyChangeListener(this);
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().addPropertyChangeListener(this);
		CVSProviderPlugin.broadcastDecoratorEnablementChanged(true /* enabled */);
	}

	public static boolean isDirty(final ICVSResource cvsResource) throws CVSException {
		return !cvsResource.isIgnored() && cvsResource.isModified(null);
	}

	public static boolean isDirty(IResource resource) {

		// No need to decorate non-existant resources
		if (!resource.exists()) return false;

		try {
			return isDirty(CVSWorkspaceRoot.getCVSResourceFor(resource));
		} catch (CVSException e) {
			//if we get an error report it to the log but assume dirty.
			boolean accessible = resource.getProject().isAccessible();
			if (accessible) {
				// We only care about the failure if the project is open
				handleException(e);
			}
			// Return dirty if the project is open and clean otherwise
			return accessible;
		}

	}
	
	/*
	 * Answers null if a provider does not exist or the provider is not a CVS provider. These resources
	 * will be ignored by the decorator.
	 */
	private CVSTeamProvider getCVSProviderFor(IResource resource) {
		RepositoryProvider p =
			RepositoryProvider.getProvider(
				resource.getProject(),
				CVSProviderPlugin.getTypeId());
		if (p == null) {
			return null;
		}
		return (CVSTeamProvider) p;
	}

	/**
	 * Returns the resource for the given input object, or
	 * null if there is no resource associated with it.
	 *
	 * @param object  the object to find the resource for
	 * @return the resource for the given object, or null
	 */
	private IResource getResource(Object object) {
		if (object instanceof IResource) {
			return (IResource) object;
		}
		if (object instanceof IAdaptable) {
			return (IResource) ((IAdaptable) object).getAdapter(
				IResource.class);
		}
		return null;
	}
	
	/**
	 * This method should only be called by the decorator thread.
	 * 
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration) {
		IResource resource = getResource(element);
		if (resource == null || resource.getType() == IResource.ROOT)
			return;
			
		CVSTeamProvider cvsProvider = getCVSProviderFor(resource);
		if (cvsProvider == null)
			return;	
		
		try {
			CVSDecoration cvsDecoration = decorate(resource);
			cvsDecoration.setWatchEditEnabled(cvsProvider.isWatchEditEnabled());	
			cvsDecoration.apply(decoration);
		} catch(CVSException e) {
			handleException(e);
		}
	}

	public static CVSDecoration decorate(IResource resource) throws CVSException {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
		CVSDecoration cvsDecoration = new CVSDecoration(resource.getName(), resource.getType());
		if (cvsResource.isIgnored()) {
			cvsDecoration.setIgnored(true);
		}
		if (!cvsDecoration.isIgnored()) {
			// Dirty
			boolean computeDeepDirtyCheck = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);
			int type = resource.getType();
			if (type == IResource.FILE || computeDeepDirtyCheck) {
				cvsDecoration.setDirty(CVSLightweightDecorator.isDirty(resource));
			}
			// Tag
			CVSTag tag = getTagToShow(resource);
			if (tag != null) {
				String name = tag.getName();
				if (tag.getType() == CVSTag.DATE) {
					Date date = tag.asDate();
					if (date != null) {
						name = decorateFormatter.format(date);
					}
				}
				cvsDecoration.setTag(name);
			}
			// Has a remote
			cvsDecoration.setHasRemote(CVSWorkspaceRoot.hasRemote(resource));
			// Is a new resource
			if (store.getBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION)) {
				if (cvsResource.exists()) {
					if (cvsResource.isFolder()) {
						if (!((ICVSFolder) cvsResource).isCVSFolder()) {
							cvsDecoration.setNewResource(true);
						}
					} else if (!cvsResource.isManaged()) {
						cvsDecoration.setNewResource(true);
					}
				}
			}
		}
		// Extract type specific properties
		if (resource.getType() == IResource.FILE) {
			extractFileProperties((IFile) resource, cvsDecoration);
		} else {
			extractContainerProperties((IContainer) resource, cvsDecoration);
		}
		return cvsDecoration;
	}

	private static void extractContainerProperties(IContainer resource, CVSDecoration cvsDecoration) throws CVSException {
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(resource);
		FolderSyncInfo folderInfo = folder.getFolderSyncInfo();
		if (folderInfo != null) {
			cvsDecoration.setLocation(KnownRepositories.getInstance().getRepository(folderInfo.getRoot()));
			cvsDecoration.setRepository(folderInfo.getRepository());
			cvsDecoration.setVirtualFolder(folderInfo.isVirtualDirectory());
		}
	}

	private static void extractFileProperties(IFile resource, CVSDecoration cvsDecoration) throws CVSException {
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(resource);
		ResourceSyncInfo fileInfo = file.getSyncInfo();
		KSubstOption option = KSubstOption.fromFile((IFile) resource);
		if (fileInfo != null) {
			cvsDecoration.setAdded(fileInfo.isAdded());
			cvsDecoration.setRevision(fileInfo.getRevision());
			ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile) resource);
			cvsDecoration.setNeedsMerge(fileInfo.isNeedsMerge(file.getTimeStamp()));
			cvsDecoration.setKeywordSubstitution(fileInfo.getKeywordMode().getShortDisplayText());
		}
	}

	/**
	 * Only show the tag if the resources tag is different than the parents. Or else, tag
	 * names will clutter the text decorations.
	 */
	protected static CVSTag getTagToShow(IResource resource) throws CVSException {
		ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
		CVSTag tag = null;

		// for unmanaged resources don't show a tag since they will be added in
		// the context of their parents tag. For managed resources only show tags
		// if different than parent.
		boolean managed = false;

		if(cvsResource.isFolder()) {
			FolderSyncInfo folderInfo = ((ICVSFolder)cvsResource).getFolderSyncInfo();
			if(folderInfo != null) {
				tag = folderInfo.getTag();
				managed = true;
			}
		} else {
			ResourceSyncInfo info = ((ICVSFile)cvsResource).getSyncInfo();
			if(info != null) {
				tag = info.getTag();
				managed = true;
			}
		}

		ICVSFolder parent = cvsResource.getParent();
		if(parent != null && managed) {
			FolderSyncInfo parentInfo = parent.getFolderSyncInfo();
			if(parentInfo != null) {
				CVSTag parentTag = parentInfo.getTag();
				parentTag = (parentTag == null ? CVSTag.DEFAULT : parentTag);
				tag = (tag == null ? CVSTag.DEFAULT : tag);
				// must compare tags by name because CVS doesn't do a good job of
				// using  T and N prefixes for folders and files.
				if( parentTag.getName().equals(tag.getName())) {
					tag = null;
				}
			}
		}
		return tag;
	}

	/*
	 * Add resource and its parents to the List
	 */
	 
	private void addWithParents(IResource resource, Set resources) {
		IResource current = resource;

		while (current.getType() != IResource.ROOT) {
			resources.add(current);
			current = current.getParent();
		}
	}
	
	/*
	* Perform a blanket refresh of all CVS decorations
	*/
	public static void refresh() {
		CVSUIPlugin.getPlugin().getWorkbench().getDecoratorManager().update(CVSUIPlugin.DECORATOR_ID);
	}

	/*
	 * Update the decorators for every resource in project
	 */
	 
	public void refresh(IProject project) {
		final List resources = new ArrayList();
		try {
			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) {
					resources.add(resource);
					return true;
				}
			});
			postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
		} catch (CoreException e) {
			handleException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceSyncInfoChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceSyncInfoChanged(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#externalSyncInfoChange(org.eclipse.core.resources.IResource[])
	 */
	public void externalSyncInfoChange(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceModificationStateChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceModified(IResource[] changedResources) {
		resourceStateChanged(changedResources);
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#resourceStateChanged(org.eclipse.core.resources.IResource[])
	 */
	public void resourceStateChanged(IResource[] changedResources) {
		// add depth first so that update thread processes parents first.
		//System.out.println(">> State Change Event");
		Set resourcesToUpdate = new HashSet();

		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		boolean showingDeepDirtyIndicators = store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY);

		for (int i = 0; i < changedResources.length; i++) {
			IResource resource = changedResources[i];

			if(showingDeepDirtyIndicators) {
				addWithParents(resource, resourcesToUpdate);
			} else {
				resourcesToUpdate.add(resource);
			}
		}

		postLabelEvent(new LabelProviderChangedEvent(this, resourcesToUpdate.toArray()));
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#projectConfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectConfigured(IProject project) {
		refresh(project);
	}
	/**
	 * @see org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener#projectDeconfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectDeconfigured(IProject project) {
		refresh(project);
	}

	/**
	 * Post the label event to the UI thread
	 *
	 * @param events  the events to post
	 */
	private void postLabelEvent(final LabelProviderChangedEvent event) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				fireLabelProviderChanged(event);
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		super.dispose();
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().removePropertyChangeListener(this);
		CVSProviderPlugin.broadcastDecoratorEnablementChanged(false /* disabled */);
		TeamUI.removePropertyChangeListener(this);
		CVSUIPlugin.removePropertyChangeListener(this);
	}
	
	/**
	 * Handle exceptions that occur in the decorator. 
	 */
	private static void handleException(Exception e) {
		exceptions.handleException(e);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (isEventOfInterest(event)) {
		    refresh();
		}	
	}

    private boolean isEventOfInterest(PropertyChangeEvent event) {
        String prop = event.getProperty();
        return prop.equals(TeamUI.GLOBAL_IGNORES_CHANGED) 
        	|| prop.equals(TeamUI.GLOBAL_FILE_TYPES_CHANGED) 
        	|| prop.equals(CVSUIPlugin.P_DECORATORS_CHANGED)
			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR)
			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR)
			|| prop.equals(CVSDecoratorConfiguration.OUTGOING_CHANGE_FONT);
    }
}
