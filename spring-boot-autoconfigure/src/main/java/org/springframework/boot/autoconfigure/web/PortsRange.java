/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.StringTokenizer;

import javax.validation.constraints.NotNull;

import org.springframework.util.SocketUtils;

/**
 * Automatic port assignment with specific port range support.
 * @author ishara
 */
public class PortsRange {

	/**
	 * Server HTTP port range. Ex 9000-9080
	 * If single port specified, this also work
	 */
	private final String portRange;

	public PortsRange(@NotNull String portRange) {
		this.portRange = portRange;
	}

	public String getPortRangeString() {
		return this.portRange;
	}

	public int getValidPort(int defaultPort) throws NumberFormatException {
		int availablePort = defaultPort;
		if (this.portRange == null) {
			return availablePort;
		}
		StringTokenizer st = new StringTokenizer(this.portRange, ",");
		while (st.hasMoreTokens()) {
			String portToken = st.nextToken().trim();
			int index = portToken.indexOf('-');
			if (index == -1) {
				availablePort = Integer.parseInt(portToken.trim());
			}
			else {
				int startPort = Integer.parseInt(portToken.substring(0, index).trim());
				int endPort = Integer.parseInt(portToken.substring(index + 1).trim());
				if (endPort < startPort) {
					throw new IllegalArgumentException("Start port [" + startPort
							+ "] must be greater than end port [" + endPort + "]");
				}
				availablePort = SocketUtils.findAvailableTcpPort(startPort, endPort);
			}
		}
		return availablePort;
	}
}
