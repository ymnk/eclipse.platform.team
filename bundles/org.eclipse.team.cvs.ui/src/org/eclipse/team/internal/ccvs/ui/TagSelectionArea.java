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
package org.eclipse.team.internal.ccvs.ui;

import java.util.*;
import java.util.List;

import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.actions.CVSAction;
import org.eclipse.team.internal.ccvs.ui.merge.*;
import org.eclipse.team.internal.ccvs.ui.merge.ProjectElement;
import org.eclipse.team.internal.ccvs.ui.merge.TagElement;
import org.eclipse.team.internal.ccvs.ui.merge.ProjectElement.ProjectElementSorter;
import org.eclipse.team.internal.ccvs.ui.repo.*;
import org.eclipse.team.internal.ui.dialogs.DialogArea;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * A dialog area that displays a list of tags for selection.
 */
public class TagSelectionArea extends DialogArea {

    /*
     * Property constant which identifies the selected tag or
     * null if no tag is selected
     */
    public static final String SELECTED_TAG = "selectedTag"; //$NON-NLS-1$
    
    /*
     * Property constant which indicates that a tag has been selected in such 
     * a way as to indicate that this is the desired tag (e.g double-click)
     */
    public static final String OPEN_SELECTED_TAG = "openSelectedTag";  //$NON-NLS-1$
    
    /*
     * Constants used to configure which tags are shown
     */
	public static final int INCLUDE_HEAD_TAG = ProjectElement.INCLUDE_HEAD_TAG;
	public static final int INCLUDE_BASE_TAG = ProjectElement.INCLUDE_BASE_TAG;
	public static final int INCLUDE_BRANCHES = ProjectElement.INCLUDE_BRANCHES;
	public static final int INCLUDE_VERSIONS = ProjectElement.INCLUDE_VERSIONS;
	public static final int INCLUDE_DATES = ProjectElement.INCLUDE_DATES;
	public static final int INCLUDE_ALL_TAGS = ProjectElement.INCLUDE_ALL_TAGS;
	
    private String message;
    private TreeViewer tagTree;
    private final int includeFlags;
    private CVSTag selection;
    
    private ICVSFolder[] folders;
    private String helpContext;

    private Text filterText;

    private TagSource tagSource;
    
