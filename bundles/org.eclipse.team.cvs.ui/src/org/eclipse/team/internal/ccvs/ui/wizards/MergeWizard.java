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
package org.eclipse.team.internal.ccvs.ui.wizards;


import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.subscriber.MergeSynchronizeParticipant;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

public class MergeWizard extends Wizard {
    MergeWizardPage page;
	IResource[] resources;

	public void addPages() {
	    setNeedsProgressMonitor(true);
	    TagSource tagSource = TagSource.create(resources);
		setWindowTitle(Policy.bind("MergeWizard.title")); //$NON-NLS-1$
		ImageDescriptor mergeImage = CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_WIZBAN_MERGE);
		page = new MergeWizardPage("mergePage", "Select the merge points", mergeImage, "Specify the branch or version to merge from and the common base.", tagSource);
		addPage(page);
	}

	/*
	 * @see IWizard#performFinish()
	 */
	public boolean performFinish() {
		
		CVSTag startTag = page.getStartTag();
		CVSTag endTag = page.getEndTag();				
		
		// First check if there is an existing matching participant, if so then re-use it
		MergeSynchronizeParticipant participant = MergeSynchronizeParticipant.getMatchingParticipant(resources, startTag, endTag);
		if(participant == null) {
			CVSMergeSubscriber s = new CVSMergeSubscriber(resources, startTag, endTag);
			participant = new MergeSynchronizeParticipant(s);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
		}
		participant.refresh(resources, Policy.bind("Participant.merging"), Policy.bind("Participant.mergingDetail", participant.getName()), null); //$NON-NLS-1$ //$NON-NLS-2$
		return true;
	}
	
	/*
	 * Set the resources that should be merged.
	 */
	public void setResources(IResource[] resources) {
		this.resources = resources;
	}
}
