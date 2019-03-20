/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.boot.autoconfigure.security.SecurityPrerequisite;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Properties for the management server (e.g. port and path settings).
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @see ServerProperties
 */
@ConfigurationProperties(prefix = "management", ignoreUnknownFields = true)
public class ManagementServerProperties implements SecurityPrerequisite {

	/**
	 * Order applied to the WebSecurityConfigurerAdapter that is used to configure basic
	 * authentication for management endpoints. If you want to add your own authentication
	 * for all or some of those endpoints the best thing to do is to add your own
	 * WebSecurityConfigurerAdapter with lower order, for instance by using
	 * {@code ACCESS_OVERRIDE_ORDER}.
	 */
	public static final int BASIC_AUTH_ORDER = SecurityProperties.BASIC_AUTH_ORDER - 5;

	/**
	 * Order before the basic authentication access control provided automatically for the
	 * management endpoints. This is a useful place to put user-defined access rules if
	 * you want to override the default access rules for the management endpoints. If you
	 * want to keep the default rules for management endpoints but want to override the
	 * security for the rest of the application, use
	 * {@code SecurityProperties.ACCESS_OVERRIDE_ORDER} instead.
	 */
	public static final int ACCESS_OVERRIDE_ORDER = ManagementServerProperties.BASIC_AUTH_ORDER
			- 1;

	/**
	 * Management endpoint HTTP port. Use the same port as the application by default.
	 */
	private Integer port;

	@NestedConfigurationProperty
	private Ssl ssl;

	/**
	 * Network address that the management endpoints should bind to.
	 */
	private InetAddress address;

	/**
	 * Management endpoint context-path.
	 */
	private String contextPath = "";

	/**
	 * Add the "X-Application-Context" HTTP header in each response.
	 */
	private boolean addApplicationContextHeader = true;

	private final Security security = new Security();

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
	 * {@link ServerProperties#getPort() server port} should be used. To disable use 0.
	 * @param port the port
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * Return the context path with no trailing slash (i.e. the '/' root context is
	 * represented as the empty string).
	 * @return the context path (no trailing slash)
	 */
	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		Assert.notNull(contextPath, "ContextPath must not be null");
		this.contextPath = cleanContextPath(contextPath);
	}

	private String cleanContextPath(String contextPath) {
		if (StringUtils.hasText(contextPath) && contextPath.endsWith("/")) {
			return contextPath.substring(0, contextPath.length() - 1);
		}
		return contextPath;
	}

	public Security getSecurity() {
		return this.security;
	}

	public boolean getAddApplicationContextHeader() {
		return this.addApplicationContextHeader;
	}

	public void setAddApplicationContextHeader(boolean addApplicationContextHeader) {
		this.addApplicationContextHeader = addApplicationContextHeader;
	}

	/**
	 * Security configuration.
	 */
	public static class Security {

		/**
		 * Enable security.
		 */
		private boolean enabled = true;

		/**
		 * Comma-separated list of roles that can access the management endpoint.
		 */
		private List<String> roles = new ArrayList<String>(
				Collections.singletonList("ACTUATOR"));

		/**
		 * Session creating policy for security use (always, never, if_required,
		 * stateless).
		 */
		private SessionCreationPolicy sessions = SessionCreationPolicy.STATELESS;

		public SessionCreationPolicy getSessions() {
			return this.sessions;
		}

		public void setSessions(SessionCreationPolicy sessions) {
			this.sessions = sessions;
		}

		public void setRoles(List<String> roles) {
			this.roles = roles;
		}

		public List<String> getRoles() {
			return this.roles;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public enum SessionCreationPolicy {

		/**
		 * Always create an {@link HttpSession}.
		 */
		ALWAYS,

		/**
		 * Never create an {@link HttpSession}, but use any {@link HttpSession} that
		 * already exists.
		 */
		NEVER,

		/**
		 * Only create an {@link HttpSession} if required.
		 */
		IF_REQUIRED,

		/**
		 * Never create an {@link HttpSession}.
		 */
		STATELESS

	}

}
