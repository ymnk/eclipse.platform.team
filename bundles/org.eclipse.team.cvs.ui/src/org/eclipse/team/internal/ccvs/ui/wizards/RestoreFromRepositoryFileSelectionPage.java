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
import java.util.List;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
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
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
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
	private CheckboxTreeViewer tree;
	
	AdaptableHierarchicalResourceList treeInput = new AdaptableHierarchicalResourceList(new IResource[0]);
	private IContainer folder;
	private boolean recurse;

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
		createWrappingLabel(composite, Policy.bind("RestoreFromRepositoryFileSelectionPage.treeLabel"), 0, 1); //$NON-NLS-1$
		
		tree = createFileSelectionTree(composite);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleResourceSelection(event);
			}
		});
		tree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				handleResourceChecked(event);
			}
		});
						
		initializeValues();
		updateWidgetEnablements();
		tree.getControl().setFocus();
	}
	
	protected CheckboxTreeViewer createFileSelectionTree(Composite composite) {
		CheckboxTreeViewer tree = new CheckboxTreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setUseHashlookup(true);
		tree.setContentProvider(treeInput.getTreeContentProvider());
		tree.setLabelProvider(
			new DecoratingLabelProvider(
				new WorkbenchLabelProvider(), 
				WorkbenchPlugin.getDefault().getWorkbench().getDecoratorManager().getLabelDecorator()));
		tree.setSorter(new ResourceSorter(ResourceSorter.NAME));
		tree.setInput(treeInput);
		
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = LIST_HEIGHT_HINT;
		data.widthHint = 200;
		data.horizontalSpan = 1;
		tree.getControl().setLayoutData(data);
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
		tree.refresh();
	}
	
	/*
	 * Set the log entry table input to the fetched entries
	 */
	private void setLogEntryTableInput(ILogEntry[] entries) {
	}
	
	private void handleResourceSelection(SelectionChangedEvent event) {
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
					} catch (CVSException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
			
			// Finally, display the log entries
			setLogEntryTableInput(entries[0]);
		} catch (CVSException e) {
			setErrorMessage(
				CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
					.getMessage());
		} catch (InvocationTargetException e) {
			setErrorMessage(
				CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
					.getMessage());
		} catch (InterruptedException e) {
		}
		
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
		} catch (InterruptedException e) {
			return null;
		}
		return new BufferedInputStream(is[0]);
	}
			
	private void handleResourceChecked(CheckStateChangedEvent event) {
		
	}
}
