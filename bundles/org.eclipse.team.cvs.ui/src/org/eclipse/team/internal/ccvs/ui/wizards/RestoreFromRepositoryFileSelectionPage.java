/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.Splitter;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSRunnable;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.CommandOutputAdapter;
import org.eclipse.team.internal.ccvs.core.client.Log;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Command.QuietOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.AdaptableHierarchicalResourceList;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.HistoryTableProvider;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

/**
 * Select the files to restore
 */
public class RestoreFromRepositoryFileSelectionPage extends CVSWizardPage {
	private TreeViewer fileTree;
	private CompareViewerPane fileSelectionPane;
	private CompareViewerPane revisionSelectionPane;
	private CheckboxTableViewer revisionsTable;
	private CompareViewerSwitchingPane fileContentPane;
	
	private HistoryTableProvider historyTableProvider;
	private AdaptableHierarchicalResourceList treeInput = new AdaptableHierarchicalResourceList(new IResource[0]);
	
	private IContainer folder;
	private boolean recurse;
	private IFile selectedFile;
	private ILogEntry selectedRevision;
	private Map entriesCache = new HashMap();
	private Map filesToRestore = new HashMap();

	class HistoryInput implements ITypedElement, IStreamContentAccessor, IModificationDate {
		IFile file;
		ILogEntry logEntry;
		
		HistoryInput(IFile file, ILogEntry logEntry) {
			this.file= file;
			this.logEntry = logEntry;
		}
		public InputStream getContents() throws CoreException {
			return getContentsFromLogEntry(logEntry);
		}
		public String getName() {
			return file.getName();
		}
		public String getType() {
			return file.getFileExtension();
		}
		public Image getImage() {
			return CompareUI.getImage(file);
		}
		public long getModificationDate() {
			return logEntry.getDate().getTime();
		}
	}
	
