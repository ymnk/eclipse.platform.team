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
package org.eclipse.team.core.subscribers;

import java.util.*;

import org.eclipse.core.internal.runtime.ListenerList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.*;
import org.eclipse.team.internal.core.*;
import org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This class manages the change sets associated with a subscriber.
 */
public class SubscriberChangeSetManager extends SubscriberResourceCollector implements ISyncInfoSetChangeListener {
    
    private static final String PREF_CHANGE_SETS = "changeSets"; //$NON-NLS-1$
    private static final String CTX_DEFAULT_SET = "defaultSet"; //$NON-NLS-1$
    
    private static final int RESOURCE_REMOVAL = 1;
    private static final int RESOURCE_CHANGE = 2;
    
    private List activeSets;
    private ListenerList listeners = new ListenerList();
    private ChangeSet defaultSet;
    private EventHandler handler;
    
    /*
     * Background event handler for serializing and batching change set changes
     */
    private class EventHandler extends BackgroundEventHandler {

        private List dispatchEvents = new ArrayList();
        
        protected EventHandler(String jobName, String errorTitle) {
            super(jobName, errorTitle);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.BackgroundEventHandler#processEvent(org.eclipse.team.internal.core.BackgroundEventHandler.Event, org.eclipse.core.runtime.IProgressMonitor)
         */
        protected void processEvent(Event event, IProgressMonitor monitor) throws CoreException {
            // Handle everything in the dispatch
            dispatchEvents.add(event);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.BackgroundEventHandler#doDispatchEvents(org.eclipse.core.runtime.IProgressMonitor)
         */
        protected boolean doDispatchEvents(IProgressMonitor monitor) throws TeamException {
            if (dispatchEvents.isEmpty()) {
                return false;
            }
            try {
                beginDispath();
                for (Iterator iter = dispatchEvents.iterator(); iter.hasNext();) {
                    Event event = (Event) iter.next();
	                switch (event.getType()) {
	                case RESOURCE_REMOVAL:
	                    handleRemove(event.getResource());
	                    break;
	                case RESOURCE_CHANGE:
	                    handleChange(event.getResource(), event.getDepth());
	                    break;
	                default:
	                    break;
	                }
                }
            } finally {
                try {
                    endDispatch(monitor);
                } finally {
                    dispatchEvents.clear();
                }
            }
            return true;
        }

        private void beginDispath() {
            for (Iterator iter = activeSets.iterator(); iter.hasNext();) {
                ChangeSet set = (ChangeSet) iter.next();
                set.getSyncInfos().beginInput();
            }
        }

        private void endDispatch(IProgressMonitor monitor) {
            monitor.beginTask(null, 100 * activeSets.size());
            for (Iterator iter = activeSets.iterator(); iter.hasNext();) {
                ChangeSet set = (ChangeSet) iter.next();
                try {
                    set.getSyncInfos().endInput(Policy.subMonitorFor(monitor, 100));
                } catch (RuntimeException e) {
                    // Don't worry about ending every set if an error occurs.
                    // Instead, log the error and suggest a restart.
                    TeamPlugin.log(IStatus.ERROR, "An error occurred while reconciling change sets. Restarting the application is recommended.", e);
                    throw e;
                }
            }
            monitor.done();
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.BackgroundEventHandler#queueEvent(org.eclipse.team.internal.core.BackgroundEventHandler.Event, boolean)
         */
        protected synchronized void queueEvent(Event event, boolean front) {
            // Override to allow access from enclosing class
            super.queueEvent(event, front);
        }
        
        /*
         * Handle the removal
         */
        private void handleRemove(IResource resource) {
            for (Iterator iter = activeSets.iterator(); iter.hasNext();) {
                ChangeSet set = (ChangeSet) iter.next();
                // This will remove any descendants from the set and callback to 
                // resourcesChanged which will batch changes
                if (!set.isEmpty()) {
	                set.rootRemoved(resource);
	                if (set.isEmpty()) {
	                    remove(set);
	                }
                }
            }
        }
        
        /*
         * Handle the change
         */
        private void handleChange(IResource resource, int depth) throws TeamException {
            SyncInfo syncInfo = getSyncInfo(resource);
            if (isModified(syncInfo)) {
                ChangeSet[] containingSets = getContainingSets(resource);
                if (containingSets.length == 0) {
	                // Consider for inclusion in the default set
	                // if the resource is not already a memebr of another set
                    if (defaultSet != null) {
                        defaultSet.add(syncInfo);
                     }
                } else {
                    for (int i = 0; i < containingSets.length; i++) {
                        ChangeSet set = containingSets[i];
                        // Update the sync info in the set
                        set.getSyncInfos().add(syncInfo);
                    }
                }
            } else {
                removeFromAllSets(resource);
            }
            if (depth != IResource.DEPTH_ZERO) {
                IResource[] members = getSubscriber().members(resource);
                for (int i = 0; i < members.length; i++) {
                    IResource member = members[i];
                    handleChange(member, depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE);
                }
            }
        }
        
        private void removeFromAllSets(IResource resource) {
            List toRemove = new ArrayList();
            for (Iterator iter = activeSets.iterator(); iter.hasNext();) {
                ChangeSet set = (ChangeSet) iter.next();
                if (set.contains(resource)) {
                    set.remove(resource);
	                if (set.isEmpty()) {
	                    toRemove.add(set);
	                }
                }
            }
            for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
                ChangeSet set = (ChangeSet) iter.next();
                remove(set);
            }
        }

        private ChangeSet[] getContainingSets(IResource resource) {
            Set sets = new HashSet();
            for (Iterator iter = activeSets.iterator(); iter.hasNext();) {
                ChangeSet set = (ChangeSet) iter.next();
                if (set.contains(resource)) {
                    sets.add(set);
                }
            }
            return (ChangeSet[]) sets.toArray(new ChangeSet[sets.size()]);
        }
    }
    
    public SubscriberChangeSetManager(Subscriber subscriber) {
        super(subscriber);
        load();
        handler = new EventHandler("Updating Change Sets for {0}" + subscriber.getName(), "Errors occurred while updating the change sets for {0}" + subscriber.getName());
    }
	
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#remove(org.eclipse.core.resources.IResource)
     */
    protected void remove(IResource resource) {
        handler.queueEvent(new BackgroundEventHandler.Event(resource, RESOURCE_REMOVAL, IResource.DEPTH_INFINITE), false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#change(org.eclipse.core.resources.IResource, int)
     */
    protected void change(IResource resource, int depth) {
        handler.queueEvent(new BackgroundEventHandler.Event(resource, RESOURCE_CHANGE, depth), false);
    }
    
    private Object[] getListeners() {
        return listeners.getListeners();
    }
    
    /**
     * The title of the given set has changed. Notify any listeners.
     */
    /* package */ void titleChanged(final ChangeSet set) {
        if (activeSets.contains(set)) {
            Object[] listeners = getListeners();
            for (int i = 0; i < listeners.length; i++) {
                final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
                Platform.run(new ISafeRunnable() {
                    public void handleException(Throwable exception) {
                        // Exceptions are logged by the platform
                    }
                    public void run() throws Exception {
                        listener.titleChanged(set);
                    }
                });
            }
        }
    }

    /**
     * Return whether the manager allows a resource to
     * be in mulitple sets. By default, a resource
     * may only be in one set.
     * @return whether the manager allows a resource to
     * be in mulitple sets.
     */
    protected boolean isSingleSetPerResource() {
        return true;
    }
    
    /**
     * Create a commit set with the given title and files. The created
     * set is not added to the control of the commit set manager
     * so no events are fired. The set can be added using the
     * <code>add</code> method.
     * @param title the title of the commit set
     * @param files the files contained in the set
     * @return the created set
     * @throws CVSException
     */
    public ChangeSet createSet(String title, SyncInfo[] infos) throws TeamException {
        ChangeSet commitSet = new ChangeSet(this, title);
        if (infos != null && infos.length > 0) {
            commitSet.add(infos);
        }
        return commitSet;
    }

    /**
     * Create a change set containing the given files if
     * they have been modified locally.
     * @param title the title of the commit set
     * @param files the files contained in the set
     * @return the created set
     * @throws TeamException
     */
    public ChangeSet createSet(String title, IFile[] files) throws TeamException {
        List infos = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            IFile file = files[i];
            SyncInfo info = getSyncInfo(file);
            if (info != null) {
                infos.add(info);
            }
        }
        return createSet(title, (SyncInfo[]) infos.toArray(new SyncInfo[infos.size()]));
    }
    
    /**
     * Add the set to the list of active sets.
     * @param set the set to be added
     */
    public void add(final ChangeSet set) {
        if (!contains(set)) {
            activeSets.add(set);
            set.getSyncInfos().addSyncSetChangedListener(this);
            Object[] listeners = getListeners();
            for (int i = 0; i < listeners.length; i++) {
                final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
                Platform.run(new ISafeRunnable() {
                    public void handleException(Throwable exception) {
                        // Exceptions are logged by the platform
                    }
                    public void run() throws Exception {
                        listener.setAdded(set);
                    }
                });
            }
            handleAddedResources(set, set.getSyncInfos().getSyncInfos());
        }
    }

    /**
     * Remove the set from the list of active sets.
     * @param set the set to be removed
     */
    public void remove(final ChangeSet set) {
        if (contains(set)) {
            set.getSyncInfos().removeSyncSetChangedListener(this);
            activeSets.remove(set);
            Object[] listeners = getListeners();
            for (int i = 0; i < listeners.length; i++) {
                final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
                Platform.run(new ISafeRunnable() {
                    public void handleException(Throwable exception) {
                        // Exceptions are logged by the platform
                    }
                    public void run() throws Exception {
                        listener.setRemoved(set);
                    }
                });
            }
        }
    }
    
    /**
     * Return whether the manager contains the given commit set
     * @param set the commit set being tested
     * @return whether the set is contained in the manager's list of active sets
     */
    public boolean contains(ChangeSet set) {
        return activeSets.contains(set);
    }

    /**
     * Add the listener to the set of registered listeners.
     * @param listener the listener to be added
     */
    public void addListener(IChangeSetChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the listener from the set of registered listeners.
     * @param listener the listener to remove
     */
    public void removeListener(IChangeSetChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Return the list of active commit sets.
     * @return the list of active commit sets
     */
    public ChangeSet[] getSets() {
        return (ChangeSet[]) activeSets.toArray(new ChangeSet[activeSets.size()]);
    }

    /**
     * Make the given set the default set into which all new modifications
     * that ae not already in another set go.
     * @param set the set which is to become the default set
     */
    public void makeDefault(ChangeSet set) {
        // The default set must be an active set
        if (!contains(set)) {
            add(set);
        }
        final ChangeSet oldSet = defaultSet;
        defaultSet = set;
        Object[] listeners = getListeners();
        for (int i = 0; i < listeners.length; i++) {
            final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
            Platform.run(new ISafeRunnable() {
                public void handleException(Throwable exception) {
                    // Exceptions are logged by the platform
                }
                public void run() throws Exception {
                    listener.defaultSetChanged(oldSet, defaultSet);
                }
            });
        }
    }

    /**
     * Retrn the set which is currently the default or
     * <code>null</code> if there is no default set.
     * @return
     */
    public ChangeSet getDefaultSet() {
        return defaultSet;
    }
    /**
     * Return whether the given set is the default set into which all
     * new modifications will be placed.
     * @param set the set to test
     * @return whether the set is the default set
     */
    public boolean isDefault(ChangeSet set) {
        return set == defaultSet;
    }
    
    /**
     * Return the sync info for the given resource obtained
     * from the subscriber.
     * @param resource the resource
     * @return the sync info for the resource
     * @throws TeamException
     */
    protected SyncInfo getSyncInfo(IResource resource) throws TeamException {
        Subscriber subscriber = getSubscriber();
        SyncInfo info = subscriber.getSyncInfo(resource);
        return info;
    }
    
    protected boolean isModified(SyncInfo info) {
        if (info != null) {
            if (info.getComparator().isThreeWay()) {
                int dir = (info.getKind() & SyncInfo.DIRECTION_MASK);
                return dir == SyncInfo.OUTGOING || dir == SyncInfo.CONFLICTING;
            } else {
                return (info.getKind() & SyncInfo.CHANGE_MASK) == SyncInfo.CHANGE;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#dispose()
     */
    public void dispose() {
        handler.shutdown();
        super.dispose();
        save();
    }
    
    private void save() {
		Preferences prefs = getPreferences();
		for (Iterator it = activeSets.iterator(); it.hasNext(); ) {
		    ChangeSet set = (ChangeSet) it.next();
			if (!set.isEmpty()) {
			    Preferences child = prefs.node(set.getTitle());
			    set.save(child);
			}
		}
		if (defaultSet != null) {
		    prefs.put(CTX_DEFAULT_SET, defaultSet.getTitle());
		}
		try {
            prefs.flush();
        } catch (BackingStoreException e) {
            TeamPlugin.log(IStatus.ERROR, "An error occurred saving the change set state for {0}" + getSubscriber().getName(), e);
        }
    }
    
    private void load() {
        activeSets = new ArrayList();
        Preferences prefs = getPreferences();
		String defaultSetTitle = prefs.get(CTX_DEFAULT_SET, null);
		try {
            String[] childNames = prefs.childrenNames();
            for (int i = 0; i < childNames.length; i++) {
                String string = childNames[i];
                Preferences childPrefs = prefs.node(string);
                ChangeSet set = createSet(string, childPrefs);
            	if (defaultSet == null && defaultSetTitle != null && set.getTitle().equals(defaultSetTitle)) {
            	    defaultSet = set;
            	}
            	activeSets.add(set);
            }
        } catch (BackingStoreException e) {
            TeamPlugin.log(IStatus.ERROR, "An error occurred restoring the change set state for {0}" + getSubscriber().getName(), e);
        }
    }

    /**
     * Create a change set from the given preferences that were 
     * previously saved.
     * @param childPrefs the previously saved preferences
     * @return the created change set
     */
    protected ChangeSet createSet(String title, Preferences childPrefs) {
        ChangeSet changeSet = new ChangeSet(this, title);
        changeSet.init(childPrefs);
        return changeSet;
    }

    private Preferences getPreferences() {
        return getParentPreferences().node(getSubscriberIdentifier());
    }
    
	private static Preferences getParentPreferences() {
		return getTeamPreferences().node(PREF_CHANGE_SETS);
	}
	
	private static Preferences getTeamPreferences() {
		IPreferencesService service = Platform.getPreferencesService();
		IEclipsePreferences root = service.getRootNode();
		return root.node(InstanceScope.SCOPE).node(TeamPlugin.getPlugin().getBundle().getSymbolicName());
	}
	
    /**
     * Return the id that will uniquely identify the subscriber accross
     * restarts.
     * @return
     */
    protected String getSubscriberIdentifier() {
        return getSubscriber().getName();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
        handleSyncSetChange(set, set.getSyncInfos(), set.getResources());
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoChanged(org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
        SyncInfoSet set = event.getSet();
        handleSyncSetChange(set, event.getAddedResources(), getAllResources(event));
    }

    private IResource[] getAllResources(ISyncInfoSetChangeEvent event) {
        Set allResources = new HashSet();
        SyncInfo[] addedResources = event.getAddedResources();
        for (int i = 0; i < addedResources.length; i++) {
            SyncInfo info = addedResources[i];
            allResources.add(info.getLocal());
        }
        SyncInfo[] changedResources = event.getChangedResources();
        for (int i = 0; i < changedResources.length; i++) {
            SyncInfo info = changedResources[i];
            allResources.add(info.getLocal());
        }
        allResources.addAll(Arrays.asList(event.getRemovedResources()));
        return (IResource[]) allResources.toArray(new IResource[allResources.size()]);
    }

    private void handleAddedResources(ChangeSet set, SyncInfo[] infos) {
        if (isSingleSetPerResource()) {
            IResource[] resources = new IResource[infos.length];
            for (int i = 0; i < infos.length; i++) {
                resources[i] = infos[i].getLocal();
            }
	        // Remove the added files from any other set that contains them
	        for (Iterator iter = activeSets.iterator(); iter.hasNext();) {
	            ChangeSet otherSet = (ChangeSet) iter.next();
	            if (otherSet != set) {
	                otherSet.remove(resources);
	            }
	        }
        }
    }
    
    private void handleSyncSetChange(SyncInfoSet set, SyncInfo[] addedInfos, IResource[] allAffectedResources) {
        ChangeSet changeSet = getChangeSet(set);
        if (set.isEmpty() && changeSet != null) {
            remove(changeSet);
        }
        fireResourcesChangedEvent(changeSet, allAffectedResources);
        handleAddedResources(changeSet, addedInfos);
    }

    /**
     * @param changeSet
     * @param allAffectedResources
     */
    private void fireResourcesChangedEvent(final ChangeSet changeSet, final IResource[] allAffectedResources) {
        Object[] listeners = getListeners();
        for (int i = 0; i < listeners.length; i++) {
            final IChangeSetChangeListener listener = (IChangeSetChangeListener)listeners[i];
            Platform.run(new ISafeRunnable() {
                public void handleException(Throwable exception) {
                    // Exceptions are logged by the platform
                }
                public void run() throws Exception {
                    listener.resourcesChanged(changeSet, allAffectedResources);
                }
            });
        }
    }
    
    private ChangeSet getChangeSet(SyncInfoSet set) {
        for (Iterator iter = activeSets.iterator(); iter.hasNext();) {
            ChangeSet changeSet = (ChangeSet) iter.next();
            if (changeSet.getSyncInfos() == set) {
                return changeSet;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetErrors(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
        // Nothing to do
    }
}
