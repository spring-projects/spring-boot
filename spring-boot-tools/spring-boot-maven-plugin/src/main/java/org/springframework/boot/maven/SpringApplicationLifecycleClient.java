/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.IOException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * A JMX client for the {@code SpringApplicationLifecycle} mbean. Permits to obtain
 * information about the lifecycle of a given Spring application.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
class SpringApplicationLifecycleClient {

	//Note: see org.springframework.boot.autoconfigure.test.SpringApplicationLifecycleAutoConfiguration
	static final String DEFAULT_OBJECT_NAME =
			"org.springframework.boot:type=Lifecycle,name=springApplicationLifecycle";

	private final MBeanServerConnection mBeanServerConnection;

	private final ObjectName objectName;

	public SpringApplicationLifecycleClient(MBeanServerConnection mBeanServerConnection, String jmxName) {
		this.mBeanServerConnection = mBeanServerConnection;
		this.objectName = toObjectName(jmxName);
	}

	/**
	 * Create a connector for an {@link javax.management.MBeanServer} exposed on the
	 * current machine and the current port. Security should be disabled.
	 * @param port the port on which the mbean server is exposed
	 * @return a connection
	 * @throws IOException if the connection to that server failed
	 */
	public static JMXConnector createLocalJmxConnector(int port) throws IOException {
		String url = "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + port + "/jmxrmi";
		JMXServiceURL serviceUrl = new JMXServiceURL(url);
		return JMXConnectorFactory.connect(serviceUrl, null);
	}

	/**
	 * Check if the spring application managed by this instance is ready.
	 * <p>Returns {@code false} if the mbean is not yet deployed so this method
	 * should be repeatedly called until a timeout is reached.
	 * @return {@code true} if the application is ready to service requests
	 * @throws MojoExecutionException if the JMX service could not be contacted
	 */
	public boolean isReady() throws MojoExecutionException {
		try {
			return (Boolean) this.mBeanServerConnection.getAttribute(this.objectName, "Ready");
		}
		catch (InstanceNotFoundException e) {
			return false; // Instance not available yet
		}
		catch (AttributeNotFoundException e) {
			throw new IllegalStateException("Unexpected: attribute 'Ready' not available", e);
		}
		catch (ReflectionException e) {
			throw new MojoExecutionException("Failed to retrieve Ready attribute", e.getCause());
		}
		catch (MBeanException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * Stop the application managed by this instance.
	 * @throws MojoExecutionException if the JMX service could not be contacted
	 * @throws IOException if an I/O error occurs
	 * @throws InstanceNotFoundException if the lifecycle mbean cannot be found
	 */
	public void stop() throws MojoExecutionException, IOException, InstanceNotFoundException {
		try {
			this.mBeanServerConnection.invoke(this.objectName, "shutdown", null, null);
		}
		catch (ReflectionException e) {
			throw new MojoExecutionException("Shutdown failed", e.getCause());
		}
		catch (MBeanException e) {
			throw new MojoExecutionException("Could not invoke shutdown operation", e);
		}
	}

	private ObjectName toObjectName(String name) {
		try {
			return new ObjectName(name);
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalArgumentException("Invalid jmx name '" + name + "'");
		}
	}

}
