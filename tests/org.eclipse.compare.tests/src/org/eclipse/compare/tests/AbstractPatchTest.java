/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.core.patch.FileDiff;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.LineReader;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.compare.patch.IFilePatchResult;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

public abstract class AbstractPatchTest extends TestCase {

	public AbstractPatchTest() {
		super();
	}

	public AbstractPatchTest(String name) {
		super(name);
	}

	class StringStorage implements IStorage {
		String fileName;
		public StringStorage(String old) {
			fileName = old;
		}
		public Object getAdapter(Class adapter) {
			return null;
		}
		public boolean isReadOnly() {
			return false;
		}
		public String getName() {
			return fileName;
		}
		public IPath getFullPath() {
			return null;
		}
		public InputStream getContents() throws CoreException {
			return new BufferedInputStream(asInputStream(fileName));
		}
	}

	class FileStorage implements IStorage {
		File file;
		public FileStorage(File file) {
			this.file = file;
		}
		public InputStream getContents() throws CoreException {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {
				// ignore, should never happen
			}
			return null;
		}
		public IPath getFullPath() {
			return new Path(file.getAbsolutePath());
		}
		public String getName() {
			return file.getName();
		}
		public boolean isReadOnly() {
			return true;
		}
		public Object getAdapter(Class adapter) {
			return null;
		}
	}

	protected abstract String getWorkingFolder();

	protected BufferedReader getReader(String name) {
		InputStream resourceAsStream = asInputStream(name);
		InputStreamReader reader2 = new InputStreamReader(resourceAsStream);
		return new BufferedReader(reader2);
	}

	protected InputStream asInputStream(String name) {
		IPath path = new Path(getWorkingFolder()).append(name);
		try {
			URL url = new URL(getBundle().getEntry("/"), path.toString());
			return url.openStream();
		} catch (IOException e) {
			fail("Failed while reading " + name);
			return null; // never reached
		}
	}

	protected void patch(final String old, String patch, String expt)
	throws CoreException, IOException {
		patcherPatch(old, patch, expt);
		filePatch(old, patch, expt);
	}

	void filePatch(final String old, String patch, String expt)
	throws CoreException, IOException {
		LineReader lr = new LineReader(getReader(expt));
		List inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		IStorage oldStorage = new StringStorage(old);
		IStorage patchStorage = new StringStorage(patch);
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertTrue(patches.length == 1);
		IFilePatchResult result = patches[0].apply(oldStorage,
				new PatchConfiguration(), null);
		assertTrue(result.hasMatches());
		assertFalse(result.hasRejects());
		InputStream actualStream = result.getPatchedContents();
		String actual = asString(actualStream);
		assertEquals(expected, actual);
	}

	protected String asString(InputStream exptStream) throws IOException {
		return Utilities.readString(exptStream, ResourcesPlugin.getEncoding());
	}

	void patcherPatch(String old, String patch, String expt) {
		LineReader lr = new LineReader(getReader(old));
		List inLines = lr.readLines();

		WorkspacePatcher patcher = new WorkspacePatcher();
		try {
			patcher.parse(getReader(patch));
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileDiff[] diffs = patcher.getDiffs();
		Assert.assertEquals(diffs.length, 1);

		FileDiffResult diffResult = patcher.getDiffResult(diffs[0]);
		diffResult.patch(inLines, null);

		LineReader expectedContents = new LineReader(getReader(expt));
		List expectedLines = expectedContents.readLines();

		Object[] expected = expectedLines.toArray();
		Object[] result = inLines.toArray();

		Assert.assertEquals(expected.length, result.length);

		for (int i = 0; i < expected.length; i++)
			Assert.assertEquals(expected[i], result[i]);
	}

	protected Bundle getBundle() {
		return CompareTestPlugin.getDefault().getBundle();
	}

}