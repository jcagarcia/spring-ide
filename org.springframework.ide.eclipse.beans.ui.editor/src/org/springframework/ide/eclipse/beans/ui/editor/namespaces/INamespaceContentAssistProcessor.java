/*******************************************************************************
 * Copyright (c) 2005, 2007 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.beans.ui.editor.namespaces;

import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.wst.xml.ui.internal.contentassist.ContentAssistRequest;

@SuppressWarnings("restriction")
public interface INamespaceContentAssistProcessor {

	void addAttributeValueProposals(
			IContentAssistProcessor delegatingContentAssistProcessor,
			ContentAssistRequest request);

	void addAttributeNameProposals(
			IContentAssistProcessor delegatingContentAssistProcessor,
			ContentAssistRequest request);

	void addTagCloseProposals(
			IContentAssistProcessor delegatingContentAssistProcessor,
			ContentAssistRequest request);

	void addTagInsertionProposals(
			IContentAssistProcessor delegatingContentAssistProcessor,
			ContentAssistRequest request, int childPosition);
}
