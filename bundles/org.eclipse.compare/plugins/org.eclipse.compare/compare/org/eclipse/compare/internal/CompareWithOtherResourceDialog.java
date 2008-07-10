/*******************************************************************************
 * Copyright (c) 2008 Aleksandra Wozniak and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Aleksandra Wozniak (aleksandra.k.wozniak@gmail.com) - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * This is a dialog that can invoke the compare editor on chosen files.
 * <p>
 * This class can be used as is or can be subclassed.
 * 
 * @since 3.4
 */
public class CompareWithOtherResourceDialog extends TitleAreaDialog {

	public int CLEAR_RETURN_CODE = 150; // any number != 0

	private class FileTextDragListener implements DragSourceListener {

		private InternalSection section;

		public FileTextDragListener(InternalSection section) {
			this.section = section;
		}

		public void dragFinished(DragSourceEvent event) {
			section.fileText.setText(""); //$NON-NLS-1$
		}

		public void dragSetData(DragSourceEvent event) {
			if (TextTransfer.getInstance().isSupportedType(event.dataType))
				event.data = section.fileText.getText();
		}

		public void dragStart(DragSourceEvent event) {
			if (section.fileText.getText() == null)
				event.doit = false;
		}
	}

	private class FileTextDropListener implements DropTargetListener {

		private InternalSection section;
		private ResourceTransfer resourceTransfer;
		private TextTransfer textTransfer;

		public FileTextDropListener(InternalSection section) {
			this.section = section;
			resourceTransfer = ResourceTransfer.getInstance();
			textTransfer = TextTransfer.getInstance();
		}

		public void dragEnter(DropTargetEvent event) {
			if (event.detail == DND.DROP_DEFAULT) {
				if ((event.operations & DND.DROP_COPY) != 0)
					event.detail = DND.DROP_COPY;
				else
					event.detail = DND.DROP_NONE;
			}
			for (int i = 0; i < event.dataTypes.length; i++) {
				if (resourceTransfer.isSupportedType(event.dataTypes[i])) {
					event.currentDataType = event.dataTypes[i];
					if (event.detail != DND.DROP_COPY)
						event.detail = DND.DROP_NONE;
				}
				break;
			}
		}

		public void dragLeave(DropTargetEvent event) {
		}

		public void dragOperationChanged(DropTargetEvent event) {
			if (event.detail == DND.DROP_DEFAULT) {
				if ((event.operations & DND.DROP_COPY) != 0)
					event.detail = DND.DROP_COPY;
				else
					event.detail = DND.DROP_NONE;
			}
			if (resourceTransfer.isSupportedType(event.currentDataType)) {
				if (event.detail != DND.DROP_COPY)
					event.detail = DND.DROP_NONE;
			}
		}

		public void dragOver(DropTargetEvent event) {
		}

		public void drop(DropTargetEvent event) {
			if (textTransfer.isSupportedType(event.currentDataType)) {
				String txt = (String) event.data;
				section.fileText.setText(txt);
				section.setResource(ResourcesPlugin.getWorkspace().getRoot()
						.findMember(txt));
			}
			if (resourceTransfer.isSupportedType(event.currentDataType)) {
				IResource[] files = (IResource[]) event.data;
				section.setResource(files[0]);
				section.fileText.setText(section.getResource().getFullPath()
						.toString());
			}
		}

		public void dropAccept(DropTargetEvent event) {
		}

	}

	private abstract class InternalSection {

		protected Text fileText;
		protected IResource resource;
		protected IResource[] files;
		protected String fileString = null;
		protected Group group;
		protected Button clearButton;

		public InternalSection(Composite parent) {
			createContents(parent);
		}

		public InternalSection() {
		}

		public void createContents(Composite parent) {
		}

		public IResource getResource() {
			if (resource == null) {
				if (fileString != null) { //$NON-NLS-1$
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
							.getRoot();
					resource = root.findMember(fileString);
					if (resource instanceof IWorkspaceRoot)
						return null;
					return resource;
				}
			}
			return resource;
		}

