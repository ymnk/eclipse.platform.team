/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui;

import org.eclipse.compare.MergeContext;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.synchronize.ISynchronizeModelProvider;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * A model provider specific configuration that wraps the page configuration.
 * It is used as input to the model provider creation of <code>IModelContentProviderFactory</code>.
 * It could potentially be replaced by constructor injection when that becomes available.
 * Clients are not allowed to implement this interface.
 * @since 3.2
 */
public interface ISynchronizeModelProviderConfiguration {
    
    public ISynchronizePageConfiguration getPageConfiguration();
    
    public ISynchronizeModelElement getParentNode();
    
    public ISynchronizeModelProvider getParentProvider();
    
    public SyncInfoSet getSyncInfoSet();
    
    public MergeContext getMergeContext();

}