    public TagSelectionArea(Dialog parentDialog, IDialogSettings settings, ICVSFolder[] folders, String message, int includeFlags, String helpContext) {
        super(parentDialog, settings);
        this.folders = folders;
        this.message = message;
        this.includeFlags = includeFlags;
        this.helpContext = helpContext;
        this.tagSource = new MultiFolderTagSource(folders);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ui.dialogs.DialogArea#createArea(org.eclipse.swt.widgets.Composite)
     */
    public void createArea(Composite parent) {
        // Create a composite for the entire area
		Composite outer = createComposite(parent, 1, true);
		initializeDialogUnits(outer);
		// Add F1 help
		if (helpContext != null) {
			WorkbenchHelp.setHelp(outer, helpContext);
		}
		
		// Create the tree area and refresh buttons with the possibility to add stuff in between
		createTagTree(outer);	
		createCustomArea(outer);
		createRefreshButtons(outer);
		
        Dialog.applyDialogFont(parent);
        handleSelectionChange();
    }

    private void createTagTree(Composite parent) {
        Composite inner = createGrabbingComposite(parent, 1);
        if (isFilteringEnabled()) {
            createWrappingLabel(inner, "Filter displayed tags (? = any character, * = any Strung):", 1);
            filterText = createText(inner, 1);
            filterText.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    handleFilterChange();
                }
            });
        }
		if (message != null) {
		    createWrappingLabel(inner, message, 1);
		}
		tagTree = createTree(inner);
		createTreeMenu();
    }

    protected void handleFilterChange() {
        Object input = tagTree.getInput();
        if (input instanceof FilteredTagList) {
            String filter = filterText.getText();
            if (filter == null || filter.length() == 0) {
                tagTree.setInput(createUnfilteredInput());
            } else {
                FilteredTagList list = (FilteredTagList)input;
                list.setPattern(filter);
                tagTree.refresh();
            }
        } else {
            String filter = filterText.getText();
            if (filter != null && filter.length() > 0) {
                FilteredTagList list = createFilteredInput();
                list.setPattern(filter);
                tagTree.setInput(list);
            }
        }
    }
    
    private FilteredTagList createFilteredInput() {
        return new FilteredTagList(tagSource, convertIncludeFlaqsToTagTypes());
    }

    private int[] convertIncludeFlaqsToTagTypes() {
        if ((includeFlags & (INCLUDE_BRANCHES + INCLUDE_VERSIONS)) > 0) {
            return new int [] { CVSTag.VERSION, CVSTag.BRANCH };
        } else if ((includeFlags & (INCLUDE_BRANCHES)) > 0) {
            return new int [] { CVSTag.BRANCH };
        } else if ((includeFlags & (INCLUDE_VERSIONS)) > 0) {
            return new int [] { CVSTag.VERSION };
        }
        return new int[] { };
    }

    private Text createText(Composite parent, int horizontalSpan) {
        Text text = new Text(parent, SWT.BORDER);
		GridData data = new GridData();
		data.horizontalSpan = horizontalSpan;
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.widthHint= 0;
		text.setLayoutData(data);
        return text;
    }

    /**
     * Return whether filtering tags shown in the tag tree is supported.
     * The default is to support filtering (<code>true</code>)
     * @return whether filtering tags shown in the tag tree is supported
     */
    protected boolean isFilteringEnabled() {
        return true;
    }

    protected void createRefreshButtons(Composite parent) {
		Runnable refresh = new Runnable() {
			public void run() {
				getShell().getDisplay().syncExec(new Runnable() {
					public void run() {
						tagTree.refresh();
					}
				});
			}
		};
        TagConfigurationDialog.createTagDefinitionButtons(getShell(), parent, folders, 
														  Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_HEIGHT), 
														  Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH),
														  refresh, refresh);
    }

    protected void createTreeMenu() {
        if ((includeFlags & ProjectElement.INCLUDE_DATES) != 0) {
	        // Create the popup menu
			MenuManager menuMgr = new MenuManager();
			Tree tree = tagTree.getTree();
			Menu menu = menuMgr.createContextMenu(tree);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					addMenuItemActions(manager);
				}
	
			});
			menuMgr.setRemoveAllWhenShown(true);
			tree.setMenu(menu);
        }
    }

    /**
     * Create aq custom area that is below the tag selection area but above the refresh busson group
     * @param parent
     */
	protected void createCustomArea(Composite parent) {
		// No default custom area
    }
	
    protected TreeViewer createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.MULTI | SWT.BORDER);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));	
		TreeViewer result = new TreeViewer(tree);
		result.setContentProvider(new WorkbenchContentProvider());
		result.setLabelProvider(new WorkbenchLabelProvider());
		result.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {				
				handleSelectionChange();
			}
		});
		// select and close on double click
		// To do: use defaultselection instead of double click
		result.getTree().addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
			    CVSTag tag = internalGetSelectedTag();
			    if (tag != null) {
			        firePropertyChangeChange(OPEN_SELECTED_TAG, null, tag);
			    }
			}
		});
		result.getControl().addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent event) {
				handleKeyPressed(event);
			}
			public void keyReleased(KeyEvent event) {
				handleKeyReleased(event);
			}
		});
		result.setSorter(new ProjectElementSorter());
		result.setInput(createUnfilteredInput());
		return result;
	}
    
	private ProjectElement createUnfilteredInput() {
        return new ProjectElement(tagSource, includeFlags);
    }

    public void handleKeyPressed(KeyEvent event) {
		if (event.character == SWT.DEL && event.stateMask == 0) {			
			deleteDateTag();
		}
	}
	private void deleteDateTag() {
		TagElement[] selectedDateTagElements = getSelectedDateTagElement();
		if (selectedDateTagElements.length == 0) return;
		for(int i = 0; i < selectedDateTagElements.length; i++){
			RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
			CVSTag tag = selectedDateTagElements[i].getTag();
			if(tag.getType() == CVSTag.DATE){
				mgr.removeDateTag(getLocation(),tag);
			}				
		}
		tagTree.refresh();
		handleSelectionChange();
	}
	
	/**
	 * Returns the selected date tag elements
	 */
	private TagElement[] getSelectedDateTagElement() {
		ArrayList dateTagElements = null;
		IStructuredSelection selection = (IStructuredSelection)tagTree.getSelection();
		if (selection!=null && !selection.isEmpty()) {
			dateTagElements = new ArrayList();
			Iterator elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = CVSAction.getAdapter(elements.next(), TagElement.class);
				if (next instanceof TagElement) {
					if(((TagElement)next).getTag().getType() == CVSTag.DATE){
						dateTagElements.add(next);
					}
				}
			}
		}
		if (dateTagElements != null && !dateTagElements.isEmpty()) {
			TagElement[] result = new TagElement[dateTagElements.size()];
			dateTagElements.toArray(result);
			return result;
		}
		return new TagElement[0];
	}
	private void addDateTag(CVSTag tag){
		if(tag == null) return;
		List dateTags = new ArrayList();
		dateTags.addAll(Arrays.asList(CVSUIPlugin.getPlugin().getRepositoryManager().getKnownTags(folders[0],CVSTag.DATE)));
		if(!dateTags.contains( tag)){
			CVSUIPlugin.getPlugin().getRepositoryManager().addDateTag(getLocation(),tag);
		}
		try {
			tagTree.getControl().setRedraw(false);
			tagTree.refresh();
			// TODO: Hack to instantiate the model before revealing the selection
			Object[] expanded = tagTree.getExpandedElements();
			tagTree.expandToLevel(2);
			tagTree.collapseAll();
			for (int i = 0; i < expanded.length; i++) {
				Object object = expanded[i];
				tagTree.expandToLevel(object, 1);
			}
			// Reveal the selection
			tagTree.reveal(new TagElement(tag));
			tagTree.setSelection(new StructuredSelection(new TagElement(tag)));
		} finally {
			tagTree.getControl().setRedraw(true);
		}
		handleSelectionChange();
	}
	private void addMenuItemActions(IMenuManager manager) {
		manager.add(new Action(Policy.bind("TagSelectionDialog.0")) { //$NON-NLS-1$
			public void run() {
				CVSTag dateTag = NewDateTagAction.getDateTag(getShell(), CVSUIPlugin.getPlugin().getRepositoryManager().getRepositoryLocationFor(folders[0]));
				addDateTag(dateTag);
			}
		});
		if(getSelectedDateTagElement().length > 0){
			manager.add(new Action(Policy.bind("TagSelectionDialog.1")) { //$NON-NLS-1$
				public void run() {
					deleteDateTag();
				}
			});			
		}

	}

	protected void handleKeyReleased(KeyEvent event) {
	}
	
	/**
	 * Updates the dialog enablement.
	 */
	protected void handleSelectionChange() {
	    CVSTag newSelection = internalGetSelectedTag();
	    if (selection != null && newSelection != null && selection.equals(newSelection)) {
	        // the selection hasn't change so return
	        return;
	    }
	    CVSTag oldSelection = selection;
	    selection = newSelection;
	    firePropertyChangeChange(SELECTED_TAG, oldSelection, selection);
	}
	
	private CVSTag internalGetSelectedTag() {
		IStructuredSelection selection = (IStructuredSelection)tagTree.getSelection();
		Object o = selection.getFirstElement();
		if (o instanceof TagElement)
		    return ((TagElement)o).getTag();
		return null;
	}
	
	private ICVSRepositoryLocation getLocation(){
		RepositoryManager mgr = CVSUIPlugin.getPlugin().getRepositoryManager();
		ICVSRepositoryLocation location = mgr.getRepositoryLocationFor(folders[0]);
		return location;
	}
    public CVSTag getSelection() {
        return selection;
    }
}
