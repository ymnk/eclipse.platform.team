/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import java.util.Date;

/**
 * A checked-in change set represents a group of resource
 * changes that were previously checked into a repository
 * as a single logical change. 
 * <p>
 * A previously checked-in set of changes may not apply directly
 * to the local versions of the resources involved. However,
 * a <code>SyncInfo</code> is still used to reprsent each change.
 * The base and remote slots of the <code>SyncInfo</code> identify
 * the state before and after the resources were checked-in.
 * @since 3.1
 */
public class CheckedInChangeSet extends ChangeSet {

    private String comment;
    private String author;
    private Date date;
    
    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.ChangeSet#getComment()
     */
    public String getComment() {
        return comment;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public Date getDate() {
        return date;
    }
}
