package org.eclipse.team.internal.ccvs.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.ui.IDecoratorManager;

/**
 * Classes registered with the workbench decoration extension point. The <code>CVSDecorationRunnable</code> class
 * actually calculates the decoration, while this class is responsible for listening to the following sources
 * for indications that the decorators need updating:
 * <ul>
 * 	<li>workbench label requests (decorateText/decorateImage)
 * 	<li>workspace resource change events (resourceChanged)
 * 	<li>cvs state changes (resourceStateChanged)
 * </ul>
 * <p>
 * [Note: There are several optimization that can be implemented in this class: (1) cache something
 * so that computing the dirty state of containers does not imply traversal of all children, (2) improve
 * the queue used between the decorator and the decorator runnable such that priority can be
 * given to visible elements when decoration requests are made.]
 */
public class CVSDecorator implements IResourceChangeListener, IResourceStateChangeListener {
	
	// Resources that need an icon and text computed for display to the user
	private List decoratorNeedsUpdating = new ArrayList();

	// When decorations are computed they are added to this cache via decorated() method
	private Map cache = Collections.synchronizedMap(new HashMap());

	// Updater thread, computes decoration labels and images
	private Thread decoratorUpdateThread;

	private boolean shutdown = false;

	private OverlayIconCache iconCache = new OverlayIconCache();
	
	// Keep track of deconfigured projects
	private Set deconfiguredProjects = new HashSet();
	
	/*
	 * Blanket refresh the displaying of our decorator.
	 */ 
	public static void refresh() {
/*		CVSDecorator activeDecorator = getActiveCVSDecorator();
		
		if(activeDecorator == null)
			return;	//nothing to do, our decorator isn't active
		activeDecorator.clearCache();  //clear the cache of previous decorations so we can compute them anew
		
		//update all displaying of our decorator;
		activeDecorator.fireLabelProviderChanged(new LabelProviderChangedEvent(activeDecorator));
*/	}
	
	/*
	 * Answers null if a provider does not exist or the provider is not a CVS provider. These resources
	 * will be ignored by the decorator.
	 */
	private static CVSTeamProvider getCVSProviderFor(IResource resource) {
		RepositoryProvider p = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
		if (p == null) {
			return null;
		}
		return (CVSTeamProvider) p;
	}	


	public CVSDecorator() {
		// thread that calculates the decoration for a resource
//		decoratorUpdateThread = new Thread(new CVSDecorationRunnable(this), "CVS"); //$NON-NLS-1$
//		decoratorUpdateThread.start();
//		CVSProviderPlugin.addResourceStateChangeListener(this);
//		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.PRE_AUTO_BUILD);
	}

	/*
	 * Handle resource changes and project description changes
	 * 
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		try {
			final List changedResources = new ArrayList();
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource = delta.getResource();
					
					if(resource.getType()==IResource.ROOT) {
						// continue with the delta
						return true;
					}
					
					if (resource.getType() == IResource.PROJECT) {
						// deconfigure if appropriate (see CVSDecorator#projectDeconfigured(IProject))
						// do this even if there is a provider (this is required since old projects may still have a cvs nature)
						if (deconfiguredProjects.contains(resource)) {
							deconfiguredProjects.remove(resource);
							refresh((IProject)resource);
						}
						// If there is no CVS provider, don't continue
						if (RepositoryProvider.getProvider((IProject)resource, CVSProviderPlugin.getTypeId()) == null) {
							return false;
						}
					}
					
					switch (delta.getKind()) {
						case IResourceDelta.REMOVED:
							// remove the cached decoration for any removed resource
							cache.remove(resource);
							break;
						case IResourceDelta.CHANGED:
							// for changed resources we have to update the decoration
							changedResources.add(resource);	
					}
					
					return true;
				}
			});
			resourceStateChanged((IResource[])changedResources.toArray(new IResource[changedResources.size()]));
			changedResources.clear();	
		} catch (CoreException e) {
			CVSProviderPlugin.log(e.getStatus());
		}
	}
	/*
	 * @see IResourceStateChangeListener#resourceStateChanged(IResource[])
	 */
	public void resourceStateChanged(IResource[] changedResources) {
		// add depth first so that update thread processes parents first.
		//System.out.println(">> State Change Event");
		List resources = new ArrayList();
		List noProviderResources = new ArrayList();
		for (int i = 0; i < changedResources.length; i++) {
			// ignore subtrees that aren't associated with a provider, this can happen on import
			// of a new project to CVS.
			IResource resource = changedResources[i];
			if (getCVSProviderFor(resource) == null) {
				// post a changed event but forget any cached information about this resource
				noProviderResources.add(resource);
			}
			resources.addAll(computeParents(resource));
		}
		
//		addResourcesToBeDecorated((IResource[]) resources.toArray(new IResource[resources.size()]));
		
		// post label events for resources that cannot or should not be decorated by CVS
		if(!noProviderResources.isEmpty()) {
			List resourcesToUpdate = new ArrayList();
			for (Iterator it = resources.iterator(); it.hasNext();) {
				IResource element = (IResource) it.next();
				resourcesToUpdate.add(element);
			}
//			postLabelEvent(new LabelProviderChangedEvent(this, resourcesToUpdate.toArray()));
		}
	}

