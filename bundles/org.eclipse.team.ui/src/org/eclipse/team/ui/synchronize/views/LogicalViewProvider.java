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
package org.eclipse.team.ui.synchronize.views;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.subscribers.TeamSubscriber;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Implementations can be contributed via extension point and used by team participants.
 * 
 * TODO: Add project nature filter to extension point
 * 
 * @since 3.0
 */
public abstract class LogicalViewProvider {

	/**
	 * Return a content provider that can be used in a <code>TreeViewer</code>
	 * to show the hiearchical structure appropriate for this provider.
	 * @return a <code>SyncInfoSetTreeContentProvider</code>
	 */
	public abstract SyncInfoSetTreeContentProvider getContentProvider();
	
	/**
	 * Return a label provider that provides the text and image labels for
	 * the logical elements associated with this provider.
	 * @return
	 */
	public abstract SyncInfoLabelProvider getLabelProvider();
	
	/**
	 * Return the sorter to be used to sort elements from the logical view's
	 * content provider.
	 * @return a <code>SyncViewerSorter</code>
	 */
	public SyncViewerSorter getSorter() {
		return new SyncViewerSorter(ResourceSorter.NAME);
	}
	
	/**
	 * Return an array of <code>TeamOperationInput</code> objects that describe the resources
	 * that are contained by or make up the given logical elements. The size of the
	 * returned array of <code>TeamOperationInput</code> need not corrolate to the
	 * size of the array of logical elements. An array is returned to give the logical view
	 * provider flexibility in describing what resources make up a logical element. It 
	 * is up to the client of this interface to translate the <code>TeamOperationInput</code>
	 * array into an appropriate operation input.
	 * <p>
	 * The implementor of this method is encouraged to use the provided subscriber to determine
	 * which resources are included. The <code>TeamSubscriber#members(IResource)</code> method
	 * will return resources that exists locally and also those that do not exist locally but do
	 * have a remote counterpart. This method will also exclude resources that are not supervised 
	 * by the subscriber (e.g. ignored from version contgrol). The significance of this is that the 
	 * logical view provider can prepare a team operation input that includes resources that need to 
	 * be created locally or deleted remotely and excludes unnecessay resources. 
	 * <p>
	 * By default, this method returns a deep (<code>IResource.DEPTH_INFINITE</code>) operations
	 * on any resources that can be obtained from he given elements. Subclasses should override
	 * to provide the proper mapping from their logical elements to a set of 
	 * <code>TeamOperationInput</code>.
	 * @param elements the logical elements
	 * @param subscriber the team subscriber for which the input is being prepared
	 * @param monitor a progress monitor
	 * @return the input to a team operation
	 */
	public TeamOperationInput[] getTeamOperationInput(Object[] elements, TeamSubscriber subscriber, IProgressMonitor monitor) throws CoreException {
		Set resources = new HashSet();
		for (int i = 0; i < elements.length; i++) {
			Object object = elements[i];
			IResource resource = SyncInfoSetContentProvider.getResource(object);
			if (resource != null) {
				resources.add(resource);
			}
		}
		if (resources.isEmpty()) {
			return new TeamOperationInput[0];
		}
		return new TeamOperationInput[] { 
				new TeamOperationInput(
					(IResource[]) resources.toArray(new IResource[resources.size()]), 
					IResource.DEPTH_INFINITE) };
	}
}
