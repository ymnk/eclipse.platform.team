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
package org.eclipse.team.internal.ui.synchronize.views;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration;
import org.eclipse.team.ui.synchronize.views.SyncInfoLabelProvider;
import org.eclipse.team.ui.synchronize.views.SyncInfoSetTreeContentProvider;

public class DefaultLogicalView extends LogicalViewProvider implements IPropertyChangeListener {

	public DefaultLogicalView(DiffTreeViewerConfiguration configuration) {
		super(configuration);
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.LogicalViewProvider#getContentProvider()
	 */
	public SyncInfoSetTreeContentProvider getContentProvider() {
		if(getShowCompressedFolders()) {
			return new CompressedFolderContentProvider();
		} else {
			return new SyncInfoSetTreeContentProvider();
		}
	}
	
	private boolean getShowCompressedFolders() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getBoolean(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.synchronize.views.LogicalViewProvider#getLabelProvider()
	 */
	public SyncInfoLabelProvider getLabelProvider() {
		if(getShowCompressedFolders()) {
			return new CompressedFolderLabelProvider();
		} else {
			return new SyncInfoLabelProvider();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {	
		if (event.getProperty().equals(IPreferenceIds.SYNCVIEW_COMPRESS_FOLDERS)) {
			getConfiguration().setLogicalViewProvider(this);
		}
	}
	
	public void dispose() {
		TeamUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(this);		
	}
}
