/*******************************************************************************
 * Copyright (c) 2003 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc.   - initial implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.text.MessageFormat;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.internal.ccvs.core.IConnectionMethod;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Wizard page for entering information about a standard CVS
 * repository location.
 */
public class StandardWizardMainPage extends ConfigurationWizardMainPage {
	// The standard repositories we know about.
	protected StandardRepository[] standardRepositories;

	private Button anonButton;
	private Button userButton;
	private Label repoLabel;

	/**
	 * ConfigurationWizardMainPage constructor.
	 * 
	 * @param pageName  the name of the page
	 * @param title  the title of the page
	 * @param titleImage  the image for the page
	 */
	public StandardWizardMainPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	/**
	 * Creates the UI part of the page.
	 * 
	 * @param parent  the parent of the created widgets
	 */
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 2);
		// set F1 help
		WorkbenchHelp.setHelp(composite, IHelpContextIds.SHARING_NEW_REPOSITORY_PAGE);

		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				updateWidgetEnablements();
			}
		};
		
		Group g = createGroup(composite, Policy.bind("ConfigurationWizardMainPage.Location_1")); //$NON-NLS-1$
		
		// Standard cvs repository.
		createLabel(g, Policy.bind("ConfigurationWizardMainPage.host")); //$NON-NLS-1$
		hostCombo = createCombo(g);
		hostCombo.addListener(SWT.Selection, listener);
		// FIXME
		// hostCombo.setEditable(false);

		createLabel(g, Policy.bind("ConfigurationWizardMainPage.project")); //$NON-NLS-1$
		repositoryPathCombo = createEditableCombo(g);
		repositoryPathCombo.addListener(SWT.Selection, listener);
		repositoryPathCombo.addListener(SWT.Modify, listener);

		g = createGroup(composite, Policy.bind("ConfigurationWizardMainPage.Authentication_2")); //$NON-NLS-1$
		
		anonButton = new Button(g, SWT.RADIO);
		anonButton.setText(Policy.bind("ConfigurationWizardMainPage.anonymous")); //$NON-NLS-1$
		anonButton.addListener(SWT.Selection, listener);

		userButton = new Button(g, SWT.RADIO);
		userButton.setText(Policy.bind("ConfigurationWizardMainPage.user")); //$NON-NLS-1$
		userButton.addListener(SWT.Selection, listener);

		// User name
		createLabel(g, Policy.bind("ConfigurationWizardMainPage.userName")); //$NON-NLS-1$
		userCombo = createEditableCombo(g);
		userCombo.addListener(SWT.Selection, listener);
		userCombo.addListener(SWT.Modify, listener);
		
		// Password
		createLabel(g, Policy.bind("ConfigurationWizardMainPage.password")); //$NON-NLS-1$
		passwordText = createTextField(g);
		passwordText.setEchoChar('*');

		// Connection type
		createLabel(g, Policy.bind("ConfigurationWizardMainPage.connection")); //$NON-NLS-1$
		connectionMethodCombo = createCombo(g);

		Composite repo = createGroup(composite, Policy.bind("ConfigurationWizardMainPage.repositorySpec")); //$NON-NLS-1$
		repoLabel = createWrappingLabel(repo, "", 0 /* indent */, 1 /* columns */); //$NON-NLS-1$
		
		// create a composite to ensure the validate button is in its own tab group
		if (showValidate) {
			Composite validateButtonTabGroup = new Composite(composite, SWT.NONE);
			GridData data = new GridData();
			data.horizontalSpan = 2;
			validateButtonTabGroup.setLayoutData(data);
			validateButtonTabGroup.setLayout(new FillLayout());

			validateButton = new Button(validateButtonTabGroup, SWT.CHECK);
			validateButton.setText(Policy.bind("ConfigurationWizardAutoconnectPage.validate")); //$NON-NLS-1$
			validateButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					validate = validateButton.getSelection();
				}
			});
		}

		initializeValues();
		updateWidgetEnablements();
		hostCombo.setFocus();
		
		setControl(composite);
	}

	/**
	 * @see CVSWizardPage#finish
	 */
	public boolean finish(IProgressMonitor monitor) {
		// Set the result to be the current values
		Properties result = new Properties();

		StandardRepository repo = getCurrentRepository(hostCombo.getText());

		String user;
		boolean anon = anonButton.getSelection();

		if (anon) {
			result.setProperty("connection", repo.getConnectionMethod()); //$NON-NLS-1$ //$NON-NLS-2
			user = repo.anonymousUserName;
			result.setProperty("password", repo.anonymousPassword); //$NON-NLS-1$
		} else {
			result.setProperty("connection", connectionMethodCombo.getText()); //$NON-NLS-1$
			user = userCombo.getText();
			result.setProperty("password", passwordText.getText()); //$NON-NLS-1$
		}
		result.setProperty("user", user);

		String hostPlusPath = repo.computeCvsroot(repositoryPathCombo.getText(), user);
		int index = hostPlusPath.indexOf(':');

		result.setProperty("host", hostPlusPath.substring(0, index)); //$NON-NLS-1$
		result.setProperty("root", hostPlusPath.substring(index + 1)); //$NON-NLS-1$
		this.properties = result;
		
		saveWidgetValues();
		
		return true;
	}

	/**
	 * Initializes states of the controls.
	 */
	protected void initializeValues() {
		// Set remembered values
		IDialogSettings settings = getDialogSettings();

		standardRepositories = getStandardRepositories();
		for (int i = 0; i < standardRepositories.length; i++) {
			hostCombo.add(standardRepositories[i].repositoryName);
		}

		if (settings != null) {
			String[] paths = settings.getArray(STORE_PROJECT_ID);
			if (paths != null) {
				for (int i = 0; i < paths.length; i++) {
					repositoryPathCombo.add(paths[i]);
				}
			}
			String[] userNames = settings.getArray(STORE_USERNAME_ID);
			if (userNames == null) {
				userNames = new String[] {
					System.getProperty("user.name"),
				};
				settings.put(STORE_USERNAME_ID, userNames);
			}
			if (userNames != null) {
				for (int i = 0; i < userNames.length; i++) {
					userCombo.add(userNames[i]);
				}
			}
			if (showValidate) {
				validate = !settings.getBoolean(STORE_DONT_VALIDATE_ID);
				validateButton.setSelection(validate);
			}
		}
		
		// Initialize other values and widget states
		IConnectionMethod[] methods = CVSRepositoryLocation.getPluggedInConnectionMethods();
		for (int i = 0; i < methods.length; i++) {
			connectionMethodCombo.add(methods[i].getName());
		}
		
		connectionMethodCombo.select(0);
		
		// FIXME.
		userButton.setSelection(true);

		if(properties != null) {
			String method = (String)properties.getProperty("connection"); //$NON-NLS-1$
			if (method == null) {
				connectionMethodCombo.select(0);
			} else {
				connectionMethodCombo.select(connectionMethodCombo.indexOf(method));
			}
	
			String user = (String)properties.getProperty("user"); //$NON-NLS-1$
			if (user != null) {
				userCombo.setText(user);
			}
	
			String password = (String)properties.getProperty("password"); //$NON-NLS-1$
			if (password != null) {
				passwordText.setText(password);
			}
		}
	}

	/**
	 * Saves the widget values
	 */
	protected void saveWidgetValues() {
		// Update history
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String[] userNames = settings.getArray(STORE_USERNAME_ID);
			if (userNames == null) userNames = new String[0];
			userNames = addToHistory(userNames, userCombo.getText());
			settings.put(STORE_USERNAME_ID, userNames);

			String[] paths = settings.getArray(STORE_PROJECT_ID);
			if (paths == null) paths = new String[0];
			paths = addToHistory(paths, repositoryPathCombo.getText());
			settings.put(STORE_PATH_ID, paths);

			if (showValidate) {
				settings.put(STORE_DONT_VALIDATE_ID, !validate);
			}
		}
	}

	/**
	 * Find the current standard repository object, given the host
	 * name.
	 */
	protected StandardRepository getCurrentRepository(String name) {
		for (int i = 0; i < standardRepositories.length; i++) {
			if (name.equals(standardRepositories[i].repositoryName))
				return standardRepositories[i];
		}
		return null;
	}

	/**
	 * Updates widget enablements and sets error message if appropriate.
	 */
	protected void updateWidgetEnablements() {
		StandardRepository repo = getCurrentRepository(hostCombo.getText());
		if (repo == null || repo.soleProjectName != null)
			repositoryPathCombo.setEnabled(false);
		else
			repositoryPathCombo.setEnabled(true);

		boolean isanon = anonButton.getSelection();
		userCombo.setEnabled(!isanon);
		passwordText.setEnabled(!isanon);
		connectionMethodCombo.setEnabled(!isanon);

		validateFields();

		String host = hostCombo.getText();
		String repospec;
		if (repo == null) {
			repospec = "";
		} else {
			String user;
			if (isanon)
				user = repo.anonymousUserName;
			else
				user = userCombo.getText();
			if (user.length() == 0)
				user = "???";
			String project;
			if (repositoryPathCombo.isEnabled()) {
				project = repositoryPathCombo.getText();
				if (project.length() == 0)
					project = "???";
			} else {
				project = "";	// doesn't matter
			}
			String hostPlusPath = repo.computeCvsroot(project, user);
			repospec = (":" + connectionMethodCombo.getText() + ":"
						+ user + "@" + hostPlusPath);
		}
		repoLabel.setText(repospec);
	}

	/**
	 * Validates the contents of the editable fields and set page completion 
	 * and error messages appropriately.
	 */
	protected void validateFields() {
		// First ensure that there is a host.
		String host = hostCombo.getText();
		if (getCurrentRepository(host) == null) {
			setErrorMessage(null);
			setPageComplete(false);
			return;
		}
		if (host.indexOf(':') != -1) {
			setErrorMessage(Policy.bind("ConfigurationWizardMainPage.invalidHostName")); //$NON-NLS-1$
			setPageComplete(false);
			return;
		}
		
		// Validate the user name
		String user = userCombo.getText();
		boolean isanon = anonButton.getSelection();
		if (!isanon && user.length() == 0) {
			setErrorMessage("A user name is required for user athentication.");
			setPageComplete(false);
			return;
		}
		if ((user.indexOf('@') != -1) || (user.indexOf(':') != -1)) {
			setErrorMessage(Policy.bind("ConfigurationWizardMainPage.invalidUserName")); //$NON-NLS-1$
			setPageComplete(false);
			return;
		}

		// Validate the project
		if (!repositoryPathCombo.isEnabled()) {
			// Nothing.
		} else if (repositoryPathCombo.getText().length() == 0) {
			setErrorMessage("A project is required for this host.");
			setPageComplete(false);
			return;
		} else {
			// The standard repositories use a single word specifying
			// the project name.  So disallow `/'.
			String pathString = repositoryPathCombo.getText();
			if (pathString.indexOf("/") != -1) { //$NON-NLS-1$
				setErrorMessage(Policy.bind("ConfigurationWizardMainPage.invalidProjectName"));
				setPageComplete(false);
				return;
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
	}

	// This holds information about a given standard repository.
	public static final class StandardRepository {
		// Name of repository as presented to user.
		public String repositoryName;

		// Name of the anonymous user.
		public String anonymousUserName;

		// The name of the only project available at that repository.
		// If null, then the user must enter a project name.
		public String soleProjectName;

		// Used to construct the CVSROOT for anonymous access.
		// The name of the project is passed as argument {0}.
		public MessageFormat anonymousCvsrootFormat;

		// Used to construct the CVSROOT for non-anonymous access.
		// The name of the project is passed as argument {0}.
		public MessageFormat userCvsrootFormat;

		// Password for anonymous user.
		public String anonymousPassword;

		public StandardRepository(String repositoryName, String anonymousUserName, String anonymousCvsroot, String userCvsroot) {
			this(repositoryName, anonymousUserName, null, anonymousCvsroot, userCvsroot, "");
		}

		public StandardRepository(String repositoryName, String anonymousUserName, String soleProjectName, String anonymousCvsroot, String userCvsroot) {
			this(repositoryName, anonymousUserName, soleProjectName, anonymousCvsroot, userCvsroot, "");
		}

		public StandardRepository(String repositoryName, String anonymousUserName, String soleProjectName, String anonymousCvsroot, String userCvsroot, String anonymousPassword) {
			this.repositoryName = repositoryName;
			this.anonymousUserName = anonymousUserName;
			this.soleProjectName = soleProjectName;
			this.anonymousCvsrootFormat = new MessageFormat(anonymousCvsroot);
			this.userCvsrootFormat = new MessageFormat(userCvsroot);
			this.anonymousPassword = anonymousPassword;
		}

		public String computeCvsroot(String project, String userName) {
			MessageFormat format;
			if (soleProjectName != null) {
				project = soleProjectName;
			}
			if (userName.equals(anonymousUserName)) {
				format = anonymousCvsrootFormat;
			} else {
				format = userCvsrootFormat;
			}
			return format.format(new String[] { project });
		}
		public String getConnectionMethod() {
			return "pserver"; //$NON-NLS-1$
		}
	}

	public static StandardRepository[] getStandardRepositories () {
		return new StandardRepository[] {
			new StandardRepository("sourceforge.net", "anonymous", "cvs.{0}.sourceforge.net:/cvsroot/{0}", "cvs.{0}.sourceforge.net:/cvsroot/{0}"),
			new StandardRepository("sources.redhat.com", "anoncvs", null, "sources.redhat.com:/cvs/{0}", "sources.redhat.com:/cvs/{0}", "anoncvs"),
			new StandardRepository("gcc.gnu.org", "anoncvs", "gcc", "anoncvs.gnu.org:/cvsroot/{0}", "gcc.gnu.org:/cvs/{0}"),
			new StandardRepository("subversions.gnu.org", "anoncvs", "anoncvs.gnu.org:/cvsroot/{0}", "subversions.gnu.org:/cvsroot/{0}"),
			new StandardRepository("cvs.gnome.org", "anonymous", "gnome", "anoncvs.gnome.org:/cvs/{0}", "cvs.gnome.org:/cvs/{0}"),
			new StandardRepository("cvs.kde.org", "anonymous", "kde", "anoncvs.kde.org:/home/{0}", "cvs.kde.org:/home/{0}"),
			new StandardRepository("dev.eclipse.org", "anonymous", "dev.eclipse.org:/home/{0}", "dev.eclipse.org:/home/{0}"),
		};
	}
}
