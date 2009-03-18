/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corportation - initial API and implementation
 *     Krzysztof Poglodzinski (intuicje@gmail.com) - Bug 71374 [Patch] Generate diff from "Compare With"
 *     Mariusz Tanski (mariusztanski@gmail.com) - Bug 71374 [Patch] Generate diff from "Compare With"
 *     Kacper Zdanowicz (kacper.zdanowicz@gmail.com) - Bug 71374 [Patch] Generate diff from "Compare With"
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Wizard for generating patch from currently open comparison. Major part of the
 * code is taken from original GenerateDiffFileWizard from CVS/UI.
 */
public class GenerateDiffFileWizard extends Wizard {

	//The initial size of this wizard.
	protected final static int INITIAL_WIDTH = 300;
	protected final static int INITIAL_HEIGHT = 350;

	public static void run(DocumentMerger merger, Shell shell, boolean rightToLeft) {
		final String title = CompareMessages.GenerateLocalDiff_title;
		final GenerateDiffFileWizard wizard = new GenerateDiffFileWizard(merger, rightToLeft);
		wizard.setWindowTitle(title);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setMinimumPageSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		dialog.open();
	}

	protected class DirectionSelectionPage extends WizardPage {

		public final static int LEFT_OPTION = 1;
		public final static int RIGHT_OPTION = 2;

		private Button fromLeftOption;
		private Button fromRightOption;

		private Label fromLeftLabel;
		private Label fromRightLabel;

		private RadioButtonGroup fromRadioGroup = new RadioButtonGroup();

		protected DirectionSelectionPage(String pageName, String title,
				ImageDescriptor titleImage) {
			super(pageName, title, titleImage);
		}

