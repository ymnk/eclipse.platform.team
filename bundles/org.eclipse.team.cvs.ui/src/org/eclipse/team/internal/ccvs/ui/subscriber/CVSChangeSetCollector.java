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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.subscribers.SyncInfoSetChangeSetCollector;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSSyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * Collector that fetches the log for incoming CVS change sets
 */
public class CVSChangeSetCollector extends SyncInfoSetChangeSetCollector {

	// Log operation that is used to fetch revision histories from the server. It also
	// provides caching so we keep it around.
    private LogEntryCache logs;
	
	// Job that builds the layout in the background.
	private boolean shutdown = false;
	private FetchLogEntriesJob fetchLogEntriesJob;
	
	/* *****************************************************************************
	 * Background job to fetch commit comments and update view
	 */
	private class FetchLogEntriesJob extends Job {
		private Set syncSets = new HashSet();
		public FetchLogEntriesJob() {
			super(Policy.bind("ChangeLogModelProvider.4"));  //$NON-NLS-1$
			setUser(false);
		}
		public boolean belongsTo(Object family) {
			return family == ISynchronizeManager.FAMILY_SYNCHRONIZE_OPERATION;
		}
		public IStatus run(IProgressMonitor monitor) {
			
				if (syncSets != null && !shutdown) {
					// Determine the sync sets for which to fetch comment nodes
					SyncInfoSet[] updates;
					synchronized (syncSets) {
						updates = (SyncInfoSet[]) syncSets.toArray(new SyncInfoSet[syncSets.size()]);
						syncSets.clear();
					}
					for (int i = 0; i < updates.length; i++) {
						calculateRoots(updates[i], monitor);
					}
				}
				return Status.OK_STATUS;
		
		}
		public void add(SyncInfoSet set) {
			synchronized(syncSets) {
				syncSets.add(set);
			}
			schedule();
		}
		public boolean shouldRun() {
			return !syncSets.isEmpty();
		}
	};
	
