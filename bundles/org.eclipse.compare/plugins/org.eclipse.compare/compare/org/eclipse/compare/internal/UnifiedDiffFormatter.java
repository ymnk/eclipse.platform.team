/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Krzysztof Poglodzinski (intuicje@gmail.com) - initial API and implementation
 *     Mariusz Tanski (mariusztanski@gmail.com) - initial API and implementation
 *     Kacper Zdanowicz (kacper.zdanowicz@gmail.com) - initial API and implementation
 *     IBM Corportation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.compare.internal.merge.DocumentMerger.Diff;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

public class UnifiedDiffFormatter {

	public static final char RIGHT_CONTRIBUTOR = 'R';
	public static final char LEFT_CONTRIBUTOR = 'L';

	public final static int FORMAT_UNIFIED = 1;
	public final static int FORMAT_CONTEXT = 2;
	public final static int FORMAT_STANDARD = 3;

	public static final String INDEX_MARKER = "Index: "; //$NON-NLS-1$
	public static final String DELIMITER = "==================================================================="; //$NON-NLS-1$
	public static final String OLD_FILE_PREFIX = "--- "; //$NON-NLS-1$
	public static final String NEW_FILE_PREFIX = "+++ "; //$NON-NLS-1$	
	public static final String OLD_LINE_PREFIX = "-"; //$NON-NLS-1$
	public static final String NEW_LINE_PREFIX = "+"; //$NON-NLS-1$
	public static final String CONTEXT_LINE_PREFIX = " "; //$NON-NLS-1$

	private DocumentMerger merger;
	private IDocument leftDoc;
	private IDocument rightDoc;
	private String resourcePath;
	private boolean rightToLeft;

	public UnifiedDiffFormatter(DocumentMerger merger, IDocument leftDoc,
			IDocument rightDoc, String resourcePath, boolean rightToLeft) {
		this.merger = merger;
		this.leftDoc = leftDoc;
		this.rightDoc = rightDoc;
		this.resourcePath = resourcePath;
		this.rightToLeft = rightToLeft;
	}

