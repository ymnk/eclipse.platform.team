/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 35390 Three-way compare cannot select (mis-selects) )ancestor resource
 *     Aleksandra Wozniak (aleksandra.k.wozniak@gmail.com) - Bug 239959, Bug 73923
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.osgi.util.NLS;

public final class CompareMessages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.compare.internal.CompareMessages";//$NON-NLS-1$

	private CompareMessages() {
		// Do not instantiate
	}

	public static String CompareContainer_0;
	public static String CompareDialog_commit_button;
	public static String CompareDialog_error_message;
	public static String CompareDialog_error_title;
	public static String CompareEditor_0;
	public static String CompareEditor_1;
	public static String CompareEditor_10;
	public static String CompareEditor_11;
	public static String DocumentMerger_0;
	public static String DocumentMerger_1;
	public static String DocumentMerger_2;
	public static String DocumentMerger_3;
	public static String CompareEditor_2;
	public static String CompareEditor_3;
	public static String CompareEditor_4;
	public static String CompareEditor_5;
	public static String CompareEditor_6;
	public static String CompareEditor_7;
	public static String CompareEditor_8;
	public static String CompareEditor_9;
	public static String CompareEditorInput_0;
	public static String ComparePlugin_internal_error;
	public static String ComparePreferencePage_0;
	public static String ComparePreferencePage_1;
	public static String ComparePreferencePage_2;
	public static String ComparePreferencePage_3;
	public static String CompareUIPlugin_0;
	public static String CompareUIPlugin_1;
	public static String ContentMergeViewer_resource_changed_description;
	public static String ContentMergeViewer_resource_changed_title;
	public static String ExceptionDialog_seeErrorLogMessage;
	public static String CompareViewerSwitchingPane_Titleformat;
	public static String NavigationEndDialog_0;
	public static String NavigationEndDialog_1;
	public static String ShowWhitespaceAction_0;
	public static String StructureDiffViewer_0;
	public static String StructureDiffViewer_1;
	public static String StructureDiffViewer_2;
	public static String StructureDiffViewer_3;
	public static String StructureDiffViewer_NoStructuralDifferences;
	public static String StructureDiffViewer_StructureError;
	public static String TextMergeViewer_0;
	public static String TextMergeViewer_1;
	public static String TextMergeViewer_10;
	public static String TextMergeViewer_11;
	public static String TextMergeViewer_12;
	public static String TextMergeViewer_13;
	public static String TextMergeViewer_14;
	public static String TextMergeViewer_15;
	public static String TextMergeViewer_16;
	public static String TextMergeViewer_2;
	public static String TextMergeViewer_3;
	public static String TextMergeViewer_4;
	public static String TextMergeViewer_5;
	public static String TextMergeViewer_6;
	public static String TextMergeViewer_7;
	public static String TextMergeViewer_8;
	public static String TextMergeViewer_9;
	public static String TextMergeViewer_accessible_ancestor;
	public static String TextMergeViewer_accessible_left;
	public static String TextMergeViewer_accessible_right;
	public static String TextMergeViewer_cursorPosition_format;
	public static String TextMergeViewer_beforeLine_format;
	public static String TextMergeViewer_range_format;
	public static String TextMergeViewer_changeType_addition;
	public static String TextMergeViewer_changeType_deletion;
	public static String TextMergeViewer_changeType_change;
	public static String TextMergeViewer_direction_outgoing;
	public static String TextMergeViewer_direction_incoming;
	public static String TextMergeViewer_direction_conflicting;
	public static String TextMergeViewer_diffType_format;
	public static String TextMergeViewer_diffDescription_noDiff_format;
	public static String TextMergeViewer_diffDescription_diff_format;
	public static String TextMergeViewer_statusLine_format;
	public static String TextMergeViewer_atEnd_title;
	public static String TextMergeViewer_atEnd_message;
	public static String TextMergeViewer_atBeginning_title;
	public static String TextMergeViewer_atBeginning_message;
	public static String CompareNavigator_atEnd_title;
	public static String CompareNavigator_atEnd_message;
	public static String CompareNavigator_atBeginning_title;
	public static String CompareNavigator_atBeginning_message;
	public static String WorkerJob_0;
	public static String SelectAncestorDialog_title;
	public static String SelectAncestorDialog_message;
	public static String SelectAncestorDialog_option;
	public static String CompareWithOther_fileLabel;
	public static String CompareWithOther_ancestor;
	public static String CompareWithOther_rightPanel;
	public static String CompareWithOther_leftPanel;
	public static String CompareWithOther_dialogTitle;
	public static String CompareWithOther_dialogMessage;
	public static String CompareWithOther_error_not_comparable;
	public static String CompareWithOther_error_empty;
	public static String CompareWithOther_clear;
	public static String CompareWithOther_info;
	public static String CompareWithOther_externalFileButton;
	public static String CompareWithOther_externalFile_errorTitle;
	public static String CompareWithOther_externalFile_errorMessage;
	public static String CompareWithOther_pathLabel;
	public static String CompareWithOther_externalFolderBUtton;
	public static String CompareWithOtherResourceDialog_externalFileMainButton;
	public static String CompareWithOtherResourceDialog_externalFileRadioButton;
	public static String CompareWithOtherResourceDialog_externalFolderMainButton;
	public static String CompareWithOtherResourceDialog_externalFolderRadioButton;
	public static String CompareWithOtherResourceDialog_workspaceMainButton;
	public static String CompareWithOtherResourceDialog_workspaceRadioButton;

	public static String CreatePatchActionTitle;
	public static String WorkspacePatchDialogTitle;
	public static String WorkspacePatchDialogDescription;
	public static String Save_Patch_As_5;
	public static String Save_To_Clipboard_2;
	public static String Save_In_File_System_3;
	public static String Browse____4;
	public static String patch_txt_6;
	public static String Save_In_Workspace_7;
	public static String Fi_le_name__9;
	public static String Context_14;
	public static String Standard_15;
	public static String Diff_output_format_12;
	public static String Advanced_options_19;
	public static String Configure_the_options_used_for_the_CVS_diff_command_20;
	public static String Unified__format_required_by_Compare_With_Patch_feature__13;
	public static String GenerateLocalDiff_title;
	public static String GenerateLocalDiff_pageTitle;
	public static String GenerateLocalDiff_pageDescription;
	public static String GenerateLocalDiff_Specify_the_file_which_contributes_the_changes;
	public static String GenerateLocalDiff_overwriteTitle;
	public static String GenerateLocalDiff_overwriteMsg;
	public static String GenerateLocalDiff_1;
	public static String GenerateLocalDiff_2;
	public static String GenerateDiffFileWizard_6;
	public static String GenerateDiffFileWizard_7;
	public static String GenerateDiffFileWizard_8;
	public static String GenerateDiffFileWizard_9;
	public static String GenerateDiffFileWizard_10;
	public static String GenerateDiffFileWizard_0;
	public static String GenerateDiffFileWizard_2;
	public static String GenerateDiffFileWizard_3;
	public static String GenerateDiffFileWizard_4;
	public static String GenerateDiffFileWizard_5;
	public static String GenerateDiffFileWizard_browseFilesystem;
	public static String GenerateDiffFileWizard_FolderExists;
	public static String GenerateDiffFileWizard_ProjectClosed;
	public static String GenerateDiffFileWizard_13;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CompareMessages.class);
	}
}
