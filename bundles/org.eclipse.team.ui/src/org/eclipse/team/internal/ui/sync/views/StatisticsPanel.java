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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.subscribers.SyncInfo;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.sync.sets.SyncInfoStatistics;
import org.eclipse.team.ui.ISharedImages;

/**
 * Composite that displays statistics relating to Synchronization information.
 *
 * @since 3.0 
 */
public class StatisticsPanel extends Composite {
			
	private ViewStatusInformation stats;
	
	private Text nOutgoing;
	private Text nIncoming;
	private Text nConflicting;
	
	private final Image iOutgoing = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_OUTGOING).createImage();
	private final Image iIncoming = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_INCOMING).createImage();
	private final Image iConflicting = TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DLG_SYNC_CONFLICTING).createImage();
			
	public StatisticsPanel(Composite parent) {
		super(parent, SWT.WRAP);
		
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns= 9;
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 3;
		gridLayout.marginHeight= 1;
		setLayout(gridLayout);
		
		nConflicting = createLabel(Policy.bind("StatisticsPanel.conflicting"), iConflicting, "0/0");
		nIncoming = createLabel(Policy.bind("StatisticsPanel.incoming"), iIncoming, "0/0");
		nOutgoing = createLabel(Policy.bind("StatisticsPanel.outgoing"), iOutgoing, "0/0");
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
		
		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
// Need code to get rid of the direction text if the control is resized too much				
//				System.out.println("Client Area: " + nOutgoing.getClientArea().width);
//				System.out.println("Bounds Area: " + nOutgoing.getBorderWidth());
//				System.out.println("Border Area: " + nOutgoing.getBounds().width);
//				GC gc = new GC(StatisticsPanel.this);
//				gc.setFont(nOutgoing.getFont());
//				FontMetrics fontMetrics = gc.getFontMetrics();
//				int pixelWidth = fontMetrics.getAverageCharWidth() * nOutgoing.getCharCount();
//				System.out.println("Char width: " + pixelWidth);
				redraw();
			}
		});	
		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});
		
		setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
	}

	protected void disposeIcons() {
		iOutgoing.dispose();
		iIncoming.dispose();
		iConflicting.dispose();
	}
	
	private Text createLabel(String name, Image image, String init) {
		Label label= new Label(this, SWT.NONE);
		if (image != null) {
			image.setBackground(label.getBackground());
			label.setImage(image);
		}
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
	
		label= new Label(this, SWT.NONE);
		label.setText(name);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		Text value= new Text(this, SWT.READ_ONLY);
		value.setText(init);
		 
		value.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
		return value;
	}
	
	private void paint(PaintEvent e) {				
	}
	
	public void update(ViewStatusInformation stats) {
		this.stats = stats;
		updateStats();
	}

	private void updateStats() {
		if(stats != null) {
			
			SyncInfoStatistics workspaceSetStats = stats.getSubscriberInput().getSubscriberSyncSet().getStatistics();
			SyncInfoStatistics workingSetSetStats = stats.getSubscriberInput().getWorkingSetSyncSet().getStatistics();
			
			int workspaceConflicting = (int)workspaceSetStats.countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK);
			int workspaceOutgoing = (int)workspaceSetStats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
			int workspaceIncoming = (int)workspaceSetStats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
			int workingSetConflicting = (int)workingSetSetStats.countFor(SyncInfo.CONFLICTING, SyncInfo.DIRECTION_MASK);
			int workingSetOutgoing = (int)workingSetSetStats.countFor(SyncInfo.OUTGOING, SyncInfo.DIRECTION_MASK);
			int workingSetIncoming = (int)workingSetSetStats.countFor(SyncInfo.INCOMING, SyncInfo.DIRECTION_MASK);
			
			nConflicting.setText(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetConflicting).toString(), new Integer(workspaceConflicting).toString()));
			nIncoming.setText(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetIncoming).toString(), new Integer(workspaceIncoming).toString()));
			nOutgoing.setText(Policy.bind("StatisticsPanel.changeNumbers", new Integer(workingSetOutgoing).toString(), new Integer(workspaceOutgoing).toString()));
														
			redraw();						
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();
		disposeIcons();
	}
}