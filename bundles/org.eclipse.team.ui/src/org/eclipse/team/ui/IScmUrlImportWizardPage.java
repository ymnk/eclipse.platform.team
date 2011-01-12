/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.ScmUrlImportDescription;

/**
 * TODO: <strong>EXPERIMENTAL</strong>
 * 
 * @since 3.6
 */
public interface IScmUrlImportWizardPage extends IWizardPage {

	public static final String ATT_EXTENSION = "scmUrlImportPages"; //$NON-NLS-1$
	public static final String ATT_PAGE = "page"; //$NON-NLS-1$
	public static final String ATT_REPOSITORY = "repository"; //$NON-NLS-1$

	/**
	 * TODO:
	 * 
	 * @return if the operation was successful. The wizard will only close when
	 *         <code>true</code> is returned.
	 */
	public boolean finish();

	/**
	 * TODO:
	 * 
	 * @return the SCM URLs edited or created on the page.
	 */
	public ScmUrlImportDescription[] getSelection();

	/**
	 * TODO:
	 * 
	 * @param scmUrls
	 *            the SCM URLs edited on the page.
	 */
	public void setSelection(ScmUrlImportDescription[] scmUrls);

	public void setProvider(RepositoryProviderType provider);

	public RepositoryProviderType getProvider();

}
