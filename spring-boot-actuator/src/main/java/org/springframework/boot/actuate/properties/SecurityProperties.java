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

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.util.StringUtils;

/**
 * Properties for the security aspects of an application.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "security", ignoreUnknownFields = false)
public class SecurityProperties {

	private boolean requireSsl;

	private Basic basic = new Basic();

	private SessionCreationPolicy sessions = SessionCreationPolicy.STATELESS;

	private String[] ignored = new String[] { "/css/**", "/js/**", "/images/**",
			"/**/favicon.ico" };

	private Management management = new Management();

	private User user = new User();

	public User getUser() {
		return this.user;
	}

	public Management getManagement() {
		return this.management;
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

	public void setIgnored(String... ignored) {
		this.ignored = ignored;
	}

	public String[] getIgnored() {
		return this.ignored;
	}

	public static class Basic {

		private boolean enabled = true;

		private String realm = "Spring";

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

	public static class Management {

		private String role = "ADMIN";

		private SessionCreationPolicy sessions = SessionCreationPolicy.STATELESS;

		public SessionCreationPolicy getSessions() {
			return this.sessions;
		}

		public void setSessions(SessionCreationPolicy sessions) {
			this.sessions = sessions;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public String getRole() {
			return this.role;
		}

	}

	public static class User {

		private String name = "user";

		private String password = UUID.randomUUID().toString();

		private String role = "USER";

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
