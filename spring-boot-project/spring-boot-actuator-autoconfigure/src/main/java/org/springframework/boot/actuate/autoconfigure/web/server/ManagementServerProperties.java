/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.net.InetAddress;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.server.Ssl;
import org.springframework.util.StringUtils;

/**
 * Properties for the management server (e.g. port and path settings).
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @since 2.0.0
 * @see ServerProperties
 */
@ConfigurationProperties(prefix = "management.server", ignoreUnknownFields = true)
public class ManagementServerProperties {

	/**
	 * Management endpoint HTTP port (uses the same port as the application by default).
	 * Configure a different port to use management-specific SSL.
	 */
	private Integer port;

	/**
	 * Network address to which the management endpoints should bind. Requires a custom
	 * management.server.port.
	 */
	private InetAddress address;

	/**
	 * Management endpoint base path (for instance, '/management'). Requires a custom
	 * management.server.port.
	 */
	private String basePath = "";

	@NestedConfigurationProperty
	private Ssl ssl;

	/**
	 * Returns the management port or {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used.
	 * @return the port
	 * @see #setPort(Integer)
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the management server, use {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used. Set to 0 to use a
	 * random port or set to -1 to disable.
	 * @param port the port
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	public InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = cleanBasePath(basePath);
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	private String cleanBasePath(String basePath) {
		String candidate = null;
		if (StringUtils.hasLength(basePath)) {
			candidate = basePath.strip();
		}
		if (StringUtils.hasText(candidate)) {
			if (!candidate.startsWith("/")) {
				candidate = "/" + candidate;
			}
			if (candidate.endsWith("/")) {
				candidate = candidate.substring(0, candidate.length() - 1);
			}
		}
		return candidate;
	}

}
