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
package org.eclipse.team.internal.ccvs.ui;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.*;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.Assert;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

/**
 * A decoration describes the annotations to a user interface element. The
 * annotations can apply to text (e.g. prefix, suffix, color, font) and to an
 * image (e.g. overlays).
 * <p>
 * This class is derived from an internal workbench class
 * <code>IDecoration</code> and is often used in conjunction with the label
 * decoration APIs. As such a client can convert between them using helpers
 * defined in this class.
 * </p>
 * 
 * @since 3.1
 */
public class CVSDecoration {

	// Decorations
	private String prefix;
	private String suffix;
	private ImageDescriptor overlay;
	private Color bkgColor;
	private Color fgColor;
	private Font font;
	// Type of the resource being decorated (e.g. IResource.PROJECT,
	// IResource.FILE, IResource.FOLDER)
	private int resourceType;
	// Properties
	private boolean watchEditEnabled = false;
	private boolean isDirty = false;
	private boolean isIgnored = false;
	private boolean isAdded = false;
	private boolean isNewResource = false;
	private boolean hasRemote = false;
	private boolean readOnly = false;
	private boolean needsMerge = false;
	private boolean virtualFolder = false;
	private String tag;
	private String revision;
	private String repository;
	private ICVSRepositoryLocation location;
	private String keywordSubstitution;
	// Preferences
	// Image states to show
	private boolean[] preferences;
	private int PREF_SHOW_DIRTY_IMAGE_DECORATION = 0;
	private int PREF_SHOW_ADDED_IMAGE_DECORATION = 1;
	private int PREF_SHOW_HASREMOTE_IMAGE_DECORATION = 2;
	private int PREF_SHOW_NEWRESOURCE_IMAGE_DECORATION = 3;
	private int PREF_CALCULATE_DIRTY = 4;
	// Text states to show
	private String fileFormatter;
	private String folderFormatter;
	private String projectFormatter;
	private String dirtyTextIndicator;
	private String addedTextIndicator;
	private String resourceName;
	// Color indicators
	// Font indicators
	//	Images cached for better performance
	private static ImageDescriptor dirty;
	private static ImageDescriptor checkedIn;
	private static ImageDescriptor noRemoteDir;
	private static ImageDescriptor added;
	private static ImageDescriptor merged;
	private static ImageDescriptor newResource;
	private static ImageDescriptor edited;

	/*
	 * Define a cached image descriptor which only creates the image data once
	 */
	public static class CachedImageDescriptor extends ImageDescriptor {

		ImageDescriptor descriptor;
		ImageData data;

		public CachedImageDescriptor(ImageDescriptor descriptor) {
			Assert.isNotNull(descriptor);
			this.descriptor = descriptor;
		}

		public ImageData getImageData() {
			if (data == null) {
				data = descriptor.getImageData();
			}
			return data;
		}
	}

