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
package org.eclipse.team.internal.ccvs.ui.merge;

import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;

/**
 * A tag source provides access to a set of tags.
 */
public abstract class TagSource {
    
    public abstract CVSTag[] getTags(int type);
    
    public CVSTag[] getTags(int[] types) {
        if (types.length == 0) {
            return new CVSTag[0];
        }
        if (types.length == 1) {
            return getTags(types[0]);
        }
        List result = new ArrayList();
        for (int i = 0; i < types.length; i++) {
            int type = types[i];
            CVSTag[] tags = getTags(type);
            result.addAll(Arrays.asList(tags));
        }
        return (CVSTag[]) result.toArray(new CVSTag[result.size()]);
    }

    /**
     * Refresh the tags by contacting the server if appropriate
     * @param monitor a progress monitor
     */
    public abstract void refresh(IProgressMonitor monitor) throws TeamException;
}
