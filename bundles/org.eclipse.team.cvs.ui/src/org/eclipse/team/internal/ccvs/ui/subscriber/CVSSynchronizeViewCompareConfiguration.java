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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ccvs.ui.CVSLightweightDecorator;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.views.*;
import org.eclipse.team.ui.synchronize.views.SyncInfoLabelProvider;

public class CVSSynchronizeViewCompareConfiguration extends TeamSubscriberPageDiffTreeViewerConfiguration {

	private static class CVSLabelProvider extends SyncInfoDecoratingLabelProvider {
		protected CVSLabelProvider(SyncInfoLabelProvider syncInfoLabelProvider) {
			super(syncInfoLabelProvider);
		}
		protected String decorateText(String input, Object element) {
			String text = input;
			if (element instanceof SyncInfoDiffNode) {
				IResource resource =  ((SyncInfoDiffNode)element).getResource();
				if(resource != null) {
					CVSLightweightDecorator.Decoration decoration = new CVSLightweightDecorator.Decoration();
					CVSLightweightDecorator.decorateTextLabel(resource, decoration, false, true);
					StringBuffer output = new StringBuffer(25);
					if(decoration.prefix != null) {
						output.append(decoration.prefix);
					}
					output.append(text);
					if(decoration.suffix != null) {
						output.append(decoration.suffix);
					}
					return output.toString();
				}
			} else if(element instanceof ChangeLogDiffNode) {
				return ((ChangeLogDiffNode)element).getComment();
			}
			return text;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.TeamSubscriberParticipantLabelProvider#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
		 */
		protected Image decorateImage(Image base, Object element) {
			if(element instanceof ChangeLogDiffNode) {
				//TODO: return getCompressedFolderImage();
			}
			return super.decorateImage(base, element);
		}
	}
	
	public CVSSynchronizeViewCompareConfiguration(ISynchronizeView view, TeamSubscriberParticipant participant) {
		super(view, participant);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SyncInfoSetCompareConfiguration#getLabelProvider(org.eclipse.team.ui.synchronize.content.SyncInfoLabelProvider)
	 */
	protected ILabelProvider getLabelProvider(SyncInfoLabelProvider logicalProvider) {
		return new CVSLabelProvider(logicalProvider);
	}
}
