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
package resourcemapping;

import org.eclipse.core.resources.mapping.IResourceMapper;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.model.*;

public class ResourceMappingSelectionDialog extends Dialog {

	private final IResourceMapper[] mappers;
	private StructuredViewer viewer;

	protected ResourceMappingSelectionDialog(Shell parentShell, IResourceMapper[] mappers) {
		super(parentShell);
		this.mappers = mappers;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}
		
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("CVS Commit");
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite top = (Composite) super.createDialogArea(parent);
		viewer = new TreeViewer(top);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 300;
		viewer.getControl().setLayoutData(data);
		viewer.setContentProvider(new BaseWorkbenchContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.setInput(new AdaptableList(mappers));
		return top;
	}
}
