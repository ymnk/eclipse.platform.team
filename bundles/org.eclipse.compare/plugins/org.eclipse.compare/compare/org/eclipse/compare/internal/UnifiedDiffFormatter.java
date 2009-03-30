/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.compare.internal.merge.DocumentMerger.Diff;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.ibm.icu.text.DateFormat;

public class UnifiedDiffFormatter {

		
	/**
	 * This is a switch between strict unix format and Eclipse format of the patch.
	 * GNU diff command has default line format different than Eclipse has.
	 * <br><br>
	 * Issues:<br><br>
	 * In Eclipse format, if last line doesn't end with newline character
	 * add an marker saying about this
	 * <br><br>
	 * In Unix format, if last line ends with newline character
	 * add additional empty line
	 * <br><br>
	 * This issue is the subject of bug 259636.
	 */
	private static final boolean STRICT_UNIX_FORMAT = false;

	private static final char RIGHT_CONTRIBUTOR = 'R';
	private static final char LEFT_CONTRIBUTOR = 'L';

	private final static int NUMBER_OF_CONTEXT_LINES = 3;

	public static final String INDEX_MARKER = "Index: "; //$NON-NLS-1$
	public static final String DELIMITER = "==================================================================="; //$NON-NLS-1$
	public static final String OLD_FILE_PREFIX = "--- "; //$NON-NLS-1$
	public static final String NEW_FILE_PREFIX = "+++ "; //$NON-NLS-1$
	public static final String OLD_LINE_PREFIX = "-"; //$NON-NLS-1$
	public static final String NEW_LINE_PREFIX = "+"; //$NON-NLS-1$
	public static final String CONTEXT_LINE_PREFIX = " "; //$NON-NLS-1$
	public static final String RANGE_INFORMATION_PREFIX = "@@ -"; //$NON-NLS-1$
	public static final String RANGE_INFORMATION_AFFIX = " @@"; //$NON-NLS-1$

	private List fAllDiffs;
	private IDocument leftDoc;
	private IDocument rightDoc;
	private String oldPath;
	private String newPath;
	private boolean rightToLeft;

	public UnifiedDiffFormatter(DocumentMerger merger, String oldPath, String newPath, boolean rightToLeft) {
		this.fAllDiffs = merger.getAllDiffs();
		this.leftDoc = merger.getDocument(LEFT_CONTRIBUTOR);
		this.rightDoc = merger.getDocument(RIGHT_CONTRIBUTOR);
		this.oldPath = oldPath;
		this.newPath = newPath;
		this.rightToLeft = rightToLeft;
	}

