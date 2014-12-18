/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.util.StringUtils;

/**
 * Properties for the security aspects of an application.
 *
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "security", ignoreUnknownFields = false)
public class SecurityProperties implements SecurityPrerequisite {

	/**
	 * Order before the basic authentication access control provided by Boot. This is a
	 * useful place to put user-defined access rules if you want to override the default
	 * access rules.
	 */
	public static final int ACCESS_OVERRIDE_ORDER = SecurityProperties.BASIC_AUTH_ORDER - 2;

	/**
	 * Order applied to the WebSecurityConfigurerAdapter that is used to configure basic
	 * authentication for application endpoints. If you want to add your own
	 * authentication for all or some of those endpoints the best thing to do is add your
	 * own WebSecurityConfigurerAdapter with lower order.
	 */
	public static final int BASIC_AUTH_ORDER = Ordered.LOWEST_PRECEDENCE - 5;

	/**
	 * Order applied to the WebSecurityConfigurer that ignores standard static resource
	 * paths.
	 */
	public static final int IGNORED_ORDER = Ordered.HIGHEST_PRECEDENCE;

	/**
	 * The default order of Spring Security's Filter
	 */
	public static final int DEFAULT_FILTER_ORDER = 0;

	/**
	 * Enable secure channel for all requests.
	 */
	private boolean requireSsl;

	/**
	 * Enable Cross Site Request Forgery support.
	 */
	// Flip this when session creation is disabled by default
	private boolean enableCsrf = false;

	private Basic basic = new Basic();

	private final Headers headers = new Headers();

	/**
	 * Session creation policy (always, never, if_required, stateless).
	 */
	private SessionCreationPolicy sessions = SessionCreationPolicy.STATELESS;

	/**
	 * Comma-separated list of paths to exclude from the default secured paths.
	 */
	private List<String> ignored = new ArrayList<String>();

	private final User user = new User();

	/**
	 * Security filter chain order.
	 */
	private int filterOrder = DEFAULT_FILTER_ORDER;

	public Headers getHeaders() {
		return this.headers;
	}

	public User getUser() {
		return this.user;
	}

	public SessionCreationPolicy getSessions() {
		return this.sessions;
	}

	public void setSessions(SessionCreationPolicy sessions) {
		this.sessions = sessions;
	}

	public Basic getBasic() {
		return this.basic;
	}

	public void setBasic(Basic basic) {
		this.basic = basic;
	}

	public boolean isRequireSsl() {
		return this.requireSsl;
	}

	public void setRequireSsl(boolean requireSsl) {
		this.requireSsl = requireSsl;
	}

	public boolean isEnableCsrf() {
		return this.enableCsrf;
	}

	public void setEnableCsrf(boolean enableCsrf) {
		this.enableCsrf = enableCsrf;
	}

	public void setIgnored(List<String> ignored) {
		this.ignored = new ArrayList<String>(ignored);
	}

	public List<String> getIgnored() {
		return this.ignored;
	}

	public int getFilterOrder() {
		return this.filterOrder;
	}

	public void setFilterOrder(int filterOrder) {
		this.filterOrder = filterOrder;
	}

	public static class Headers {

		public static enum HSTS {
			NONE, DOMAIN, ALL
		}

		/**
		 * Enable cross site scripting (XSS) protection.
		 */
		private boolean xss;

		/**
		 * Enable cache control HTTP headers.
		 */
		private boolean cache;

		/**
		 * Enable "X-Frame-Options" header.
		 */
		private boolean frame;

		/**
		 * Enable "X-Content-Type-Options" header.
		 */
		private boolean contentType;

		/**
		 * HTTP Strict Transport Security (HSTS) mode (none, domain, all).
		 */
		private HSTS hsts = HSTS.ALL;

		public boolean isXss() {
			return this.xss;
		}

		public void setXss(boolean xss) {
			this.xss = xss;
		}

		public boolean isCache() {
			return this.cache;
		}

		public void setCache(boolean cache) {
			this.cache = cache;
		}

		public boolean isFrame() {
			return this.frame;
		}

		public void setFrame(boolean frame) {
			this.frame = frame;
		}

		public boolean isContentType() {
			return this.contentType;
		}

		public void setContentType(boolean contentType) {
			this.contentType = contentType;
		}

		public HSTS getHsts() {
			return this.hsts;
		}

		public void setHsts(HSTS hsts) {
			this.hsts = hsts;
		}

	}

	public static class Basic {

		/**
		 * Enable basic authentication.
		 */
		private boolean enabled = true;

		/**
		 * HTTP basic realm name.
		 */
		private String realm = "Spring";

		/**
		 * Comma-separated list of paths to secure.
		 */
		private String[] path = new String[] { "/**" };

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getRealm() {
			return this.realm;
		}

		public void setRealm(String realm) {
			this.realm = realm;
		}

		public String[] getPath() {
			return this.path;
		}

		public void setPath(String... paths) {
			this.path = paths;
		}

	}

	public static class User {

		/**
		 * Default user name.
		 */
		private String name = "user";

		/**
		 * Password for the default user name.
		 */
		private String password = UUID.randomUUID().toString();

		/**
		 * Granted roles for the default user name.
		 */
		private List<String> role = new ArrayList<String>(Arrays.asList("USER"));

		private boolean defaultPassword = true;

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
			if (password.startsWith("${") && password.endsWith("}")
					|| !StringUtils.hasLength(password)) {
				return;
			}
			this.defaultPassword = false;
			this.password = password;
		}

		public List<String> getRole() {
			return this.role;
		}

		public void setRole(List<String> role) {
			this.role = new ArrayList<String>(role);
		}

		public boolean isDefaultPassword() {
			return this.defaultPassword;
		}

	}

}
