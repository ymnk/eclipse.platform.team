package org.eclipse.team.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

public interface ITeamManager {
	/**
	 * Returns the list of global ignore patterns.
	 * 
	 * @return ignore patterns
	 */
	public IIgnoreInfo[] getGlobalIgnore();	
	
	/**
	 * Sets the list of ignore patterns. These are persisted between workspace sessions.
	 * 
	 * @param patterns an array of file name patterns (e.g. *.exe)
	 * @param enabled an array of pattern enablements
	 */
	public void setGlobalIgnore(String[] patterns, boolean[] enabled);
}