	/**
	 * Generates diff and writes it into the clipboard.
	 * 
	 * @throws IOException
	 */
	public void generateDiffToClipboard() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);

		generateDiff(ps);
		ps.close();

		TextTransfer plainTextTransfer = TextTransfer.getInstance();
		Clipboard clipboard = new Clipboard(Display.getDefault());
		clipboard.setContents(new String[] { bos.toString() },
				new Transfer[] { plainTextTransfer });
		clipboard.dispose();
		bos.close();
	}

	/**
	 * Generates diff and writes it into the given file.
	 * 
	 * @param file file where diff will be written
	 * @throws IOException
	 */
	public void generateDiff(File file) throws IOException {
		FileOutputStream fos = null;
		PrintStream ps = null;
		try {
			fos = new FileOutputStream(file);
			try {
				ps = new PrintStream(fos);
				generateDiff(ps);
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

	/**
	 * Generates diff and writes it into the given output.
	 * 
	 * @param output output to which diff will be written
	 */
	private void generateDiff(PrintStream output) {
		// If the first block isn't the only one, or first block is different
		if (fAllDiffs.size() > 1 || isPartDifferent(0)) {
			output.println(INDEX_MARKER + newPath);
			output.println(DELIMITER);
			Date oldDate = Calendar.getInstance().getTime();
			Date newDate = Calendar.getInstance().getTime();
			String oldDateFormat = DateFormat.getDateTimeInstance().format(oldDate);
			String newDateFormat = DateFormat.getDateTimeInstance().format(newDate);
			output.println(OLD_FILE_PREFIX + oldPath + '\t'
					+ oldDateFormat + " -0000"); //$NON-NLS-1$
			output.println(NEW_FILE_PREFIX + newPath + '\t'
					+ newDateFormat + " -0000"); //$NON-NLS-1$

			boolean firstHunk = true;
			Hunk currentHunk = null;

			int currentLineNumberOld = 0;
			int currentLineNumberNew = 0;

			for (int i = 0; i < fAllDiffs.size(); i++) {

				List oldPart = getPart(i, LEFT_CONTRIBUTOR);
				List newPart = getPart(i, RIGHT_CONTRIBUTOR);

				if (isPartDifferent(i)) {
					// This part has some changes
					if (firstHunk) {
						// Hunk will start with changed block
						currentHunk = new Hunk(0, 0);
						firstHunk = false;
					}
					if (i == (fAllDiffs.size() - 1)) {
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
						currentHunk = new Hunk((oldPart.size() - 1) - NUMBER_OF_CONTEXT_LINES,
								(oldPart.size() - 1) - NUMBER_OF_CONTEXT_LINES);
						firstHunk = false;
						currentHunk.addPartRangeToBoth(oldPart,
								(oldPart.size() - 1) - NUMBER_OF_CONTEXT_LINES, oldPart.size(), false);
					} else {
						if (i == (fAllDiffs.size() - 1)) {
							// If it is the last part
							currentHunk.addPartRangeToBoth(oldPart, 0, NUMBER_OF_CONTEXT_LINES, true);
						} else {
							if (oldPart.size() - 1 < 2*NUMBER_OF_CONTEXT_LINES) {
								// Context too short to start new hunk
								currentHunk.addPartRangeToBoth(oldPart, 0,
										oldPart.size(), false);
							} else {
								// Context long enough to start new hunk
								currentHunk.addPartRangeToBoth(oldPart, 0, NUMBER_OF_CONTEXT_LINES,
										false);
								currentHunk.printTo(output);
								currentHunk = new Hunk(currentLineNumberOld
										+ (oldPart.size() - 1) - NUMBER_OF_CONTEXT_LINES,
										currentLineNumberNew + (oldPart.size() - 1)
										- NUMBER_OF_CONTEXT_LINES);
								currentHunk.addPartRangeToBoth(oldPart,
										(oldPart.size() - 1) - NUMBER_OF_CONTEXT_LINES,
										oldPart.size(), false);
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

	private List getPart(int i, char side) {
		try {
			String s = extract(i, side).replaceAll("\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
			List diffLines = new ArrayList(Arrays
					.asList(s.split("\n", -1))); //$NON-NLS-1$
			return diffLines;
		} catch (BadLocationException e) {
			CompareUIPlugin.log(e);
		}
		return null;
	}

	private String extract(int i, char side) throws BadLocationException {
		Diff diff = ((Diff) fAllDiffs.get(i));
		if ((side == LEFT_CONTRIBUTOR && !rightToLeft) || 
				(side == RIGHT_CONTRIBUTOR && rightToLeft)) {
			return leftDoc.get(diff.getPosition(LEFT_CONTRIBUTOR).offset, diff
					.getPosition(LEFT_CONTRIBUTOR).length);
		}
		return rightDoc.get(diff.getPosition(RIGHT_CONTRIBUTOR).offset, diff
				.getPosition(RIGHT_CONTRIBUTOR).length);
	}

	private boolean isPartDifferent(int i) {
		Diff diff = ((Diff) fAllDiffs.get(i));
		if (diff.getKind() == RangeDifference.CHANGE) {
			return true;
		}
		return false;
	}

	private class Hunk {
		private int oldStart;
		private int oldLength;
		private int newStart;
		private int newLength;
		private boolean printNoNewlineMarker;
		List lines;

		public Hunk(int oldStart, int newStart) {
			if (oldStart < 0)
				oldStart = 0;
			if (newStart < 0)
				newStart = 0;
			this.oldStart = oldStart;
			this.newStart = newStart;
			this.oldLength = 0;
			this.newLength = 0;
			printNoNewlineMarker = false;
			lines = new ArrayList();
		}

		public void addPartRangeToOld(List part, int start, int end,
				boolean lastPart) {
			if (start < 0)
				start = 0;
			if (STRICT_UNIX_FORMAT) {
				if ((lastPart) && part.size() != 1)
					end = Math.min(end, part.size());
				else
					end = Math.min(end, part.size() - 1);
				for (int lineNr = start; lineNr < end; lineNr++) {
					lines.add(OLD_LINE_PREFIX + part.get(lineNr));
					oldLength++;
				}
			}
			else {
				end = Math.min(end, part.size() - 1);
				for (int lineNr = start; lineNr < end; lineNr++) {
					lines.add(OLD_LINE_PREFIX + part.get(lineNr));
					oldLength++;
				}
				if (!part.get(part.size() - 1).toString().equals(""))	//$NON-NLS-1$
				{
					//part doesn't end with newline character
					//don't cut the last line, because it isn't empty
					lines.add(OLD_LINE_PREFIX + part.get(part.size() - 1));
					oldLength++;
					printNoNewlineMarker = true;
				}
			}
		}

		public void addPartRangeToNew(List part, int start, int end,
				boolean lastPart) {
			if (start < 0)
				start = 0;
			if (STRICT_UNIX_FORMAT) {
				if ((lastPart) && part.size() != 1)
					end = Math.min(end, part.size());
				else
					end = Math.min(end, part.size() - 1);
				for (int lineNr = start; lineNr < end; lineNr++) {
					lines.add(NEW_LINE_PREFIX + part.get(lineNr));
					newLength++;
				}
			}
			else {
				end = Math.min(end, part.size() - 1);
				for (int lineNr = start; lineNr < end; lineNr++) {
					lines.add(NEW_LINE_PREFIX + part.get(lineNr));
					newLength++;
				}
				if (!part.get(part.size() - 1).toString().equals("")) {	//$NON-NLS-1$
					//part doesn't end with newline character
					//don't cut the last line, because it isn't empty
					lines.add(NEW_LINE_PREFIX + part.get(part.size() - 1));
					newLength++;
					printNoNewlineMarker = true;
				}
			}
		}

		public void addPartRangeToBoth(List part, int start, int end,
				boolean lastPart) {
			if (start < 0)
				start = 0;
			if (STRICT_UNIX_FORMAT) {
				if (lastPart)
					end = Math.min(end, part.size());
				else
					end = Math.min(end, part.size() - 1);
				for (int lineNr = start; lineNr < end; lineNr++) {
					lines.add(CONTEXT_LINE_PREFIX + part.get(lineNr));
					oldLength++;
					newLength++;
				}
			}
			else {
				end = Math.min(end, part.size() - 1);
				for (int lineNr = start; lineNr < end; lineNr++) {
					lines.add(CONTEXT_LINE_PREFIX + part.get(lineNr));
					oldLength++;
					newLength++;
				}
				if (!part.get(part.size() - 1).toString().equals("")) {	//$NON-NLS-1$
					//part doesn't end with newline character
					//don't cut the last line, because it isn't empty
					lines.add(CONTEXT_LINE_PREFIX + part.get(part.size() - 1));
					oldLength++;
					newLength++;
					printNoNewlineMarker = true;
				}
			}
		}

		private void printMarkerTo(PrintStream output) {
			if (oldLength == 0)
			{
				//all lines are new lines
				oldStart = -1;
			}
			if (newLength == 0) {
				//all lines are old lines
				newStart = -1;
			}
			output.println(RANGE_INFORMATION_PREFIX + (oldStart+1) + "," + oldLength + " +" + (newStart+1) //$NON-NLS-1$ //$NON-NLS-2$
					+ "," + newLength + RANGE_INFORMATION_AFFIX); //$NON-NLS-1$
		}

		public void printTo(PrintStream output) {
			printMarkerTo(output);
			for (int i = 0; i < lines.size(); i++) {
				output.println(lines.get(i));
			}
			if (printNoNewlineMarker) {
				output.println("\\ No newline at end of file");	//$NON-NLS-1$
			}
		}
	}

}