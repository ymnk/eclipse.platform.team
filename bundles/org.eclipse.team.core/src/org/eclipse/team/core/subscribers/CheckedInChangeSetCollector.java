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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfoSet;

/**
 * Contains a set of CheckedInChangeSets.
 * @since 3.1
 */
public class CheckedInChangeSetCollector extends ChangeSetCollector {

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.ChangeSetCollector#add(org.eclipse.team.core.subscribers.ChangeSet)
     */
    public void add(CheckedInChangeSet set) {
        super.add(set);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetReset(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
        handleChangeEvent(set);

    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoChanged(org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoChanged(ISyncInfoSetChangeEvent event, IProgressMonitor monitor) {
        handleChangeEvent(event.getSet());
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener#syncInfoSetErrors(org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.team.core.ITeamStatus[], org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
        // TODO Auto-generated method stub
    }
    
    /*
     * The collector removes change sets once they are empty
     */
    private void handleChangeEvent(SyncInfoSet set) {
        if (set.isEmpty()) {
            ChangeSet changeSet = getChangeSet(set);
            if (changeSet != null) {
                remove(changeSet);
            }
        }
    }

}
