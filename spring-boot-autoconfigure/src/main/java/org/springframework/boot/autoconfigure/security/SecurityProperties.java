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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.util.StringUtils;

/**
 * Properties for the security aspects of an application.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "security", ignoreUnknownFields = false)
public class SecurityProperties implements SecurityPrequisite {

	private boolean requireSsl;

	// Flip this when session creation is disabled by default
	private boolean enableCsrf = false;

	private Basic basic = new Basic();

	private final Headers headers = new Headers();

	private SessionCreationPolicy sessions = SessionCreationPolicy.STATELESS;

	private List<String> ignored = new ArrayList<String>();

	private final User user = new User();

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

	public static class Headers {

		public static enum HSTS {
			none, domain, all
		}

		private boolean xss;

		private boolean cache;

		private boolean frame;

		private boolean contentType;

		private HSTS hsts = HSTS.all;

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

	public static class User {

		private String name = "user";

		private String password = UUID.randomUUID().toString();

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

		public boolean isDefaultPassword() {
			return this.defaultPassword;
		}

	}

}
