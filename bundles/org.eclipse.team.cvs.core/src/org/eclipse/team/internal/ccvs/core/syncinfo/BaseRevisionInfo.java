/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.core.syncinfo;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.util.EmptyTokenizer;

/**
 * This class represents an entry in the CVS/Baserev file
 */
public class BaseRevisionInfo {
	
	protected static final String SEPERATOR = "/"; //$NON-NLS-1$
	protected static final char BASE_REVISION_PREFIX = 'B';
	
	private String filename;
	private String revision;
	
	public BaseRevisionInfo(String filename, String revision) {
		this.filename = filename;
		this.revision = revision;
	}
	
	public BaseRevisionInfo(String line) throws CVSException {
		if (line.charAt(0) != BASE_REVISION_PREFIX) {
			throw new CVSException(Policy.bind("BaseRevisionInfo.MalformedLine", line)); //$NON-NLS-1$
		};
		EmptyTokenizer tokenizer = new EmptyTokenizer(line.substring(1), SEPERATOR);
		if(tokenizer.countTokens() != 4) {
			throw new CVSException(Policy.bind("BaseRevisionInfo.MalformedLine", line)); //$NON-NLS-1$
		}
		filename = tokenizer.nextToken();
		revision = tokenizer.nextToken();
	}

	public String getBaserevLine() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(BASE_REVISION_PREFIX);
		buffer.append(getName());
		buffer.append(SEPERATOR);
		buffer.append(getRevision());
		buffer.append(SEPERATOR);
		return buffer.toString();
	}

	private Object getRevision() {
		return revision;
	}

	private Object getName() {
		return filename;
	}

}
