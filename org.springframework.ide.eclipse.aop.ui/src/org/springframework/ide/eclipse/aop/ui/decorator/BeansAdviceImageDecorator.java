/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ide.eclipse.aop.ui.decorator;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.springframework.ide.eclipse.aop.core.Activator;
import org.springframework.ide.eclipse.aop.core.model.IAopModelChangedListener;
import org.springframework.ide.eclipse.aop.core.model.IAopReference;
import org.springframework.ide.eclipse.aop.core.model.internal.AnnotationAspectDefinition;
import org.springframework.ide.eclipse.aop.ui.BeansAopUIImages;
import org.springframework.ide.eclipse.aop.ui.navigator.model.AdviceRootAopReferenceNode;
import org.springframework.ide.eclipse.aop.ui.navigator.model.AdvisedAopSourceNode;
import org.springframework.ide.eclipse.core.SpringCoreUtils;
import org.springframework.ide.eclipse.ui.SpringUIUtils;

@SuppressWarnings("restriction")
public class BeansAdviceImageDecorator extends LabelProvider implements ILightweightLabelDecorator {

	public static final String DECORATOR_ID = org.springframework.ide.eclipse.aop.ui.Activator.PLUGIN_ID
			+ ".decorator.adviceimagedecorator";

	private IAopModelChangedListener listener;
	
	public BeansAdviceImageDecorator() {
		listener = new IAopModelChangedListener() {
			public void changed() {
				update();
			}
		};
		Activator.getModel().registerAopModelChangedListener(listener);
	}

	public void decorate(Object element, IDecoration decoration) {
		// add the orange triangle to the icon if this method,
		// class or aspect is advised
		if ((element instanceof IMethod || element instanceof SourceType)) {
			IJavaElement je = (IJavaElement) element;
			IJavaProject jp = je.getJavaProject();
			// only query the model if the element is in an Spring project

			if ((jp != null)
					&& SpringCoreUtils.isSpringProject(jp.getProject())) {
				if (Activator.getModel().isAdvised(je)) {
					decoration.addOverlay(BeansAopUIImages.DESC_OVR_ADVICE,
							IDecoration.TOP_LEFT);
				}
				/*
				 * else if (BeansAopPlugin.getModel().isAdvice(je)) {
				 * decoration.addOverlay(BeansAopUIImages.DESC_OVR_SPRING,
				 * IDecoration.TOP_LEFT); }
				 */
			}
		} else if (element instanceof AdviceRootAopReferenceNode) {
			List<IAopReference> references = ((AdviceRootAopReferenceNode) element)
					.getReference();
			for (IAopReference reference : references) {
				if (reference.getDefinition() instanceof AnnotationAspectDefinition) {
					decoration.addOverlay(BeansAopUIImages.DESC_OVR_ANNOTATION,
							IDecoration.TOP_LEFT);
					break;
				}
			}
		} else if (element instanceof AdvisedAopSourceNode) {
			IAopReference reference = ((AdvisedAopSourceNode) element)
					.getReference();
			if (reference.getDefinition() instanceof AnnotationAspectDefinition) {
				decoration.addOverlay(BeansAopUIImages.DESC_OVR_ANNOTATION,
						IDecoration.TOP_LEFT);
			}
		}
	}


	public void dispose() {
		if (listener != null) {
			Activator.getModel().unregisterAopModelChangedListener(
					listener);
			listener = null;
		}
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public static final void update() {
		SpringUIUtils.getStandardDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				workbench.getDecoratorManager().update(DECORATOR_ID);
			}
		});
	}

}
