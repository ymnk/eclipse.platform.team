/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ccvs.core.mapping.ChangeSetModelProvider;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.ComparePreferencePage;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSParticipantLabelDecorator;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

public abstract class CVSModelSynchronizeParticipant extends ModelSynchronizeParticipant {

	public static PreferencePage[] addCVSPreferencePages(PreferencePage[] inheritedPages) {
	    PreferencePage[] pages = new PreferencePage[inheritedPages.length + 1];
	    for (int i = 0; i < inheritedPages.length; i++) {
	        pages[i] = inheritedPages[i];
	    }
	    pages[pages.length - 1] = new ComparePreferencePage();
	    pages[pages.length - 1].setTitle(CVSUIMessages.CVSParticipant_2); 
	    return pages;
	}

	public CVSModelSynchronizeParticipant() {
		super();
	}

	public CVSModelSynchronizeParticipant(SynchronizationContext context) {
		super(context);
	}

	public PreferencePage[] getPreferencePages() {
	    return addCVSPreferencePages(super.getPreferencePages());
	}
	
	public ModelProvider[] getEnabledModelProviders() {
		ModelProvider[] enabledProviders = super.getEnabledModelProviders();
		if (this instanceof IChangeSetProvider) {
			for (int i = 0; i < enabledProviders.length; i++) {
				ModelProvider provider = enabledProviders[i];
				if (provider.getId().equals(ChangeSetModelProvider.ID))
					return enabledProviders;
			}
			ModelProvider[] extended = new ModelProvider[enabledProviders.length + 1];
			for (int i = 0; i < enabledProviders.length; i++) {
				extended[i] = enabledProviders[i];
			}
			ChangeSetModelProvider provider = ChangeSetModelProvider.getProvider();
			if (provider == null)
				return enabledProviders;
			extended[extended.length - 1] = provider;
			return extended;
		}
		return enabledProviders;
	}

	protected  ILabelDecorator getLabelDecorator(ISynchronizePageConfiguration configuration) {
	    return new CVSParticipantLabelDecorator(configuration);
	}
	
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.addLabelDecorator(getLabelDecorator(configuration));
	}
}
