/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.springframework.boot.convert.DurationUnit;

/**
 * Session properties.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class Session {

	/**
	 * Session timeout. If a duration suffix is not specified, seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private Duration timeout = Duration.ofMinutes(30);

	/**
	 * Session tracking modes (one or more of the following: "cookie", "url", "ssl").
	 */
	private Set<Session.SessionTrackingMode> trackingModes;

	/**
	 * Whether to persist session data between restarts.
	 */
	private boolean persistent;

	/**
	 * Directory used to store session data.
	 */
	private File storeDir;

	private final Cookie cookie = new Cookie();

	private final SessionStoreDirectory sessionStoreDirectory = new SessionStoreDirectory();

	public Cookie getCookie() {
		return this.cookie;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public Set<Session.SessionTrackingMode> getTrackingModes() {
		return this.trackingModes;
	}

	public void setTrackingModes(Set<Session.SessionTrackingMode> trackingModes) {
		this.trackingModes = trackingModes;
	}

	public boolean isPersistent() {
		return this.persistent;
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public File getStoreDir() {
		return this.storeDir;
	}

	public void setStoreDir(File storeDir) {
		this.sessionStoreDirectory.setDirectory(storeDir);
		this.storeDir = storeDir;
	}

	SessionStoreDirectory getSessionStoreDirectory() {
		return this.sessionStoreDirectory;
	}

	/**
	 * Cookie properties.
	 */
	public static class Cookie {

		/**
		 * Session cookie name.
		 */
		private String name;

		/**
		 * Domain for the session cookie.
		 */
		private String domain;

		/**
		 * Path of the session cookie.
		 */
		private String path;

		/**
		 * Comment for the session cookie.
		 */
		private String comment;

		/**
		 * "HttpOnly" flag for the session cookie.
		 */
		private Boolean httpOnly;

		/**
		 * "Secure" flag for the session cookie.
		 */
		private Boolean secure;

		/**
		 * Maximum age of the session cookie.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration maxAge;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDomain() {
			return this.domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getComment() {
			return this.comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public Boolean getHttpOnly() {
			return this.httpOnly;
		}

		public void setHttpOnly(Boolean httpOnly) {
			this.httpOnly = httpOnly;
		}

		public Boolean getSecure() {
			return this.secure;
		}

		public void setSecure(Boolean secure) {
			this.secure = secure;
		}

		public Duration getMaxAge() {
			return this.maxAge;
		}

		public void setMaxAge(Duration maxAge) {
			this.maxAge = maxAge;
		}

	}

	/**
	 * Available session tracking modes (mirrors
	 * {@link javax.servlet.SessionTrackingMode}.
	 */
	public enum SessionTrackingMode {

		/**
		 * Send a cookie in response to the client's first request.
		 */
		COOKIE,

		/**
		 * Rewrite the URL to append a session ID.
		 */
		URL,

		/**
		 * Use SSL build-in mechanism to track the session.
		 */
		SSL

	}

}
