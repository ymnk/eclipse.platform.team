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
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ccvs.ui.CVSLightweightDecorator;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.team.ui.synchronize.views.SyncInfoLabelProvider;

public class CVSSynchronizeViewCompareConfiguration extends TeamSubscriberPageDiffTreeViewerConfiguration {

	private boolean isGroupIncomingByComment = false;

	private static class CVSLabelDecorator extends LabelProvider implements ILabelDecorator  {
		public String decorateText(String input, Object element) {
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
		public Image decorateImage(Image base, Object element) {
			return base;
		}
	}
	
	public CVSSynchronizeViewCompareConfiguration(ISynchronizeView view, TeamSubscriberParticipant participant) {
		super(view, participant);
		participant.addPropertyChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration#getLabelProvider()
	 */
	protected ILabelProvider getLabelProvider() {
		return new SyncInfoLabelProvider.DecoratingColorLabelProvider(new SyncInfoLabelProvider(), new CVSLabelDecorator());
	}
	
	public boolean isGroupIncomingByComment() {
		return isGroupIncomingByComment;
	}
	
	public void setGroupIncomingByComment(boolean enabled) {
		this.isGroupIncomingByComment = enabled;
		if(getParticipant().getMode() == TeamSubscriberParticipant.INCOMING_MODE) {
			getViewer().setInput(getInput());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration#getInput()
	 */
	protected SyncInfoDiffNodeRoot getInput() {
		if(isGroupIncomingByComment() && getParticipant().getMode() == TeamSubscriberParticipant.INCOMING_MODE) {
			return new ChangeLogDiffNodeRoot(getSyncSet());
		} else {
			return super.getInput();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.DiffTreeViewerConfiguration#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if(property.equals(TeamSubscriberParticipant.P_SYNCVIEWPAGE_MODE) && isGroupIncomingByComment()) {
			int oldMode = ((Integer)event.getOldValue()).intValue();
			int newMode = ((Integer)event.getNewValue()).intValue();
			if(newMode == TeamSubscriberParticipant.INCOMING_MODE || 
			   oldMode == TeamSubscriberParticipant.INCOMING_MODE) {
				getViewer().setInput(getInput());
			}
		}
		super.propertyChange(event);
	}
}
