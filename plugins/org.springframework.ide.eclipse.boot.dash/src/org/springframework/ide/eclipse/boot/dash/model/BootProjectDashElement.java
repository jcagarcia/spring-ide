/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.springframework.ide.eclipse.boot.core.BootPropertyTester;
import org.springframework.ide.eclipse.boot.dash.livexp.LiveSets;
import org.springframework.ide.eclipse.boot.dash.metadata.IPropertyStore;
import org.springframework.ide.eclipse.boot.dash.metadata.IScopedPropertyStore;
import org.springframework.ide.eclipse.boot.dash.metadata.PropertyStoreFactory;
import org.springframework.ide.eclipse.boot.dash.model.BootDashModel.ElementStateListener;
import org.springframework.ide.eclipse.boot.dash.util.CollectionUtils;
import org.springframework.ide.eclipse.boot.dash.util.DebugUtil;
import org.springframework.ide.eclipse.boot.launch.util.BootLaunchUtils;
import org.springframework.ide.eclipse.boot.util.Log;
import org.springsource.ide.eclipse.commons.frameworks.core.workspace.ClasspathListenerManager;
import org.springsource.ide.eclipse.commons.frameworks.core.workspace.ClasspathListenerManager.ClasspathListener;
import org.springsource.ide.eclipse.commons.livexp.core.DisposeListener;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ObservableSet;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;
import org.springsource.ide.eclipse.commons.livexp.ui.Disposable;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Concrete BootDashElement that wraps an IProject
 *
 * @author Kris De Volder
 */
public class BootProjectDashElement extends AbstractLaunchConfigurationsDashElement<IProject> {

	private static final boolean DEBUG = DebugUtil.isDevelopment();

	private static void debug(String string) {
		if (DEBUG) {
			System.err.println(string);
		}
	}

	private IScopedPropertyStore<IProject> projectProperties;

	private LaunchConfDashElementFactory childFactory;
	private ObservableSet<BootDashElement> children;
	private ObservableSet<Integer> ports;
	private LiveExpression<Boolean> hasDevtools = null;

	public BootProjectDashElement(IProject project, LocalBootDashModel context, IScopedPropertyStore<IProject> projectProperties,
			BootProjectDashElementFactory factory, LaunchConfDashElementFactory childFactory) {
		super(context, project);
		this.projectProperties = projectProperties;
		this.childFactory = childFactory;
		if (DEBUG) {
			onDispose((e) -> {
				debug("disposed: "+this);
			});
		}
	}

	@Override
	public boolean hasDevtools() {
		if (hasDevtools==null) {
			hasDevtools = new LiveExpression<Boolean>(false) {
				@Override
				protected Boolean compute() {
					boolean val = BootPropertyTester.hasDevtools(getProject());
					return val;
				}
			};
			hasDevtools.refresh();
			ClasspathListenerManager classpathListener = new ClasspathListenerManager(new ClasspathListener() {
				public void classpathChanged(IJavaProject jp) {
					if (jp.getProject().equals(getProject())) {
						hasDevtools.refresh();
					}
				}
			});
			this.dependsOn(hasDevtools);
			this.addDisposableChild(classpathListener);
		}
		return hasDevtools.getValue();
	}

	@Override
	public IProject getProject() {
		return delegate;
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	protected IPropertyStore createPropertyStore() {
		return PropertyStoreFactory.createForScope(delegate, projectProperties);
	}

	@Override
	public ImmutableSet<ILaunchConfiguration> getLaunchConfigs() {
		return getLaunchConfigsExp().getValues();
	}

	@Override
	public int getLivePort() {
		return CollectionUtils.getAnyOr(getLivePorts(), -1);
	}

	@Override
	public ImmutableSet<Integer> getLivePorts() {
		return getLivePortsExp().getValues();
	}

	private ObservableSet<Integer> getLivePortsExp() {
		if (ports==null) {
			ports = createSortedLiveSummary((BootDashElement element) -> {
				int port = element.getLivePort();
				if (port>0) {
					return port;
				}
				return null;
			});
			this.dependsOn(ports);
		}
		return ports;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void dependsOn(LiveExpression<?> liveProperty) {
		liveProperty.addListener(new ValueListener() {
			public void gotValue(LiveExpression exp, Object value) {
				getBootDashModel().notifyElementChanged(BootProjectDashElement.this);
			}
		});
	}

	/**
	 * Creates a ObservableSet that is a 'summary' of some property of the children of this node. The summary
	 * is a set of all the values of the given property on all the children. The set is sorted using the element's
	 * natural ordering.
	 */
	private <T extends Comparable<T>> ObservableSet<T> createSortedLiveSummary(final Function<BootDashElement, T> getter) {

		final ObservableSet<T> summary = new ObservableSet<T>() {

			@Override
			protected ImmutableSet<T> compute() {
				debug("port-summary["+getName()+"]: compute()...");
				ImmutableSet.Builder<T> builder = ImmutableSortedSet.naturalOrder();
				for (BootDashElement child : getCurrentChildren()) {
					add(builder, child);
				}
				ImmutableSet<T> result = builder.build();
				debug("port-summary["+getName()+"]: compute() => "+result);
				return result;
			}

			protected void add(ImmutableSet.Builder<T> builder, BootDashElement child) {
				debug("port-summary["+getName()+"]: add port for "+child);
				T v = getter.apply(child);
				debug("port-summary["+getName()+"]: add port for "+child+" = "+v);
				if (v!=null) {
					builder.add(v);
				}
			}
		};
		final ElementStateListener elementListener = new ElementStateListener() {
			public void stateChanged(BootDashElement e) {
				summary.refresh();
			}
		};
		getBootDashModel().addElementStateListener(elementListener);
		summary.onDispose(new DisposeListener() {
			public void disposed(Disposable disposed) {
				getBootDashModel().removeElementStateListener(elementListener);
			}
		});
		addDisposableChild(summary);
		return summary;
	}

	private ObservableSet<ILaunchConfiguration> getLaunchConfigsExp() {
		return getBootDashModel().launchConfTracker.getConfigs(delegate);
	}

	/**
	 * All children including 'invisible ones' that may be hidden from the children returned
	 * by getChildren.
	 */
	@Override
	public ObservableSet<BootDashElement> getChildren() {
		if (children==null) {
			children = LiveSets.mapSync(getBootDashModel().launchConfTracker.getConfigs(delegate),
					new Function<ILaunchConfiguration, BootDashElement>() {
						public BootDashElement apply(ILaunchConfiguration input) {
							return childFactory.createOrGet(input);
						}
					}
			);
			children.addListener(new ValueListener<ImmutableSet<BootDashElement>>() {
				public void gotValue(LiveExpression<ImmutableSet<BootDashElement>> exp, ImmutableSet<BootDashElement> value) {
					getBootDashModel().notifyElementChanged(BootProjectDashElement.this);
					refreshRunState();
				}
			});
			addDisposableChild(children);
		}
		return this.children;
	}

	@Override
	public void refreshLivePorts() {
		for (BootDashElement child : getChildren().getValues()) {
			try {
				((AbstractLaunchConfigurationsDashElement<?>)child).refreshLivePorts();
			} catch (ClassCastException e) {
				//Should be impossible (unless something changes in how elements in dash are nested)
				Log.log(e);
			}
		}
	}

	@Override
	public ImmutableSet<ILaunch> getLaunches() {
		return ImmutableSet.copyOf(BootLaunchUtils.getLaunches(getLaunchConfigs()));
	}

	@Override
	public Object getParent() {
		return getBootDashModel();
	}
}
