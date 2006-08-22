package org.eclipse.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.part.FileEditorInput;

/**
 * 
 * @since 3.3
 */
public class FileDocumentNode extends DocumentNode implements IEncodedStreamContentAccessor, ITypedElement,
	IEditableContent, IModificationDate, IResourceProvider{

	private final IFile fFile;

	/**
	 * Create a node for the given file
	 * @param file the file
	 */
	public FileDocumentNode(IFile file) {
		super(new FileEditorInput(file));
		this.fFile = file;
	}

	public ImageDescriptor getImageDescriptor() {
		return ImageDescriptor.createFromImage(getImage());
	}

	public String getToolTipText() {
		return fFile.getFullPath().toString();
	}

	public String getCharset() throws CoreException {
		return Utilities.getCharset(fFile);
	}

	public boolean isEditable() {
		return true;
	}

	public ITypedElement replace(ITypedElement dest, ITypedElement src) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns the corresponding resource for this object.
	 *
	 * @return the corresponding resource
	 */
	public IResource getResource() {
		return fFile;
	}
	
	/* (non Javadoc)
	 * see IModificationDate.getModificationDate
	 */
	public long getModificationDate() {
		return fFile.getLocalTimeStamp();
	}
	
	/* (non Javadoc)
	 * see ITypedElement.getName
	 */
	public String getName() {
		if (fFile != null)
			return fFile.getName();
		return null;
	}
		
	/* (non Javadoc)
	 * see ITypedElement.getType
	 */
	public String getType() {
		if (fFile != null) {
			String s= fFile.getFileExtension();
			if (s != null)
				return s;
		}
		return ITypedElement.UNKNOWN_TYPE;
	}
	
	/* (non Javadoc)
	 * see ITypedElement.getImage
	 */
	public Image getImage() {
		return CompareUI.getImage(fFile);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.DocumentNode#internalGetContents()
	 */
	protected InputStream internalGetContents() throws CoreException {
		return fFile.getContents();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.DocumentNode#internalSetContents(byte[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void internalSetContents(byte[] contents, IProgressMonitor monitor)
			throws CoreException {
		if (contents == null) contents = new byte[0];
		final InputStream is = new ByteArrayInputStream(contents);
		if (is != null) {
			if (!fFile.exists()) {
				fFile.create(is, false, monitor);
			} else {
				fFile.setContents(is, false, true, monitor);
			}
		} else {
			fFile.delete(false, true, monitor);
		}
	}

}
