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

import org.eclipse.team.core.change.SubscriberChangeSetManager;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;

/**
 * Thsi class keeps the active commit sets up-to-date.
 */
public class CommitSetManager {
    
    private static SubscriberChangeSetManager instance;
    
    public synchronized static SubscriberChangeSetManager getInstance() {
        if (instance == null) {
            instance = new SubscriberChangeSetManager(CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber());
        }
        return instance;
    }
    
}