    public CVSChangeSetCollector(SyncInfoSet seedSet) {
        super(seedSet);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.SyncInfoSetChangeSetCollector#add(org.eclipse.team.core.synchronize.SyncInfo[])
     */
    protected void add(SyncInfo[] infos) {
        startUpdateJob(new SyncInfoSet(infos));
    }

	private synchronized void startUpdateJob(SyncInfoSet set) {
		if(fetchLogEntriesJob == null) {
			fetchLogEntriesJob = new FetchLogEntriesJob();
		}
		fetchLogEntriesJob.add(set);
	}
	
	private void calculateRoots(SyncInfoSet set, IProgressMonitor monitor) {
		try {
			monitor.beginTask(null, 100);
			// Decide which nodes we have to fetch log histories
			SyncInfo[] infos = set.getSyncInfos();
			ArrayList remoteChanges = new ArrayList();
			ArrayList localChanges = new ArrayList();
			for (int i = 0; i < infos.length; i++) {
				SyncInfo info = infos[i];
				if(isRemoteChange(info)) {
					remoteChanges.add(info);
				}
			}	
			handleRemoteChanges((SyncInfo[]) remoteChanges.toArray(new SyncInfo[remoteChanges.size()]), monitor);
		} catch (CVSException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
		} finally {
			monitor.done();
		}
	}
	
	/*
	 * Return if this sync info should be considered as part of a remote change
	 * meaning that it can be placed inside an incoming commit set (i.e. the
	 * set is determined using the comments from the log entry of the file). 
	 */
	private boolean isRemoteChange(SyncInfo info) {
		int kind = info.getKind();
		if(info.getLocal().getType() != IResource.FILE) return false;
		if(info.getComparator().isThreeWay()) {
			return (kind & SyncInfo.DIRECTION_MASK) != SyncInfo.OUTGOING;
		}
		// For two-way, the change is only remote if it has a remote or has a base locally
		if (info.getRemote() != null) return true;
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor((IFile)info.getLocal());
		try {
            return file.getSyncBytes() != null;
        } catch (CVSException e) {
            // Log the error and exclude the file from consideration
            CVSUIPlugin.log(e);
            return false;
        }
	}
	
	/**
	 * Fetch the log histories for the remote changes and use this information
	 * to add each resource to an appropriate commit set.
     */
    private void handleRemoteChanges(final SyncInfo[] infos, final IProgressMonitor monitor) throws CVSException, InterruptedException {
        final LogEntryCache logs = getSyncInfoComment(infos, Policy.subMonitorFor(monitor, 80));
        runViewUpdate(new Runnable() {
            public void run() {
                addLogEntries(infos, logs, Policy.subMonitorFor(monitor, 10));
            }
        });
    }
    
	/**
	 * How do we tell which revision has the interesting log message? Use the later
	 * revision, since it probably has the most up-to-date comment.
	 */
	private LogEntryCache getSyncInfoComment(SyncInfo[] infos, IProgressMonitor monitor) throws CVSException, InterruptedException {
		if (logs == null) {
		    logs = new LogEntryCache();
		}
	    if (isTagComparison()) {
	        CVSTag tag = getCompareSubscriber().getTag();
            if (tag != null) {
	            // This is a comparison against a single tag
                // TODO: The local tags could be different per root or even mixed!!!
                fetchLogs(infos, logs, getLocalResourcesTag(infos), tag, monitor);
	        } else {
	            // Perform a fetch for each root in the subscriber
	            Map rootToInfosMap = getRootToInfosMap(infos);
	            monitor.beginTask(null, 100 * rootToInfosMap.size());
	            for (Iterator iter = rootToInfosMap.keySet().iterator(); iter.hasNext();) {
                    IResource root = (IResource) iter.next();
                    List infoList = ((List)rootToInfosMap.get(root));
                    SyncInfo[] infoArray = (SyncInfo[])infoList.toArray(new SyncInfo[infoList.size()]);
                    fetchLogs(infoArray, logs, getLocalResourcesTag(infoArray), getCompareSubscriber().getTag(root), Policy.subMonitorFor(monitor, 100));
                }
	            monitor.done();
	        }
	        
	    } else {
	        // Run the log command once with no tags
			fetchLogs(infos, logs, null, null, monitor);
	    }
		return logs;
	}
	
	private void fetchLogs(SyncInfo[] infos, LogEntryCache cache, CVSTag localTag, CVSTag remoteTag, IProgressMonitor monitor) throws CVSException, InterruptedException {
	    ICVSRemoteResource[] remoteResources = getRemotes(infos);
	    if (remoteResources.length > 0) {
			RemoteLogOperation logOperation = new RemoteLogOperation(getConfiguration().getSite().getPart(), remoteResources, localTag, remoteTag, cache);
			logOperation.execute(monitor);
	    }
	    
	}
	private ICVSRemoteResource[] getRemotes(SyncInfo[] infos) {
		List remotes = new ArrayList();
		for (int i = 0; i < infos.length; i++) {
			CVSSyncInfo info = (CVSSyncInfo)infos[i];
			if (info.getLocal().getType() != IResource.FILE) {
				continue;
			}	
			ICVSRemoteResource remote = getRemoteResource(info);
			if(remote != null) {
				remotes.add(remote);
			}
		}
		return (ICVSRemoteResource[]) remotes.toArray(new ICVSRemoteResource[remotes.size()]);
	}
	
    private boolean isTagComparison() {
        return getCompareSubscriber() != null;
    }
    
	/*
     * Return a map of IResource -> List of SyncInfo where the resource
     * is a root of the compare subscriber and the SyncInfo are children
     * of that root
     */
    private Map getRootToInfosMap(SyncInfo[] infos) {
        Map rootToInfosMap = new HashMap();
        IResource[] roots = getCompareSubscriber().roots();
        for (int i = 0; i < infos.length; i++) {
            SyncInfo info = infos[i];
            IPath localPath = info.getLocal().getFullPath();
            for (int j = 0; j < roots.length; j++) {
                IResource resource = roots[j];
                if (resource.getFullPath().isPrefixOf(localPath)) {
                    List infoList = (List)rootToInfosMap.get(resource);
                    if (infoList == null) {
                        infoList = new ArrayList();
                        rootToInfosMap.put(resource, infoList);
                    }
                    infoList.add(info);
                    break; // out of inner loop
                }
            }
            
        }
        return rootToInfosMap;
    }

    private CVSTag getLocalResourcesTag(SyncInfo[] infos) {
		try {
			for (int i = 0; i < infos.length; i++) {
				IResource local = infos[i].getLocal();
                ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(local);
				CVSTag tag = null;
				if(cvsResource.isFolder()) {
					FolderSyncInfo info = ((ICVSFolder)cvsResource).getFolderSyncInfo();
					if(info != null) {
						tag = info.getTag();									
					}
					if (tag != null && tag.getType() == CVSTag.BRANCH) {
						tag = Util.getAccurateFolderTag(local, tag);
					}
				} else {
					tag = Util.getAccurateFileTag(cvsResource);
				}
				if(tag == null) {
					tag = new CVSTag();
				}
				return tag;
			}
			return new CVSTag();
		} catch (CVSException e) {
			return new CVSTag();
		}
	}
	
    private CVSCompareSubscriber getCompareSubscriber() {
        ISynchronizeParticipant participant = getConfiguration().getParticipant();
        if (participant instanceof CompareParticipant) {
            return ((CompareParticipant)participant).getCVSCompareSubscriber();
        }
        return null;
    }

    private ICVSRemoteResource getRemoteResource(CVSSyncInfo info) {
		try {
			ICVSRemoteResource remote = (ICVSRemoteResource) info.getRemote();
			ICVSRemoteResource local = CVSWorkspaceRoot.getRemoteResourceFor(info.getLocal());
			if(local == null) {
				local = (ICVSRemoteResource)info.getBase();
			}
			
			boolean useRemote = true;
			if (local != null && remote != null) {
				String remoteRevision = getRevisionString(remote);
				String localRevision = getRevisionString(local);
				useRemote = useRemote(localRevision, remoteRevision);
			} else if (remote == null) {
				useRemote = false;
			}
			if (useRemote) {
				return remote;
			} else if (local != null) {
				return local;
			}
			return null;
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return null;
		}
	}
	
    private boolean useRemote(String localRevision, String remoteRevision) {
        boolean useRemote;
        if (remoteRevision == null && localRevision == null) {
            useRemote = true;
        } else if (localRevision == null) {
            useRemote = true;
        } else if (remoteRevision == null) {
            useRemote = false;
        } else {
            useRemote = ResourceSyncInfo.isLaterRevision(remoteRevision, localRevision);
        }
        return useRemote;
    }

    private String getRevisionString(ICVSRemoteResource remoteFile) {
		if(remoteFile instanceof RemoteFile) {
			return ((RemoteFile)remoteFile).getRevision();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.views.HierarchicalModelProvider#dispose()
	 */
	public void dispose() {
		shutdown = true;
		if(fetchLogEntriesJob != null && fetchLogEntriesJob.getState() != Job.NONE) {
			fetchLogEntriesJob.cancel();
		}
		if (logs != null) {
		    logs.clearEntries();
		}
		super.dispose();
	}
}
