/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.internal.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The steps of an optimistic merge operation are:
 * <ol>
 * <li>Obtain the selection to be operated on.
 * <li>Determine the projection of the selection onto resources
 * using resource mappings and traversals.
 * 		<ul>
 * 		<li>this will require traversals using both the ancestor and remote
 *      for three-way merges.
 *      <li>for model providers with registered merger, mapping set need 
 *      not be expanded (this is tricky if one of the model providers doesn't
 *      have a merge but all others do).
 *      <li>if the model does not have a custom merger, ensure that additional
 *      mappings are included (i.e. for many model elements to one resource case)
 * 		</ul>
 * <li>Create a MergeContext for the merge (this will be per model provider)
 *      <ul>
 * 		<li>Determine the synchronization state of all resources
 *      covered by the input.
 *      <li>Pre-fetch the required contents.
 * 		</ul>
 * <li>Obtain and invoke the merger for the provider
 *      <ul>
 * 		<li>This will auto-merge as much as possible
 *      <li>If everything was merged, cleanup and stop
 *      <li>Otherwise, a set of un-merged resource mappings is returned
 * 		</ul>
 * <li>Restrict merge context to remaining mappings
 *      <ul>
 * 		<li>This needs to re-determine the traversals
 *      <li>I'm not sure if this step is needed?
 * 		</ul>
 * <li>Delegate manual merge to the model provider
 *      <ul>
 * 		<li>This hands off the context to the manual merge
 *      <li>Once completed, the manual merge must clean up
 * 		</ul>
 * </ol>
 * 
 * <p>
 * Handle multiple model providers where one extends all others by using
 * the top-most model provider. The assumption is that the model provider
 * will delegate to lower level model providers when appropriate.
 * <p>
 * Special case to support sub-file merges.
 * <ul>
 * <li>Restrict when sub-file merging is supported
 * 		<ul>
 * 		<li>Only one provider involved (i.e. consulting participants results
 * 		in participants that are from the model provider or below).
 * 		<li>The provider has a custom auto and manual merger.
 * 		</ul>
 * <li>Prompt to warn when sub-file merging is not possible.
 * <li>Need to display the additional elements that will be affected.
 * This could be done in a diff tree or some other view. It needs to
 * consider incoming changes including additions.
 * </ul>
 * <p>
 * Special case to handle conflicting model providers.
 * <ul>
 * <li>Prompt user to indicate the conflict
 * <li>Allow user to exclude one of the models?
 * <li>Allow use to choose order of evaluation?
 * <li>Support tabbed sync view
 * </ul>
 * <p>
 * TODO: What about support for down grading a merge. That is, the user wants
 * to perform the merge at the file level even though a higher level model owns
 * the files. We could provide a preference that the user can set to perform
 * the merge at the level selected and not involve participants.
 */
public abstract class ResourceMappingMergeOperation extends ResourceMappingOperation {
	
	private Map providerToMappings;
	private Map providerToTraversalsMap;

	protected ResourceMappingMergeOperation(IWorkbenchPart part, ResourceMapping[] mappings) {
		super(part, mappings);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ResourceMappingOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			Map contexts = buildMergeContexts(monitor);
			for (Iterator iter = contexts.keySet().iterator(); iter.hasNext();) {
				ModelProvider provider = (ModelProvider) iter.next();	
				performMerge(provider, (MergeContext)contexts.get(provider), monitor);
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private Map buildMergeContexts(IProgressMonitor monitor) throws CoreException {
		Map providerToMappings = getProviderToMappingsMap();
		Map result = new HashMap();
		for (Iterator iter = providerToMappings.keySet().iterator(); iter.hasNext();) {
			ModelProvider provider = (ModelProvider) iter.next();
			List list = (List)providerToMappings.get(provider);
			MergeContext context = buildMergeContext(provider, (ResourceMapping[]) list.toArray(new ResourceMapping[list.size()]), monitor);
			result.put(provider, context);
		}
		return result;
	}

	/**
	 * Build and initialize a merge context for the given mappings.
	 * The mappings will all be from the given provider or one
	 * of the providers the provider extends.
	 * @param provider the provider that owns the mappings being merged
	 * @param mappings the mappings being merged
	 * @param monitor a progress monitor
	 * @return a merge context for merging the mappings
	 */
	protected abstract MergeContext buildMergeContext(ModelProvider provider, ResourceMapping[] mappings, IProgressMonitor monitor);

	private Map getProviderToMappingsMap() throws CoreException {
		if (providerToMappings == null) {
			Map result = new HashMap();
			ResourceMapping[] mappings = getMappings();
			for (int i = 0; i < mappings.length; i++) {
				ResourceMapping mapping = mappings[i];
				ModelProvider provider = mapping.getModelProvider();
				List l = (List)result.get(provider);
				if (l == null) {
					l = new ArrayList();
					result.put(provider, l);
				}
				l.add(mapping);
			}
			providerToMappings = result;
		}
		return providerToMappings;
	}

	protected void buildInput(IProgressMonitor monitor) throws InvocationTargetException {
		try {
			promptToIncludeAdditionalMappings();
			buildProviderToTraversalMap(monitor);
			// TODO Determine the projection of the selection onto resources
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
		
		super.buildInput(monitor);
	}
	
	private void promptToIncludeAdditionalMappings() throws CoreException {
		Map providerToMappings = getProviderToMappingsMap();
		// TODO Auto-generated method stub
		
	}

	private Map buildProviderToTraversalMap(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		providerToTraversalsMap = new HashMap();
		return null;
	}

	/**
	 * Merge all the mappings that come from the given provider. By default,
	 * an automatic merge is attempted. After this, a manual merge (i.e. with user
	 * intervention) is attempted on any mappings that could not be merged
	 * automatically.
	 * @param provider the model provider
	 * @param mappings the mappings to be merged
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	protected void performMerge(ModelProvider provider, MergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(null, 100);
			IStatus status = performAutoMerge(provider, mergeContext, Policy.subMonitorFor(monitor, 95));
			if (status.isOK()) {
				mergeContext.dispose();
			} else {
				if (status.getCode() == MergeStatus.CONFLICTS) {
					MergeStatus ms = (MergeStatus)status;
					performManualMerge(provider, ms.getConflictingMappings(), mergeContext, Policy.subMonitorFor(monitor, 5));
				} else {
					throw new TeamException(status);
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Attempt to merge automatically. The returned status will indicate which
	 * mappings could not be merged automatically.
	 * @param provider the provider for the mappings being merged
	 * @param mergeContext the context for the merge
	 * @param monitor a progress monitor
	 * @return a status indicating success or failure. A failure status
	 * will be a MergeStatus that includes the mappings that could not be merged. 
	 * @throws CoreException if errors occurred
	 */
	protected IStatus performAutoMerge(ModelProvider provider, MergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
		IResourceMappingMerger merger = getMerger(provider);
		IStatus status = merger.merge(mergeContext, monitor);
		return status;
	}

	protected void performManualMerge(ModelProvider provider, ResourceMapping[] conflictingMappings, MergeContext mergeContext, IProgressMonitor monitor) throws CoreException {
//		mergeContext.restrictTo(conflictingMappings, monitor);
		IResourceMappingManualMerger merger = getManualMerger(provider);
		merger.performManualMerge(getPart(), mergeContext, monitor);
	}

}
