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
package org.eclipse.team.internal.ccvs.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.subscribers.utils.SynchronizationCache;
import org.eclipse.team.internal.ccvs.core.syncinfo.CVSSynchronizationCache;

/**
 * This subscriber is used when comparing the local workspace with its
 * corresponding remote.
 */
public class CVSCompareSubscriber extends CVSSyncTreeSubscriber {

	public static final String QUALIFIED_NAME = CVSProviderPlugin.ID + ".compare"; //$NON-NLS-1$
	private static final String UNIQUE_ID_PREFIX = "compare-"; //$NON-NLS-1$
	
	private CVSTag tag;
	private SynchronizationCache remoteSynchronizer;
	private IResource[] resources;

	public CVSCompareSubscriber(IResource[] resources, CVSTag tag) {
		super(getUniqueId(), "CVS Compare with {0}" + tag.getName(), "Shows the differences between a tag and the workspace.");
		this.resources = resources;
		this.tag = tag;
		initialize();
	}

	private void initialize() {
		QualifiedName id = getId();
		String syncKeyPrefix = id.getLocalName();
		remoteSynchronizer = new CVSSynchronizationCache(new QualifiedName(SYNC_KEY_QUALIFIER, syncKeyPrefix + tag.getName()));
	}

	public void dispose() {	
		// ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);		
		remoteSynchronizer.dispose();	
	}
	
	private static QualifiedName getUniqueId() {
		String uniqueId = Long.toString(System.currentTimeMillis());
		return new QualifiedName(QUALIFIED_NAME, UNIQUE_ID_PREFIX + uniqueId); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getRemoteTag()
	 */
	protected CVSTag getRemoteTag() {
		return tag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getBaseTag()
	 */
	protected CVSTag getBaseTag() {
		// No base tag needed since it's a two way compare
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getBaseSynchronizationCache()
	 */
	protected SynchronizationCache getBaseSynchronizationCache() {
		// No base cache needed since it's a two way compare
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.CVSSyncTreeSubscriber#getRemoteSynchronizationCache()
	 */
	protected SynchronizationCache getRemoteSynchronizationCache() {
		return remoteSynchronizer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#isThreeWay()
	 */
	public boolean isThreeWay() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#roots()
	 */
	public IResource[] roots() {
		return resources;
	}
}
