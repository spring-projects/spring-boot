/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devtools.tunnel.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.lang.UsesUnsafeJava;

/**
 * {@link PortProvider} that provides the port being used by the Java remote debugging.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class RemoteDebugPortProvider implements PortProvider {

	private static final String JDWP_ADDRESS_PROPERTY = "sun.jdwp.listenerAddress";

	private static final Log logger = LogFactory.getLog(RemoteDebugPortProvider.class);

	@Override
	public int getPort() {
		if (!isRemoteDebugRunning()) {
			throw new RemoteDebugNotRunningException();
		}
		return getRemoteDebugPort();
	}

	public static boolean isRemoteDebugRunning() {
		return getRemoteDebugPort() != -1;
	}

	@UsesUnsafeJava
	@SuppressWarnings("restriction")
	private static int getRemoteDebugPort() {
		String property = sun.misc.VMSupport.getAgentProperties()
				.getProperty(JDWP_ADDRESS_PROPERTY);
		try {
			if (property != null && property.contains(":")) {
				return Integer.valueOf(property.split(":")[1]);
			}
		}
		catch (Exception ex) {
			logger.trace(
					"Unable to get JDWP port from property value '" + property + "'");
		}
		return -1;
	}

}
