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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;


/**
 * This class provides functionality for defining composite synchronize model
 * providers. A composite provider is one that breaks up the displayed
 * {@link SyncInfoSet} into subsets that may be didsplayed using one
 * or more synchronize model providers.
 */
public abstract class CompositeModelProvider extends AbstractSynchronizeModelProvider {

    protected CompositeModelProvider(SynchronizeModelElement parent, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
        super(null, parent, configuration, set);
    }

}
