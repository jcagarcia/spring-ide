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
package org.springframework.ide.eclipse.beans.ui.editor.namespaces.webflow;

import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeLabelProvider;
import org.springframework.ide.eclipse.beans.ui.editor.outline.BeansContentOutlineConfiguration;
import org.springframework.ide.eclipse.beans.ui.editor.util.BeansEditorUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Node;

/**
 * 
 */
@SuppressWarnings("restriction")
public class WebflowConfigOutlineLabelProvider extends JFaceNodeLabelProvider {

	/**
	 * 
	 * 
	 * @param object 
	 * 
	 * @return 
	 */
	@Override
	public Image getImage(Object object) {
		Node node = (Node) object;
		String ns = node.getNamespaceURI();
		if ("http://www.springframework.org/schema/webflow-config".equals(ns)) {
			return WebflowConfigUIImages.getImage(WebflowConfigUIImages.IMG_OBJS_WEBFLOW);
		}
		return null;
	}

	/**
	 * 
	 * 
	 * @param o 
	 * 
	 * @return 
	 */
	@Override
	public String getText(Object o) {

		// Create Spring beans label text
		Node node = (Node) o;
		String nodeName = node.getNodeName();
		String shortNodeName = node.getLocalName();

		String text = null;
		if ("executor".equals(shortNodeName)) {
			text = nodeName;
			String id = BeansEditorUtils.getAttribute(node, "id");
			if (StringUtils.hasText(id)) {
				text += " " + id;
			}
			if (BeansContentOutlineConfiguration.isShowAttributes()
					&& BeansEditorUtils.hasAttribute(node, "registry-ref")) {
				text += " <" + BeansEditorUtils.getAttribute(node, "registry-ref") + ">";
			}
		}
		else if ("registry".equals(shortNodeName)) {
			text = nodeName;
			String id = BeansEditorUtils.getAttribute(node, "id");
			if (StringUtils.hasText(id)) {
				text += " " + id;
			}
		}
		else if ("location".equals(shortNodeName)) {
			text = nodeName;
			if (BeansContentOutlineConfiguration.isShowAttributes() && BeansEditorUtils.hasAttribute(node, "path")) {
				text += " [" + BeansEditorUtils.getAttribute(node, "path") + "]";
			}
		}
		else if ("listener".equals(shortNodeName)) {
			text = nodeName;
			if (BeansContentOutlineConfiguration.isShowAttributes() && BeansEditorUtils.hasAttribute(node, "ref")) {
				text += " <" + BeansEditorUtils.getAttribute(node, "ref") + ">";
			}
		}
		else if ("attribute".equals(shortNodeName)) {
			text = nodeName;
			if (BeansContentOutlineConfiguration.isShowAttributes() && BeansEditorUtils.hasAttribute(node, "name")) {
				text += " " + BeansEditorUtils.getAttribute(node, "name") + "="
						+ BeansEditorUtils.getAttribute(node, "value");
			}
		}
		else if ("repository".equals(shortNodeName)) {
			text = nodeName;
			if (BeansContentOutlineConfiguration.isShowAttributes() && BeansEditorUtils.hasAttribute(node, "type")) {
				text += " " + BeansEditorUtils.getAttribute(node, "type");
			}
		}
		else if ("alwaysRedirectOnPause".equals(shortNodeName)) {
			text = nodeName;
			if (BeansContentOutlineConfiguration.isShowAttributes() && BeansEditorUtils.hasAttribute(node, "value")) {
				text += " " + BeansEditorUtils.getAttribute(node, "value");
			}
		}
		return text;
	}
}