		public void setResource(IResource resource) {
			this.resource = resource;
			fileText.setText(resource.getFullPath().toString());
		}

		protected void clearResource() {
			resource = null;
			fileText.setText(""); //$NON-NLS-1$
		}

		protected void initDrag() {
			DragSource source = new DragSource(fileText, DND.DROP_MOVE
					| DND.DROP_COPY);
			Transfer[] types = new Transfer[] { TextTransfer.getInstance(),
					ResourceTransfer.getInstance() };
			source.setTransfer(types);
			source.addDragListener(new FileTextDragListener(this));
		}

		protected void initDrop() {
			DropTarget target = new DropTarget(fileText, DND.DROP_MOVE
					| DND.DROP_COPY | DND.DROP_DEFAULT);
			final TextTransfer textTransfer = TextTransfer.getInstance();
			final ResourceTransfer resourceTransfer = ResourceTransfer
					.getInstance();
			Transfer[] types = new Transfer[] { textTransfer, resourceTransfer };
			target.setTransfer(types);
			target.addDropListener(new FileTextDropListener(this));
		}

		protected void createGroup(Composite parent) {
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			group = new Group(parent, SWT.NONE);
			group.setLayout(layout);
			GridData gridData = new GridData();
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			group.setLayoutData(gridData);
		}

