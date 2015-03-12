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
import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Stop a spring application that has been started by the "start" goal. Typically invoked
 * once a test suite has completed.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Mojo(name = "stop", requiresProject = true, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopMojo extends AbstractMojo {

	/**
	 * Flag to indicate if the run processes should be forked. Must be aligned to the value
	 * used to {@link StartMojo start} the process
	 * @since 1.2
	 */
	@Parameter(property = "fork")
	private Boolean fork;

	/**
	 * The JMX name of the automatically deployed MBean managing the lifecycle
	 * of the application.
	 */
	@Parameter
	private String jmxName = SpringApplicationLifecycleClient.DEFAULT_OBJECT_NAME;

	/**
	 * The port to use to lookup the platform MBeanServer if the application
	 * has been forked.
	 */
	@Parameter
	private int jmxPort = 9001;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Stopping application...");
		try {
			if (Boolean.TRUE.equals(this.fork)) {
				stopForkedProcess();
			}
			else {
				stop();
			}
		}
		catch (IOException e) {
			// The response won't be received as the server has died - ignoring
			getLog().debug("Service is not reachable anymore (" + e.getMessage() + ")");
		}
	}

	private void stop() throws IOException, MojoFailureException, MojoExecutionException {
		doStop(ManagementFactory.getPlatformMBeanServer());
	}

	private void stopForkedProcess() throws IOException, MojoFailureException, MojoExecutionException {
		JMXConnector jmxConnector = SpringApplicationLifecycleClient.createLocalJmxConnector(this.jmxPort);
		try {
			MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
			doStop(mBeanServerConnection);
		}
		finally {
			jmxConnector.close();
		}
	}

	private void doStop(MBeanServerConnection connection)
			throws IOException, MojoExecutionException {
		SpringApplicationLifecycleClient helper = new SpringApplicationLifecycleClient(connection, this.jmxName);
		try {
			helper.stop();
		}
		catch (InstanceNotFoundException e) {
			throw new MojoExecutionException("Spring application lifecycle JMX bean not found (fork is " +
					"" + this.fork + "). Could not stop application gracefully", e);
		}
	}

}
