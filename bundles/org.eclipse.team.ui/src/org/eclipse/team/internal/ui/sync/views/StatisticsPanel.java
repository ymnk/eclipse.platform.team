/*************.******************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.sync.views;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.ui.IWorkingSet;

/**
 * Composite that displays statistics relating to Synchronization information. This panel displays the number of changes
 * for the three change directions. The labels have smart resize behavior and when more space is available the labels
 * are more verbose, whereas when the space is no longer available the labels are made less verbose.
 *
 * @since 3.0 
 */
public class StatisticsPanel extends Composite {
	
	private class DirectionLabel {
		public Image image;
		public String descriptionText;
		public Label descriptionLabel;
		public Label valueLabel;
		DirectionLabel(Composite parent, String description, String initialValue, Image image) {
			Label label= new Label(parent, SWT.NONE);
			if (image != null) {
				this.image = image;
				image.setBackground(label.getBackground());
				label.setImage(image);
			}
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			
			this.descriptionText = description;
			descriptionLabel= new Label(parent, SWT.NONE);
			descriptionLabel.setText(description);
			descriptionLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
			
			valueLabel= new Label(parent, SWT.NONE);
			valueLabel.setText(initialValue);			
			
			valueLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			valueLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
		}
		
		void updateTooltips() {
			if(stats != null) {
				IWorkingSet ws = stats.getSubscriberInput().getWorkingSet();
				if(ws != null) {
					valueLabel.setToolTipText(Policy.bind("StatisticsPanel.numbersWorkingSetTooltip", descriptionText, ws.getName())); //$NON-NLS-1$
					valueLabel.setToolTipText(Policy.bind("StatisticsPanel.numbersWorkingSetTooltip", descriptionText, ws.getName())); //$NON-NLS-1$
				} else {
					valueLabel.setToolTipText(Policy.bind("StatisticsPanel.numbersTooltip", descriptionText)); //$NON-NLS-1$
					valueLabel.setToolTipText(Policy.bind("StatisticsPanel.numbersTooltip", descriptionText)); //$NON-NLS-1$
				}
			}
		}
	}
	
	private DirectionLabel outgoingDirectionLabel;
	private DirectionLabel incomingDirectionLabel;
	private DirectionLabel conflictingDirectionLabel;
	
	private ViewStatusInformation stats;
	
	private Tracker tracker;
	private ToolBarManager toolbarManager;
	
	public StatisticsPanel(Composite parent) {
		super(parent, SWT.NONE);
		
		GridLayout gridLayout= new GridLayout();
		//gridLayout.numColumns= 9;
		//gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		//gridLayout.numColumns = 2;
		setLayout(gridLayout);
		
		GridData data = new GridData();
		//		data.horizontalAlignment = GridData.FILL;
		setLayoutData(data);
		
		//conflictingDirectionLabel = new DirectionLabel(this, Policy.bind("StatisticsPanel.conflicting"), "0", TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_CONFLICTING).createImage()); //$NON-NLS-1$ //$NON-NLS-2$
		//incomingDirectionLabel = new DirectionLabel(this, Policy.bind("StatisticsPanel.incoming"), "0", TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_INCOMING).createImage()); //$NON-NLS-1$ //$NON-NLS-2$
		//outgoingDirectionLabel = new DirectionLabel(this, Policy.bind("StatisticsPanel.outgoing"), "0", TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_OUTGOING).createImage()); //$NON-NLS-1$ //$NON-NLS-2$
		
//		Label l = new Label(this, SWT.NONE);
//		l.setImage(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_OUTGOING).createImage());
//		MouseMoveListener mouseMoveListener = new MouseMoveListener() {
//			public void mouseMove(MouseEvent event) {
//				if(tracker == null) {
//					tracker = new Tracker(StatisticsPanel.this, SWT.LEFT);
//					tracker.setRectangles(new Rectangle[] {getBounds()});					
//				}
//				tracker.open();
//			}
//		};
//		l.addMouseMoveListener(mouseMoveListener);
//		MouseListener mouseListener = new MouseListener() {
//			public void mouseDoubleClick(MouseEvent e) {
//				//handleMouseDoubleClick(e);
//			}
//			public void mouseDown(MouseEvent e) {}
//			public void mouseUp(MouseEvent e) {
//				//handleMouseUp(e);
//			}
//		};
//		l.addMouseListener(mouseListener);
		
	}
	
