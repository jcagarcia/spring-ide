/*******************************************************************************
 *  Copyright (c) 2012 VMware, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.beans.ui.livegraph.model;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONException;
import org.springframework.context.support.LiveBeansViewMBean;
import org.springframework.ide.eclipse.beans.ui.livegraph.LiveGraphUiPlugin;
import org.springsource.ide.eclipse.commons.core.StatusHandler;

/**
 * Loads an MBean exposed by the Spring Framework and generates a
 * {@link LiveBeansModel} from the JSON contained within.
 * 
 * @author Leo Dos Santos
 */
public class LiveBeansModelGenerator {

	/**
	 * This method will not attempt to close the given {@link JMXConnector}. If
	 * the connection has failed, clients may capture the thrown
	 * {@link CoreException} and inform the user. This method is not UI safe,
	 * and may block the UI with network operations. Clients will need to call
	 * this method from a non-blocking {@link Job}. This method will never
	 * return a <code>null</code> {@link LiveBeansModel}
	 * 
	 * @param connector
	 * @param appName
	 * @return A valid {@link LiveBeansModel} model, empty if connection has
	 * failed
	 * @throws CoreException
	 */
	public static LiveBeansModel connectToModel(JMXConnector connector, String appName) throws CoreException {
		try {
			if (connector != null && appName != null && appName.length() > 0) {
				ObjectName name = ObjectName.getInstance("", "application", "/".concat(appName));
				MBeanServerConnection connection = connector.getMBeanServerConnection();
				// Test the MBean's existence before proceeding. Will throw
				// InstanceNotFoundException
				connection.getObjectInstance(name);
				LiveBeansViewMBean mbean = MBeanServerInvocationHandler.newProxyInstance(connection, name,
						LiveBeansViewMBean.class, false);
				return generateModel(mbean, appName);
			}
		}
		catch (MalformedObjectNameException e) {
			throw new CoreException(new Status(IStatus.ERROR, LiveGraphUiPlugin.PLUGIN_ID,
					"An error occurred while connecting to server. Please check that the application name is correct.",
					e));
		}
		catch (InstanceNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, LiveGraphUiPlugin.PLUGIN_ID,
					"An error occurred while connecting to server. Please check that the application name is correct.",
					e));
		}
		catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, LiveGraphUiPlugin.PLUGIN_ID,
					"An error occurred while connecting to server.", e));
		}
		return new LiveBeansModel();
	}

	/**
	 * This method will attempt to create a {@link JMXConnector} from the given
	 * parameters and will close it when it is finished. If the connection has
	 * failed, clients may capture the thrown {@link CoreException} and inform
	 * the user. This method is UI safe, and will not block the UI with network
	 * operations. It will never return a <code>null</code>
	 * {@link LiveBeansModel}
	 * 
	 * @param serviceUrl
	 * @param username
	 * @param password
	 * @param appName
	 * @return A valid {@link LiveBeansModel} model, empty if connection has
	 * failed
	 * @throws CoreException
	 */
	public static LiveBeansModel connectToModel(final String serviceUrl, final String username, final String password,
			final String appName) throws CoreException {
		final CountDownLatch latch = new CountDownLatch(1);
		final LiveBeansModel[] result = new LiveBeansModel[1];
		final CoreException[] status = new CoreException[1];

		Job jmxOperation = new Job("Executing Server Command") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				JMXConnector connector = null;
				try {
					connector = setupConnector(serviceUrl, username, password);
					result[0] = connectToModel(connector, appName);
				}
				catch (CoreException e) {
					status[0] = e;
				}
				finally {
					latch.countDown();
					if (connector != null) {
						try {
							connector.close();
						}
						catch (IOException e) {
							StatusHandler.log(new Status(IStatus.ERROR, LiveGraphUiPlugin.PLUGIN_ID,
									"An error occurred while closing connection to server.", e));
						}
					}
				}
				return Status.OK_STATUS;
			}
		};
		jmxOperation.schedule();

		try {
			if (latch.await(30, TimeUnit.SECONDS)) {
				if (status[0] != null) {
					throw status[0];
				}
				return result[0];
			}
		}
		catch (InterruptedException e) {
			// swallowed
		}
		return new LiveBeansModel();
	}

	private static LiveBeansModel generateModel(LiveBeansViewMBean mbean, String appName) throws CoreException {
		LiveBeansModel model = new LiveBeansModel();
		try {
			if (mbean != null) {
				String json = mbean.getSnapshotAsJson();
				Collection<LiveBean> collection = LiveBeansJsonParser.parse(json, appName);
				model.getBeans().addAll(collection);
				// add model to collection
				LiveBeansModelCollection.getInstance().addModel(model);
			}
		}
		catch (JSONException e) {
			throw new CoreException(new Status(IStatus.ERROR, LiveGraphUiPlugin.PLUGIN_ID,
					"An error occurred while generating graph model.", e));
		}
		return model;
	}

	private static JMXConnector setupConnector(String serviceUrl, String username, String password)
			throws CoreException {
		JMXConnector connector = null;
		try {
			if (serviceUrl != null && serviceUrl.length() > 0) {
				Map env = new HashMap();
				if (username != null && password != null) {
					String[] creds = new String[] { username, password };
					env.put(JMXConnector.CREDENTIALS, creds);
				}
				connector = JMXConnectorFactory.connect(new JMXServiceURL(serviceUrl), env);
			}
		}
		catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, LiveGraphUiPlugin.PLUGIN_ID,
					"An error occurred while connecting to server. Please check that the service URL is correct.", e));
		}
		catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, LiveGraphUiPlugin.PLUGIN_ID,
					"An error occurred while connecting to server.", e));
		}
		return connector;
	}

}