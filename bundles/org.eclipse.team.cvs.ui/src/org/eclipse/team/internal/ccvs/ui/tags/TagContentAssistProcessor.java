/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.tags;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.contentassist.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.ui.contentassist.ContentAssistHandler;


public class TagContentAssistProcessor implements ISubjectControlContentAssistProcessor {

    private FilteredTagList tags;
    private String lastError;

    public static void createContentAssistant(Text text, TagSource tagSource, int includeFlags) {
		ContentAssistHandler.createHandlerForText(text, createSubjectContentAssistant(new TagContentAssistProcessor(tagSource, includeFlags)));
	}
	
	public static SubjectControlContentAssistant createSubjectContentAssistant(IContentAssistProcessor processor) {
		final SubjectControlContentAssistant contentAssistant= new SubjectControlContentAssistant();
		
		contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
		//ContentAssistPreference.configure(contentAssistant, JavaPlugin.getDefault().getPreferenceStore());
		
		contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent);
			}
		});
		
		return contentAssistant;
	}
	
    public TagContentAssistProcessor(TagSource tagSource, int includeFlags) {
        tags = new FilteredTagList(tagSource, TagSource.convertIncludeFlaqsToTagTypes(includeFlags));
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.contentassist.IContentAssistSubjectControl, int)
     */
    public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
        Control c = contentAssistSubjectControl.getControl();
        int docLength = contentAssistSubjectControl.getDocument().getLength();
        if (c instanceof Text) {
            Text t = (Text)c;
            String filter = t.getText();
            tags.setPattern(filter);
            CVSTag[] matching = tags.getMatchingTags();
            if (matching.length > 0) {
                List proposals = new ArrayList();
                for (int i = 0; i < matching.length; i++) {
                    CVSTag tag = matching[i];
                    String name = tag.getName();
                    Image image = null;
                    CompletionProposal proposal = new CompletionProposal(name, 0, docLength, name.length(), image, name, null, null);
                    proposals.add(proposal);
                }
                lastError = null;
                return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
            }
        }
        lastError = "No matching tags found";
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor#computeContextInformation(org.eclipse.jface.contentassist.IContentAssistSubjectControl, int)
     */
    public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    public String getErrorMessage() {
        return lastError;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }
    
}