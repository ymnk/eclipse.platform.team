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
package org.eclipse.team.ui.synchronize;

import org.eclipse.team.core.subscribers.ActiveChangeSet;
import org.eclipse.team.core.subscribers.SubscriberChangeSetCollector;
import org.eclipse.team.core.synchronize.SyncInfo;

/**
 * A change set capability is used by a SubscriberSynchronizePage
 * to determine what, if any change set capabilities should be enabled
 * for the pags of the particpant.
 * @since 3.1
 */
public abstract class ChangeSetCapability {

    /**
     * Return whether the associated participant supports
     * the display of checked-in change sets.
     * @return whether the associated participant supports
     * the display of checked-in change sets
     */
    public abstract boolean supportsCheckedInChangeSets();
    
    /**
     * Return whether the associated participant supports
     * the use of active change sets.
     * @return whether the associated participant supports
     * the use of active change sets
     */
    public abstract boolean supportsActiveChangeSets();
    
    /**
     * Return the change set collector that manages the active change
     * set for the particpant associated with this capability.
     * @return the change set collector that manages the active change
     * set for the particpant associated with this capability
     */
    public abstract SubscriberChangeSetCollector getActiveChangeSetManager();
    
    /**
     * Create a change set from the given manager that contains the given sync info.
     * This method is invoked from the UI thread.
     * @param configuration TODO
     * @param infos the sync info to be added to the change set
     * @param manager a change set manager
     * @return the created set.
     */
    public abstract ActiveChangeSet createChangeSet(ISynchronizePageConfiguration configuration, SyncInfo[] infos);
    
    /**
     * Edit the title and comment of the given change set.
     * This method is invoked from the UI thread.
     * @param configuration TODO
     * @param set the set to be edited
     */
    public abstract void editChangeSet(ISynchronizePageConfiguration configuration, ActiveChangeSet set);
    
    /**
     * Return a collector that can be used to group a set of checked-in changes
     * into a set of checked-in change sets.
     * @param configuration the configuration for the page that will be displaying the change sets
     * @return a change set collector
     */
    public abstract SyncInfoSetChangeSetCollector createCheckedInChangeSetCollector(ISynchronizePageConfiguration configuration);
    
}
