package org.eclipse.compare.internal;

import java.io.InputStream;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An {@link IEditorInput} that is used as the document key.
 */
public class TypedElementEditorInput extends PlatformObject implements IEditorInput {

	private final ICompareInput compareInput;
	private final boolean left;
	
	public TypedElementEditorInput(ICompareInput compareInput, boolean left) {
		this.compareInput = compareInput;
		this.left = left;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		Image image = null;
		if (getTypedElement() != null)
			image = getTypedElement().getImage();
		if (image == null)
			image = compareInput.getImage();
		return ImageDescriptor.createFromImage(image);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		if (getTypedElement() != null)
			return getTypedElement().getName();
		return compareInput.getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return getName();
	}
	
	public ITypedElement getTypedElement() {
		if (left) {
			return compareInput.getLeft();
		}
		return compareInput.getRight();
	}
	
	public ITypedElement getOtherElement() {
		if (left) {
			return compareInput.getRight();
		}
		return compareInput.getLeft();
	}

	public ICompareInput getCompareInput() {
		return compareInput;
	}

	public boolean isLeft() {
		return left;
	}
	
	public String getEncoding() {
		String encoding = getStreamEncoding(getTypedElement());
		if (encoding != null)
			return encoding;
		encoding = getStreamEncoding(getOtherElement());
		if (encoding != null)
			return encoding;
		return null;
	}
	
	public InputStream getContents() throws CoreException {
		ITypedElement element = getTypedElement();
		if (element instanceof IStreamContentAccessor) {
			IStreamContentAccessor accessor = (IStreamContentAccessor) element;
			return accessor.getContents();
		}
		return null;
	}
	
	private static String getStreamEncoding(Object o) {
		if (o instanceof IEncodedStreamContentAccessor) {
			try {
				return ((IEncodedStreamContentAccessor)o).getCharset();
			} catch (CoreException e) {
				// silently ignored
			}
		}
		return null;
	}
	
}