package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.subscribers.ITeamResourceChangeListener;
import org.eclipse.team.core.subscribers.TeamDelta;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.widgets.ControlFactory;
import org.eclipse.team.internal.ui.widgets.FormSection;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;

/**
 * Section shown at the top of a participant page to describe the details about a team subscriber 
 * participant.
 * 
 * @since 3.0
 */
public class SummarySection extends FormSection {
	
	private TeamSubscriberParticipant participant;
	private Composite parent;
	private ParticipantOverviewComposite participantComposite;
	private ISynchronizeView view;
	private Composite client;
	private Label workingSetLabel;
	
	public SummarySection(Composite parent, TeamSubscriberParticipant participant, ISynchronizeView view) {
		this.participant = participant;
		this.parent = parent;
		this.view = view;
		setCollapsable(true);
		setCollapsed(true);
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
		client = new TeamSubscriberParticipantComposite(parent, false, factory, participant, view);
		participant.addPropertyChangeListener(this);
		return client;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.widgets.FormSection#createHeaderRight(org.eclipse.swt.widgets.Composite, org.eclipse.team.internal.ui.widgets.ControlFactory)
	 */
	protected Composite createHeaderRight(Composite parent, ControlFactory factory) {
		Composite top = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		top.setLayout(layout);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		workingSetLabel = factory.createLabel(top, Utils.workingSetToString(participant.getWorkingSet(), 20));
		return top;
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
		workingSetLabel.setText(Utils.workingSetToString(participant.getWorkingSet(), 20));
		reflow();
	}
}