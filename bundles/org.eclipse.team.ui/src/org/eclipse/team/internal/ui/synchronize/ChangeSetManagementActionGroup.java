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
package org.eclipse.team.internal.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.core.subscribers.ActiveChangeSet;
import org.eclipse.team.core.subscribers.SubscriberChangeSetManager;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.AndSyncInfoFilter;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter.SyncInfoDirectionFilter;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * This action group contributes actions that support the management
 * of Change sets to a synchronize page.
 */
public abstract class ChangeSetManagementActionGroup extends SynchronizePageActionGroup {

    public final static String CHANGE_SET_GROUP = "chaneg_set_group"; //$NON-NLS-1$
    
    public static final AndSyncInfoFilter OUTGOING_FILE_FILTER = new AndSyncInfoFilter(new FastSyncInfoFilter[] {
            new FastSyncInfoFilter() {
                public boolean select(SyncInfo info) {
                    return info.getLocal().getType() == IResource.FILE;
                }
            },
            new SyncInfoDirectionFilter(new int[] { SyncInfo.OUTGOING, SyncInfo.CONFLICTING })
    });
    
	private class CreateChangeSetAction extends SynchronizeModelAction {
	    
        public CreateChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(Policy.bind("ChangeLogModelProvider.0"), configuration); //$NON-NLS-1$
        }
        