	public void generateDiff(Display dis) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);

		createDiff(ps);
		ps.close();

		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		Clipboard clipboard = new Clipboard(dis);
		clipboard.setContents(new String[] { bos.toString() },
				new Transfer[] { plainTextTransfer });
		clipboard.dispose();

		bos.close();
	}

	public void generateDiff(File file) throws IOException {
		FileOutputStream fos = null;
		PrintStream ps = null;
		try {
			fos = new FileOutputStream(file);
			try {
				ps = new PrintStream(fos);
				createDiff(ps);
				if (ps.checkError()) {
					throw new IOException("Error while writing patch file: " //$NON-NLS-1$
							+ file);
				}
			} finally {
				if (ps != null) {
					ps.close();
				}
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	public void createDiff(PrintStream output) {
		ArrayList allDiffs = merger.getAllDiffs();
		// If the first block isn't the only one, or first block is different
		if (allDiffs.size() > 1 || isPartDifferent(allDiffs, 0)) {
			output.println(INDEX_MARKER + resourcePath);
			output.println(DELIMITER);

			SimpleDateFormat format = new SimpleDateFormat(
					"dd MMM yyyy hh:mm:ss", Locale.US); //$NON-NLS-1$
			Date oldDate = Calendar.getInstance(Locale.US).getTime();
			Date newDate = Calendar.getInstance(Locale.US).getTime();
			output.println(OLD_FILE_PREFIX + resourcePath + '\t'
					+ format.format(oldDate) + " -0000"); //$NON-NLS-1$
			output.println(NEW_FILE_PREFIX + resourcePath + '\t'
					+ format.format(newDate) + " -0000"); //$NON-NLS-1$

			boolean firstHunk = true;
			Hunk currentHunk = null;

			int currentLineNumberOld = 0;
			int currentLineNumberNew = 0;

			for (int partNumber = 0; partNumber < allDiffs.size(); partNumber++) {

				ArrayList oldPart = getPart(partNumber, 'R');
				ArrayList newPart = getPart(partNumber, 'L');

				if (isPartDifferent(allDiffs, partNumber)) {
					// This part has some changes
					if (firstHunk) {
						// Hunk will start with changed block
						currentHunk = new Hunk(0, 0);
						firstHunk = false;
					}
					if (partNumber == (allDiffs.size() - 1)) {
						// If it is the last part
						currentHunk.addPartRangeToOld(oldPart, 0, oldPart
								.size(), true);
						currentHunk.addPartRangeToNew(newPart, 0, newPart
								.size(), true);
					} else {
						currentHunk.addPartRangeToOld(oldPart, 0, oldPart
								.size(), false);
						currentHunk.addPartRangeToNew(newPart, 0, newPart
								.size(), false);
					}
				} else {
					if (firstHunk) {
						// Hunk will start with context
						currentHunk = new Hunk(oldPart.size() - 3, oldPart
								.size() - 3);
						firstHunk = false;
						currentHunk.addPartRangeToBoth(oldPart,
								oldPart.size() - 3, oldPart.size(), false);
					} else {
						if (partNumber == (allDiffs.size() - 1)) {
							// If it is the last part
							currentHunk.addPartRangeToBoth(oldPart, 0, 3, true);
						} else {
							if (oldPart.size() < 6) {
								// Context too short to start new hunk
								currentHunk.addPartRangeToBoth(oldPart, 0,
										oldPart.size(), false);
							} else {
								// Context long enough to start new hunk
								currentHunk.addPartRangeToBoth(oldPart, 0, 3,
										false);
								currentHunk.printTo(output);
								currentHunk = new Hunk(currentLineNumberOld
										+ oldPart.size() - 3,
										currentLineNumberNew + oldPart.size()
												- 3);
								currentHunk.addPartRangeToBoth(oldPart, oldPart
										.size() - 3, oldPart.size(), false);
							}
						}
					}
				}
				currentLineNumberOld += oldPart.size();
				currentLineNumberNew += newPart.size();
			}
			// Print the last hunk
			currentHunk.printTo(output);
		}
	}

	private ArrayList getPart(int nr, char side) {
		try {
			String s = extract(nr, side).replaceAll("\r\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			s.replaceAll("\r", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			ArrayList diffLines = new ArrayList(Arrays
					.asList(s.split("\n", -1))); //$NON-NLS-1$
			return diffLines;
		} catch (BadLocationException e) {
			CompareUIPlugin.log(e);
		}
		return null;
	}

	private String extract(int nr, char side) throws BadLocationException {
		Diff diff = ((Diff) merger.getAllDiffs().get(nr));
		if (side == LEFT_CONTRIBUTOR && !rightToLeft
				|| side == RIGHT_CONTRIBUTOR && rightToLeft) {
			return leftDoc.get(diff.getPosition(LEFT_CONTRIBUTOR).offset, diff
					.getPosition(LEFT_CONTRIBUTOR).length);
		}
		return rightDoc.get(diff.getPosition(RIGHT_CONTRIBUTOR).offset, diff
				.getPosition(RIGHT_CONTRIBUTOR).length);
	}

	public boolean isPartDifferent(ArrayList allDiffs, int nr) {
		Diff diff = ((Diff) allDiffs.get(nr));
		if (diff.getKind() == RangeDifference.CHANGE) {
			return true;
		}
		return false;
	}

	private class Hunk {
		private int oldStart;
		private int oldEnd;
		private int newStart;
		private int newEnd;
		ArrayList lines;

		public Hunk(int oldStart, int newStart) {
			if (oldStart < 0)
				oldStart = 0;
			if (newStart < 0)
				newStart = 0;
			this.oldStart = oldStart;
			this.newStart = newStart;
			this.oldEnd = oldStart;
			this.newEnd = newStart;
			lines = new ArrayList();
		}

		public void addPartRangeToOld(ArrayList part, int start, int end,
				boolean lastPart) {
			if (start < 0)
				start = 0;
			if (lastPart)
				end = Math.min(end, part.size());
			else
				end = Math.min(end, part.size() - 1);
			for (int lineNr = start; lineNr < end; lineNr++) {
				lines.add(OLD_LINE_PREFIX + part.get(lineNr));
				oldEnd++;
			}
		}

		public void addPartRangeToNew(ArrayList part, int start, int end,
				boolean lastPart) {
			if (start < 0)
				start = 0;
			if (lastPart)
				end = Math.min(end, part.size());
			else
				end = Math.min(end, part.size() - 1);
			for (int lineNr = start; lineNr < end; lineNr++) {
				lines.add(NEW_LINE_PREFIX + part.get(lineNr));
				newEnd++;
			}
		}

		public void addPartRangeToBoth(ArrayList part, int start, int end,
				boolean lastPart) {
			if (start < 0)
				start = 0;
			if (lastPart)
				end = Math.min(end, part.size());
			else
				end = Math.min(end, part.size() - 1);
			for (int lineNr = start; lineNr < end; lineNr++) {
				lines.add(CONTEXT_LINE_PREFIX + part.get(lineNr));
				oldEnd++;
				newEnd++;
			}
		}

		private void printMarkerTo(PrintStream output) {
			output.println("@@ -" + oldStart + "," + oldEnd + " +" + newStart //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "," + newEnd + " @@"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		public void printTo(PrintStream output) {
			printMarkerTo(output);
			for (int i = 0; i < lines.size(); i++) {
				output.println(lines.get(i));
			}
		}
	}

}