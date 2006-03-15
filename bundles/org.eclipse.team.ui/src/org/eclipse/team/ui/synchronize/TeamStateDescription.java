/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.provider.Diff;
import org.eclipse.team.ui.mapping.ITeamStateDescription;

/**
 * An implementation of {@link ITeamStateDescription} which may be used or
 * subclassed by clients.
 * @since 3.2
 */
public class TeamStateDescription implements ITeamStateDescription {

	private int state;

	/**
	 * Create a description with the given state.
	 * @param state the state
	 */
	public TeamStateDescription(int state) {
		this.state = state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getStateFlags()
	 */
	public int getStateFlags() {
		return state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getKind()
	 */
	public int getKind() {
		return state & Diff.KIND_MASK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getDirection()
	 */
	public int getDirection() {
		return state & IThreeWayDiff.DIRECTION_MASK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getProperties()
	 */
	public String[] getProperties() {
		return new String[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getProperty(java.lang.String)
	 */
	public Object getProperty(String property) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof TeamStateDescription) {
			TeamStateDescription dsd = (TeamStateDescription) obj;
			if (dsd.getStateFlags() == state) {
				if (haveSameProperties(this, dsd)) {
					String[] properties = getProperties();
					for (int i = 0; i < properties.length; i++) {
						String property = properties[i];
						Object o1 = this.getProperty(property);
						Object o2 = dsd.getProperty(property);
						if (!o1.equals(o2)) {
							return false;
						}
					}
				}
				return true;
			}
			return false;
		}
		return super.equals(obj);
	}

	private boolean haveSameProperties(TeamStateDescription d1, TeamStateDescription d2) {
		String[] p1 = d1.getProperties();
		String[] p2 = d2.getProperties();
		if (p1.length != p2.length) {
			return false;
		}
		for (int i = 0; i < p1.length; i++) {
			String s1 = p1[i];
			boolean found = false;
			for (int j = 0; j < p2.length; j++) {
				String s2 = p2[j];
				if (s1.equals(s2)) {
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		return true;
	}

}
