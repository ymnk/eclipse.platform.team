package org.eclipse.team.internal.ccvs.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.EventListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * A resource state change listener is notified of changes to resources
 * regarding their team state. 
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see ITeamManager#addResourceStateChangeListener(IResourceStateChangeListener)
 */
public interface IResourceStateChangeListener extends EventListener{
	
	public static final int CONTENTS_MODIFIED = 0;
	public static final int NO_LONGER_MODIFIED = 1;
	public static final int SYNC_INFO_EXTERNALLY_MODIFIED = 2;
	
	/**
	 * Notifies this listener that some resource sync info state changes have
	 * already happened. For example, a resource's base revision may have
	 * changed. The resource tree is open for modification when this method is
	 * invoked, so markers can be created, etc.
	 * <p>
	 * Note: This method is called by the CVS core; it is not intended to be
	 * called directly by clients.
	 * </p>
	 *
	 * @param changedResources that have sync info state changes
	 * 
	 * [Note: The changed state event is purposely vague. For now it is only
	 * a hint to listeners that they should query the provider to determine the
	 * resources new sync info.]
	 */
	public void resourceSyncInfoChanged(IResource[] changedResources);
	
	/**
	 * Notifies this listener that the resource's modification state has
	 * changed. For example, the resource may have become dirty due to an edit
	 * or may have become clean due to a commit. The method is only invoked for
	 * resources that existed before and exist after the state change.
	 * The resource tree may not be open to modification when this method is
	 * invoked.
	 * <p>
	 * A changeType of NO_LONGER_MODIFIED indicates that the files are no longer
	 * modified from a CVS standpoint (i.e. it was either committed or
	 * reverted). A changeType of CONTENTS_MODIFIED indicates that the contents
	 * of the files have been modified but does not gaurentee that the files are
	 * modified from a CVS standpoint. The receiver must verify the proper
	 * state. A changeType of SYNC_INFO_EXTERNALLY_MODIFIED indicates that the
	 * sync info was modified by an external tool so the files may or may not
	 * still be modified.
	 * </p>
	 * <p>
	 * Resources reported with a changeType of NO_LONGER_MODIFIED or
	 * SYNC_INFO_EXTERNALLY_MODIFIED will also be reported via a
	 * <code>resourceSyncInfoChanged</code> invocation.
	 * </p>
	 * <p>
	 * Note: This method is called by CVS team core; it is not intended to be
	 * called directly by clients.
	 * </p>
	 *
	 * @param changedResources that have changed state
	 * @param changeType the type of state change.
	 */
	public void resourceModificationStateChanged(IResource[] changedResources, int changeType);
	
	/**
	 * Notifies this listener that the project has just been configured
	 * to be a CVS project.
	 * <p>
	 * Note: This method is called by the CVS core; it is not intended to be
	 * called directly by clients.
	 * </p>
	 *
	 * @param project The project that has just been configured
	 */
	public void projectConfigured(IProject project);
	
	/**
	 * Notifies this listener that the project has just been deconfigured
	 * and no longer has the CVS nature.
	 * <p>
	 * Note: This method is called by the CVS core; it is not intended to be
	 * called directly by clients.
	 * </p>
	 *
	 * @param project The project that has just been configured
	 */
	public void projectDeconfigured(IProject project);
	
}

