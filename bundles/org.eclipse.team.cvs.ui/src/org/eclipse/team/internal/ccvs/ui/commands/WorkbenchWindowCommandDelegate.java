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
package org.eclipse.team.internal.ccvs.ui.commands;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.*;

public class WorkbenchWindowCommandDelegate implements IWorkbenchWindowActionDelegate {

	private IAction action;
	private IWorkbenchWindow window;
	private ICommandManager commandManager;
	private ICommand command;
	
	public WorkbenchWindowCommandDelegate() {
		
	}
	
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		if(action != null)
			try {
				commandManager.getCommand(action.getActionDefinitionId()).execute(null);
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (NotHandledException e) {
				e.printStackTrace();
			}
	}

	public void selectionChanged(final IAction action, ISelection selection) {
		if(command == null) {
		this.action = action;
		commandManager = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
		command = commandManager.getCommand(action.getActionDefinitionId());
		command.addCommandListener(new ICommandListener() {
			public void commandChanged(CommandEvent e) {
				Boolean enabled;
				try {
					enabled = (Boolean) command.getAttributeValuesByName().get("enabled");
				} catch (NotHandledException e1) {
					enabled = Boolean.FALSE;
				}
				if(action != null)
					action.setEnabled(enabled.booleanValue());
			}
		});
		}
	}
}