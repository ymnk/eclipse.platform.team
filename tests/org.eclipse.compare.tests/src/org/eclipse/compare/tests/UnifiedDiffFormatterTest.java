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
package org.eclipse.compare.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TokenComparator;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.UnifiedDiffFormatter;
import org.eclipse.compare.internal.core.patch.PatchReader;
import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.compare.internal.merge.DocumentMerger.IDocumentMergerInput;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

public class UnifiedDiffFormatterTest extends AbstractPatchTest {

	public UnifiedDiffFormatterTest(String name) {
		super(name);
	}

	private static final String TESTPATCHFILE = "testPatch.txt";

	protected void setUp() throws Exception {
		super.setUp();
		File file = getTestPatchFile();
		if (!file.exists()) {
			file.createNewFile();
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		getTestPatchFile().delete();
	}

	protected String getWorkingFolder() {
		return "patchdata/unifiedDiffFormatter";
	}

	class FormatterTestDocMerger implements IDocumentMergerInput {

		private Document fromDoc;
		private Document toDoc;

		public FormatterTestDocMerger(Document fromDoc, Document toDoc) {
			this.fromDoc = fromDoc;
			this.toDoc = toDoc;
		}
		public ITokenComparator createTokenComparator(String line) {
			return new TokenComparator(line);
		}
		public CompareConfiguration getCompareConfiguration() {
			return new CompareConfiguration();
		}
		public IDocument getDocument(char contributor) {
			switch (contributor) {
			case MergeViewerContentProvider.LEFT_CONTRIBUTOR:
				return fromDoc;
			case MergeViewerContentProvider.RIGHT_CONTRIBUTOR:
				return toDoc;
			default:
				return null;
			}
		}
		public int getHunkStart() {
			return 0;
		}
		public Position getRegion(char contributor) {
			switch (contributor) {
			case MergeViewerContentProvider.LEFT_CONTRIBUTOR:
				return new Position(0, fromDoc.getLength());
			case MergeViewerContentProvider.RIGHT_CONTRIBUTOR:
				return new Position(0, toDoc.getLength());
			}
			return null;
		}
		public boolean isHunkOnLeft() {
			return false;
		}
		public boolean isIgnoreAncestor() {
			return true;
		}
		public boolean isPatchHunk() {
			return false;
		}
		public boolean isShowPseudoConflicts() {
			return false;
		}
		public boolean isThreeWay() {
			return false;
		}
		public boolean isPatchHunkOk() {
			return false;
		}
	}

	public void testLeftEmptyPatch() throws CoreException, IOException {
		createPatch("addition.txt", "exp_addition.txt", "patch_additionA2.txt");
		patch("addition.txt", "exp_addition.txt");
	}

	public void testRightEmptyPatch() throws CoreException, IOException {
		createPatch("exp_addition.txt", "addition.txt", "patch_additionB2.txt");
		patch("exp_addition.txt", "addition.txt");
	}

	public void testEmptyFilesPatch() throws CoreException, IOException {
		createPatch("empty1.txt", "empty2.txt", "patch_empty.txt");
		patch("empty1.txt", "empty2.txt");
	}

	public void testUnterminatedCreatePatch() throws CoreException, IOException {
		createPatch("addition.txt", "exp_addition2.txt", "patch_additionC2.txt");
		patch("addition.txt", "exp_addition2.txt");
	}

	public void testCreateExamplePatch() throws CoreException, IOException {
		createPatch("context.txt", "exp_context.txt", "patch_additionD2.txt");
		patch("context.txt", "exp_context.txt");
	}

	public void testBothFilesWithoutEndingNewlinePatch() throws CoreException,
	IOException {
		createPatch("no_newline.txt", "exp_no_newline.txt", "patch_no_newline.txt");
		patch("no_newline.txt", "exp_no_newline.txt");
	}

	private void patch(final String old, String expt) throws CoreException,	IOException {
		patch(old, TESTPATCHFILE, expt);
	}

	private void createPatch(String fromFilePath, String toFilePath, String expectedPatch) throws CoreException, IOException{

		final Document fromDoc = new Document(readFileToString(fromFilePath));
		final Document toDoc = new Document(readFileToString(toFilePath));

		DocumentMerger merger = new DocumentMerger(new FormatterTestDocMerger(fromDoc, toDoc));
		// Compare Editor calculates diffs while building the UI
		merger.doDiff();

		UnifiedDiffFormatter formatter = new UnifiedDiffFormatter(merger,
				fromFilePath, toFilePath, false);
		formatter.generateDiff(getTestPatchFile());

		String patchContent = readFileToString(TESTPATCHFILE);
		String expectedContent = readFileToString(expectedPatch);

		String[] patchContents = patchContent.split("\r\n");
		String[] expectedContents = expectedContent.split("\r\n");

		patchContent = getContentFromLines(patchContents);
		expectedContent = getContentFromLines(expectedContents);
		assertEquals(expectedContent, patchContent);
	}

	private File getTestPatchFile() throws IOException {
		IPath path= new Path(getWorkingFolder()).append(TESTPATCHFILE);
		return path.toFile();
	}

	private String readFileToString(String name) throws IOException {
		InputStream resourceAsStream = asInputStream(name);
		return asString(resourceAsStream);
	}

	private String getContentFromLines(String[] patchContents) {
		// TODO: test only part of the patch content
		StringBuffer patchContent = new StringBuffer();
		for (int i = 0; i < patchContents.length; i++) {
			String line = patchContents[i];
			if (line.startsWith(UnifiedDiffFormatter.NEW_FILE_PREFIX)
					|| line.startsWith(UnifiedDiffFormatter.OLD_FILE_PREFIX)) {
				String[] line_split = line.split("\t");
				patchContent.append(line_split[0]);
				continue;
			} else if (line.startsWith(UnifiedDiffFormatter.INDEX_MARKER)
					|| line.startsWith(UnifiedDiffFormatter.DELIMITER)) {
				continue;
			} else if (line.startsWith(PatchReader.MULTIPROJECTPATCH_HEADER)
					|| line.startsWith(PatchReader.MULTIPROJECTPATCH_PROJECT)) {
				continue;
			} else if (line.startsWith("RCS file: ")
					|| line.startsWith("retrieving revision ")
					|| line.startsWith("diff")) {
				continue;
			}
			patchContent.append(line);
		}
		return patchContent.toString();
	}
}
