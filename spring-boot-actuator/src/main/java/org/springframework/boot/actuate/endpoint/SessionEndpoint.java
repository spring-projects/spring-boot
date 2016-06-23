/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.session.ExpiringSession;
import org.springframework.session.FindByIndexNameSessionRepository;

/**
 * {@link Endpoint} to manage web sessions.
 *
 * @author Eddú Meléndez
 * @since 1.4.0
 */
@ConfigurationProperties("endpoints.session")
public class SessionEndpoint implements Endpoint<Object> {

	@Autowired
	private FindByIndexNameSessionRepository<? extends ExpiringSession> sessionRepository;

	/**
	 * Enable the endpoint.
	 */
	private boolean enabled = false;

	@Override
	public String getId() {
		return "session";
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public boolean isSensitive() {
		return true;
	}

	@Override
	public Object invoke() {
		return null;
	}

	public Map<Object, Object> result(String username) {
		List<Object> sessions = new ArrayList<Object>();
		for (ExpiringSession session : this.sessionRepository.findByIndexNameAndIndexValue(
				FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, username)
				.values()) {
			sessions.add(new Session(session));
		}
		Map<Object, Object> sessionEntries = new HashMap<Object, Object>();
		sessionEntries.put("sessions", sessions);
		return Collections.unmodifiableMap(sessionEntries);
	}

	public boolean delete(String sessionId) {
		ExpiringSession session = this.sessionRepository.getSession(sessionId);
		if (session != null) {
			this.sessionRepository.delete(sessionId);
			return true;
		}
		return false;
	}

	/**
	 * Session properties.
	 */
	public static class Session {

		private String id;

		private long creationTime;

		private long lastAccessedTime;

		public Session(ExpiringSession session) {
			this.id = session.getId();
			this.creationTime = session.getCreationTime();
			this.lastAccessedTime = session.getLastAccessedTime();
		}

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public long getCreationTime() {
			return this.creationTime;
		}

		public void setCreationTime(long creationTime) {
			this.creationTime = creationTime;
		}

		public long getLastAccessedTime() {
			return this.lastAccessedTime;
		}

		public void setLastAccessedTime(long lastAccessedTime) {
			this.lastAccessedTime = lastAccessedTime;
		}

	}

}
