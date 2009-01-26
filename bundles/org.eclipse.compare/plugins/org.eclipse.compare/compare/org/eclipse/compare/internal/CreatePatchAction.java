/*******************************************************************************
 * Copyright (c) 2009 Krzysztof Poglodzinski, Mariusz Tanski, Kacper Zdanowicz and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Krzysztof Poglodzinski (intuicje@gmail.com) - initial API and implementation
 *     Mariusz Tanski (mariusztanski@gmail.com) - initial API and implementation
 *     Kacper Zdanowicz (kacper.zdanowicz@gmail.com) - initial API and implementation
 *     IBM Corporation - implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;

public class CreatePatchAction extends Action {

	private Shell fShell;
	private boolean fRightToLeft;
	private DocumentMerger fMerger;

	public CreatePatchAction(Shell shell, DocumentMerger fMerger,
			boolean rightToLeft) {
		super(CompareMessages.CreatePatchActionTitle);
		this.fShell = shell;
		this.fRightToLeft = rightToLeft;
		this.fMerger = fMerger;
	}

	public void run() {
		GenerateDiffFileWizard.run(fMerger, fShell, fRightToLeft);
	}

}