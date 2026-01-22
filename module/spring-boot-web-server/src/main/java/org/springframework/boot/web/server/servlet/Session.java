/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.servlet;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Cookie;

/**
 * Session properties.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class Session {

	/**
	 * Session timeout. If a duration suffix is not specified, seconds will be used.
	 */
	@DurationUnit(ChronoUnit.SECONDS)
	private @Nullable Duration timeout = Duration.ofMinutes(30);

	/**
	 * Session tracking modes.
	 */
	private @Nullable Set<Session.SessionTrackingMode> trackingModes;

	/**
	 * Whether to persist session data between restarts.
	 */
	private boolean persistent;

	/**
	 * Directory used to store session data.
	 */
	private @Nullable File storeDir;

	@NestedConfigurationProperty
	private final Cookie cookie = new Cookie();

	private final SessionStoreDirectory sessionStoreDirectory = new SessionStoreDirectory();

	public @Nullable Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(@Nullable Duration timeout) {
		this.timeout = timeout;
	}

	/**
	 * Return the {@link SessionTrackingMode session tracking modes}.
	 * @return the session tracking modes
	 */
	public @Nullable Set<Session.SessionTrackingMode> getTrackingModes() {
		return this.trackingModes;
	}

	public void setTrackingModes(@Nullable Set<Session.SessionTrackingMode> trackingModes) {
		this.trackingModes = trackingModes;
	}

	/**
	 * Return whether to persist session data between restarts.
	 * @return {@code true} to persist session data between restarts.
	 */
	public boolean isPersistent() {
		return this.persistent;
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	/**
	 * Return the directory used to store session data.
	 * @return the session data store directory
	 */
	public @Nullable File getStoreDir() {
		return this.storeDir;
	}

	public void setStoreDir(@Nullable File storeDir) {
		this.sessionStoreDirectory.setDirectory(storeDir);
		this.storeDir = storeDir;
	}

	public Cookie getCookie() {
		return this.cookie;
	}

	public SessionStoreDirectory getSessionStoreDirectory() {
		return this.sessionStoreDirectory;
	}

	/**
	 * Available session tracking modes (mirrors
	 * {@link jakarta.servlet.SessionTrackingMode}).
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
