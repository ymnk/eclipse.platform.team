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
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;

/**
 * This interface defines the set of basic team operations that a TeamSubscriber
 * may wish to provide. A TeamSubscriber is not required to implement this
 * interface or may choose to only implement part of the interface. The portions
 * implemented by a subscriber are indicated by the <code>getSupportedOperations()</code>
 * method which returns a bit field which can be used to determine if a desored operation
 * is supported.
 * 
 * <p>
 * The operations in this interface do not provide access to the synchronization state
 * of resources. The TeamSubscriber from which the instance of this interface was obtained
 * can be used to query this state. The synchronization state of a resource will often effect
 * it's eligibility for use in the methods of this interface (as descibed in the documentation
 * for each of the methods of this interface).
 * </p>
 * <p>
 * The operations in this interface do not provide any local workspace management related
 * methods (e.g. checkout, uncheckout, delete, move). Implementors of this interface can
 * control these types of operations through the <code>IMoveDeleteHook</code>
 * and <code>IFileModificationValidator</code> interfaces that are associated with a 
 * <code>RepositoryProvider</code>.
 * <p>
 * In general, operations in this interface will return a status that either indicates
 * outright success of partial success. For outright success, the status will has a severity
 * of <code>IStatus.OK</code>. For partial success, the status will contain a child status for
 * each resource tha failed or was skipped for some reason). The documentation for each method
 * describes the contents of the status in more detail. Failure of the entire operation results
 * in a TeamException being thrown. The operation may fail due to the state of certain resources,
 * in which case the status of the excepion will indicate the resources and the reasons for the
 * failure. Or the operation may fail due to other circumstances (e.g. authentication failure)
 * which is also communicated in the stats of the exception.
 * 
 * @since 3.0
 */
public interface ITeamOperations {
	
	public static final int GET = 1;
	public static final int PUT = 2;
	public static final int CONTROL = 4;
	public static final int LABEL = 8;
	
	/**
	 * Depth constant indicating this resource, but not any of its members.
	 * Equivalent to the corresponding depth constant on <code>IResource</code>.
	 */
	public static final int DEPTH_ZERO = IResource.DEPTH_ZERO;
	
	/**
	 * Depth constant indicating this resource and its direct members.
	 * Equivalent to the corresponding depth constant on <code>IResource</code>.
	 */
	public static final int DEPTH_ONE = IResource.DEPTH_ONE;
	
	/**
	 * Depth constant indicating this resource and its direct and
	 * indirect members at any depth.
	 * Equivalent to the corresponding depth constant on <code>IResource</code>.
	 */
	public static final int DEPTH_INFINITE = IResource.DEPTH_INFINITE;
	
	/**
	 * Return a bit field describing the operations supported by this instance.
	 * To known if a particular operation is support, use the following expression
	 * (or something equivalent):
	 * <ul>
	 * <li>supported = operations.getSupportedOperations() & ITeamOperations.GET != 0</li>
	 * </ul>
	 * @return the supported operations
	 */
	public int getSupportedOperations();
	
	/**
	 * Updates the local resource to have the same content as the corresponding remote
	 * resource. Where the local resource does not exist, this method will create it. If
	 * the remote no longer exists, the local will be removed.
	 * <p>
	 * The depth parameter indicates how the resources are traversed. If the depth is DEPTH_ZERO
	 * then only those resources in the array, but not their children, will be included.
	 * If the depth is DEPTH_ONE, then only the resources and their direct children will
	 * be included. A depth of DEPTH_INFINITE indicates that the resources and all their 
	 * descendants are included.
	 * </p>
	 * <p>If the overwriteLocalChanges flag is false, then any changes made to local resources should 
	 * not be overwritten. This does not necessarily mean that files that have been modified 
	 * will not be changed as some implementators of this interface may support auto-merging 
	 * of non-conflicting changes within a file. If the overwriteLocalChanges flag is true then all local
	 * changes will be discarded and replaced with the contents of the corresponding remote
	 * resources. The client may query the
	 * sync state through the <code>TeamSubscriber</code> using <code>getSyncInfo(Resource)</code> to
	 * determine what the sync state of a resource is. Those resources that have a sync kind of
	 * <code>SyncInfo.OUTGOING</code> or <code>SyncInfo.CONFLICTING</code> have local changes.
	 * <p>
	 * Interrupting the method (via the progress monitor) may lead to partial, 
	 * but consistent, results.</p>
	 * <p>
	 * The status returned by this method may contain a single status or multiple status.
	 * If a single status is returned, it is either an OK status or it will contain information 
	 * about a single resource.
	 * If a multi-status is returned, each child status will contain information about a single
	 * resource.
	 * An OK status is returned when the operation succeeded outright. Additional status
	 * information includes:
	 * <ul>
	 * <li>AUTO_MERGED: If a locally modified file was auto-merged. 
	 * This status will have a severify of IStatus.INFO.</li>
	 * <li>IS_LOCALLY_MODIFIED: If overwriteLocalChanges was false and a file was locally modified.  
	 * This status will have a severify of IStatus.INFO.</li>
	 * <li>UNCONTROLLED_LOCAL_EXISTS: If overwriteLocalChanges was false a file exists 
	 * locally that is not controlled by the subscriber.  
	 * This status will have a severify of IStatus.INFO.</li>
	 * </ul>
	 * All other circumstances result in a TeamException being thrown.
	 * </p>
	 * 
	 * @param resources an array of local resources to update from the corresponding remote
	 * resources.
	 * @param depth the depth to traverse the given resources.  
	 *     One of <code>DEPTH_ZERO</code>, <code>DEPTH_ONE</code>, or <code>DEPTH_INFINITE</code>.
	 * @param overwriteLocalChanges whether local changes are to be maintained or discarded
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @return a status containing the results of the operations 
	 * @throws TeamException if there is a problem getting one or more of the resources. The
	 * exception may contain a single status indicating a general failure (e.g authentication 
	 * problem, etc.) or a multi-status indicating failures for particular resources.
	 */
	public IStatus get(IResource[] resources, int depth, boolean overwriteLocalChanges, IProgressMonitor progress) throws TeamException;
	