	protected void disposeIcons() {
		//outgoingDirectionLabel.image.dispose();
		//incomingDirectionLabel.image.dispose();
		//conflictingDirectionLabel.image.dispose();
	}
	
	private void fixLabelsOnResize() {
		// setup all labels for initial calculation
		//		outgoingDirectionLabel.descriptionLabel.setText(outgoingDirectionLabel.descriptionText);
		//		incomingDirectionLabel.descriptionLabel.setText(incomingDirectionLabel.descriptionText);
		//		conflictingDirectionLabel.descriptionLabel.setText(conflictingDirectionLabel.descriptionText);
		//		layout(true);	
		//		
		//		Point preferredSize = outgoingDirectionLabel.valueLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		//		Rectangle currentSize = outgoingDirectionLabel.valueLabel.getBounds();
		//		if(currentSize.width < preferredSize.y && outgoingDirectionLabel.descriptionLabel.getText().length() != 0) {
		//			outgoingDirectionLabel.descriptionLabel.setText(""); //$NON-NLS-1$
		//			incomingDirectionLabel.descriptionLabel.setText(""); //$NON-NLS-1$
		//			conflictingDirectionLabel.descriptionLabel.setText("");					 //$NON-NLS-1$
		//		} else if(outgoingDirectionLabel.descriptionLabel.getText().length() == 0){
		//			outgoingDirectionLabel.descriptionLabel.setText(outgoingDirectionLabel.descriptionText);
		//			incomingDirectionLabel.descriptionLabel.setText(incomingDirectionLabel.descriptionText);
		//			conflictingDirectionLabel.descriptionLabel.setText(conflictingDirectionLabel.descriptionText);
		//		}
		//		layout(true);
		//		redraw();
	}
	
	public void update(ViewStatusInformation stats) {
		this.stats = stats;
		updateStats();
	}
	
	private void updateStats() {
		//		if(stats != null && ! isDisposed()) {
		//			
		//			SyncInfoStatistics workspaceSetStats = stats.getSubscriberInput().getSubscriberSyncSet().getStatistics();
		//			SyncInfoStatistics workingSetSetStats = stats.getSubscriberInput().getWorkingSetSyncSet().getStatistics();
		//			
		//			int workspaceConflicting = (int)workspaceSetStats.countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK);
		//			int workspaceOutgoing = (int)workspaceSetStats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		//			int workspaceIncoming = (int)workspaceSetStats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
		//			int workingSetConflicting = (int)workingSetSetStats.countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK);
		//			int workingSetOutgoing = (int)workingSetSetStats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
		//			int workingSetIncoming = (int)workingSetSetStats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
		//			
		//			if(stats.getSubscriberInput().getWorkingSet() != null) {
		//				conflictingDirectionLabel.valueLabel.setText(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetConflicting).toString(), new Integer(workspaceConflicting).toString())); //$NON-NLS-1$
		//				incomingDirectionLabel.valueLabel.setText(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetIncoming).toString(), new Integer(workspaceIncoming).toString())); //$NON-NLS-1$
		//				outgoingDirectionLabel.valueLabel.setText(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetOutgoing).toString(), new Integer(workspaceOutgoing).toString())); //$NON-NLS-1$
		//			} else {
		//				conflictingDirectionLabel.valueLabel.setText(new Integer(workspaceConflicting).toString()); //$NON-NLS-1$
		//				incomingDirectionLabel.valueLabel.setText(new Integer(workspaceIncoming).toString()); //$NON-NLS-1$
		//				outgoingDirectionLabel.valueLabel.setText(new Integer(workspaceOutgoing).toString()); //$NON-NLS-1$
		//			}
		//			updateTooltips();											
		//			redraw();						
		//		}
	}
	
	void updateTooltips() {
		conflictingDirectionLabel.updateTooltips();
		incomingDirectionLabel.updateTooltips();
		outgoingDirectionLabel.updateTooltips();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();
		disposeIcons();
	}
}