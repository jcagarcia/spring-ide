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
package org.springframework.ide.eclipse.beans.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.dialogs.FilteredList.FilterMatcher;
import org.eclipse.ui.internal.misc.StringMatcher;
import org.springframework.ide.eclipse.beans.core.BeansCorePlugin;
import org.springframework.ide.eclipse.beans.core.internal.model.BeansModelUtils;
import org.springframework.ide.eclipse.beans.core.model.IBean;
import org.springframework.ide.eclipse.beans.core.model.IBeansModel;
import org.springframework.ide.eclipse.beans.ui.BeansUIPlugin;

/**
 * Spring Bean selection dialog.
 * 
 * @author Christian Dupuis
 */
@SuppressWarnings("restriction")
public class BeanListSelectionDialog extends ElementListSelectionDialog {

	private class BeanFilterMatcher implements FilterMatcher {
		private StringMatcher fMatcher;

		public boolean match(Object element) {
			if (element instanceof IBean) {
				IBean bean = (IBean) element;
				if (fMatcher.match(bean.getElementName())) {
					return true;
				}
				String[] aliases = bean.getAliases();
				if (aliases != null) {
					for (String alias : aliases) {
						if (fMatcher.match(alias)) {
							return true;
						}
					}
				}
				
			} 
			return false;
		}

		public void setFilter(String pattern, boolean ignoreCase,
				boolean ignoreWildCards) {
			fMatcher = new StringMatcher(pattern + '*', ignoreCase,
					ignoreWildCards);
		}
	}

	private static final String DIALOG_SETTINGS = BeanListSelectionDialog.class
			.getName();

	private static final String HEIGHT = "height";

	private static final String WIDTH = "width";

	private int fHeight = 18;

	private CLabel fLabel;

	private Point fLocation;

	private Object[] fSelection = new Object[0];

	private IDialogSettings fSettings;

	private Point fSize;

	private int fWidth = 60;

	protected ILabelProvider labelProvider;

	public BeanListSelectionDialog(Shell parent, ILabelProvider renderer) {
		super(parent, renderer);
		setBlockOnOpen(true);
		setEmptySelectionMessage(BeansUIPlugin
				.getResourceString("BeanListSelectionDialog.selectionMessage"));
		setIgnoreCase(true);
		setStatusLineAboveButtons(true);
		setTitle(BeansUIPlugin
				.getResourceString("BeanListSelectionDialog.title"));
		setMessage(BeansUIPlugin
				.getResourceString("BeanListSelectionDialog.message"));
		setMultipleSelection(false);
		this.labelProvider = renderer;

		IDialogSettings settings = BeansUIPlugin.getDefault()
				.getDialogSettings();
		fSettings = settings.getSection(DIALOG_SETTINGS);
		if (fSettings == null) {
			fSettings = new DialogSettings(DIALOG_SETTINGS);
			settings.addSection(fSettings);
			fSettings.put(WIDTH, 480);
			fSettings.put(HEIGHT, 320);
		}
	}

	@Override
	public boolean close() {
		writeSettings();
		return super.close();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		readSettings();
		Composite contents = (Composite) super.createDialogArea(parent);

		ViewForm fForm = new ViewForm(contents, SWT.BORDER | SWT.FLAT);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fForm.setLayoutData(gd);
		fLabel = new CLabel(fForm, SWT.FLAT);
		fLabel.setFont(fForm.getFont());
		fForm.setContent(fLabel);

		return contents;
	}

	/**
	 * Creates a filtered list.
	 * 
	 * @param parent
	 *            the parent composite.
	 * @return returns the filtered list widget.
	 */
	@Override
	protected FilteredList createFilteredList(Composite parent) {
		int flags = SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL
				| (false ? SWT.MULTI : SWT.SINGLE);

		FilteredList list = new FilteredList(parent, flags, labelProvider,
				true, false, true);
		list.setFilterMatcher(new BeanFilterMatcher());
		GridData data = new GridData();
		data.widthHint = convertWidthInCharsToPixels(fWidth);
		data.heightHint = convertHeightInCharsToPixels(fHeight);
		data.grabExcessVerticalSpace = true;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		list.setLayoutData(data);
		list.setFont(parent.getFont());
		list.setFilter((getFilter() == null ? "" : getFilter()));		

		list.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				handleDefaultSelected();
			}

			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
			}
		});

		fFilteredList = list;

		return list;
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point result = super.getInitialLocation(initialSize);
		if (fLocation != null) {
			result.x = fLocation.x;
			result.y = fLocation.y;
			Rectangle display = getShell().getDisplay().getClientArea();
			int xe = result.x + initialSize.x;
			if (xe > display.width) {
				result.x -= xe - display.width;
			}
			int ye = result.y + initialSize.y;
			if (ye > display.height) {
				result.y -= ye - display.height;
			}
		}
		return result;
	}

	@Override
	protected Point getInitialSize() {
		Point result = super.getInitialSize();
		if (fSize != null) {
			result.x = Math.max(result.x, fSize.x);
			result.y = Math.max(result.y, fSize.y);
			Rectangle display = getShell().getDisplay().getClientArea();
			result.x = Math.min(result.x, display.width);
			result.y = Math.min(result.y, display.height);
		}
		return result;
	}

	/**
	 * Handles a selection changed event. By default, the current selection is
	 * validated.
	 */
	@Override
	protected void handleSelectionChanged() {
		validateCurrentSelection();
		if (getSelectedElements().length == 1) {
			IBean bean = (IBean) getSelectedElements()[0];
			fLabel.setImage(labelProvider.getImage(bean.getElementParent()));
			fLabel.setText(bean.getElementResource().getFullPath().toString());
		} else {
			fLabel.setImage(null);
			fLabel.setText(null);
		}
	}

	private void handleWidgetSelected() {
		Object[] newSelection = fFilteredList.getSelection();

		if (newSelection.length != fSelection.length) {
			fSelection = newSelection;
			handleSelectionChanged();
		} else {
			for (int i = 0; i != newSelection.length; i++) {
				if (!newSelection[i].equals(fSelection[i])) {
					fSelection = newSelection;
					handleSelectionChanged();
					break;
				}
			}
		}
	}

	@Override
	public int open() {
		final Set<IBean> beanList = new LinkedHashSet<IBean>();

		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(final IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				IBeansModel beansModel = BeansCorePlugin.getModel();
				try {
					beanList.addAll(BeansModelUtils.getBeans(beansModel, monitor));
				} catch (OperationCanceledException e) {
					throw new InterruptedException();
				}
			}
		};

		try {
			IRunnableContext context = new ProgressMonitorDialog(getShell());
			context.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			return CANCEL;
		} catch (InterruptedException e) {
			// cancelled by user
			return CANCEL;
		}

		setElements(beanList.toArray());

		return super.open();
	}

	/**
	 * Initializes itself from the dialog settings with the same state as at the
	 * previous invocation.
	 */
	private void readSettings() {
		try {
			int x = fSettings.getInt("x");
			int y = fSettings.getInt("y");
			fLocation = new Point(x, y);
		} catch (NumberFormatException e) {
			fLocation = null;
		}
		try {
			int width = fSettings.getInt("width");
			int height = fSettings.getInt("height");
			fSize = new Point(width, height);

		} catch (NumberFormatException e) {
			fSize = null;
		}
	}

	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeSettings() {
		Point location = getShell().getLocation();
		fSettings.put("x", location.x);
		fSettings.put("y", location.y);

		Point size = getShell().getSize();
		fSettings.put("width", size.x);
		fSettings.put("height", size.y);
	}
}
