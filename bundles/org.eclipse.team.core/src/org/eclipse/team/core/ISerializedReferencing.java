package org.eclipse.team.core;

import org.eclipse.core.resources.IProject;

/**
 * ISerializedReferencing manages the serializing and deserializing
 * of references to projects.  Given a project, it can produce a
 * UTF-8 encoded String which can be stored in a file.
 * Given this String, it can create in the workspace an IProject.
 */

public interface ISerializedReferencing {
	
	/**
	 * For every IProject in providerProjects, return an opaque
	 * UTF-8 encoded String to act as a reference to that project.
	 * The format of the String is specific to the provider.
	 * The format of the String must be such that
	 * ISerializedReferencing.addToWorskpace() will be able to
	 * consume it and recreate a corresponding project.
	 * @see ISerializedReferencing.addToWorkspace()
	 */
	public String[] asReference(IProject[] providerProjects) throws TeamException;
	
	/**
	 * For every String in referenceStrings, create in the workspace a
	 * corresponding IProject.  Return an Array of the resulting IProjects.
	 * Result is unspecified in the case where an IProject of that name
	 * already exists. In the case of failure, a TeamException must be thrown.
	 * The opaque strings in referenceStrings are guaranteed to have been previously
	 * produced by ISerializedReferencing.asReference().
	 * @see ISerializedReferencing.asReference()
	 */
	public IProject[] addToWorkspace(String[] referenceStrings) throws TeamException;
}
