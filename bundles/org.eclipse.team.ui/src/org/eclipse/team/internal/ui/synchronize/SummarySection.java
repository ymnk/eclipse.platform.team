package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ui.widgets.FormSection;
import org.eclipse.team.ui.controls.IControlFactory;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.TeamSubscriberParticipant;

public class SummarySection extends FormSection {
	
	private TeamSubscriberParticipant participant;
	private Composite parent;
	private ParticipantComposite participantComposite;
	private ISynchronizeView view;
	
	public SummarySection(Composite parent, TeamSubscriberParticipant participant, ISynchronizeView view) {
		this.participant = participant;
		this.parent = parent;
		this.view = view;
		setCollapsable(true);
		setCollapsed(true);
		setHeaderRightText("Outgoing | No Working Set");
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
		return participant.createOverviewComposite(parent, factory, view);
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
}
