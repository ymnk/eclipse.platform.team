/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.part.Page;

/**
 * Page that displays the overview information for Synchronize participants.
 * 
 * @since 3.0
 */
public class SynchronizeOverviewPage extends Page implements ISynchronizeParticipantListener {

	private Composite pageComposite;
	private Map participantsToComposites = new HashMap();
	private Color white;
	private SynchronizeView view;
	
	public SynchronizeOverviewPage(SynchronizeView view) {
		this.view = view;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		white = new Color(parent.getDisplay(), new RGB(255, 255, 255));
		pageComposite = new Composite(parent, SWT.NONE);
		pageComposite.setBackground(white);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		pageComposite.setLayout(layout);

		createTitleArea(pageComposite);

		Label titleBarSeparator = new Label(pageComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		titleBarSeparator.setLayoutData(gd);
		
		createParticipants(pageComposite);
		TeamUI.getSynchronizeManager().addSynchronizeParticipantListener(this);
	}

	/**
	 * @param pageComposite2
	 */
	private void createParticipants(Composite parent) {
		ISynchronizeParticipant[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (int i = 0; i < participants.length; i++) {
			ISynchronizeParticipant participant = participants[i];
			participantsToComposites.put(participant, new ParticipantComposite(parent, participant, view, SWT.NONE));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#getControl()
	 */
	public Control getControl() {
		return pageComposite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.Page#setFocus()
	 */
	public void setFocus() {
		pageComposite.setFocus();
	}	
	
	/**
	 * Creates the wizard's title area.
	 *
	 * @param parent the SWT parent for the title area composite
	 * @return the created title area composite
	 */
	private Composite createTitleArea(Composite parent) {
		// Get the background color for the title area
		Display display = parent.getDisplay();
		Color background = JFaceColors.getBannerBackground(display);
		Color foreground = JFaceColors.getBannerForeground(display);

		// Create the title area which will contain
		// a title, message, and image.
		Composite titleArea = new Composite(parent, SWT.NONE | SWT.NO_FOCUS);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		layout.numColumns = 2;
		titleArea.setLayout(layout);
		titleArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		titleArea.setBackground(background);

		// Message label
		final CLabel messageLabel = new CLabel(titleArea, SWT.LEFT) {
			protected String shortenText(GC gc, String text, int width) {
				if (gc.textExtent(text, SWT.DRAW_MNEMONIC).x <= width) return text;
				final String ellipsis= "..."; //$NON-NLS-1$
				int ellipseWidth = gc.textExtent(ellipsis, SWT.DRAW_MNEMONIC).x;
				int length = text.length();
				int end = length - 1;
				while (end > 0) {
					text = text.substring(0, end);
					int l1 = gc.textExtent(text, SWT.DRAW_MNEMONIC).x;
					if (l1 + ellipseWidth <= width) {
						return text + ellipsis;
					}
					end--;
				}
				return text + ellipsis;			
			}
		};
		JFaceColors.setColors(messageLabel,foreground,background);
		messageLabel.setText("Synchronize Overview");
		messageLabel.setFont(JFaceResources.getHeaderFont());
		
		final IPropertyChangeListener fontListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if(JFaceResources.HEADER_FONT.equals(event.getProperty())) {
					messageLabel.setFont(JFaceResources.getHeaderFont());
				}
			}
		};
		
		messageLabel.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				JFaceResources.getFontRegistry().removeListener(fontListener);
			}
		});
		
		JFaceResources.getFontRegistry().addListener(fontListener);
		
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		messageLabel.setLayoutData(gd);

		// Title image
		Label titleImage = new Label(titleArea, SWT.LEFT);
		titleImage.setBackground(background);
		titleImage.setImage(
				PlatformUI.getWorkbench().getSharedImages().getImage(
						IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_BANNER));
		gd = new GridData(); 
		gd.horizontalAlignment = GridData.END;
		titleImage.setLayoutData(gd);

		return titleArea;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsAdded(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	public void participantsAdded(final ISynchronizeParticipant[] participants) {
		if (isAvailable()) {
			Runnable r = new Runnable() {
				public void run() {
					for (int i = 0; i < participants.length; i++) {
						if (isAvailable()) {
							ISynchronizeParticipant participant = participants[i];
							participantsToComposites.put(participant, new ParticipantComposite(pageComposite, participant, view, SWT.NONE));
							pageComposite.redraw();
						}
					}
				}
			};
			asyncExec(r);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.sync.ISynchronizeParticipantListener#participantsRemoved(org.eclipse.team.ui.sync.ISynchronizeParticipant[])
	 */
	public void participantsRemoved(final ISynchronizeParticipant[] consoles) {
		if (isAvailable()) {
			Runnable r = new Runnable() {
				public void run() {
					for (int i = 0; i < consoles.length; i++) {
						if (isAvailable()) {
							ISynchronizeParticipant console = consoles[i];
							Composite composite = (Composite)participantsToComposites.get(console);
							composite.dispose();
							pageComposite.layout(true);
							pageComposite.redraw();
						}
					}
				}
			};
			asyncExec(r);
		}
	}
	
	/**
	 * @return
	 */
	private boolean isAvailable() {
		return ! pageComposite.isDisposed() && ! pageComposite.getDisplay().isDisposed();
	}

	/**
	 * Registers the given runnable with the display
	 * associated with this view's control, if any.
	 * 
	 * @see org.eclipse.swt.widgets.Display#asyncExec(java.lang.Runnable)
	 */
	public void asyncExec(Runnable r) {
		if (isAvailable()) {
			pageComposite.getDisplay().asyncExec(r);
		}
	}
}