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
package org.eclipse.team.core.change;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.TeamPlugin;
import org.osgi.service.prefs.Preferences;

/**
 * A change set is used to group a set of changes together
 * for some purpose, whether it be to display them together in
 * the UI or check them into a repository as one checkin.
 */
public class ChangeSet {
    
    private static final String CTX_TITLE = "title"; //$NON-NLS-1$
    private static final String CTX_COMMENT = "comment"; //$NON-NLS-1$
    private static final String CTX_RESOURCES = "resources"; //$NON-NLS-1$
    
    Set resources = new HashSet();
    private String title;
    private String comment;
    private final SubscriberChangeSetManager manager;
    
	/**
	 * Create a change set with the given title
	 * @param manager the manager that owns this set
     * @param title the title of the set
     */
    public ChangeSet(SubscriberChangeSetManager manager, String title) {
        this.manager = manager;
        this.title = title;
    }

    /**
     * Get the title of the change set. The title is used
     * as the comment when the set is checking in if no comment
     * has been explicitly set using <code>setComment</code>.
     * @return the title of the set
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Set the title of the set. The title is used
     * as the comment when the set is committed if no comment
     * has been explicitly set using <code>setComment</code>.
     * @param title the title of the set
     */
    public void setTitle(String title) {
        this.title = title;
        getManager().titleChanged(this);
    }
    
    /**
     * Get the comment of this change set. If the comment
     * as never been set, the title is returned as the comment
     * @return the comment to be used when the set is committed
     */
    public String getComment() {
        if (comment == null) {
            return getTitle();
        }
        return comment;
    }
    
    /**
     * Set the comment to be used when the change set is committed.
     * If <code>null</code> is passed, the title of the set
     * will be used as the comment.
     * @param comment the comment for the set or <code>null</code>
     * if the title should be the comment
     */
    public void setComment(String comment) {
        if (comment != null && comment.equals(getTitle())) {
            this.comment = null;
        } else {
            this.comment = comment;
        }
    }
    
    /**
     * Remove the resource from the set.
     * @param resource the resource to be removed
     */
    public void remove(IResource resource) {
        if (this.resources.remove(resource)) {
            getManager().resourceChanged(this, resource);
        }
    }
    
    /**
     * Remove the resources from the set.
     * @param resources the resources to be removed
     */
    public void remove(IResource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            remove(resource);
        }
    }

    /**
     * The given resource was removed. Ensure that it and
     * any descendants are also removed.
     */
    public void rootRemoved(IResource root) {
        for (Iterator iter = resources.iterator(); iter.hasNext();) {
            IResource resource = (IResource) iter.next();
            if (root.getFullPath().isPrefixOf(resource.getFullPath())) {
                remove(resource);
            }
        }
    }
    
    /**
     * Add the resource to this set if it is modified
     * w.r.t. the subscriber.
     * @param resource
     * @throws TeamException
     */
    public void add(IResource resource) throws TeamException {
        if (addResource(resource)) {
            getManager().resourceAdded(this, resource);
        }
    }
    
    /**
     * Add the resources to this set if they are modified
     * w.r.t. the subscriber.
     * @param resources the resources to be added.
     * @throws TeamException
     */
    public void add(IResource[] resources) throws TeamException {
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            add(resource);
        }
    }

    private boolean addResource(IResource resource) throws TeamException {
        if (getManager().isModified(resource)) {
            resources.add(resource);
            return true;
        }
        return false;
    }

    private SubscriberChangeSetManager getManager() {
        return manager;
    }

    /**
     * Return whether the set contains any files.
     * @return whether the set contains any files
     */
    public boolean isEmpty() {
        return resources.isEmpty();
    }

    /**
     * Return true if the given file is included in this set.
     * @param local a ocal file
     * @return true if the given file is included in this set
     */
    public boolean contains(IResource local) {
        return resources.contains(local);
    }

    /**
     * Return whether the set has a comment that differs from the title.
     * @return whether the set has a comment that differs from the title
     */
    public boolean hasComment() {
        return comment != null;
    }
    
    /**
     * Return the resources that are contained in this set.
     * @return the resources that are contained in this set
     */
    public IResource[] getResources() {
        return (IResource[]) resources.toArray(new IResource[resources.size()]);
    }
    
    public void save(Preferences prefs) {
        prefs.put(CTX_TITLE, getTitle());
        if (comment != null) {
            prefs.put(CTX_COMMENT, comment);
        }
        if (!resources.isEmpty()) {
	        StringBuffer buffer = new StringBuffer();
	        for (Iterator iter = resources.iterator(); iter.hasNext();) {
	            IResource resource = (IResource) iter.next();
	            buffer.append(resource.getFullPath().toString());
	            buffer.append('\n');
	        }
	        prefs.put(CTX_RESOURCES, buffer.toString());
        }
    }

    public void init(Preferences prefs) {
        title = prefs.get(CTX_TITLE, ""); //$NON-NLS-1$
        comment = prefs.get(CTX_COMMENT, null);
        String resourcePaths = prefs.get(CTX_RESOURCES, null);
        if (resourcePaths != null) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            StringTokenizer tokenizer = new StringTokenizer(resourcePaths, "\n"); //$NON-NLS-1$
            while (tokenizer.hasMoreTokens()) {
                String next = tokenizer.nextToken();
                if (next.trim().length() > 0) {
                    IResource resource = root.findMember(next);
                    if (resource != null) {
                        try {
                            addResource(resource);
                        } catch (TeamException e) {
                            TeamPlugin.log(e);
                        }
                    }
                }
            }
        }
    }
}