		/* (non-Javadoc)
		 * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#needsToSaveDirtyEditors()
		 */
		protected boolean needsToSaveDirtyEditors() {
			return false;
		}
        
        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
         */
        protected FastSyncInfoFilter getSyncInfoFilter() {
            return OUTGOING_FILE_FILTER;
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
         */
        protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
            return new SynchronizeModelOperation(configuration, elements) {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    syncExec(new Runnable() {
                        public void run() {
		                    SyncInfo[] infos = getSyncInfoSet().getSyncInfos();
		                    ActiveChangeSet set = createChangeSet(getChangeSetManager(), infos);
		            		getChangeSetManager().add(set);
                        }
                    });
                }
            };
        }
	}

	private abstract class ChangeSetAction extends BaseSelectionListenerAction {

        public ChangeSetAction(String title, ISynchronizePageConfiguration configuration) {
            super(title);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.ui.actions.BaseSelectionListenerAction#updateSelection(org.eclipse.jface.viewers.IStructuredSelection)
         */
        protected boolean updateSelection(IStructuredSelection selection) {
            return getSelectedSet() != null;
        }

        protected ActiveChangeSet getSelectedSet() {
            IStructuredSelection selection = getStructuredSelection();
            if (selection.size() == 1) {
                Object first = selection.getFirstElement();
                return (ActiveChangeSet)TeamAction.getAdapter(first, ActiveChangeSet.class); 
            }
            return null;
        }
	}
	
	private class EditChangeSetAction extends ChangeSetAction {

        public EditChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(Policy.bind("ChangeLogModelProvider.6"), configuration); //$NON-NLS-1$
        }
        
        public void run() {
            ActiveChangeSet set = getSelectedSet();
            if (set == null) return;
    		editChangeSet(set);
        }
	}
	
	private class MakeDefaultChangeSetAction extends ChangeSetAction {

        public MakeDefaultChangeSetAction(ISynchronizePageConfiguration configuration) {
            super(Policy.bind("ChangeLogModelProvider.9"), configuration); //$NON-NLS-1$
        }
        
        public void run() {
            ActiveChangeSet set = getSelectedSet();
            if (set == null) return;
    		getChangeSetManager().makeDefault(set);
        }
	    
	}
	
	private class AddToChangeSetAction extends SynchronizeModelAction {
	 
        private final ActiveChangeSet set;
	    
        public AddToChangeSetAction(ISynchronizePageConfiguration configuration, ActiveChangeSet set, ISelection selection) {
            super(set.getTitle(), configuration);
            this.set = set;
            selectionChanged(selection);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSyncInfoFilter()
         */
        protected FastSyncInfoFilter getSyncInfoFilter() {
            return OUTGOING_FILE_FILTER;
        }
        
		protected boolean needsToSaveDirtyEditors() {
			return false;
		}
        
        /* (non-Javadoc)
         * @see org.eclipse.team.ui.synchronize.SynchronizeModelAction#getSubscriberOperation(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration, org.eclipse.compare.structuremergeviewer.IDiffElement[])
         */
        protected SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
            return new SynchronizeModelOperation(configuration, elements) {
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    set.add(getSyncInfoSet().getSyncInfos());
                }
            };
        }
	}
	
	private CreateChangeSetAction createChangeSet;
	private MenuManager addToChangeSet;
    private EditChangeSetAction editChangeSet;
    private MakeDefaultChangeSetAction makeDefault;
    private boolean alive = false;
    
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);
		if (getChangeSetManager() == null) return;
		alive = true;
		addToChangeSet = new MenuManager(Policy.bind("ChangeLogModelProvider.12")); //$NON-NLS-1$
		addToChangeSet.setRemoveAllWhenShown(true);
		addToChangeSet.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                addChangeSets(manager);
            }
        });
		createChangeSet = new CreateChangeSetAction(configuration);
		addToChangeSet.add(createChangeSet);
		addToChangeSet.add(new Separator());
		editChangeSet = new EditChangeSetAction(configuration);
		makeDefault = new MakeDefaultChangeSetAction(configuration);
		
		appendToGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CHANGE_SET_GROUP, 
				addToChangeSet);
		appendToGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CHANGE_SET_GROUP, 
				editChangeSet);
		appendToGroup(
				ISynchronizePageConfiguration.P_CONTEXT_MENU, 
				CHANGE_SET_GROUP, 
				makeDefault);
	}
	
    protected void addChangeSets(IMenuManager manager) {
        ActiveChangeSet[] sets = getChangeSetManager().getSets();
        ISelection selection = getContext().getSelection();
        createChangeSet.selectionChanged(selection);
		addToChangeSet.add(createChangeSet);
		addToChangeSet.add(new Separator());
        for (int i = 0; i < sets.length; i++) {
            ActiveChangeSet set = sets[i];
            AddToChangeSetAction action = new AddToChangeSetAction(getConfiguration(), set, selection);
            manager.add(action);
        }
    }

    /**
     * Return the change set manager for the current page.
     * @return the change set manager for the current page
     */
    protected SubscriberChangeSetManager getChangeSetManager() {
        return (SubscriberChangeSetManager)getConfiguration().getProperty(ISynchronizePageConfiguration.P_CHANGE_SET_MANAGER);
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#dispose()
	 */
	public void dispose() {
	    if (alive) {
			addToChangeSet.dispose();
			addToChangeSet.removeAll();
	    }
		super.dispose();
	}
	
	
    public void updateActionBars() {
        if (alive) {
	        editChangeSet.selectionChanged((IStructuredSelection)getContext().getSelection());
	        makeDefault.selectionChanged((IStructuredSelection)getContext().getSelection());
        }
        super.updateActionBars();
    }
    
    private void syncExec(final Runnable runnable) {
		final Control ctrl = getConfiguration().getPage().getViewer().getControl();
		if (ctrl != null && !ctrl.isDisposed()) {
			ctrl.getDisplay().syncExec(new Runnable() {
				public void run() {
					if (!ctrl.isDisposed()) {
					    runnable.run();
					}
				}
			});
		}
    }
    
    /**
     * Create a change set from the given manager that contains the given sync info.
     * This method is invoked from the UI thread.
     * @param manager a change set manager
     * @param infos the sync info to be added to the change set
     * @return the created set.
     */
    protected abstract ActiveChangeSet createChangeSet(SubscriberChangeSetManager manager, SyncInfo[] infos);
    
    /**
     * Edit the title and comment of the given change set.
     * This method is invoked from the UI thread.
     * @param set the set to be edited
     */
    protected abstract void editChangeSet(ActiveChangeSet set);
}