	private void clearCache() {
		cache.clear();
	}
	
	public void refresh(IProject project) {
		final List resources = new ArrayList();
		try {
			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) {
					resources.add(resource);
					return true;
				}
			});
			resourceStateChanged((IResource[]) resources.toArray(new IResource[resources.size()]));	
		} catch (CoreException e) {
		}
	}

	private List computeParents(IResource resource) {
		IResource current = resource;
		List resources = new ArrayList();
		if(CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY)) {
			while (current.getType() != IResource.ROOT) {
				resources.add(current);
				current = current.getParent();
			}
		} else {
			resources.add(current);			
		}
		return resources;
	}
	
	private void shutdown() {
		shutdown = true;
		// Wake the thread up if it is asleep.
		synchronized (this) {
			notifyAll();
		}
		try {
			// Wait for the decorator thread to finish before returning.
			decoratorUpdateThread.join();
		} catch (InterruptedException e) {
		}
	}
		
	/**
	 * @see IResourceStateChangeListener#projectConfigured(IProject)
	 */
	public void projectConfigured(IProject project) {
		refresh(project);
	}

	/**
	 * @see IResourceStateChangeListener#projectDeconfigured(IProject)
	 */
	public void projectDeconfigured(IProject project) {
		// Unfortunately, the nature is still associated with the project at this point.
		// Therefore, we will remember that the project has been deconfigured and we will 
		// refresh the decorators in the resource delta listener
		deconfiguredProjects.add(project);
	}
	
	public static boolean isDirty(final ICVSFile cvsFile) {
		try {
			final boolean[] isDirty = new boolean[] {false};
			cvsFile.getParent().run(new ICVSRunnable() {
				public void run(IProgressMonitor monitor) throws CVSException {
					// file is dirty or file has been merged by an update
					if(!cvsFile.isIgnored()) {
						isDirty[0] = cvsFile.isModified();
					}
				}
			}, ICVSFolder.READ_ONLY, null);
			return isDirty[0];
		} catch (CVSException e) {
			//if we get an error report it to the log but assume dirty
			CVSUIPlugin.log(e.getStatus());
			return true;
		}
	}

	public static boolean isDirty(IFile file) {
		return isDirty(CVSWorkspaceRoot.getCVSFileFor(file));
	}

	public static boolean isDirty(IResource resource) {
		
		// No need to decorate non-existant resources
		if (!resource.exists()) return false;
		
		if(resource.getType() == IResource.FILE) {
			return isDirty((IFile) resource);
		}
		
		final CVSException DECORATOR_EXCEPTION = new CVSException(new Status(IStatus.OK, "id", 1, "", null)); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
			cvsResource.accept(new ICVSResourceVisitor() {
				public void visitFile(ICVSFile file) throws CVSException {
					if(isDirty(file)) {
						throw DECORATOR_EXCEPTION;
					}
				}
				public void visitFolder(ICVSFolder folder) throws CVSException {
					if(!folder.exists()) {
						if (folder.isCVSFolder()) {
							// The folder may contain outgoing file deletions
							folder.acceptChildren(this);
						}
						return;
					}
					if (!folder.isCVSFolder() && !folder.isIgnored()) {
						// new resource, show as dirty
						throw DECORATOR_EXCEPTION;
					}
					folder.acceptChildren(this);
				}
			});
		} catch (CVSException e) {
			//if our exception was caught, we know there's a dirty child
			return e == DECORATOR_EXCEPTION;
		}
		return false;
	}
	/**
	 * This method is used to indicate whether a particular resource is a member of
	 * a project that is in the process of being deconfigured. Such resources should not
	 * be decorated.
	 */
	/* package */ boolean isMemberDeconfiguredProject(IResource resource) {
		if (deconfiguredProjects.isEmpty()) return false;
		return deconfiguredProjects.contains(resource.getProject());
	}

}