/*
 * Created on Dec 10, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;

/**
 * @author Jean-Michel Lemieux
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class aa extends Dialog {

	protected aa(Shell parentShell) {
		super(parentShell);
	}
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		area.setLayout(new GridLayout());
		{
			final Table table = new Table(area, SWT.BORDER);
			table.setToolTipText("asdasd");
			final GridData gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 4;
			table.setLayoutData(gridData);
		}
		{
			final ProgressBar progressBar = new ProgressBar(area, SWT.SMOOTH);
			progressBar.setSelection(56);
		}
		{
			final ToolBar toolBar = new ToolBar(area, SWT.FLAT);
			{
				final ToolItem toolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
				toolItem.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
					}
				});
				toolItem.setText("New item");
			}
		}
		{
			new Combo(area, SWT.NONE);
		}
		return area;
	}
	protected void createButtonsForButtonBar(Composite parent) {
	}
}
