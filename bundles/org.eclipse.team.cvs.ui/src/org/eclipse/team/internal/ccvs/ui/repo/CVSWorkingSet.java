/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;

public class CVSWorkingSet {
	private String name;
	private ICVSRemoteFolder[] folders;

	public CVSWorkingSet(String name) {
		this.name = name;
		this.folders = new ICVSRemoteFolder[0];
	}
	
	/**
	 * Returns the folders.
	 * @return ICVSRemoteFolder[]
	 */
	public ICVSRemoteFolder[] getFolders() {
		return folders;
	}

	/**
	 * Returns the locations.
	 * @return ICVSRepositoryLocation[]
	 */
	public ICVSRepositoryLocation[] getRepositoryLocations() {
		Set locations = new HashSet();
		for (int i = 0; i < folders.length; i++) {
			locations.add(folders[i].getRepository());
		}
		return (ICVSRepositoryLocation[]) locations.toArray(new ICVSRepositoryLocation[locations.size()]);
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the folders.
	 * @param folders The folders to set
	 */
	public void setFolders(ICVSRemoteFolder[] folders) {
		this.folders = folders;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
