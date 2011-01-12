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
package org.eclipse.team.core;

import java.net.URI;


/**
 * @since 3.6
 */
public class ScmUrlImportDescription {
	private String url;
	private String project;

	public ScmUrlImportDescription(String url, String project) {
		this.url = url;
		this.project = project;
	}

	public String getProject() {
		return project;
	}

	public String getUrl() {
		return url;
	}

	public URI getUri() {
		return URI.create(url);
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Object getProperty(String plugin) {
		// TODO Auto-generated method stub
		// called here: org.eclipse.pde.internal.ui.wizards.imports.PluginImportWizardFirstPage.configureBundleImportPages(IPluginModelBase[])
		return null;
	}
}
