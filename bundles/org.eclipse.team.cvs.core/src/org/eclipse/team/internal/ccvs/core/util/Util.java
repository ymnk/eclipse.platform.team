package org.eclipse.team.internal.ccvs.core.util;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.ccvs.core.*;
import org.eclipse.team.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.ccvs.core.ICVSFolder;
import org.eclipse.team.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.Policy;

/**
 * Unsorted static helper-methods 
 */
public class Util {
	
	private static final String AT = "@"; //$NON-NLS-1$
	private static final String COLON = ":"; //$NON-NLS-1$
	
	
	// private static final String newLine = System.getProperty("line.separator");
	
		
	/**
	 * @see Util#getOptions(String[],String,boolean)
	 */
	public static String getOption(String[] options, String key, boolean deleteOption) {
		
		String[] result;
		
		result = getOptions(options,key,deleteOption);
		
		if (result.length == 0) {
			return null;
		} else {
			return result[0];
		}
	}
	
	/**
	 * Get an option out of an array of options. It assumes, that 
	 * the next field to the key contains the parameter to the 
	 * option.
	 * 
	 * @param options not null
	 * @param key not null
	 * @param deleteOption nulls both the option-tag and the information 
	 * @return String[0] if the option could not be found
	 */	
	public static String[] getOptions(String[] options, String key, boolean deleteOption) {
		
		String[] tmpResult;
		String[] result;
		int size = 0;
		
		Assert.isNotNull(options);
		Assert.isNotNull(key);
		
		tmpResult = new String[options.length];
		
		for (int i=0; i<options.length; i++) {
			if (key.equals(options[i]) && i<options.length-1) {
				tmpResult[size++] = options[i+1];
				
				// This should be done in another way maybe we should 
				// have an options Object or give the array modified
				// back.
				// Maybe we are going to change that.
				if (deleteOption) {
					options[i] = null;
					options[i+1] = null;
				}
			}
		}
		
		result = new String[size];
		System.arraycopy(tmpResult,0,result,0,size);
		return result;
	}
	
	/**
	 * Checks wether the Array options contains the String 
	 * key.
	 * @param options not null
	 * @param key not null
	 */	
	public static boolean isOption(String[] options, String key) {

		Assert.isNotNull(options);
		Assert.isNotNull(key);

		for (int i=0; i<options.length; i++) {
			if (key.equals(options[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove or get the password out of a repoString. 
	 */
	// FIXME: This is only used for tests ... move it	
	private static String passwordHandle(String repoName, boolean remove) {
		
		int atPlace = -1;
		int colonPlace = -1;
		int colonCount = 0;
		String currentChar; 
		
		Assert.isTrue(repoName.indexOf(AT) != -1);
		Assert.isTrue(repoName.indexOf(COLON) != -1);
		
		for (int i=0; i<repoName.length(); i++) {
			
			currentChar = repoName.substring(i,i+1);
			
			if (currentChar.equals(COLON)) {
				colonCount++;
				
				if (colonCount == 3) {
					colonPlace = i;
				}
			}
			
			if (currentChar.equals(AT)) {
				if (colonPlace == -1) {
					
					// If the @ comes before the third colon, then 
					// we do not have a password and return with the
					// same string
					return repoName;
				} else {
					atPlace = i;
				}
				
			}
		}
		
		if (atPlace == -1) {
			return repoName;
		}
		
		if (remove) {
			return repoName.substring(0,colonPlace) + repoName.substring(atPlace);
		} else {
			return repoName.substring(colonPlace + 1, atPlace);
		}
	}

	/**
	 * returns ":pserver:nkrambro@fiji:/home/nkrambro/repo"
	 *         when you insert ":pserver:nkrambro:password@fiji:/home/nkrambro/repo"
	 */
	// FIXME: This is only used for tests ... move it	
	public static String removePassword(String root) {
		return passwordHandle(root,true);
	}
	
	/**
	 * 
	 * returns "password"
	 *         when you insert ":pserver:nkrambro:password@fiji:/home/nkrambro/repo"
	 */	
	// FIXME: This is only used for tests ... move it	
	
	// FIXME: This is only used for tests ... move it	
	
	/**
	 * Get the extention of the path of resource
	 * relative to the path of root
	 * 
	 * @throws CVSException if root is not a root-folder of resource
	 */
	public static String getRelativePath(String rootName, String resourceName) 
		throws CVSException {

		if (!resourceName.startsWith(rootName)) {
			throw new CVSException(Policy.bind("Util.Internal_error,_resource_does_not_start_with_root_3")); //$NON-NLS-1$
		}
		
		// Otherwise we would get an ArrayOutOfBoundException
		// in case of two equal Resources
		if (rootName.length() == resourceName.length()) {
			return ""; //$NON-NLS-1$
		}
		
		// Get rid of the seperator, that would be in the 
		// beginning, if we did not go from +1
		return resourceName.substring(rootName.length() + 1).replace('\\', '/');
	}
	
	/**
	 * Append the prefix and suffix to form a valid CVS path.
	 */
	public static String appendPath(String prefix, String suffix) {
		if (prefix.endsWith(Session.SERVER_SEPARATOR)) {
			if (suffix.startsWith(Session.SERVER_SEPARATOR))
				return prefix + suffix.substring(1);
			else
				return prefix + suffix;
		} else if (suffix.startsWith(Session.SERVER_SEPARATOR))
			return prefix + suffix;
		else
			return prefix + Session.SERVER_SEPARATOR + suffix;
	}
	
	
	public static void logError(String message, Throwable throwable) {
		CVSProviderPlugin.log(new Status(IStatus.ERROR, CVSProviderPlugin.ID, IStatus.ERROR, message, throwable));
	}
	
	/**
	 * If the number of segments in the relative path of <code>resource</code> to <code>root</code> is 
	 * greater than <code>split</code> then the returned path is truncated to <code>split</code> number
	 * of segments and '...' is shown as the first segment of the path.
	 */
	public static String toTruncatedPath(ICVSResource resource, ICVSFolder root, int split) {
		try {
			IPath path = new Path(resource.getRelativePath(root));
			int segments = path.segmentCount();
			if(segments>split) {				
				IPath last = path.removeFirstSegments(segments - split);
				return "..." + path.SEPARATOR + last.toString();
			}
			return path.toString();
		} catch(CVSException e) {
			return resource.getName();
		}
	}
}