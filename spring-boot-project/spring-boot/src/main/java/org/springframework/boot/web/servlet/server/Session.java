/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;

/**
 * Session properties.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class Session {

	@DurationUnit(ChronoUnit.SECONDS)
	private Duration timeout = Duration.ofMinutes(30);

	private Set<Session.SessionTrackingMode> trackingModes;

	private boolean persistent;

	/**
	 * Directory used to store session data.
	 */
	private File storeDir;

	@NestedConfigurationProperty
	private final Cookie cookie = new Cookie();

	private final SessionStoreDirectory sessionStoreDirectory = new SessionStoreDirectory();

	/**
	 * Returns the timeout duration for the session.
	 * @return the timeout duration as a Duration object
	 */
	public Duration getTimeout() {
		return this.timeout;
	}

	/**
	 * Sets the timeout for the session.
	 * @param timeout the duration of the timeout
	 */
	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	/**
	 * Return the {@link SessionTrackingMode session tracking modes}.
	 * @return the session tracking modes
	 */
	public Set<Session.SessionTrackingMode> getTrackingModes() {
		return this.trackingModes;
	}

	/**
	 * Sets the tracking modes for the session.
	 * @param trackingModes the set of session tracking modes to be set
	 */
	public void setTrackingModes(Set<Session.SessionTrackingMode> trackingModes) {
		this.trackingModes = trackingModes;
	}

	/**
	 * Return whether to persist session data between restarts.
	 * @return {@code true} to persist session data between restarts.
	 */
	public boolean isPersistent() {
		return this.persistent;
	}

	/**
	 * Sets the persistence flag for the session.
	 * @param persistent the persistence flag to be set
	 */
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	/**
	 * Return the directory used to store session data.
	 * @return the session data store directory
	 */
	public File getStoreDir() {
		return this.storeDir;
	}

	/**
	 * Sets the directory for storing session data.
	 * @param storeDir the directory to set as the session store directory
	 */
	public void setStoreDir(File storeDir) {
		this.sessionStoreDirectory.setDirectory(storeDir);
		this.storeDir = storeDir;
	}

	/**
	 * Returns the cookie associated with this session.
	 * @return the cookie associated with this session
	 */
	public Cookie getCookie() {
		return this.cookie;
	}

	/**
	 * Returns the session store directory.
	 * @return the session store directory
	 */
	SessionStoreDirectory getSessionStoreDirectory() {
		return this.sessionStoreDirectory;
	}

	/**
	 * Session cookie properties. This class is provided only for back-compatibility
	 * reasons, consider using {@link org.springframework.boot.web.server.Cookie} whever
	 * possible.
	 */
	public static class Cookie extends org.springframework.boot.web.server.Cookie {

	}

	/**
	 * Available session tracking modes (mirrors
	 * {@link jakarta.servlet.SessionTrackingMode}.
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
