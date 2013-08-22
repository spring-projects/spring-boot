/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.properties;

import java.net.InetAddress;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.embedded.properties.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the management server (e.g. port and path settings).
 * 
 * @author Dave Syer
 * @see ServerProperties
 */
@ConfigurationProperties(name = "management", ignoreUnknownFields = false)
public class ManagementServerProperties {

	private Integer port;

	private InetAddress address;

	@NotNull
	private String contextPath = "";

	private User user = new User();

	private boolean allowShutdown = false;

	public User getUser() {
		return this.user;
	}

	public boolean isAllowShutdown() {
		return this.allowShutdown;
	}

	public void setAllowShutdown(boolean allowShutdown) {
		this.allowShutdown = allowShutdown;
	}

	/**
	 * Returns the management port or {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used.
	 * @see #setPort(Integer)
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the management server, use {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used. To disable use 0.
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

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public static class User {

		private String name = "user";

		private String password = UUID.randomUUID().toString();

		private String role = "ADMIN";

		private boolean defaultPassword;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.defaultPassword = false;
			this.password = password;
		}

		public String getRole() {
			return this.role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public boolean isDefaultPassword() {
			return this.defaultPassword;
		}

	}

}
