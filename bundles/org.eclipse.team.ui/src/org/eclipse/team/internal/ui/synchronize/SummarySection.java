package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.widgets.FormSection;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;

public class SummarySection extends FormSection {
	
	private TeamSubscriberParticipant participant;
	private Composite parent;
	private ParticipantComposite participantComposite;
	private ISynchronizeView view;
	private Composite client;
	
	public SummarySection(Composite parent, TeamSubscriberParticipant participant, ISynchronizeView view) {
		this.participant = participant;
		this.parent = parent;
		this.view = view;
		setCollapsable(true);
		setCollapsed(true);
		updateHeaderRightText();
		participant.addPropertyChangeListener(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#getHeaderText()
	 */
	public String getHeaderText() {
		return "Summary";
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#createClient(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.team.internal.ui.widgets.FormWidgetFactory)
	 */
	public Composite createClient(Composite parent, IControlFactory factory) {
		client = participant.createOverviewComposite(parent, factory, view);
		return client;
	}
	
	protected void reflow() {
		super.reflow();
		parent.setRedraw(false);
		parent.getParent().setRedraw(false);
		parent.layout(true);
		parent.getParent().layout(true);
		parent.setRedraw(true);
		parent.getParent().setRedraw(true);
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#dispose()
	 */
	public void dispose() {
		super.dispose();
		client.dispose();
		participant.removePropertyChangeListener(this);		
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		//super.propertyChange(event);
		String property = event.getProperty();
		if(property.equals(TeamSubscriberParticipant.P_SYNCVIEWPAGE_MODE) ||
		   property.equals(TeamSubscriberParticipant.P_SYNCVIEWPAGE_WORKINGSET)) {
			updateHeaderRightText();
		}
	}
	
	public void updateHeaderRightText() {
		setHeaderRightText(Utils.modeToString(participant.getMode()) + " | " + Utils.workingSetToString(participant.getWorkingSet(), 20));
		reflow();
	}
}