	/**
	 * Transfers the content of the local resources to the corresponding remote resources. 
	 * If a remote resource does not exist this method creates a new remote resource with the same content
	 * as the given local resource.  The local resource is said to <i>correspond</i> to the new remote resource.
	 * Also, for local resource that have been deleted, the corresponding remote resource is deleted
	 * as a result of this operation.</p>
	 * <p>
	 * The depth parameter indicates how the resources are traversed. If the depth is DEPTH_ZERO
	 * then only those resources in the array, but not their children, will be included.
	 * If the depth is DEPTH_ONE, then only the resources and their direct children will
	 * be included. A depth of DEPTH_INFINITE indicates that the resources and all their 
	 * descendants are included.
	 * </p>
	 * <p>
	 * Where providers deal with stores that check-out or lock resources this method is an opportunity
	 * to transfer the content and make the corresponding remote check-in or unlock.  It is envisaged that
	 * where the server maintains resource versions, checkin creates a new version of the remote resource.</p>
	 * <p>
	 * Note that some providers may <em>require</em> that a resource is checked-out before it can be
	 * checked-in.  However, this is done through the use of <code>IWorkspace#validateEdit()</code>.</p>
	 * <p>
	 * If the overwriteRemoteChanges flag is false, then the existance of remote conflicts
	 * will cause the operation to fail. The status in the resulting TeamException will 
	 * include child status for each resource that is in conflict. Some implementators may 
	 * rely on the state of the TeamSubscriber to decide if the operation should fail. For example,
	 * non-versioning subscribers may overwrite conflicting changes if the subscriber's state
	 * is not up-to-date. Other implementations may fork the resource if there are conflicts.
	 * It is the responsibility of the client of this API to perform a refresh
	 * on the subscriber to ensure that the subscriber is up-to-date. The client may also query the
	 * sync state through the <code>TeamSubscriber</code> using <code>getSyncInfo(Resource)</code> to
	 * determine what the sync state of a resource is.
	 * <p>
	 * If the overwriteRemoteChanges flag is true, then any conflicting or incoming changes should 
	 * be replaced with the local state of the resources involved. Some implementors may not support
	 * this type of override, in which case, an TeamException with a code of UNSUPPORTED_OPERATION 
	 * is thrown.
	 * <p>
	 * The status returned by this method may contain a single status or multiple status.
	 * If a single status is returned, it is either an OK status or it will contain information 
	 * about a single resource.
	 * If a multi-status is returned, each child status will contain information about a single
	 * resource.
	 * An OK status is returned when the operation succeeded outright. Additional status
	 * information includes:
	 * <ul>
	 * <li>REMOTE_IS_STALE: If overwriteRemoteChanges was true a remote file was stale and was replaced.  
	 * This status will have a severify of IStatus.INFO.</li>
	 * </ul>
	 * All other circumstances result in a TeamException being thrown.
	 * </p>
	 * @param resources an array of local resources to be uploaded to the server.
	 * @param the depth to traverse the given resources.
	 *     One of <code>DEPTH_ZERO</code>, <code>DEPTH_ONE</code>, or <code>DEPTH_INFINITE</code>.
	 * @param overwriteRemoteChanges whether incoming remote changes should be replaced with the local resource contents.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @return a status containing the results of the operations 
	 * @throws TeamException if there is a problem putting one or more of the resources.
	 * The exception may contain a single status indicating a general failure (e.g authentication 
	 * problem, etc.) or a multi-status indicating failures for particular resources.
	 */
	public IStatus put(IResource[] resources, int depth, boolean overwriteRemoteChanges, IProgressMonitor progress) throws TeamException;
	
