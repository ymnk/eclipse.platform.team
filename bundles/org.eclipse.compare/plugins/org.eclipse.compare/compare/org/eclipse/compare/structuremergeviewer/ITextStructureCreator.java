/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.structuremergeviewer;

/**
 * An extension to the {@link IStructureCreator} interface that supports the
 * use of shared documents.
 * 
 * TODO: Should be an abstract class
 * 
 * @since 3.3
 */
public interface ITextStructureCreator extends IStructureCreator {
	
	/**
	 * Creates a tree structure consisting of <code>IStructureComparator</code>s
	 * from the given object and returns its root object. Implementing this
	 * method typically involves parsing the input object. In case of an error
	 * (e.g. a parsing error) the value <code>null</code> is returned.
	 * <p>
	 * If the calculation or editing of the structure uses a document, the
	 * provided document manager can be used to ensure that a shared document
	 * (e.g. file buffer) is used if available.
	 * 
	 * @param input
	 *            the object from which to create the tree of
	 *            <code>IStructureComparator</code>
	 * @param manager
	 *            a document manager that can be used to share a document
	 *            between the structure comparator and other compare facilities
	 *            (e.g. a content merge viewer).
	 * @return the root node of the structure or <code>null</code> in case of
	 *         error
	 * @see IStructureCreator#getStructure(Object)
	 */
	IStructureComparator getStructure(Object input, IDocumentManager manager);
	
	/**
	 * Creates the single node specified by path from the given input object.
	 * In case of an error (e.g. a parsing error) the value <code>null</code> is returned.
	 * This method is similar to <code>getStructure</code> but in
	 * contrast to <code>getStructure</code> only a single node without any children must be returned.
	 * This method is used in the <code>ReplaceWithEditionDialog</code> to locate a sub element
	 * (e.g. a method) within an input object (e.g. a file containing source code).
	 * <p>
	 * One (not optimized) approach to implement this method is calling <code>getStructure(input)</code>
	 * to build the full tree, and then finding that node within the tree that is specified
	 * by <code>path</code>.
	 * <p>
	 * The syntax of <code>path</code> is not specified, because it is treated by the compare subsystem
	 * as an opaque entity and is not further interpreted. Clients using this functionality
	 * will pass a value of <code>path</code> to the <code>selectEdition</code>
	 * method of <code>ReplaceWithEditionDialog</code> and will receive this value unchanged
	 * as an argument to <code>locate</code>.
	 * <p>
	 * If the calculation or editing of the structure uses a document, the
	 * provided document manager can be used to ensure that a shared document
	 * (e.g. file buffer) is used if available.
	 *
	 * @param path specifies a sub object within the input object
	 * @param input the object from which to create the <code>IStructureComparator</code>
	 * @param manager
	 *            a document manager that can be used to share a document
	 *            between the structure comparator and other compare facilities
	 *            (e.g. a content merge viewer).
	 * @return the single node specified by <code>path</code> or <code>null</code>
	 *
	 * @see org.eclipse.compare.EditionSelectionDialog#selectEdition
	 */
	IStructureComparator locate(Object path, Object input, IDocumentManager manager);
}
