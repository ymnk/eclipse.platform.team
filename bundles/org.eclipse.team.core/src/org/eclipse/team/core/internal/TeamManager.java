package org.eclipse.team.core.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.IIgnoreInfo;
import org.eclipse.team.core.ITeamManager;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamPlugin;

public class TeamManager implements ITeamManager {

	private final static String GLOBALIGNORE_FILE = ".globalIgnores";
	
	private static Map globalIgnore = new HashMap(11);
	
	/**
	 * Start the team manager.
	 * 
	 * If this method throws an exception, it is taken as an indication that
	 * the manager initialization has failed; as a result, the client should consider
	 * team support disabled.
	 */
	public void startup() throws TeamException {
		// XXX forces loading of all repository plugins, may instead want to
		// lazilly initialize plugin descriptors then on demand, create the executable
		// extension.
		initializeRepositoryProviderTypes();
		readState();
		initializePluginIgnores();
	}
	
	/**
	 * Utility for adding a nature to a project
	 */
	static public void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws TeamException {
		try {
			IProjectDescription description = proj.getDescription();
			String[] prevNatures= description.getNatureIds();
			String[] newNatures= new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length]= natureId;
			description.setNatureIds(newNatures);
			proj.setDescription(description, monitor);
		} catch(CoreException e) {
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, Policy.bind("manager.errorSettingNature", 
														 proj.getName(), natureId), e));
		}
	}
	
	static public void removeNatureFromProject(IProject proj, String natureId, IProgressMonitor monitor) throws TeamException {
		try {
			IProjectDescription description = proj.getDescription();
			String[] prevNatures= description.getNatureIds();
			List newNatures = new ArrayList(Arrays.asList(prevNatures));
			newNatures.remove(natureId);
			description.setNatureIds((String[])newNatures.toArray(new String[newNatures.size()]));
			proj.setDescription(description, monitor);
		} catch(CoreException e) {
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, Policy.bind("manager.errorRemovingNature", 
														 proj.getName(), natureId), e));
		}
	}
	
	/**
	 * Find and initialize all the registered providers
	 */
	private void initializeRepositoryProviderTypes() throws TeamException {

		IExtensionPoint extensionPoint = Platform.getPluginRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.PROVIDER_EXTENSION);
		if (extensionPoint == null) {
			throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, Policy.bind("manager.providerExtensionNotFound"), null));
		}

		IExtension[] extensions = extensionPoint.getExtensions();
		if (extensions.length == 0)
			return;
		for (int i = 0; i < extensions.length; i++) {
			IExtension extension = extensions[i];
			IConfigurationElement[] configs = extension.getConfigurationElements();
			if (configs.length == 0) {
				// there is no configuration element
				// log as an error but continue to instantiate other executable extensions.
				TeamPlugin.log(IStatus.ERROR, Policy.bind("manager.providerNoConfigElems", extension.getUniqueIdentifier()), null);
				continue;
			}
			IConfigurationElement config = configs[0];
			String configName = config.getName();
			if (!"repository".equals(config.getName())) {
				String message = Policy.bind("resources.natureFormat", configName);
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, message, null));
			}

			try {
				RepositoryProviderType providerType = (RepositoryProviderType) config.createExecutableExtension("provider-type");
				RepositoryProviderType.addProviderType(providerType);
			} catch (ClassCastException e) {
				String message = Policy.bind("resources.natureImplement", configName);
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, message, null));
			} catch (CoreException e){
				String message = Policy.bind("resources.natureImplement", configName);
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, message, null));
			}
		}
	}
		
	public IIgnoreInfo[] getGlobalIgnore() {
		IIgnoreInfo[] result = new IIgnoreInfo[globalIgnore.size()];
		Iterator e = globalIgnore.keySet().iterator();
		int i = 0;
		while (e.hasNext() ) {
			String pattern = (String)e.next();
			boolean enabled = ((Boolean)globalIgnore.get(pattern)).booleanValue();
			result[i++] = new IgnoreInfo(pattern, enabled);
		}
		return result;
	}
	
	public void setGlobalIgnore(String[] patterns, boolean[] enabled) {
		globalIgnore = new Hashtable(11);
		for (int i = 0; i < patterns.length; i++) {
			globalIgnore.put(patterns[i], new Boolean(enabled[i]));
		}
		try {
			// make sure that we update our state on disk
			savePluginState();
		} catch (TeamException ex) {
			TeamPlugin.log(IStatus.WARNING, "setting global ignore", ex);
		}
	}
	
	/*
	 * Reads the ignores currently defined by extensions.
	 */
	private void initializePluginIgnores() {
		TeamPlugin plugin = TeamPlugin.getPlugin();
		if (plugin != null) {
			IExtensionPoint extension = plugin.getDescriptor().getExtensionPoint(TeamPlugin.IGNORE_EXTENSION);
			if (extension != null) {
				IExtension[] extensions =  extension.getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
					for (int j = 0; j < configElements.length; j++) {
						String pattern = configElements[j].getAttribute("pattern");
						if (pattern != null) {
							String selected = configElements[j].getAttribute("selected");
							boolean enabled = selected != null && selected.equalsIgnoreCase("true");
							// if this ignore doesn't already exist, add it to the global list
							if (!globalIgnore.containsKey(pattern)) {
								globalIgnore.put(pattern, new Boolean(enabled));
							}
						}
					}
				}
			}		
		}
	}
	
	/*
	 * Save global ignore file
	 */
	private void savePluginState() throws TeamException {
		// save global ignore list to disk
		IPath pluginStateLocation = TeamPlugin.getPlugin().getStateLocation();
		File tempFile = pluginStateLocation.append(GLOBALIGNORE_FILE + ".tmp").toFile();
		File stateFile = pluginStateLocation.append(GLOBALIGNORE_FILE).toFile();
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(tempFile));
			writeState(dos);
			dos.close();
			if (stateFile.exists())
				stateFile.delete();
			boolean renamed = tempFile.renameTo(stateFile);
			if (!renamed)
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, "renaming", null));
		} catch (IOException ex) {
			throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, "closing stream", ex));
		}
	}
	
	/*
	 * Write the global ignores to the stream
	 */
	private void writeState(DataOutputStream dos) throws IOException {
		// write the global ignore list
		int ignoreLength = globalIgnore.size();
		dos.writeInt(ignoreLength);
		Iterator e = globalIgnore.keySet().iterator();
		while (e.hasNext()) {
			String pattern = (String)e.next();
			boolean enabled = ((Boolean)globalIgnore.get(pattern)).booleanValue();
			dos.writeUTF(pattern);
			dos.writeBoolean(enabled);
		}
	}
	
	private void readState() throws TeamException {
		// read saved repositories list and ignore list from disk, only if the file exists
		IPath pluginStateLocation = TeamPlugin.getPlugin().getStateLocation().append(GLOBALIGNORE_FILE);
		File f = pluginStateLocation.toFile();
		if(f.exists()) {
			try {
				DataInputStream dis = new DataInputStream(new FileInputStream(f));
				globalIgnore = new Hashtable(11);
				int ignoreCount = 0;
				try {
					ignoreCount = dis.readInt();
				} catch (EOFException e) {
					// Ignore the exception, it will occur if there are no ignore
					// patterns stored in the provider state file.
					return;
				}
				for (int i = 0; i < ignoreCount; i++) {
					String pattern = dis.readUTF();
					boolean enabled = dis.readBoolean();
					globalIgnore.put(pattern, new Boolean(enabled));
				}
			} catch(FileNotFoundException e) {
				// not a fatal error, there just happens not to be any state to read
			} catch (IOException ex) {
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, "closing stream", ex));			
			}
		}
	}
}