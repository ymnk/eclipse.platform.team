/*
 * Created on 2-Jun-03
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSException;

/**
 * This method checks out one or more remote folders from the same repository
 * into an existing project or folder in the workspace. The target project
 * must either be shared with the same repository or it must not be shared 
 * with any repository
 */
public class CheckoutIntoOperation extends CVSOperation {

	/**
	 * @param shell
	 */
	public CheckoutIntoOperation(Shell shell) {
		super(shell);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		// TODO Auto-generated method stub

	}

}
