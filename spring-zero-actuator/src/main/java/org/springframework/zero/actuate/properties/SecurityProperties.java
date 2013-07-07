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

package org.springframework.zero.actuate.properties;

import org.springframework.security.config.annotation.web.configurers.SessionCreationPolicy;
import org.springframework.zero.context.properties.ConfigurationProperties;

/**
 * Properties for the security aspects of an application.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "security", ignoreUnknownFields = false)
public class SecurityProperties {

	private boolean requireSsl;

	private Basic basic = new Basic();

	private SessionCreationPolicy sessions = SessionCreationPolicy.stateless;

	private String[] ignored = new String[] { "/css/**", "/js/**", "/images/**",
			"/**/favicon.ico" };

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

		private String role = "USER";

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

		public String getRole() {
			return this.role;
		}

		public void setRole(String role) {
			this.role = role;
		}

	}

}