		protected void createFileText() {
			fileText = new Text(group, SWT.SINGLE | SWT.BORDER);
			fileText.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					if (files != null)
						resource = files[0];
					fileString = fileText.getText();
					if (fileString == "") //$NON-NLS-1$
						resource = null;
					else
						resource = getResource();
					if (okButton != null)
						okButton.setEnabled(comparePossible());
					updateErrorInfo();
				}

			});

			GridData textData = new GridData();
			textData.grabExcessHorizontalSpace = true;
			textData.horizontalAlignment = SWT.FILL;
			fileText.setLayoutData(textData);
		}

		protected void createFileLabel() {
			final Label fileLabel = new Label(group, SWT.NONE);
			fileLabel.setText(CompareMessages.CompareWithOther_fileLabel);
		}

		protected void createClearButton(Composite parent) {
			clearButton = createButton(parent, CLEAR_RETURN_CODE,
					CompareMessages.CompareWithOther_clear, false);
			clearButton.addSelectionListener(new SelectionListener() {

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}

				public void widgetSelected(SelectionEvent e) {
					clearResource();
				}

			});
		}

		abstract public void setLayoutData(GridData layoutData);

		abstract public void setText(String text);

	}

	private class InternalGroup extends InternalSection {

		public InternalGroup(Composite parent) {
			super();
			createContents(parent);
		}

		public void createContents(Composite parent) {
			createGroup(parent);
			createFileLabel();
			createFileText();
			createClearButton(group);
			initDrag();
			initDrop();
		}

		public void setText(String text) {
			group.setText(text);
		}

		public void setLayoutData(GridData layoutData) {
			group.setLayoutData(layoutData);
		}
	}

	private class InternalExpandable extends InternalSection {

		private ExpandableComposite expandable;

		public InternalExpandable(Composite parent) {
			super();
			createContents(parent);
		}

		public void createContents(Composite parent) {

			final Composite p = parent;

			expandable = new ExpandableComposite(parent, SWT.NONE,
					ExpandableComposite.TREE_NODE | ExpandableComposite.TWISTIE);

			createGroup(expandable);
			expandable.setClient(group);
			expandable.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {
					p.getShell().pack();
				}
			});

			createFileLabel();
			createFileText();
			createClearButton(group);
			initDrag();
			initDrop();
		}

		public void setText(String text) {
			expandable.setText(text);
			group.setText(text);
		}

		public void setLayoutData(GridData layoutData) {
			expandable.setLayoutData(layoutData);
		}

	}

	private Button okButton, cancelButton, clearAllButton;
	private InternalGroup rightPanel, leftPanel;
	private InternalExpandable ancestorPanel;
	private ISelection fselection;

	/**
	 * Creates the dialog.
	 * 
	 * @param shell
	 *            a shell
	 * @param selection
	 *            if the selection is not null, it will be set as initial files
	 *            for comparison
	 * @since 3.4
	 */
	protected CompareWithOtherResourceDialog(Shell shell, ISelection selection) {
		super(shell);
		setShellStyle(SWT.MODELESS | SWT.RESIZE | SWT.MAX);
		fselection = selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
	 * .Composite)
	 */
	protected Control createDialogArea(Composite parent) {

		Composite mainPanel = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		mainPanel.setLayout(layout);
		mainPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		ancestorPanel = new InternalExpandable(mainPanel);
		ancestorPanel.setText(CompareMessages.CompareWithOther_ancestor);
		GridData ancestorGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		ancestorGD.horizontalSpan = 2;
		ancestorPanel.setLayoutData(ancestorGD);

		rightPanel = new InternalGroup(mainPanel);
		rightPanel.setText(CompareMessages.CompareWithOther_rightPanel);
		GridData rightGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		rightPanel.setLayoutData(rightGD);

		leftPanel = new InternalGroup(mainPanel);
		leftPanel.setText(CompareMessages.CompareWithOther_leftPanel);
		GridData leftGD = new GridData(SWT.FILL, SWT.FILL, true, true);
		leftPanel.setLayoutData(leftGD);

		setSelection(fselection);
		getShell().setText(CompareMessages.CompareWithOther_dialogTitle);
		setTitle(CompareMessages.CompareWithOther_dialogMessage);

		return mainPanel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse
	 * .swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, OK, IDialogConstants.OK_LABEL, false);
		okButton.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {

				IResource[] resources;

				IResource rightResource = rightPanel.getResource();
				IResource leftResource = leftPanel.getResource();
				IResource ancestorResource = ancestorPanel.getResource();
				if (ancestorResource == null)
					resources = new IResource[] { leftResource, rightResource };
				else
					resources = new IResource[] { ancestorResource,
							leftResource, rightResource };

				if (CompareAction.isEnabled(resources))
					CompareAction.run(resources);
			}

		});
		okButton.setEnabled(comparePossible());

		cancelButton = createButton(parent, CANCEL,
				IDialogConstants.CANCEL_LABEL, true);
		cancelButton.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
			}

		});

		clearAllButton = createButton(parent, CLEAR_RETURN_CODE,
				CompareMessages.CompareWithOther_clearAll, false);
		clearAllButton.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			public void widgetSelected(SelectionEvent e) {
				leftPanel.clearResource();
				rightPanel.clearResource();
				ancestorPanel.clearResource();
			}

		});
	}

	private void setSelection(ISelection selection) {
		IResource[] selectedResources = Utilities.getResources(selection);
		switch (selectedResources.length) {
		case 0:
			break;
		case 1:
			leftPanel.setResource(selectedResources[0]);
			break;
		case 2:
			leftPanel.setResource(selectedResources[0]);
			rightPanel.setResource(selectedResources[1]);
			break;
		case 3:
			ancestorPanel.setResource(selectedResources[0]);
			ancestorPanel.expandable.setExpanded(true);
			leftPanel.setResource(selectedResources[1]);
			rightPanel.setResource(selectedResources[2]);
			break;
		}
	}

	private boolean comparePossible() {
		IResource[] resources;
		if (ancestorPanel.resource == null)
			resources = new IResource[] { leftPanel.resource,
					rightPanel.resource };
		else
			resources = new IResource[] { ancestorPanel.resource,
					leftPanel.resource, rightPanel.resource };

		ResourceCompareInput r = new ResourceCompareInput(
				new CompareConfiguration());
		return r.isEnabled(resources);
	}

	private void updateErrorInfo() {
		if (leftPanel.resource == null || rightPanel.resource == null)
			setMessage(CompareMessages.CompareWithOther_error_empty,
					IMessageProvider.ERROR);
		else if (!comparePossible())
			setMessage(CompareMessages.CompareWithOther_error_not_comparable,
					IMessageProvider.ERROR);
		else
			setMessage(null);
	}
}