	static {
		dirty = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_DIRTY_OVR));
		checkedIn = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		added = new CachedImageDescriptor(TeamUIPlugin.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
		merged = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_MERGED));
		newResource = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_QUESTIONABLE));
		edited = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_EDITED));
		noRemoteDir = new CachedImageDescriptor(CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_NO_REMOTEDIR));
	}

	/**
	 * Default constructor uses the preferences to determine text decoration
	 * formatters and
	 *  
	 * @param resourceType
	 */
	public CVSDecoration(String resourceName, int resourceType) {
		// 	TODO: for efficiency don't look up a pref until its needed
		IPreferenceStore store = getStore();
		initialize(resourceName, resourceType, new boolean[]{store.getBoolean(ICVSUIConstants.PREF_SHOW_DIRTY_DECORATION),// dirty image
				store.getBoolean(ICVSUIConstants.PREF_SHOW_ADDED_DECORATION), // added image
				store.getBoolean(ICVSUIConstants.PREF_SHOW_HASREMOTE_DECORATION), // has remote
				store.getBoolean(ICVSUIConstants.PREF_SHOW_NEWRESOURCE_DECORATION), // new resource
				store.getBoolean(ICVSUIConstants.PREF_CALCULATE_DIRTY) // calculate deep dirty
				}, store.getString(ICVSUIConstants.PREF_FILETEXT_DECORATION), store.getString(ICVSUIConstants.PREF_FOLDERTEXT_DECORATION), store.getString(ICVSUIConstants.PREF_PROJECTTEXT_DECORATION));
	}

	public CVSDecoration(String resourceName, int resourceType, boolean[] preferences, String fileFormater, String folderFormatter, String projectFormatter) {
		initialize(resourceName, resourceType, preferences, fileFormater, folderFormatter, projectFormatter);
	}

	private IPreferenceStore getStore() {
		return CVSUIPlugin.getPlugin().getPreferenceStore();
	}

	private void initialize(String resourceName, int resourceType, boolean[] preferences, String fileFormater, String folderFormatter, String projectFormatter) {
		this.resourceType = resourceType;
		this.resourceName = resourceName;
		this.preferences = preferences;
		this.fileFormatter = fileFormater;
		this.folderFormatter = folderFormatter;
		this.projectFormatter = projectFormatter;
	}

	public void addPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void addSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void addOverlay(ImageDescriptor overlay) {
		this.overlay = overlay;
	}

	public void setForegroundColor(Color fgColor) {
		this.fgColor = fgColor;
	}

	public void setBackgroundColor(Color bkgColor) {
		this.bkgColor = bkgColor;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Color getBackgroundColor() {
		return bkgColor;
	}

	public Color getForegroundColor() {
		return fgColor;
	}

	public Font getFont() {
		return font;
	}

	public ImageDescriptor getOverlay() {
		return overlay;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void apply(IDecoration decoration) {
		compute();
		// apply changes
		String suffix = getSuffix();
		if(suffix != null)
			decoration.addSuffix(suffix);
		String prefix = getPrefix();
		if(prefix != null)
			decoration.addPrefix(prefix);
		ImageDescriptor overlay = getOverlay();
		if(overlay != null)
			decoration.addOverlay(getOverlay());
		Color bc = getBackgroundColor();
		if(bc != null)
			decoration.setBackgroundColor(getBackgroundColor());
		Color fc = getForegroundColor();
		if(fc != null)
			decoration.setForegroundColor(getForegroundColor());
		Font f = getFont();
		if(f != null)
			decoration.setFont(getFont());
	}

	public void compute() {
		computeText();
		computeImage();
		computeColorsAndFonts();
	}

	private void computeText() {
		Map bindings = new HashMap();
		IPreferenceStore store = getStore();
		if (isDirty()) {
			bindings.put(CVSDecoratorConfiguration.DIRTY_FLAG, store.getString(ICVSUIConstants.PREF_DIRTY_FLAG));
		}
		if (isAdded()) {
			bindings.put(CVSDecoratorConfiguration.ADDED_FLAG, store.getString(ICVSUIConstants.PREF_ADDED_FLAG));
		}
		bindings.put(CVSDecoratorConfiguration.FILE_REVISION, getRevision());
		bindings.put(CVSDecoratorConfiguration.RESOURCE_TAG, getTag());
		bindings.put(CVSDecoratorConfiguration.RESOURCE_NAME, getResourceName());
		bindings.put(CVSDecoratorConfiguration.FILE_KEYWORD, getKeywordSubstitution());
		if (resourceType != IResource.FILE && location != null) {
			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_HOST, location.getHost());
			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_METHOD, location.getMethod().getName());
			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_USER, location.getUsername());
			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_ROOT, location.getRootDirectory());
			bindings.put(CVSDecoratorConfiguration.REMOTELOCATION_REPOSITORY, repository);
		}
		CVSDecoratorConfiguration.decorate(this, getTextFormatter(), bindings);
	}

	private ImageDescriptor computeImage() {
		// show newResource icon
		if (preferences[PREF_SHOW_NEWRESOURCE_IMAGE_DECORATION] && isNewResource()) {
			return newResource;
		}
		// show dirty icon
		if (preferences[PREF_SHOW_DIRTY_IMAGE_DECORATION] && isDirty()) {
			return dirty;
		}
		// show added
		if (preferences[PREF_SHOW_ADDED_IMAGE_DECORATION] && isAdded()) {
			return added;
		}
		// show watch edit
		if (isWatchEditEnabled() && resourceType == IResource.FILE && !isReadOnly() && isHasRemote()) {
			return edited;
		}
		// show checked in
		if (preferences[PREF_SHOW_ADDED_IMAGE_DECORATION] && isHasRemote()) {
			if (resourceType != IResource.FILE && isVirtualFolder()) {
				return noRemoteDir;
			}
			return checkedIn;
		}
		//nothing matched
		return null;
	}	
	
	private void computeColorsAndFonts() {
		ITheme current = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
		if(isIgnored()) {
			setBackgroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.IGNORED_BACKGROUND_COLOR));
			setForegroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.IGNORED_FOREGROUND_COLOR));
			setFont(current.getFontRegistry().get(CVSDecoratorConfiguration.IGNORED_FONT));
		} else if(isDirty()) {
			setBackgroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.OUTGOING_CHANGE_BACKGROUND_COLOR));
			setForegroundColor(current.getColorRegistry().get(CVSDecoratorConfiguration.OUTGOING_CHANGE_FOREGROUND_COLOR));
			setFont(current.getFontRegistry().get(CVSDecoratorConfiguration.OUTGOING_CHANGE_FONT));
		}
	}

	private String getResourceName() {
		return resourceName;
	}

	private String getTextFormatter() {
		switch (resourceType) {
			case IResource.FILE :
				return fileFormatter;
			case IResource.FOLDER :
				return folderFormatter;
			case IResource.PROJECT :
				return projectFormatter;
		}
		return "no format specified"; //$NON-NLS-1$
	}

	public boolean isAdded() {
		return isAdded;
	}

	public void setAdded(boolean isAdded) {
		this.isAdded = isAdded;
	}

	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}

	public boolean isIgnored() {
		return isIgnored;
	}

	public void setIgnored(boolean isIgnored) {
		this.isIgnored = isIgnored;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public boolean isWatchEditEnabled() {
		return watchEditEnabled;
	}

	public void setWatchEditEnabled(boolean watchEditEnabled) {
		this.watchEditEnabled = watchEditEnabled;
	}

	public boolean isNewResource() {
		return isNewResource;
	}

	public void setNewResource(boolean isNewResource) {
		this.isNewResource = isNewResource;
	}

	public ICVSRepositoryLocation getLocation() {
		return location;
	}

	public void setLocation(ICVSRepositoryLocation location) {
		this.location = location;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getKeywordSubstitution() {
		return keywordSubstitution;
	}

	public void setKeywordSubstitution(String keywordSubstitution) {
		this.keywordSubstitution = keywordSubstitution;
	}

	public void setNeedsMerge(boolean needsMerge) {
		this.needsMerge = needsMerge;
	}

	public boolean isHasRemote() {
		return hasRemote;
	}

	public void setHasRemote(boolean hasRemote) {
		this.hasRemote = hasRemote;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isVirtualFolder() {
		return virtualFolder;
	}

	public void setVirtualFolder(boolean virtualFolder) {
		this.virtualFolder = virtualFolder;
	}
}