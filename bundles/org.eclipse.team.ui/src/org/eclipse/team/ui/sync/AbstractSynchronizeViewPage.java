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
package org.eclipse.team.ui.sync;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;

public abstract class AbstractSynchronizeViewPage implements ISynchronizeViewPage {
	
	// property listeners
	private ListenerList fListeners;
	
	private String fName = null;
	
	private ImageDescriptor fImageDescriptor = null;
	
	/**
	 * Used to notify this console of lifecycle methods <code>init()</code>
	 * and <code>dispose()</code>.
	 */
	class Lifecycle implements ISynchronizePageListener {
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.console.IConsoleListener#consolesAdded(org.eclipse.ui.console.IConsole[])
		 */
		public void consolesAdded(ISynchronizeViewPage[] consoles) {
			for (int i = 0; i < consoles.length; i++) {
				ISynchronizeViewPage console = consoles[i];
				if (console == AbstractSynchronizeViewPage.this) {
					init();
				}
			}

		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.console.IConsoleListener#consolesRemoved(org.eclipse.ui.console.IConsole[])
		 */
		public void consolesRemoved(ISynchronizeViewPage[] consoles) {
			for (int i = 0; i < consoles.length; i++) {
				ISynchronizeViewPage console = consoles[i];
				if (console == AbstractSynchronizeViewPage.this) {
					//ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
					dispose();
				}
			}
		}
	}
	
	/**
	 * Notifies listeners of property changes, handling any exceptions
	 */
	class PropertyNotifier implements ISafeRunnable {
		
		private IPropertyChangeListener fListener;
		private PropertyChangeEvent fEvent;
		
		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		public void handleException(Throwable exception) {
			// TODO:
			//IStatus status = new Status(IStatus.ERROR, ConsolePlugin.getUniqueIdentifier(), IConsoleConstants.INTERNAL_ERROR, ConsoleMessages.getString("AbstractConsole.0"), exception); //$NON-NLS-1$
			//ConsolePlugin.log(status);
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		public void run() throws Exception {
			fListener.propertyChange(fEvent);
		}

		/**
		 * Notifies listeners of the property change
		 * 
		 * @param property the property that has changed
		 */
		public void notify(PropertyChangeEvent event) {
			if (fListeners == null) {
				return;
			}
			fEvent = event;
			Object[] copiedListeners= fListeners.getListeners();
			for (int i= 0; i < copiedListeners.length; i++) {
				fListener = (IPropertyChangeListener)copiedListeners[i];
				Platform.run(this);
			}	
			fListener = null;			
		}
	}		
	
	/**
	 * Constructs a new console with the given name and image.
	 * 
	 * @param name console name, cannot be <code>null</code>
	 * @param imageDescriptor image descriptor, or <code>null</code> if none
	 */
	public AbstractSynchronizeViewPage(String name, ImageDescriptor imageDescriptor) {
		setName(name);
		setImageDescriptor(imageDescriptor);
		//ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(new Lifecycle());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsole#getName()
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Sets the name of this console to the specified value and notifies
	 * property listeners of the change.
	 * 
	 * @param name the new name
	 */
	protected void setName(String name) {
		String old = fName;
		fName = name;
		firePropertyChange(this, IBasicPropertyConstants.P_TEXT, old, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsole#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return fImageDescriptor;
	}
	
	/**
	 * Sets the image descriptor for this console to the specified value and notifies
	 * property listeners of the change.
	 * 
	 * @param imageDescriptor the new image descriptor
	 */
	protected void setImageDescriptor(ImageDescriptor imageDescriptor) {
		ImageDescriptor old = fImageDescriptor;
		fImageDescriptor =imageDescriptor;
		firePropertyChange(this, IBasicPropertyConstants.P_IMAGE, old, imageDescriptor);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsole#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList();
		}
		fListeners.add(listener);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.console.IConsole#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}

	/**
	 * Notify all listeners that the given property has changed.
	 * 
	 * @param source the object on which a property has changed 
	 * @param property identifier of the property that has changed
	 * @param oldValue the old value of the property, or <code>null</code>
	 * @param newValue the new value of the property, or <code>null</code>
	 */
	public void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
		if (fListeners == null) {
			return;
		}
		PropertyNotifier notifier = new PropertyNotifier();
		notifier.notify(new PropertyChangeEvent(source, property, oldValue, newValue));
	}
	
	/**
	 * Called when this console is added to the console manager. Default
	 * implementation does nothing. Subclasses may override.
	 */
	protected void init() {
	}
	
	/**
	 * Called when this console is removed from the console manager. Default
	 * implementation does nothing. Subclasses may override.
	 */
	protected void dispose() {
	}
}