		public void createControl(Composite parent) {
			Composite composite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();

			initializeDialogUnits(composite);

			layout.marginHeight = IDialogConstants.VERTICAL_MARGIN;
			layout.marginWidth = IDialogConstants.HORIZONTAL_MARGIN;
			layout.verticalSpacing = IDialogConstants.VERTICAL_SPACING*2;
			layout.horizontalSpacing = IDialogConstants.HORIZONTAL_SPACING*2;
			layout.numColumns = 2;

			composite.setLayout(layout);
			composite.setLayoutData(new GridData());
			setControl(composite);

			fromLeftLabel = new Label(composite, SWT.HORIZONTAL|SWT.SHADOW_OUT);
			fromLeftLabel.setText(CompareMessages.GenerateDiffFileWizard_Left);
			fromLeftLabel.setLayoutData(new GridData(
					SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));

			fromLeftOption = new Button(composite, SWT.RADIO);
			fromLeftOption.setText(leftPath);
			fromLeftOption.setLayoutData(new GridData(
					SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));

			fromRightLabel = new Label(composite, SWT.HORIZONTAL|SWT.SHADOW_OUT);
			fromRightLabel.setText(CompareMessages.GenerateDiffFileWizard_Right);
			fromRightLabel.setLayoutData(new GridData(
					SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));

			fromRightOption = new Button(composite, SWT.RADIO);
			fromRightOption.setText(rightPath);
			fromRightOption.setLayoutData(new GridData(
					SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));

			fromRadioGroup.add(LEFT_OPTION, fromLeftOption);
			fromRadioGroup.add(RIGHT_OPTION, fromRightOption);

			Dialog.applyDialogFont(parent);

			// Add listeners
			fromLeftOption.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fromRadioGroup.setSelection(LEFT_OPTION, true);
					targetFileEdited = false;
				}
			});

			fromRightOption.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					fromRadioGroup.setSelection(RIGHT_OPTION, true);
					targetFileEdited = false;
				}
			});

			fromRadioGroup.setSelection(rightToLeft ? RIGHT_OPTION
					: LEFT_OPTION, true);
		}

		public boolean isRightToLeft() {
			return fromRadioGroup.getSelected() == LEFT_OPTION;
		}

	}

	/**
	 * Page to select a patch file. Overriding validatePage was necessary to
	 * allow entering a file name that already exists.
	 */
	protected class LocationPage extends WizardPage {

		/**
		 * The possible locations to save a patch.
		 */
		public final static int CLIPBOARD = 1;
		public final static int FILESYSTEM = 2;
		public final static int WORKSPACE = 3;

		/**
		 * GUI controls for clipboard (cp), filesystem (fs) and workspace (ws).
		 */
		private Button cpRadio;

		private Button fsRadio;
		protected Text fsPathText;
		private Button fsBrowseButton;
		private boolean fsBrowsed = false;

		private Button wsRadio;
		protected Text wsPathText;
		private Button wsBrowseButton;
		private boolean wsBrowsed = false;

		protected boolean pageValid;
		protected IContainer wsSelectedContainer;
		protected IPath[] foldersToCreate;
		protected int selectedLocation;

		/**
		 * The default values store used to initialize the selections.
		 */
		private final DefaultValuesStore store;


		class LocationPageContentProvider extends BaseWorkbenchContentProvider {
			//Never show closed projects
			boolean showClosedProjects=false;

			public Object[] getChildren(Object element) {
				if (element instanceof IWorkspace) {
					// check if closed projects should be shown
					IProject[] allProjects = ((IWorkspace) element).getRoot().getProjects();
					if (showClosedProjects)
						return allProjects;

					ArrayList accessibleProjects = new ArrayList();
					for (int i = 0; i < allProjects.length; i++) {
						if (allProjects[i].isOpen()) {
							accessibleProjects.add(allProjects[i]);
						}
					}
					return accessibleProjects.toArray();
				}

				return super.getChildren(element);
			}
		}

		class WorkspaceDialog extends TitleAreaDialog {

			protected TreeViewer wsTreeViewer;
			protected Text wsFilenameText;
			protected Image dlgTitleImage;

			private boolean modified = false;

			public WorkspaceDialog(Shell shell) {
				super(shell);
			}

			protected Control createContents(Composite parent) {
				Control control = super.createContents(parent);
				setTitle(CompareMessages.WorkspacePatchDialogTitle);
				setMessage(CompareMessages.WorkspacePatchDialogDescription);
				//create title image
				dlgTitleImage = CompareUIPlugin.getImageDescriptor(ICompareUIConstants.IMG_WIZBAN_DIFF).createImage();
				setTitleImage(dlgTitleImage);

				return control;
			}

			protected Control createDialogArea(Composite parent) {
				Composite parentComposite = (Composite) super.createDialogArea(parent);

				// create a composite with standard margins and spacing
				Composite composite = new Composite(parentComposite, SWT.NONE);
				GridLayout layout = new GridLayout();
				layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
				layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
				layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
				layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
				composite.setLayout(layout);
				composite.setLayoutData(new GridData(GridData.FILL_BOTH));
				composite.setFont(parentComposite.getFont());

				getShell().setText(CompareMessages.GenerateDiffFileWizard_9);

				wsTreeViewer = new TreeViewer(composite, SWT.BORDER);
				final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.widthHint= 550;
				gd.heightHint= 250;
				wsTreeViewer.getTree().setLayoutData(gd);

				wsTreeViewer.setContentProvider(new LocationPageContentProvider());
				wsTreeViewer.setComparator(new ResourceComparator(ResourceComparator.NAME));
				wsTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
				wsTreeViewer.setInput(ResourcesPlugin.getWorkspace());

				//Open to whatever is selected in the workspace field
				IPath existingWorkspacePath = new Path(wsPathText.getText());
				if (existingWorkspacePath != null){
					//Ensure that this workspace path is valid
					IResource selectedResource = ResourcesPlugin.getWorkspace().getRoot().findMember(existingWorkspacePath);
					if (selectedResource != null) {
						wsTreeViewer.expandToLevel(selectedResource, 0);
						wsTreeViewer.setSelection(new StructuredSelection(selectedResource));
					}
				}

				final Composite group = new Composite(composite, SWT.NONE);
				layout = new GridLayout(2, false);
				layout.marginWidth = 0;
				group.setLayout(layout);
				group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

				final Label label = new Label(group, SWT.NONE);
				label.setLayoutData(new GridData());
				label.setText(CompareMessages.Fi_le_name__9);

				wsFilenameText = new Text(group,SWT.BORDER);
				wsFilenameText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

				setupListeners();

				return parent;
			}

			protected Button createButton(Composite parent, int id,
					String label, boolean defaultButton) {
				Button button = super.createButton(parent, id, label,
						defaultButton);
				if (id == IDialogConstants.OK_ID) {
					button.setEnabled(false);
				}
				return button;
			}

			private void validateDialog() {
				String fileName = wsFilenameText.getText();

				if (fileName.equals("")) { //$NON-NLS-1$
					if (modified) {
						setErrorMessage(CompareMessages.GenerateDiffFileWizard_2);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
						return;
					} else {
						setErrorMessage(null);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
						return;
					}
				}
				// make sure that the filename is valid
				if (!(ResourcesPlugin.getWorkspace().validateName(fileName,
						IResource.FILE)).isOK() && modified) {
					setErrorMessage(CompareMessages.GenerateDiffFileWizard_5);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					return;
				}

				// Make sure that a container has been selected
				if (getSelectedContainer() == null) {
					setErrorMessage(CompareMessages.GenerateDiffFileWizard_0);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
					return;
				} else {
					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IPath fullPath = wsSelectedContainer.getFullPath().append(
							fileName);
					if (workspace.getRoot().getFolder(fullPath).exists()) {
						setErrorMessage(CompareMessages.GenerateDiffFileWizard_FolderExists);
						getButton(IDialogConstants.OK_ID).setEnabled(false);
						return;
					}
				}

				setErrorMessage(null);
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}

			protected void okPressed() {
				IFile file = wsSelectedContainer.getFile(new Path(
						wsFilenameText.getText()));
				if (file != null)
					wsPathText.setText(file.getFullPath().toString());

				validatePage();
				super.okPressed();
			}

			private IContainer getSelectedContainer() {
				Object obj = ((IStructuredSelection)wsTreeViewer.getSelection()).getFirstElement();
				if (obj instanceof IContainer) {
					wsSelectedContainer = (IContainer) obj;
				} else if (obj instanceof IFile) {
					wsSelectedContainer = ((IFile) obj).getParent();
				}
				return wsSelectedContainer;
			}

			protected void cancelPressed() {
				validatePage();
				super.cancelPressed();
			}

			public boolean close() {
				if (dlgTitleImage != null)
					dlgTitleImage.dispose();
				return super.close();
			}

			void setupListeners(){
				wsTreeViewer.addSelectionChangedListener(
						new ISelectionChangedListener() {
							public void selectionChanged(SelectionChangedEvent event) {
								IStructuredSelection s = (IStructuredSelection)event.getSelection();
								Object obj=s.getFirstElement();
								if (obj instanceof IContainer)
									wsSelectedContainer = (IContainer) obj;
								else if (obj instanceof IFile){
									IFile tempFile = (IFile) obj;
									wsSelectedContainer = tempFile.getParent();
									wsFilenameText.setText(tempFile.getName());
								}
								validateDialog();
							}
						});

				wsTreeViewer.addDoubleClickListener(
						new IDoubleClickListener() {
							public void doubleClick(DoubleClickEvent event) {
								ISelection s= event.getSelection();
								if (s instanceof IStructuredSelection) {
									Object item = ((IStructuredSelection)s).getFirstElement();
									if (wsTreeViewer.getExpandedState(item))
										wsTreeViewer.collapseToLevel(item, 1);
									else
										wsTreeViewer.expandToLevel(item, 1);
								}
								validateDialog();
							}
						});

				wsFilenameText.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						modified = true;
						validateDialog();
					}
				});
			}
		}

		LocationPage(String pageName, String title, ImageDescriptor image, DefaultValuesStore store) {
			super(pageName, title, image);
			setPageComplete(false);
			this.store= store;
		}

		/**
		 * Allow the user to finish if a valid file has been entered.
		 */
		protected boolean validatePage() {
			switch (selectedLocation) {
			case WORKSPACE:
				pageValid= validateWorkspaceLocation();
				break;
			case FILESYSTEM:
				pageValid= validateFilesystemLocation();
				break;
			case CLIPBOARD:
				pageValid= true;
				break;
			}

			/**
			 * Avoid draw flicker by clearing error message
			 * if all is valid.
			 */
			if (pageValid) {
				setMessage(null);
				setErrorMessage(null);
			}
			setPageComplete(pageValid);
			return pageValid;
		}

		/**
		 * The following conditions must hold for the file system location
		 * to be valid:
		 * - the path must be valid and non-empty
		 * - the path must be absolute
		 * - the specified file must be of type file
		 * - the parent must exist (new folders can be created via the browse button)
		 */
		private boolean validateFilesystemLocation() {
			final String pathString= fsPathText.getText().trim();
			if (pathString.length() == 0 || !new Path("").isValidPath(pathString)) { //$NON-NLS-1$
				if (fsBrowsed)
					setErrorMessage(CompareMessages.GenerateDiffFileWizard_0);
				else
					setErrorMessage(CompareMessages.GenerateDiffFileWizard_browseFilesystem);
				return false;
			}

			final File file= new File(pathString);
			if (!file.isAbsolute()) {
				setErrorMessage(CompareMessages.GenerateDiffFileWizard_0);
				return false;
			}

			if (file.isDirectory()) {
				setErrorMessage(CompareMessages.GenerateDiffFileWizard_2);
				return false;
			}

			if (pathString.endsWith("/") || pathString.endsWith("\\")) {  //$NON-NLS-1$//$NON-NLS-2$
				setErrorMessage(CompareMessages.GenerateDiffFileWizard_3);
				return false;
			}

			final File parent= file.getParentFile();
			if (!(parent.exists() && parent.isDirectory())) {
				setErrorMessage(CompareMessages.GenerateDiffFileWizard_3);
				return false;
			}
			return true;
		}

		/**
		 * The following conditions must hold for the file system location to be valid:
		 * - a parent must be selected in the workspace tree view
		 * - the resource name must be valid
		 */
		private boolean validateWorkspaceLocation() {
			//make sure that the field actually has a filename in it - making
			//sure that the user has had a chance to browse the workspace first
			if (wsPathText.getText().equals("")){ //$NON-NLS-1$
				if (selectedLocation ==WORKSPACE && wsBrowsed)
					setErrorMessage(CompareMessages.GenerateDiffFileWizard_5);
				else
					setErrorMessage(CompareMessages.GenerateDiffFileWizard_4);
				return false;
			}

			//Make sure that all the segments but the last one (i.e. project + all
			//folders) exist - file doesn't have to exist. It may have happened that
			//some folder refactoring has been done since this path was last saved.
			//
			// The path will always be in format project/{folders}*/file - this
			// is controlled by the workspace location dialog and by
			// validatePath method when path has been entered manually.


			IPath pathToWorkspaceFile = new Path(wsPathText.getText());
			IStatus status = ResourcesPlugin.getWorkspace().validatePath(wsPathText.getText(), IResource.FILE);
			if (status.isOK()) {
				//Trim file name from path
				IPath containerPath = pathToWorkspaceFile.removeLastSegments(1);
				IResource container =ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
				if (container == null) {
					if (selectedLocation == WORKSPACE)
						setErrorMessage(CompareMessages.GenerateDiffFileWizard_4);
					return false;
				} else if (!container.isAccessible()) {
					if (selectedLocation == WORKSPACE)
						setErrorMessage(CompareMessages.GenerateDiffFileWizard_ProjectClosed);
					return false;
				} else {
					if (ResourcesPlugin.getWorkspace().getRoot().getFolder(
							pathToWorkspaceFile).exists()) {
						setErrorMessage(CompareMessages.GenerateDiffFileWizard_FolderExists);
						return false;
					}
				}
			} else {
				setErrorMessage(status.getMessage());
				return false;
			}

			return true;
		}

		/**
		 * Answers a full path to a file system file or <code>null</code> if the user
		 * selected to save the patch in the clipboard.
		 */
		public File getFile() {
			if (pageValid && selectedLocation == FILESYSTEM) {
				return new File(fsPathText.getText().trim());
			}
			if (pageValid && selectedLocation == WORKSPACE) {
				final String filename= wsPathText.getText().trim();
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				final IFile file= root.getFile(new Path(filename));
				return file.getLocation().toFile();
			}
			return null;
		}

		/**
		 * Answers the workspace string entered in the dialog or <code>null</code> if the user
		 * selected to save the patch in the clipboard or file system.
		 */
		public String getWorkspaceLocation() {

			if (pageValid && selectedLocation == WORKSPACE) {
				final String filename= wsPathText.getText().trim();
				return filename;
			}
			return null;
		}

		/**
		 * Get the selected workspace resource if the patch is to be saved in the
		 * workspace, or null otherwise.
		 */
		public IResource getResource() {
			if (pageValid && selectedLocation == WORKSPACE) {
				IPath pathToWorkspaceFile = new Path(wsPathText.getText().trim());
				//Trim file name from path
				IPath containerPath = pathToWorkspaceFile.removeLastSegments(1);
				return ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
			}
			return null;
		}

		/**
		 * Allow the user to chose to save the patch to the workspace or outside
		 * of the workspace.
		 */
		public void createControl(Composite parent) {

			final Composite composite= new Composite(parent, SWT.NULL);
			composite.setLayout(new GridLayout());
			setControl(composite);
			initializeDialogUnits(composite);

			// set F1 help
			// TODO: PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PATCH_SELECTION_PAGE);

			//Create a location group
			setupLocationControls(composite);

			initializeDefaultValues();

			Dialog.applyDialogFont(parent);

			validatePage();

			updateEnablements();
			setupListeners();
		}

		/**
		 * Setup the controls for the location.
		 */
		private void setupLocationControls(final Composite parent) {
			final Composite composite = new Composite(parent, SWT.NULL);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 3;
			composite.setLayout(gridLayout);
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// clipboard
			GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
			gd.horizontalSpan = 3;
			cpRadio = new Button(composite, SWT.RADIO);
			cpRadio.setText(CompareMessages.Save_To_Clipboard_2);
			cpRadio.setLayoutData(gd);

			// filesystem
			fsRadio = new Button(composite, SWT.RADIO);
			fsRadio.setText(CompareMessages.Save_In_File_System_3);

			fsPathText = new Text(composite, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			fsPathText.setLayoutData(gd);

			fsBrowseButton = new Button(composite, SWT.PUSH);
			fsBrowseButton.setText(CompareMessages.Browse____4);
			GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			Point minSize = fsBrowseButton.computeSize(SWT.DEFAULT,
					SWT.DEFAULT, true);
			data.widthHint = Math.max(widthHint, minSize.x);
			fsBrowseButton.setLayoutData(data);

			// workspace
			wsRadio = new Button(composite, SWT.RADIO);
			wsRadio.setText(CompareMessages.Save_In_Workspace_7);

			wsPathText = new Text(composite, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			wsPathText.setLayoutData(gd);

			wsBrowseButton = new Button(composite, SWT.PUSH);
			wsBrowseButton.setText(CompareMessages.Browse____4);
			data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
			minSize = fsBrowseButton
			.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
			data.widthHint = Math.max(widthHint, minSize.x);
			wsBrowseButton.setLayoutData(data);

			// change the cpRadio layout to be of the same height as other rows' layout
			((GridData)cpRadio.getLayoutData()).heightHint = minSize.y;
		}

		/**
		 * Initialize the controls with the saved default values which are
		 * obtained from the DefaultValuesStore.
		 */
		private void initializeDefaultValues() {

			selectedLocation= store.getLocationSelection();

			updateRadioButtons();

			/**
			 * Text fields.
			 */
			// We need to ensure that we have a valid workspace path - user
			//could have altered workspace since last time this was saved
			wsPathText.setText(store.getWorkspacePath());
			if(!validateWorkspaceLocation()) {
				wsPathText.setText(""); //$NON-NLS-1$

				//Don't open wizard with an error - instead change selection
				//to clipboard
				if (selectedLocation == WORKSPACE){
					//clear the error message caused by the workspace not having
					//any workspace path entered
					setErrorMessage(null);
					selectedLocation=CLIPBOARD;
					updateRadioButtons();
				}
			}
			// Do the same thing for the filesystem field
			fsPathText.setText(store.getFilesystemPath());
			if (!validateFilesystemLocation()) {
				fsPathText.setText(""); //$NON-NLS-1$
				if (selectedLocation == FILESYSTEM) {
					setErrorMessage(null);
					selectedLocation = CLIPBOARD;
					updateRadioButtons();
				}
			}

		}

		private void updateRadioButtons() {
			/**
			 * Radio buttons
			 */
			cpRadio.setSelection(selectedLocation == CLIPBOARD);
			fsRadio.setSelection(selectedLocation == FILESYSTEM);
			wsRadio.setSelection(selectedLocation == WORKSPACE);
		}

		/**
		 * Setup all the listeners for the controls.
		 */
		private void setupListeners() {

			cpRadio.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedLocation= CLIPBOARD;
					validatePage();
					updateEnablements();
				}
			});
			fsRadio.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedLocation= FILESYSTEM;
					validatePage();
					updateEnablements();
				}
			});

			wsRadio.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					selectedLocation= WORKSPACE;
					validatePage();
					updateEnablements();
				}
			});

			ModifyListener pathTextModifyListener = new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			};
			fsPathText.addModifyListener(pathTextModifyListener);
			wsPathText.addModifyListener(pathTextModifyListener);

			fsBrowseButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					final FileDialog dialog = new FileDialog(getShell(), SWT.PRIMARY_MODAL | SWT.SAVE);
					if (pageValid) {
						final File file= new File(fsPathText.getText());
						dialog.setFilterPath(file.getParent());
					}
					dialog.setText(CompareMessages.Save_Patch_As_5);
					dialog.setFileName(CompareMessages.patch_txt_6);
					final String path = dialog.open();
					fsBrowsed = true;
					if (path != null) {
						fsPathText.setText(new Path(path).toOSString());
					}
					validatePage();
				}
			});



			wsBrowseButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					final WorkspaceDialog dialog = new WorkspaceDialog(getShell());
					wsBrowsed = true;
					dialog.open();
					validatePage();
				}
			});

		}

		/**
		 * Enable and disable controls based on the selected radio button.
		 */
		public void updateEnablements() {
			fsBrowseButton.setEnabled(selectedLocation == FILESYSTEM);
			fsPathText.setEnabled(selectedLocation == FILESYSTEM);
			if (selectedLocation == FILESYSTEM)
				fsBrowsed=false;
			wsPathText.setEnabled(selectedLocation == WORKSPACE);
			wsBrowseButton.setEnabled(selectedLocation == WORKSPACE);
			if (selectedLocation == WORKSPACE)
				wsBrowsed=false;
		}

		public int getSelectedLocation() {
			return selectedLocation;
		}

	}

	/**
	 * Page to select the options for creating the patch.
	 */
	private class OptionsPage extends WizardPage {

		/**
		 * The possible file format to save a patch.
		 */
		public final static int FORMAT_UNIFIED = 1;
		public final static int FORMAT_CONTEXT = 2;
		public final static int FORMAT_STANDARD = 3;

		/**
    	The possible root of the patch
		 */
		public final static int ROOT_WORKSPACE = 1;
		public final static int ROOT_PROJECT = 2;
		public final static int ROOT_SELECTION = 3;
		public final static int ROOT_CUSTOM = 4;

		protected boolean initialized = false;

		protected Button unifiedDiffOption;
		protected Button unified_workspaceRelativeOption; //multi-patch format
		protected Button unified_projectRelativeOption; //full project path
		protected Button unified_selectionRelativeOption; //use path of whatever is selected
		protected Button contextDiffOption;
		protected Button regularDiffOption;

		protected Group unifiedGroup;
		protected Group diffTypeGroup;

		protected final RadioButtonGroup diffTypeRadioGroup = new RadioButtonGroup();
		protected final RadioButtonGroup unifiedRadioGroup = new RadioButtonGroup();

		private final DefaultValuesStore store;

		/**
		 * Constructor for PatchFileCreationOptionsPage.
		 */
		protected OptionsPage(String pageName, String title, ImageDescriptor titleImage, DefaultValuesStore store) {
			super(pageName, title, titleImage);
			this.store = store;
		}

		/*
		 * @see IDialogPage#createControl(Composite)
		 */
		public void createControl(Composite parent) {
			Composite composite= new Composite(parent, SWT.NULL);
			GridLayout layout= new GridLayout();
			composite.setLayout(layout);
			composite.setLayoutData(new GridData());
			setControl(composite);

			// set F1 help
			// TODO: PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PATCH_OPTIONS_PAGE);

			diffTypeGroup = new Group(composite, SWT.NONE);
			layout = new GridLayout();
			layout.marginHeight = 0;
			diffTypeGroup.setLayout(layout);
			GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
			diffTypeGroup.setLayoutData(data);
			diffTypeGroup.setText(CompareMessages.Diff_output_format_12);

			unifiedDiffOption = new Button(diffTypeGroup, SWT.RADIO);
			unifiedDiffOption.setText(CompareMessages.Unified__format_required_by_Compare_With_Patch_feature__13);

			contextDiffOption = new Button(diffTypeGroup, SWT.RADIO);
			contextDiffOption.setText(CompareMessages.Context_14);
			regularDiffOption = new Button(diffTypeGroup, SWT.RADIO);
			regularDiffOption.setText(CompareMessages.Standard_15);

			diffTypeRadioGroup.add(FORMAT_UNIFIED, unifiedDiffOption);
			diffTypeRadioGroup.add(FORMAT_CONTEXT, contextDiffOption);
			diffTypeRadioGroup.add(FORMAT_STANDARD, regularDiffOption);

			// Unified Format Options
			unifiedGroup = new Group(composite, SWT.None);
			layout = new GridLayout();
			layout.numColumns = 2;
			unifiedGroup.setLayout(layout);
			data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
			unifiedGroup.setLayoutData(data);
			unifiedGroup.setText(CompareMessages.GenerateDiffFileWizard_10);

			unified_workspaceRelativeOption = new Button(unifiedGroup, SWT.RADIO);
			unified_workspaceRelativeOption.setText(CompareMessages.GenerateDiffFileWizard_6);
			unified_workspaceRelativeOption.setLayoutData(new GridData(
					SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

			unified_projectRelativeOption = new Button(unifiedGroup, SWT.RADIO);
			unified_projectRelativeOption.setText(CompareMessages.GenerateDiffFileWizard_7);
			unified_projectRelativeOption.setLayoutData(new GridData(
					SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));

			unified_selectionRelativeOption = new Button(unifiedGroup, SWT.RADIO);
			unified_selectionRelativeOption.setText(CompareMessages.GenerateDiffFileWizard_8);
			unified_selectionRelativeOption.setLayoutData(new GridData(
					SWT.BEGINNING, SWT.CENTER, false, true, 2, 1));

			unifiedRadioGroup.add(ROOT_WORKSPACE, unified_workspaceRelativeOption);
			unifiedRadioGroup.add(ROOT_PROJECT, unified_projectRelativeOption);
			unifiedRadioGroup.add(ROOT_SELECTION, unified_selectionRelativeOption);

			Dialog.applyDialogFont(parent);

			initializeDefaultValues();

			//add listeners
			unifiedDiffOption.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setEnableUnifiedGroup(true);
					updateEnablements();
					diffTypeRadioGroup.setSelection(FORMAT_UNIFIED, false);
				}
			});

			contextDiffOption.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setEnableUnifiedGroup(false);
					updateEnablements();
					diffTypeRadioGroup.setSelection(FORMAT_CONTEXT, false);
				}
			});

			regularDiffOption.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setEnableUnifiedGroup(false);
					updateEnablements();
					diffTypeRadioGroup.setSelection(FORMAT_STANDARD, false);
				}
			});

			unified_workspaceRelativeOption
			.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					unifiedRadioGroup.setSelection(ROOT_WORKSPACE, false);
				}
			});

			unified_projectRelativeOption
			.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					unifiedRadioGroup.setSelection(ROOT_PROJECT, false);
				}
			});

			unified_selectionRelativeOption
			.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					unifiedRadioGroup.setSelection(ROOT_SELECTION, false);
				}
			});

			updateEnablements();

			performSpecificActions();

			// update selection
			diffTypeRadioGroup.selectEnabledOnly();
			unifiedRadioGroup.selectEnabledOnly();
		}

		protected void performSpecificActions() {
			return;
		}

		public int getFormatSelection() {
			return diffTypeRadioGroup.getSelected();
		}

		public int getRootSelection() {
			return unifiedRadioGroup.getSelected();
		}

		public String getPath() {
			return unified_customRelativeText.getText();
		}

		public Group getUnifiedGroup() {
			return unifiedGroup;
		}

		public RadioButtonGroup getUnifiedRadioGroup() {
			return unifiedRadioGroup;
		}

		private void initializeDefaultValues() {
			// Radio buttons for format
			diffTypeRadioGroup.setSelection(store.getFormatSelection(), true);
			// Radio buttons for patch root
			unifiedRadioGroup.setSelection(store.getRootSelection(), true);
		}

		protected void updateEnablements() {
			if(diffTypeRadioGroup.selected != FORMAT_UNIFIED)
				setEnableUnifiedGroup(false);
		}

		public void updateDiffTypeEnablements(int[] disabled, int enabled) {
			diffTypeRadioGroup.setEnablement(false, disabled, enabled);
		}

		public void updateUnifiedEnablements(int[] disabled, int enabled) {
			unifiedRadioGroup.setEnablement(false, disabled, enabled);
		}

		protected void setEnableUnifiedGroup(boolean enabled) {
			unifiedRadioGroup.setEnablement(enabled, new int[] {
					ROOT_WORKSPACE, ROOT_PROJECT, ROOT_SELECTION });
		}

	}

	/**
	 * Class to retrieve and store the default selected values.
	 */
	protected final class DefaultValuesStore {

		private static final String PREF_LAST_SELECTION = "org.eclipse.compare.internal.GenerateDiffFileWizard.PatchFileSelectionPage.lastselection"; //$NON-NLS-1$
		private static final String PREF_LAST_FS_PATH = "org.eclipse.compare.internal.GenerateDiffFileWizard.PatchFileSelectionPage.filesystem.path"; //$NON-NLS-1$
		private static final String PREF_LAST_WS_PATH = "org.eclipse.compare.internal.GenerateDiffFileWizard.PatchFileSelectionPage.workspace.path"; //$NON-NLS-1$
		private static final String PREF_LAST_AO_FORMAT = "org.eclipse.compare.internal.GenerateDiffFileWizard.OptionsPage.diff.format"; //$NON-NLS-1$
		private static final String PREF_LAST_AO_ROOT = "org.eclipse.compare.internal.GenerateDiffFileWizard.OptionsPage.patch.root"; //$NON-NLS-1$

		private final IDialogSettings dialogSettings;

		public DefaultValuesStore() {
			dialogSettings= CompareUIPlugin.getDefault().getDialogSettings();
		}

		public int getLocationSelection() {
			int value= LocationPage.CLIPBOARD;
			try {
				value= dialogSettings.getInt(PREF_LAST_SELECTION);
			} catch (NumberFormatException e) {
			}

			switch (value) {
			case LocationPage.FILESYSTEM:
			case LocationPage.WORKSPACE:
			case LocationPage.CLIPBOARD:
				return value;
			default:
				return LocationPage.CLIPBOARD;
			}
		}

		public String getFilesystemPath() {
			final String path= dialogSettings.get(PREF_LAST_FS_PATH);
			return path != null ? path : "";  //$NON-NLS-1$
		}

		public String getWorkspacePath() {
			final String path= dialogSettings.get(PREF_LAST_WS_PATH);
			return path != null ? path : ""; //$NON-NLS-1$
		}


		public int getFormatSelection() {
			int value = OptionsPage.FORMAT_UNIFIED;
			try {
				value = dialogSettings.getInt(PREF_LAST_AO_FORMAT);
			} catch (NumberFormatException e) {
			}

			switch (value) {
			case OptionsPage.FORMAT_UNIFIED:
			case OptionsPage.FORMAT_CONTEXT:
			case OptionsPage.FORMAT_STANDARD:
				return value;
			default:
				return OptionsPage.FORMAT_UNIFIED;
			}
		}

		public int getRootSelection() {
			int value = OptionsPage.ROOT_WORKSPACE;
			try {
				value = dialogSettings.getInt(PREF_LAST_AO_ROOT);
			} catch (NumberFormatException e) {
			}

			switch (value) {
			case OptionsPage.ROOT_WORKSPACE:
			case OptionsPage.ROOT_PROJECT:
			case OptionsPage.ROOT_SELECTION:
				return value;
			default:
				return OptionsPage.ROOT_WORKSPACE;
			}
		}

		public void storeLocationSelection(int defaultSelection) {
			dialogSettings.put(PREF_LAST_SELECTION, defaultSelection);
		}

		public void storeFilesystemPath(String path) {
			dialogSettings.put(PREF_LAST_FS_PATH, path);
		}

		public void storeWorkspacePath(String path) {
			dialogSettings.put(PREF_LAST_WS_PATH, path);
		}

		public void storeOutputFormat(int selection) {
			dialogSettings.put(PREF_LAST_AO_FORMAT, selection);
		}

		public void storePatchRoot(int selection) {
			dialogSettings.put(PREF_LAST_AO_ROOT, selection);
		}
	}

	private DirectionSelectionPage directionSelectionPage;
	private LocationPage locationPage;
	private OptionsPage optionsPage;

	private final DefaultValuesStore defaultValuesStore;

	private DocumentMerger merger;
	private IDocument leftDoc;
	private IDocument rightDoc;
	private String leftPath;
	private String rightPath;
	private boolean rightToLeft;

	private boolean targetFileEdited = false;

	private Text unified_customRelativeText;
	private Button unified_customRelativeOption;

	public GenerateDiffFileWizard() {
		super();
		setWindowTitle(CompareMessages.GenerateLocalDiff_title);
		initializeDefaultPageImageDescriptor();
		defaultValuesStore = new DefaultValuesStore();
	}

	public GenerateDiffFileWizard(DocumentMerger merger, boolean rightToLeft) {
		this();
		this.merger = merger;
		this.leftDoc = merger.getDocument(MergeViewerContentProvider.LEFT_CONTRIBUTOR);
		this.rightDoc = merger.getDocument(MergeViewerContentProvider.RIGHT_CONTRIBUTOR);
		this.leftPath = merger.getCompareConfiguration().getLeftLabel(leftDoc);
		this.rightPath = merger.getCompareConfiguration().getRightLabel(rightDoc);
		this.rightToLeft = rightToLeft;
	}

	public void addPages() {
		String pageTitle = CompareMessages.GenerateLocalDiff_pageTitle;
		String pageDescription = CompareMessages.GenerateLocalDiff_Specify_the_file_which_contributes_the_changes;
		directionSelectionPage = new DirectionSelectionPage(
				pageTitle,
				pageTitle,
				CompareUIPlugin
				.getImageDescriptor(ICompareUIConstants.IMG_WIZBAN_DIFF));
		directionSelectionPage.setDescription(pageDescription);
		addPage(directionSelectionPage);

		pageTitle = CompareMessages.GenerateLocalDiff_pageTitle;
		pageDescription = CompareMessages.GenerateLocalDiff_pageDescription;
		locationPage = new LocationPage(pageTitle, pageTitle, CompareUIPlugin.getImageDescriptor(ICompareUIConstants.IMG_WIZBAN_DIFF), defaultValuesStore);
		locationPage.setDescription(pageDescription);
		addPage(locationPage);

		pageTitle = CompareMessages.Advanced_options_19;
		pageDescription = CompareMessages.Configure_options_diff_command;
		optionsPage = new OptionsPage(pageTitle, pageTitle, CompareUIPlugin
				.getImageDescriptor(ICompareUIConstants.IMG_WIZBAN_DIFF),
				defaultValuesStore) {

			public void setVisible(boolean visible) {
				super.setVisible(visible);
				if (!initialized && visible) {
					if (directionSelectionPage.isRightToLeft()) {
						unified_customRelativeText.setText(rightPath);
					} else {
						unified_customRelativeText.setText(leftPath);
					}
					targetFileEdited = true;
				}
			}

			protected void performSpecificActions() {
				createCustomRelativeControl();
			}

			protected void updateEnablements() {
				diffTypeRadioGroup.setEnablement(false, new int[] { FORMAT_CONTEXT,
						FORMAT_STANDARD }, FORMAT_UNIFIED);
				unifiedRadioGroup.setEnablement(false, new int[] {
						ROOT_PROJECT, ROOT_SELECTION, ROOT_WORKSPACE } );
			}

			protected void setEnableUnifiedGroup(boolean enabled) {
				unifiedRadioGroup.setEnablement(false, new int[] {
						ROOT_WORKSPACE, ROOT_PROJECT, ROOT_SELECTION });
			}
		};
		optionsPage.setDescription(pageDescription);
		addPage(optionsPage);
	}

	private void createCustomRelativeControl() {
		Group unifiedGroup = optionsPage.getUnifiedGroup();
		final RadioButtonGroup unifiedRadioGroup = optionsPage.getUnifiedRadioGroup();
		unified_customRelativeOption = new Button(unifiedGroup, SWT.RADIO);
		unified_customRelativeOption.setText(CompareMessages.GenerateDiffFileWizard_13);
		unified_customRelativeOption.setSelection(true);
		unified_customRelativeOption.setLayoutData(new GridData(
				SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));

		unified_customRelativeText = new Text(unifiedGroup, SWT.BORDER);
		unified_customRelativeText.setLayoutData(new GridData(
				SWT.FILL, SWT.CENTER, true, false, 1, 1));

		optionsPage.getUnifiedRadioGroup().add(OptionsPage.ROOT_CUSTOM, unified_customRelativeOption);
		unified_customRelativeOption.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				unifiedRadioGroup.setSelection(OptionsPage.ROOT_CUSTOM, false);
			}
		});
		optionsPage.updateDiffTypeEnablements(new int[] {OptionsPage.FORMAT_CONTEXT, OptionsPage.FORMAT_STANDARD}, OptionsPage.FORMAT_UNIFIED);
		optionsPage.updateUnifiedEnablements(new int[] {OptionsPage.ROOT_PROJECT, OptionsPage.ROOT_WORKSPACE, OptionsPage.ROOT_SELECTION}, OptionsPage.ROOT_CUSTOM);
	}

	/**
	 * Declares the wizard banner iamge descriptor
	 */
	protected void initializeDefaultPageImageDescriptor() {
		final String iconPath= "icons/full/"; //$NON-NLS-1$
		try {
			final URL installURL = CompareUIPlugin.getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
			final URL url = new URL(installURL, iconPath + "wizards/newconnect_wiz.gif");	//$NON-NLS-1$
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			setDefaultPageImageDescriptor(desc);
		} catch (MalformedURLException e) {
			// Should not happen.  Ignore.
		}
	}

	/* (Non-javadoc)
	 * Method declared on IWizard.
	 */
	public boolean needsProgressMonitor() {
		return true;
	}

	/**
	 * Completes processing of the wizard. If this method returns <code>
	 * true</code>, the wizard will close; otherwise, it will stay active.
	 */
	public boolean performFinish() {
		final int location = locationPage.getSelectedLocation();

		final File file= location != LocationPage.CLIPBOARD? locationPage.getFile() : null;

		if (!(file == null || validateFile(file))) {
			return false;
		}

		//Validation of patch root
		if(optionsPage.getRootSelection() == OptionsPage.ROOT_CUSTOM) {
			String path = optionsPage.getPath();
			IFile file2;
			try {
				file2 = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
			} catch(IllegalArgumentException e) {
				final String title = CompareMessages.GenerateLocalDiff_3;
				final String msg = CompareMessages.GenerateLocalDiff_4;
				final MessageDialog dialog = new MessageDialog(getShell(), title,
						null, msg, MessageDialog.ERROR,
						new String[] { IDialogConstants.OK_LABEL }, 0);
				dialog.open();
				return false;
			}
			if(!validateFile2(file2)) {
				return false;
			}
		}

		// Create the patch
		generateDiffFile(file);

		/**
		 * Refresh workspace if necessary and save default selection.
		 */
		switch (location) {

		case LocationPage.WORKSPACE:
			final String workspaceResource= locationPage.getWorkspaceLocation();
			if (workspaceResource != null){
				defaultValuesStore.storeLocationSelection(LocationPage.WORKSPACE);
				defaultValuesStore.storeWorkspacePath(workspaceResource);
				/* try {
	                workspaceResource.getParent().refreshLocal(IResource.DEPTH_ONE, null);
	            } catch(CoreException e) {
	                CVSUIPlugin.openError(getShell(), CVSUIMessages.GenerateCVSDiff_error, null, e);
	                return false;
	            } */
			} else {
				//Problem with workspace location, open with clipboard next time
				defaultValuesStore.storeLocationSelection(LocationPage.CLIPBOARD);
			}
			break;

		case LocationPage.FILESYSTEM:
			defaultValuesStore.storeFilesystemPath(file.getPath());
			defaultValuesStore.storeLocationSelection(LocationPage.FILESYSTEM);
			break;

		case LocationPage.CLIPBOARD:
			defaultValuesStore.storeLocationSelection(LocationPage.CLIPBOARD);
			break;

		default:
			return false;
		}


		/**
		 * Save default selections of Options Page
		 */

		defaultValuesStore.storeOutputFormat(optionsPage.getFormatSelection());
		defaultValuesStore.storePatchRoot(optionsPage.getRootSelection());

		return true;
	}

	private void generateDiffFile(File file) {
		String toPath, oldPath = null;
		if (targetFileEdited) {
			oldPath = optionsPage.getPath();
		} else {
			if(directionSelectionPage.isRightToLeft()){
				oldPath = this.rightPath;
			} else {
				oldPath = this.leftPath;
			}
		}

		if(directionSelectionPage.isRightToLeft()){
			toPath = this.leftPath;
		} else {
			toPath = this.rightPath;
		}

		UnifiedDiffFormatter formatter = new UnifiedDiffFormatter(merger
				.getAllDiffs(), leftDoc, rightDoc, oldPath, toPath,
				directionSelectionPage.isRightToLeft());
		try {
			if (file == null) {
				formatter.generateDiffToClipboard();
			} else {
				formatter.generateDiff(file);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean validateFile(File file) {

		if (file == null)
			return false;

		/**
		 * Consider file valid if it doesn't exist for now.
		 */
		if (!file.exists())
			return true;

		/**
		 * The file exists.
		 */
		if (!file.canWrite()) {
			final String title= CompareMessages.GenerateLocalDiff_1;
			final String msg= CompareMessages.GenerateLocalDiff_2;
			final MessageDialog dialog= new MessageDialog(getShell(), title, null, msg, MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
			return false;
		}

		final String title = CompareMessages.GenerateLocalDiff_overwriteTitle;
		final String msg = CompareMessages.GenerateLocalDiff_overwriteMsg;
		final MessageDialog dialog = new MessageDialog(getShell(), title, null, msg, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
		dialog.open();
		if (dialog.getReturnCode() != 0)
			return false;

		return true;
	}

	public boolean validateFile2(IFile file) {
		if (file == null)
			return false;

		// Consider file invalid if it doesn't exist.
		if (!file.exists()) {
			final String title = CompareMessages.GenerateLocalDiff_3;
			final String msg = CompareMessages.GenerateLocalDiff_4;
			final MessageDialog dialog = new MessageDialog(getShell(), title,
					null, msg, MessageDialog.ERROR,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
			return false;
		}

		return true;
	}

	public LocationPage getLocationPage() {
		return locationPage;
	}

	/**
	 * The class maintain proper selection of radio button within the group:
	 * <ul>
	 * <li>Only one button can be selected at the time.</li>
	 * <li>Disabled button can't be selected unless all buttons in the group
	 * are disabled.</li>
	 * </ul>
	 */
	/*private*/ class RadioButtonGroup {

		/**
		 * List of buttons in the group. Both radio groups contain 3 elements.
		 */
		private List buttons = new ArrayList(3);

		/**
		 * Index of the selected button.
		 */
		private int selected = 0;

		/**
		 * Add a button to the group. While adding a new button the method
		 * checks if there is only one button selected in the group.
		 * 
		 * @param buttonCode
		 *            A button's code (eg. <code>ROOT_WORKSPACE</code>). To get
		 *            an index we need to subtract 1 from it.
		 * @param button
		 *            A button to add.
		 */
		public void add(int buttonCode, Button button) {
			if (button != null && (button.getStyle() & SWT.RADIO) != 0) {
				if (button.getSelection() && !buttons.isEmpty()) {
					deselectAll();
					selected = buttonCode - 1;
				}
				buttons.add(buttonCode - 1, button);
			}
		}

		/**
		 * Returns selected button's code.
		 * 
		 * @return Selected button's code.
		 */
		public int getSelected() {
			return selected + 1;
		}

		/**
		 * Set selection to the given button. When
		 * <code>selectEnabledOnly</code> flag is true the returned value can
		 * differ from the parameter when a button we want to set selection to
		 * is disabled and there are other buttons which are enabled.
		 * 
		 * @param buttonCode
		 *            A button's code (eg. <code>ROOT_WORKSPACE</code>). To get
		 *            an index we need to subtract 1 from it.
		 * @return Code of the button to which selection was finally set.
		 */
		public int setSelection(int buttonCode, boolean selectEnabledOnly) {
			deselectAll();

			((Button) buttons.get(buttonCode - 1)).setSelection(true);
			selected = buttonCode - 1;
			if (selectEnabledOnly)
				selected = selectEnabledOnly() - 1;
			return getSelected();
		}

		/**
		 * Make sure that only an enabled radio button is selected.
		 * 
		 * @return A code of the selected button.
		 */
		public int selectEnabledOnly() {
			deselectAll();

			Button selectedButton = (Button) buttons.get(selected);
			if (!selectedButton.isEnabled()) {
				// if the button is disabled, set selection to an enabled one
				for (Iterator iterator = buttons.iterator(); iterator.hasNext();) {
					Button b = (Button) iterator.next();
					if (b.isEnabled()) {
						b.setSelection(true);
						selected = buttons.indexOf(b);
						return selected + 1;
					}
				}
				// if none found, reset the initial selection
				selectedButton.setSelection(true);
			} else {
				// because selection has been cleared, set it again
				selectedButton.setSelection(true);
			}
			// return selected button's code so the value can be stored
			return getSelected();
		}

		/**
		 * Enable or disable given buttons.
		 * 
		 * @param enabled
		 *            Indicates whether to enable or disable the buttons.
		 * @param buttonsToChange
		 *            Buttons to enable/disable.
		 * @param defaultSelection
		 *            The button to select if the currently selected button
		 *            becomes disabled.
		 */
		public void setEnablement(boolean enabled, int[] buttonsToChange,
				int defaultSelection) {

			// enable (or disable) given buttons
			for (int i = 0; i < buttonsToChange.length; i++) {
				((Button) this.buttons.get(buttonsToChange[i] - 1))
				.setEnabled(enabled);
			}
			// check whether the selected button is enabled
			if (!((Button) this.buttons.get(selected)).isEnabled()) {
				if (defaultSelection != -1)
					// set the default selection and check if it's enabled
					setSelection(defaultSelection, true);
				else
					// no default selection is given, select any enabled button
					selectEnabledOnly();
			}
		}

		/**
		 * Enable or disable given buttons with no default selection. The selection
		 * will be set to an enabled button using the <code>selectEnabledOnly</code> method.
		 * 
		 * @param enabled Indicates whether to enable or disable the buttons.
		 * @param buttonsToChange Buttons to enable/disable.
		 */
		public void setEnablement(boolean enabled, int[] buttonsToChange) {
			// -1 means that no default selection is given
			setEnablement(enabled, buttonsToChange, -1);
		}

		/**
		 * Deselect all buttons in the group.
		 */
		private void deselectAll() {
			// clear all selections
			for (Iterator iterator = buttons.iterator(); iterator.hasNext();)
				((Button) iterator.next()).setSelection(false);
		}
	}

}
