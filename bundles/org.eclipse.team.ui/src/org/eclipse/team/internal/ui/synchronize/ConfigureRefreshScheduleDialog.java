package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ui.dialogs.DetailsDialog;
import org.eclipse.team.internal.ui.jobs.RefreshSchedule;


public class ConfigureRefreshScheduleDialog extends DetailsDialog {

	private RefreshSchedule schedule;
	private Button userRefreshOnly;
	private Button enableBackgroundRefresh;
	private Text time;
	private Combo hoursOrSeconds;

	protected ConfigureRefreshScheduleDialog(Shell parentShell, RefreshSchedule schedule) {
		super(parentShell, "Configure Refresh Schedule");
		this.schedule = schedule;
	}
	
	private void initializeValues() {
		boolean enableBackground = schedule.isEnabled();
		boolean hours = false;
		
		userRefreshOnly.setSelection(! enableBackground);
		enableBackgroundRefresh.setSelection(enableBackground);
		
		long seconds = schedule.getRefreshInterval();
		if(seconds <= 60) {
			seconds = 60;
		}

		long minutes = seconds / 60;
		
		if(minutes >= 60) {
			minutes = minutes / 60;
			hours = true;
		}		
		hoursOrSeconds.select(hours ? 0 : 1);
		time.setText(Long.toString(minutes));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createMainDialogArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		area.setLayout(gridLayout);
		{
			final Label label = new Label(area, SWT.NONE);
			final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalSpan = 10;
			label.setLayoutData(gridData);
			label.setText("Refresh synchronization state:");
		}
		{
			userRefreshOnly = new Button(area, SWT.RADIO);
			final GridData gridData = new GridData();
			gridData.horizontalSpan = 2;
			userRefreshOnly.setLayoutData(gridData);
			userRefreshOnly.setText("Only when I choose the Refresh action.");
		}
		{
			enableBackgroundRefresh = new Button(area, SWT.RADIO);
			final GridData gridData = new GridData();
			gridData.horizontalSpan = 2;
			enableBackgroundRefresh.setLayoutData(gridData);
			enableBackgroundRefresh.setText("Using the following schedule:");
		}
		{
			final Composite composite = new Composite(area, SWT.NONE);
			final GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING);
			gridData.horizontalSpan = 2;
			composite.setLayoutData(gridData);
			final GridLayout gridLayout_1 = new GridLayout();
			gridLayout_1.numColumns = 3;
			composite.setLayout(gridLayout_1);
			{
				final Label label = new Label(composite, SWT.NONE);
				label.setText("Every:");
			}
			{
				time = new Text(composite, SWT.BORDER | SWT.RIGHT);
				final GridData gridData_1 = new GridData();
				gridData_1.widthHint = 35;
				time.setLayoutData(gridData_1);
				time.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						updateEnablements();
					}
				});
			}
			{
				hoursOrSeconds = new Combo(composite, SWT.READ_ONLY);
				hoursOrSeconds.setItems(new String[] { "hour(s)", "minutes(s)" });
				final GridData gridData_1 = new GridData();
				gridData_1.widthHint = 75;
				hoursOrSeconds.setLayoutData(gridData_1);
			}
		}
		initializeValues();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		int hours = hoursOrSeconds.getSelectionIndex();
		long seconds = Long.parseLong(time.getText());
		if(hours == 0) {
			seconds = seconds * 3600;
		} else {
			seconds = seconds * 60;
		}
		schedule.setRefreshInterval(seconds);
		schedule.setEnabled(enableBackgroundRefresh.getSelection());
		super.okPressed();
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#createDropDownDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Composite createDropDownDialogArea(Composite parent) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#updateEnablements()
	 */
	protected void updateEnablements() {
		try {
			long number = Long.parseLong(time.getText());
			if(number <= 0) {
				setPageComplete(false);
				setErrorMessage("Number must be a positive number greater than 0");
			} else {
				setPageComplete(true);
				setErrorMessage(null);
			}
		} catch (NumberFormatException e) {
			setPageComplete(false);
			setErrorMessage("Number must be a positive number greater than 0");
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.dialogs.DetailsDialog#includeDetailsButton()
	 */
	protected boolean includeDetailsButton() {
		return false;
	}
}
