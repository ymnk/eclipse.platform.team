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
package org.eclipse.team.internal.ccvs.ui.merge;

import java.util.*;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Workbench model element that contains a list of tags
 * of the same type (BRANCH, VERSION or DATE).
 */
public class TagRootElement implements IWorkbenchAdapter, IAdaptable {
	private TagSource tagSource;
	private List cachedTags;
	private int typeOfTagRoot;
	
	public TagRootElement(TagSource tagSource, int typeOfTagRoot) {
		this.typeOfTagRoot = typeOfTagRoot;
		this.tagSource = tagSource;
	}
	
	public Object[] getChildren(Object o) {
		CVSTag[] childTags = new CVSTag[0];
		if(cachedTags==null) {
		    childTags = tagSource.getTags(typeOfTagRoot);
		} else {
			childTags = getTags();
		}
		TagElement[] result = new TagElement[childTags.length];
		for (int i = 0; i < childTags.length; i++) {
			result[i] = new TagElement(this, childTags[i]);
		}
		return result;
	}
	public void removeAll() {
		if(cachedTags!=null) {
			cachedTags.clear();
		}
	}
	public void add(CVSTag tag){
		if(cachedTags==null) {
			cachedTags = new ArrayList();
		}
		cachedTags.add(tag);
	}
	public void add(CVSTag[] tags) {
		if(cachedTags==null) {
			cachedTags = new ArrayList(tags.length);
		}
		cachedTags.addAll(Arrays.asList(tags));
	}
	public void remove(CVSTag tag) {
		if(cachedTags!=null) {
			cachedTags.remove(tag);
		}
	}
	public CVSTag[] getTags() {
		if(cachedTags!=null) {
			return (CVSTag[]) cachedTags.toArray(new CVSTag[cachedTags.size()]);
		} else {
			return new CVSTag[0];
		}
	}
	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) return this;
		return null;
	}
	public ImageDescriptor getImageDescriptor(Object object) {
		if(typeOfTagRoot==CVSTag.BRANCH) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_BRANCHES_CATEGORY);
		} else if(typeOfTagRoot==CVSTag.DATE){
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_DATES_CATEGORY);
		}else {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_VERSIONS_CATEGORY);
		}
	}
	public String getLabel(Object o) {
		if(typeOfTagRoot==CVSTag.BRANCH) {
			return Policy.bind("MergeWizardEndPage.branches"); //$NON-NLS-1$
		} else if(typeOfTagRoot==CVSTag.DATE){
			return Policy.bind("TagRootElement.0"); //$NON-NLS-1$
		}else {
			return Policy.bind("VersionsElement.versions"); //$NON-NLS-1$
		}	
	}
	public Object getParent(Object o) {
		return null;
	}
	/**
	 * Gets the typeOfTagRoot.
	 * @return Returns a int
	 */
	public int getTypeOfTagRoot() {
		return typeOfTagRoot;
	}

}