	/**
	 * Constructor for RestoreFromRepositoryFileSelectionPage.
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public RestoreFromRepositoryFileSelectionPage(
		String pageName,
		String title,
		ImageDescriptor titleImage,
		String description) {
		super(pageName, title, titleImage, description);
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= createComposite(parent, 1);
		setControl(composite);
		
		WorkbenchHelp.setHelp(composite, IHelpContextIds.RESTORE_FROM_REPOSITORY_FILE_SELECTION_PAGE);
		
		// Top and bottom panes: top is the two selection panes, bottom is the file content viewer
		Splitter vsplitter= new Splitter(composite,  SWT.VERTICAL);
		vsplitter.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
					| GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
		
		// Top left and top right panes: the left for the files, the right for the log entries
		Splitter hsplitter= new Splitter(vsplitter,  SWT.HORIZONTAL);
		
		fileSelectionPane = new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		fileSelectionPane.setLayoutData(gd);
		fileTree = createFileSelectionTree(fileSelectionPane);
		fileSelectionPane.setContent(fileTree.getControl());
		fileSelectionPane.setText(Policy.bind("RestoreFromRepositoryFileSelectionPage.fileSelectionPaneTitle"));
		
		revisionSelectionPane = new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
		gd= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		revisionSelectionPane.setLayoutData(gd);
		revisionsTable = createRevisionSelectionTable();
		revisionSelectionPane.setContent(revisionsTable.getControl());
		revisionSelectionPane.setText(Policy.bind("RestoreFromRepositoryFileSelectionPage.revisionSelectionPaneTitle"));
		
		// Bottom pane is the file content viewer
		fileContentPane = new CompareViewerSwitchingPane(vsplitter, SWT.BORDER | SWT.FLAT) {
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				return CompareUIPlugin.findContentViewer(oldViewer, input, this, null);	
			}
		};
		
		// Weights between top and bottom are 50/50 so top is not too small in wizard
		vsplitter.setWeights(new int[] { 50, 50 });
						
		initializeValues();
		updateWidgetEnablements();
		fileTree.getControl().setFocus();
	}

	protected CheckboxTableViewer createRevisionSelectionTable() {
		historyTableProvider = new HistoryTableProvider();
		CheckboxTableViewer table = historyTableProvider.createCheckBoxTable(revisionSelectionPane);
		table.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				ILogEntry[] entries = getSelectedEntries();
				if (entries != null) return entries;
				return new Object[0];		
			}
			public void dispose() {
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		table.setInput(this);
		table.getTable().addSelectionListener(
			new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					// Handle check selection in the check state listener
					if (e.detail == SWT.CHECK) return;
					handleRevisionSelection(e.item);
				}
			}
		);
		table.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				handleRevisionChecked(event);
			}
		});
		return table;
	}
	
	protected TreeViewer createFileSelectionTree(Composite composite) {
		TreeViewer tree = new CheckboxTreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setUseHashlookup(true);
		tree.setContentProvider(treeInput.getTreeContentProvider());
		tree.setLabelProvider(
			new DecoratingLabelProvider(
				new WorkbenchLabelProvider() {
					protected String decorateText(String input, Object element) {
						ILogEntry entry = (ILogEntry)filesToRestore.get(element);
						String text = super.decorateText(input, element);
						if (entry != null) {
							text = "*" + text;
						}
						return text;
					}
				}, 
				WorkbenchPlugin.getDefault().getWorkbench().getDecoratorManager().getLabelDecorator()));
		tree.setSorter(new ResourceSorter(ResourceSorter.NAME));
		tree.setInput(treeInput);
		
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.widthHint = 200;
		data.horizontalSpan = 1;
		tree.getControl().setLayoutData(data);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleFileSelection(event);
			}
		});
		return tree;
	}
	
	/**
	 * Method updateWidgetEnablements.
	 */
	private void updateWidgetEnablements() {
	}
	/**
	 * Method initializeValues.
	 */
	private void initializeValues() {
		refresh();
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible) fetchPageContents();
		super.setVisible(visible);
	}

	/**
	 * Returns the folder.
	 * @return IContainer
	 */
	public IContainer getFolder() {
		return folder;
	}

	/**
	 * Sets the folder.
	 * @param folder The folder to set
	 */
	public void setFolder(IContainer folder) {
		if (folder.equals(this.folder)) return;
		this.folder = folder;
		initializeValues();
		updateWidgetEnablements();
	}

	/*
	 * This class handles the output from "cvs log -R ..." where -R
	 * indicates that only the RCS file name is to be returned. Files
	 * that have been deleted will be in the Attic. The Attic may also
	 * contains files that exist on a branch but not in HEAD
	 */
	private class AtticLogListener extends CommandOutputAdapter {
		private static final String ATTIC = "Attic";
		private static final String RCS_FILE_POSTFIX = ",v";
		private static final String LOGGING_PREFIX = "Logging ";
		ICVSFolder currentFolder;
		List atticFiles = new ArrayList();
		public IStatus messageLine(
					String line,
					ICVSRepositoryLocation location,
					ICVSFolder commandRoot,
					IProgressMonitor monitor) {
			
			// Find all RCS file names tat contain "Attic"
			int index = line.indexOf(ATTIC);
			if (index == -1) return OK;
			// Extract the file name and path from the RCS path
			String filePath = line.substring(index);
			int start = line.indexOf(Session.SERVER_SEPARATOR, index);
			String fileName = line.substring(start + 1);
			if (fileName.endsWith(RCS_FILE_POSTFIX)) {
				fileName = fileName.substring(0, fileName.length() - RCS_FILE_POSTFIX.length());
			}
			try {
				atticFiles.add(currentFolder.getFile(fileName));
			} catch (CVSException e) {
				return e.getStatus();
			}
			return OK;
		}
		/**
		 * @see org.eclipse.team.internal.ccvs.core.client.CommandOutputAdapter#errorLine(java.lang.String, org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation, org.eclipse.team.internal.ccvs.core.ICVSFolder, org.eclipse.core.runtime.IProgressMonitor)
		 */
		public IStatus errorLine(
			String line,
			ICVSRepositoryLocation location,
			ICVSFolder commandRoot,
			IProgressMonitor monitor) {
			
			CVSRepositoryLocation repo = (CVSRepositoryLocation)location;
			String folderPath = repo.getServerMessageWithoutPrefix(line, SERVER_PREFIX);
			if (folderPath != null) {
				if (folderPath.startsWith(LOGGING_PREFIX)) {
					folderPath = folderPath.substring(LOGGING_PREFIX.length());
					try {
						currentFolder = commandRoot.getFolder(folderPath);
					} catch (CVSException e) {
						return e.getStatus();
					}
					return OK;
				}
			}
			return super.errorLine(line, location, commandRoot, monitor);
		}

		public ICVSFile[] getAtticFilePaths() {
			return (ICVSFile[]) atticFiles.toArray(new ICVSFile[atticFiles.size()]);
		}
	}

	/*
	 * Fetch the RCS paths (minus the Attic segment) of all files in the Attic.
	 * This path includes the repository root path.
	 */
	private ICVSFile[] fetchFilesInAttic(ICVSRepositoryLocation location, final ICVSFolder parent, IProgressMonitor monitor) throws CVSException {
		final AtticLogListener listener = new AtticLogListener();
		Session.run(location, parent, false, new ICVSRunnable() {
			public void run(IProgressMonitor monitor) throws CVSException {
				monitor = Policy.monitorFor(monitor);
				monitor.beginTask(Policy.bind("RestoreFromRepositoryWizard.getLogEntries"), 100); //$NON-NLS-1$
				QuietOption quietness = CVSProviderPlugin.getPlugin().getQuietness();
				try {
					CVSProviderPlugin.getPlugin().setQuietness(Command.VERBOSE);
					IStatus status = Command.LOG.execute(Command.NO_GLOBAL_OPTIONS, getLocalOptions(),
						new ICVSResource[] { parent }, listener,
						Policy.subMonitorFor(monitor, 100));
					if (status.getCode() == CVSStatus.SERVER_ERROR) {
						throw new CVSServerException(status);
					}
				} finally {
					CVSProviderPlugin.getPlugin().setQuietness(quietness);
					monitor.done();
				}
			}
		}, monitor);
		return listener.getAtticFilePaths();
	}
	
	public void fetchPageContents() {
		final ICVSFile[][] files = new ICVSFile[1][0];
		files[0] = null;
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(getFolder());
						FolderSyncInfo info = folder.getFolderSyncInfo();
						ICVSRepositoryLocation location = CVSProviderPlugin.getPlugin().getRepository(info.getRoot());
						files[0] = fetchFilesInAttic(location, folder, monitor);
					} catch (CVSException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC);
			return;
		} catch (InterruptedException e) {
			return;
		}
		setErrorMessage(null);
		setTreeInput(files[0]);
	}
	
	private LocalOption[] getLocalOptions() {
		List options = new ArrayList();
		if (!isRecurse())
			options.add(Command.DO_NOT_RECURSE);
		options.add(Log.RCS_FILE_NAMES_ONLY);
		return (LocalOption[]) options.toArray(new LocalOption[options.size()]);
	}
	/**
	 * Returns the recurse.
	 * @return boolean
	 */
	public boolean isRecurse() {
		return recurse;
	}

	/**
	 * Sets the recurse.
	 * @param recurse The recurse to set
	 */
	public void setRecurse(boolean recurse) {
		if (this.recurse == recurse) return;
		this.recurse = recurse;
		initializeValues();
		updateWidgetEnablements();
	}
	
	/*
	 * Set the resource tree input to the files that were deleted	 */
	private void setTreeInput(ICVSFile[] cvsFiles) {
		this.selectedFile = null;
		this.selectedRevision = null;
		IResource[] files = new IResource[cvsFiles.length];
		for (int i = 0; i < cvsFiles.length; i++) {
			try {
				files[i] = cvsFiles[i].getIResource();
			} catch (CVSException e) {
				// In practive, this error shold not occur.
				// It may if there is an existing folder with a name that matches the file
				// but this is bad in general when using CVS
				CVSUIPlugin.log(e);
			}
		}
		treeInput.setResources(files);
		refresh();
	}
	/**
	 * Method refresh.
	 */
	private void refresh() {
		if (folder == null) return;
		
		// Empty the file content viewer
		if (fileContentPane != null && !fileContentPane.isDisposed()) {
			fileContentPane.setInput(null);
			fileContentPane.setText(null);
			fileContentPane.setImage(null);
		}
		setErrorMessage(null);
		
		// refresh the tree
		fileTree.refresh();
		revisionsTable.refresh();
	}
	
	/*
	 * Set the log entry table input to the fetched entries
	 */
	private void setLogEntryTableInput(ILogEntry[] entries) {
		revisionsTable.refresh();
		ILogEntry selectedEntry = (ILogEntry)filesToRestore.get(selectedFile);
		if (selectedEntry != null) {
			revisionsTable.setChecked(selectedEntry, true);
		}
		setErrorMessage(null);
	}
	
	private void handleFileSelection(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection == null || selection.isEmpty()) {
			// XXX clear entries table?
		} else {
			if (selection instanceof StructuredSelection) {
				StructuredSelection structuredSelection = (StructuredSelection) selection;
				IResource resource = (IResource)structuredSelection.getFirstElement();
				if (resource instanceof IFile) {
					handleFileSelection((IFile) resource);
				}
			}
		}
	}
	
	/**
	 * Method handleFileSelection.
	 * @param file
	 */
	private void handleFileSelection(IFile file) {
		if (this.selectedFile == file) return;
		this.selectedFile = file;
		if (entriesCache.get(file) == null) {
			try {
				// First, we need to create a remote file handle so we can get the log entries
				ICVSFolder parent = CVSWorkspaceRoot.getCVSFolderFor(file.getParent());
				FolderSyncInfo info = parent.getFolderSyncInfo();
				ICVSRepositoryLocation location = CVSProviderPlugin.getPlugin().getRepository(info.getRoot());
				RemoteFolderTree remoteFolder = new RemoteFolderTree(null, location, new Path(info.getRepository()), CVSTag.DEFAULT);
				final RemoteFile remoteFile = new RemoteFile(remoteFolder, Update.STATE_ADDED_LOCAL, file.getName(), CVSTag.DEFAULT);
				remoteFolder.setChildren(new ICVSRemoteResource[] { remoteFile });
				
				// Then we need to fetch the log entries
				final ILogEntry[][] entries = new ILogEntry[1][0];
				entries[0] = null;
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							entries[0] = remoteFile.getLogEntries(monitor);
							entriesCache.put(selectedFile, entries[0]);
						} catch (CVSException e) {
							throw new InvocationTargetException(e);
						}
					}
				});
			} catch (CVSException e) {
				setErrorMessage(
					CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
						.getMessage());
				return;
			} catch (InvocationTargetException e) {
				setErrorMessage(
					CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
						.getMessage());
				return;
			} catch (InterruptedException e) {
				return;
			}
		}
		
		// Finally, display the log entries
		setLogEntryTableInput((ILogEntry[])entriesCache.get(file));
	}

	private ILogEntry[] getSelectedEntries() {
		return (ILogEntry[])entriesCache.get(selectedFile);
	}
	
	/**
	 * Method getContents.
	 * @param logEntry
	 * @return InputStream
	 */
	private InputStream getContentsFromLogEntry(final ILogEntry logEntry) {
		final InputStream[] is = new InputStream[] { null };
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						ICVSRemoteFile remoteFile = logEntry.getRemoteFile();
						is[0] = remoteFile.getContents(monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			setErrorMessage(
				CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
					.getMessage());
			return null;
		} catch (InterruptedException e) {
			return null;
		}
		setErrorMessage(null);
		return new BufferedInputStream(is[0]);
	}

	private void handleRevisionChecked(CheckStateChangedEvent event) {
		// Only allow one element to be checked
		if (event.getChecked()) {
			revisionsTable.setCheckedElements(new Object[] {event.getElement()});
			filesToRestore.put(selectedFile, event.getElement());
		}
		if (revisionsTable.getCheckedElements().length == 0) {
			filesToRestore.remove(selectedFile);
		}
		fileTree.refresh();
	}
				
	/*
	 * A revision in the revision table has been selected.
	 * Populate the file contents pane with the selected log entry.
	 */
	private void handleRevisionSelection(Widget w) {
		if (fileContentPane != null && !fileContentPane.isDisposed()) {
			Object o= w.getData();
			if (o instanceof ILogEntry) {
				ILogEntry selected= (ILogEntry) o;
				if (this.selectedRevision == selected) return;
				this.selectedRevision = selected;
				fileContentPane.setInput(new HistoryInput(selectedFile, selected));
				fileContentPane.setText(getEditionLabel(selectedFile, selected));
				fileContentPane.setImage(CompareUI.getImage(selectedFile));
			} else {
				fileContentPane.setInput(null);
			}
		}
	}
	/**
	 * Method getEditionLabel.
	 * @param selectedFile
	 * @param selected
	 * @return String
	 */
	private String getEditionLabel(IFile selectedFile, ILogEntry selected) {
		return selectedFile.getName() + " " + selected.getRevision();
	}
}