	/**
	 * Place the local resources under the control of the subscriber. Subsequent calls to 
	 * <code>get</code> and <code>put</code> will affect this resource. This method only needs
	 * to be used on resources that are outgoing or conflicting additions and whose
	 * sync state contains the flag <code>SyncInfo.UNCONTROLLED</code>.
	 * <p>
	 * The depth parameter indicates how the resources are traversed. If the depth is DEPTH_ZERO
	 * then only those resources in the array, but not their children, will be included.
	 * If the depth is DEPTH_ONE, then only the resources and their direct children will
	 * be included. A depth of DEPTH_INFINITE indicates that the resources and all their 
	 * descendants are included.
	 * </p>
	 * <p>
	 * Subscribers may have a means of determining that some resource are ignored. If the depth
	 * is <code>DEPTH_ONE</code> or <code>DEPTH_INFINITE</code> then the subscriber may decide not
	 * to control certain resources. The method
	 * <code>TeamSubscriber#isSupervised(IResource)</code> will return false for such resources.
	 * Any resources that are skipped will be communicated to
	 * the client in the resulting status. 
	 * To place these resource under subscriber control, the client must invoke the <code>control</code>
	 * method with a depth of <code>DEPTH_ZERO</code>. The subscriber may fail such a request,
	 * if for some reason it cannot control the resource (e.g. is a subscriber meta-file or is a 
	 * linked resource).
	 * <p>
	 * The status returned by this method may contain a single status or multiple status.
	 * If a single status is returned, it is either an OK status or it will contain information 
	 * about a single resource.
	 * If a multi-status is returned, each child status will contain information about a single
	 * resource.
	 * An OK status is returned when the operation succeeded outright. Additional status
	 * information includes:
	 * <ul>
	 * <li>IS_IGNORED: If a resoure is ignored and has not been controlled.  
	 * This status will have a severify of IStatus.INFO.</li>
	 * </ul>
	 * All other circumstances result in a TeamException being thrown.
	 * </p>
	 * @param resources an array of local resources to be controlled by the subscriber.
	 * @param depth the depth to traverse the given resources.
	 *     One of <code>DEPTH_ZERO</code>, <code>DEPTH_ONE</code>, or <code>DEPTH_INFINITE</code>.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @return a status containing the results of the operations 
	 * @throws TeamException if there is a problem controlling one or more of the resources.
	 * The exception may contain a single status indicating a general failure (e.g authentication 
	 * problem, etc.) or a multi-status indicating failures for particular resources.
	 */
	public IStatus control(IResource[] resources, int depth, IProgressMonitor progress) throws TeamException;
	
	/**
	 * Place a label on the remote resources that correspond to the given local resources. Local
	 * changes are not included as part of the labelling (i.e. the corresponding remote is labelled
	 * ignoring any changes made locally). Any local changes must be <code>put</code> on the server
	 * before they can be labelled.
	 * <p>
	 * The label is a String that can later be used to retrieve the corresponding remote
	 * resources. The <code>isValidLabel()</code> should be called first to ensure that the 
	 * label being provided is valid.
	 * </p>
	 * <p>
	 * The depth parameter indicates how the resources are traversed. If the depth is DEPTH_ZERO
	 * then only those resources in the array, but not their children, will be included.
	 * If the depth is DEPTH_ONE, then only the resources and their direct children will
	 * be included. A depth of DEPTH_INFINITE indicates that the resources and all their 
	 * descendants are included.
	 * </p>
	 * <p>
	 * The forceHint parameter allows clients to indicate that the label is to be moved if
	 * it already exists on another remote resource. Subscribers may or may not supprt this.
	 * </p>
	 * @param resources an array of local resources whose coresponding remote resources are to be labeled.
	 * @param label the label to be applied
	 * @param depth the depth to traverse the given resources.
	 *     One of <code>DEPTH_ZERO</code>, <code>DEPTH_ONE</code>, or <code>DEPTH_INFINITE</code>.
	 * @param forceHint force the label onto the corresponding remote resource even if it already 
	 * exists on another remote resource.
	 * @param progress a progress monitor to indicate the duration of the operation, or
	 * <code>null</code> if progress reporting is not required.
	 * @return a status containing the results of the operations 
	 * @throws TeamException if there is a problem labelling one or more of the resources.
	 * The exception may contain a single status indicating a general failure (e.g authentication 
	 * problem, etc.) or a multi-status indicating failures for particular resources.
	 */
	public IStatus label(IResource[] resources, String label, int depth, boolean forceHint, IProgressMonitor progress) throws TeamException;
	
	/**
	 * Validat that the given String label can be used in the <code>label</code> method.
	 * <p>
	 * The returned status will either have a severity of IStatus.OK or IStatus.ERROR, in which
	 * case, the message will be a user readable string that describes why the label is
	 * not valid.
	 * @param label the label be validated
	 * @return whether the label is valid for use in the <code>label</code> method.
	 */
	public IStatus isValidLabel(String label);
}
