/*
 * (c) Copyright IBM Corp. 2002, 2003.
 * All Rights Reserved.
 */

package org.eclipse.team.internal.ccvs.ssh2;

import com.jcraft.jsch.IUserInfo;

/**
 * Holds a pair of username-password suitable for JCraft implementation of SSH.
 */
public class PasswordAuthInfo implements IUserInfo {
	
	private String name, password;

	/**
	 * Create a new instance given a username and password.
	 */
	public PasswordAuthInfo(String name, String password) {
		this.name = name;
		this.password = password;
	}
	public String getPassword() {
		return password;
	}
	public String getName() {
		return name;
	}
}
