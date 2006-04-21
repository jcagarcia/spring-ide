package org.springframework.ide.eclipse.beans.ui.wizards.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.xml.core.internal.document.NodeImpl;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.springframework.ide.eclipse.beans.core.model.IBeansConfig;
import org.springframework.ide.eclipse.beans.core.model.IBeansProject;
import org.springframework.ide.eclipse.beans.ui.wizards.model.IdRefModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.ListModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.MapEntryModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.MapModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.PropModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.PropertyModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.PropsModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.RefModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.SetModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.model.ValueModelItem;
import org.springframework.ide.eclipse.beans.ui.wizards.wizards.pages.SpringBeanBasicWizardPage;
import org.springframework.ide.eclipse.beans.ui.wizards.wizards.pages.SpringBeanLifecycleWizardPage;
import org.springframework.ide.eclipse.beans.ui.wizards.wizards.pages.SpringBeanPropertiesWizardPage;
import org.springframework.ide.eclipse.core.ui.dialogs.message.ErrorDialog;
import org.springframework.ide.eclipse.core.ui.treemodel.IModelItem;
import org.springframework.ide.eclipse.ui.SpringUIUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class AbstractSpringBeanWizard extends Wizard {
	private static final String WIZARD_ID = "Spring Bean wizard";

	private SpringBeanBasicWizardPage page1;

	private SpringBeanLifecycleWizardPage page3;

	private SpringBeanPropertiesWizardPage page2;

	protected class WizardInitingDatas {
		private IBeansProject beansProject;

		private IBeansConfig beansConfig;

		private IType type;

		public IBeansConfig getBeansConfig() {
			return beansConfig;
		}

		public void setBeansConfig(IBeansConfig originatingConfigFile) {
			this.beansConfig = originatingConfigFile;
		}

		public IBeansProject getBeansProject() {
			return beansProject;
		}

		public void setBeansProject(IBeansProject originatingProject) {
			this.beansProject = originatingProject;
		}

		public IType getType() {
			return type;
		}

		public void setType(IType originatingType) {
			this.type = originatingType;
		}

	}

	/**
	 * Constructor for AbstractSpringBeanWizard.
	 */
	public AbstractSpringBeanWizard() {
		super();
		setWindowTitle("Declare as Bean");
		setNeedsProgressMonitor(true);
	}

	public abstract WizardInitingDatas initWizardInitingDatas();

	/**
	 * Adding the page(s) to the wizard.
	 */
	public void addPages() {

		WizardInitingDatas initingDatas = initWizardInitingDatas();
		page1 = new SpringBeanBasicWizardPage(initingDatas
				.getBeansProject(), initingDatas
				.getBeansConfig(), initingDatas.getType());
		page1.setTitle("Declare as Bean - basic");
		addPage(page1);

		page2 = new SpringBeanPropertiesWizardPage();
		page2.setTitle("Declare as Bean - advanced : properties");
		addPage(page2);

		page3 = new SpringBeanLifecycleWizardPage();
		page3.setTitle("Declare as Bean - advanced : lifecycle");
		addPage(page3);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We
	 * will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish(monitor);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			ErrorDialog internalError = new ErrorDialog(
					AbstractSpringBeanWizard.WIZARD_ID,
					"An exception in the bean declaration process occured : "
							+ e.getTargetException().getMessage(), e);
			internalError.open();
			return false;
		}
		return true;
	}

	/**
	 * The worker method. It will find the container, create the file if missing
	 * or just replace its contents, and open the editor on the newly created
	 * file. FIXME use constants for the tag names.
	 */
	private void doFinish(IProgressMonitor monitor) {

		try {
			IDOMModel xmlModel = (IDOMModel) StructuredModelManager
					.getModelManager().getModelForEdit(
							page1.getSelectedConfigFile().getConfigFile());
			IDOMDocument xmlDoc = (IDOMDocument) xmlModel.getDocument();
			xmlModel.beginRecording(this);
			xmlModel.aboutToChangeModel();
			NodeList tmp = xmlDoc.getElementsByTagName("beans");
			if (tmp.item(0) == null) {
				xmlDoc.appendChild(xmlDoc.createElement("beans"));
				tmp = xmlDoc.getElementsByTagName("beans");
			}
			Element beanElement = xmlDoc.createElement("bean");
			if (page1.getBeanId() != null) {
				beanElement.setAttribute("id", page1.getBeanId());
			}
			if (page1.getBeanName() != null) {
				beanElement.setAttribute("name", page1.getBeanName());
			}
			if (page1.getBeanClass() != null) {
				beanElement.setAttribute("class", page1.getBeanClass());
			}
			if (page3.getBeanAutoWireState() != SpringBeanLifecycleWizardPage.SINGLETON_DEFAULT) {
				beanElement.setAttribute("singleton",
						SpringBeanLifecycleWizardPage.SINGLETON_LABELS[page3
								.getBeanSingletonState()]);
			}
			if (page3.getBeanAutoWireState() != SpringBeanBasicWizardPage.ABSTRACT_DEFAULT) {
				beanElement.setAttribute("abstract",
						SpringBeanBasicWizardPage.ABSTRACT_LABELS[page1
								.getBeanAbstractState()]);
			}
			if (page3.getBeanAutoWireState() != SpringBeanLifecycleWizardPage.AUTOWIRE_DEFAULT) {
				beanElement.setAttribute("autowire",
						SpringBeanLifecycleWizardPage.AUTOWIRE_LABELS[page3
								.getBeanAutoWireState()]);
			}
			if (page3.getBeanLazyInitState() != SpringBeanLifecycleWizardPage.LAZYINIT_DEFAULT) {
				beanElement.setAttribute("lazy-init",
						SpringBeanLifecycleWizardPage.LAZYINIT_LABELS[page3
								.getBeanLazyInitState()]);
			}
			if (page3.getBeanDependencyCheckState() != SpringBeanLifecycleWizardPage.DEP_CHCK_DEFAULT) {
				beanElement.setAttribute("dependency-check",
						SpringBeanLifecycleWizardPage.DEP_CHECK_LABELS[page3
								.getBeanDependencyCheckState()]);
			}
			if (page3.getBeanInitMethod() != null) {
				beanElement.setAttribute("init-method", page3
						.getBeanInitMethod());
			}
			if (page3.getBeanDestroyMethod() != null) {
				beanElement.setAttribute("destroy-method", page3
						.getBeanDestroyMethod());
			}
			if (page2.getInjectionState()) {
				List propertiesList = page2.getInjectableProperties();
				for (Iterator it = propertiesList.iterator(); it.hasNext();) {
					PropertyModelItem propertyModelItem = (PropertyModelItem) it
							.next();
					Element propertyElement = xmlDoc.createElement("property");
					propertyElement.setAttribute("name", propertyModelItem
							.getName());
					if (propertyModelItem.hasChildren()) {
						handleChildren(xmlDoc, propertyModelItem.getChildren(),
								propertyElement);
					}
					beanElement.appendChild(propertyElement);
				}
			}

			tmp.item(0).appendChild(beanElement);

			// do formatting
			formatElement(beanElement);
			// double pass
			formatElement(beanElement);
			final int offset = (beanElement instanceof NodeImpl ? ((NodeImpl) beanElement)
					.getStartOffset()
					: -1);

			xmlModel.changedModel();
			xmlModel.endRecording(this);
			xmlModel.save();
			xmlModel.releaseFromEdit();

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					IEditorPart editorPart = SpringUIUtils.openInEditor(page1
							.getSelectedConfigFile().getConfigFile(), -1);
					ITextEditor editor = SpringUIUtils
							.getTextEditor(editorPart);
					if (editor != null) {
						IDocument doc = getDocument(editor);
						try {
							int line = doc.getLineOfOffset(offset);
							SpringUIUtils.openInEditor(page1
									.getSelectedConfigFile().getConfigFile(),
									line);
						} catch (BadLocationException e) {
						}
					}

				}
			});
		} catch (IOException e) {
			ErrorDialog errorDialog = new ErrorDialog(
					"Creation error",
					"An IO Exception occured while persisting bean to the configuration file.",
					e);
			errorDialog.open();
			e.printStackTrace();
		} catch (CoreException e) {
			ErrorDialog errorDialog = new ErrorDialog(
					"Creation error",
					"An Eclipse Core Exception occured while persisting bean to the configuration file.",
					e);
			errorDialog.open();
			e.printStackTrace();
		}
	}

	private void handleChildren(IDOMDocument xmlDoc, Collection children,
			Element element) {
		for (Iterator it = children.iterator(); it.hasNext();) {
			IModelItem next = (IModelItem) it.next();
			if (next instanceof ValueModelItem) {
				if (next.getParent() instanceof PropertyModelItem) {
					element.setAttribute("value", ((ValueModelItem) next)
							.getValue());
				} else {
					Element valueElement = xmlDoc.createElement("value");
					valueElement
							.appendChild(xmlDoc
									.createTextNode(((ValueModelItem) next)
											.getValue()));
					element.appendChild(valueElement);
				}
			}
			if (next instanceof RefModelItem) {
				element.setAttribute("ref", ((RefModelItem) next).getBeanId());
			}
			if (next instanceof MapModelItem) {
				Element mapElement = xmlDoc.createElement("map");
				IModelItem modelItem = (IModelItem) next;
				handleChildren(xmlDoc, modelItem.getChildren(), mapElement);
				element.appendChild(mapElement);
			}
			if (next instanceof MapEntryModelItem) {
				MapEntryModelItem mapEntryModelItem = (MapEntryModelItem) next;
				Element entryElement = xmlDoc.createElement("entry");
				Element keyElement = xmlDoc.createElement("key");
				Element valueElement = xmlDoc.createElement("value");
				valueElement.appendChild(xmlDoc
						.createTextNode(mapEntryModelItem.getKeyValue()));
				keyElement.appendChild(valueElement);
				entryElement.appendChild(keyElement);
				handleChildren(xmlDoc, mapEntryModelItem.getChildren(),
						entryElement);
				element.appendChild(entryElement);
			}
			if (next instanceof ListModelItem) {
				Element listElement = xmlDoc.createElement("list");
				IModelItem modelItem = (IModelItem) next;
				handleChildren(xmlDoc, modelItem.getChildren(), listElement);
				element.appendChild(listElement);
			}
			if (next instanceof SetModelItem) {
				Element setElement = xmlDoc.createElement("set");
				IModelItem modelItem = (IModelItem) next;
				handleChildren(xmlDoc, modelItem.getChildren(), setElement);
				element.appendChild(setElement);
			}
			if (next instanceof PropsModelItem) {
				Element propsElement = xmlDoc.createElement("props");
				IModelItem modelItem = (IModelItem) next;
				handleChildren(xmlDoc, modelItem.getChildren(), propsElement);
				element.appendChild(propsElement);
			}
			if (next instanceof PropModelItem) {
				Element propElement = xmlDoc.createElement("prop");
				PropModelItem propModelItem = (PropModelItem) next;
				propElement.setAttribute("key", propModelItem.getKey());
				propElement.appendChild(xmlDoc.createTextNode(propModelItem
						.getValue()));
				element.appendChild(propElement);
			}
			if (next instanceof IdRefModelItem) {
				Element idRefElement = xmlDoc.createElement("idref");
				IdRefModelItem idRefModelItem = (IdRefModelItem) next;
				idRefElement.setAttribute("bean", idRefModelItem.getBeanId());
				element.appendChild(idRefElement);
			}
		}
	}

	private void formatElement(Element element) {
		IStructuredFormatProcessor formatProcessor = new FormatProcessorXML();
		formatProcessor.formatNode(element);
	}

	private IDocument getDocument(ITextEditor editor) {
		IDocument document = null;
		if (editor != null)
			document = editor.getDocumentProvider().getDocument(
					editor.getEditorInput());
		return document;
	}
}
