/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.filesystem.CVSURI;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.SWTUtils;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class CVSScmUrlImportWizardPage extends WizardPage implements IScmUrlImportWizardPage {
	
	private URI[] scmUris;
	private Button useHead;
	private TableViewer bundlesViewer;
	private Label counterLabel;

	private static final String CVS_PAGE_USE_HEAD = "org.eclipse.team.cvs.ui.import.page.head"; //$NON-NLS-1$

	class CvsLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
		 */
		public void update(ViewerCell cell) {
			StyledString string = getStyledText(cell.getElement());
			cell.setText(string.getString());
			cell.setStyleRanges(string.getStyleRanges());
			cell.setImage(getImage(cell.getElement()));
			super.update(cell);
		}

		private StyledString getStyledText(Object element) {
			StyledString styledString = new StyledString();
			if (element instanceof URI) {
				URI scmUrl = (URI) element;
				String project = getProject(scmUrl);
				String version = getTag(scmUrl);
				String host = getServer(scmUrl);
				styledString.append(project);
				if (version != null && !useHead.getSelection()) {
					styledString.append(' ');
					styledString.append(version, StyledString.DECORATIONS_STYLER);
				}
				styledString.append(' ');
				styledString.append('[', StyledString.DECORATIONS_STYLER);
				styledString.append(host, StyledString.DECORATIONS_STYLER);
				styledString.append(']', StyledString.DECORATIONS_STYLER);
				return styledString;
			}
			styledString.append(element.toString());
			return styledString;
		}
	}

	/**
	 * Constructs the page.
	 */
	public CVSScmUrlImportWizardPage() {
		super("cvs", CVSUIMessages.CVSScmUrlImportWizardPage_0, null); //$NON-NLS-1$
		setDescription(CVSUIMessages.CVSScmUrlImportWizardPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTUtils.createHVFillComposite(parent, SWTUtils.MARGINS_NONE, 1);
		Composite group = SWTUtils.createHFillComposite(comp, SWTUtils.MARGINS_NONE, 1);

		Button versions = SWTUtils.createRadioButton(group, CVSUIMessages.CVSScmUrlImportWizardPage_3);
		useHead = SWTUtils.createRadioButton(group, CVSUIMessages.CVSScmUrlImportWizardPage_2);
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				bundlesViewer.refresh(true);
			}
		};
		versions.addSelectionListener(listener);
		useHead.addSelectionListener(listener);

		Table table = new Table(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		table.setLayoutData(gd);

		bundlesViewer = new TableViewer(table);
		bundlesViewer.setLabelProvider(new CvsLabelProvider());
		bundlesViewer.setContentProvider(new ArrayContentProvider());
		bundlesViewer.setComparator(new ViewerComparator());
		counterLabel = new Label(comp, SWT.NONE);
		counterLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setControl(comp);
		setPageComplete(true);

		// initialize versions versus HEAD
		IDialogSettings settings = getWizard().getDialogSettings();
		boolean head = false;
		boolean found = false;
		if (settings != null) {
			String string = settings.get(CVS_PAGE_USE_HEAD);
			if (string != null) {
				found = true;
				head = settings.getBoolean(CVS_PAGE_USE_HEAD);
			}
		}

		if (!found) {
			for (int i = 0; i < scmUris.length; i++) {
				URI scmUrl = scmUris[i];
				if (getTag(scmUrl) != null) {
					head = false;
					break;
				}
			}
		}
		useHead.setSelection(head);
		versions.setSelection(!head);

		if (scmUris != null) {
			bundlesViewer.setInput(scmUris);
			updateCount();
		}

		// TODO:
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.CVS_SCM_URL_IMPORT_PAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IScmUrlImportWizardPage#finish()
	 */
	public boolean finish() {
		boolean head = false;
		if (getControl() != null) {
			head = useHead.getSelection();
			// store settings
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				settings.put(CVS_PAGE_USE_HEAD, head);
			}
		} else {
			// use whatever was used last time
			IDialogSettings settings = getWizard().getDialogSettings();
			if (settings != null) {
				head = settings.getBoolean(CVS_PAGE_USE_HEAD);
			}
		}

		if (head) {
			// modify tags on bundle import descriptions
			for (int i = 0; i < scmUris.length; i++) {
				URI scmUri = scmUris[i];
				scmUris[i] = removeTag(scmUri);
			}
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IScmUrlImportWizardPage#getSelection()
	 */
	public URI[] getSelection() {
		return scmUris;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.IScmUrlImportWizardPage#getSelection()
	 */
	public void setSelection(URI[] scmUris) {
		this.scmUris = scmUris;
		// fill viewer
		if (bundlesViewer != null) {
			bundlesViewer.setInput(scmUris);
			updateCount();
		}
	}

	/**
	 * Updates the count of bundles that will be imported
	 */
	private void updateCount() {
		counterLabel.setText(NLS.bind(CVSUIMessages.CVSScmUrlImportWizardPage_4, new Integer(scmUris.length)));
		counterLabel.getParent().layout();
	}

	private static String getProject(URI scmUri) {
		// TODO: remove once bug 332732 is fixed
		CVSURI cvsUri = CVSURI.fromUri(scmUri);
		if (cvsUri.getProjectName() == null)
			return cvsUri.getPath().lastSegment();
		return cvsUri.getProjectName();
	}

	private static String getTag(URI scmUri) {
		return CVSURI.fromUri(scmUri).getTag().getName();
	}
	
	private static URI removeTag(URI scmUri) {
		// TODO: move to CVSURI
		StringBuffer sb = new StringBuffer();
		sb.append(scmUri.getScheme()).append(':');
		String ssp = scmUri.getSchemeSpecificPart();
		int j = ssp.indexOf(';');
		if (j != -1) {
			sb.append(ssp.substring(0, j));
			String[] params = ssp.substring(j).split(";"); //$NON-NLS-1$
			for (int k = 0; k < params.length; k++) {
				// PDE way of providing tags
				if (params[k].startsWith("tag=")) { //$NON-NLS-1$
					// remove
				} else if (params[k].startsWith("version=")) { //$NON-NLS-1$
					// remove
				} else {
					sb.append(params[k]);
				}
			}
		} else {
			sb.append(ssp);
		}
		return URI.create(sb.toString());
	}
	
	private static String getServer(URI scmUri) {
		return CVSURI.fromUri(scmUri).getRepository().getHost();
	}
	
}
