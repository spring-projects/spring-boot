/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * {@link Endpoint} to expose a user's {@link Session}s.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@Endpoint(id = "sessions")
public class SessionsEndpoint {

	private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

	/**
	 * Create a new {@link SessionsEndpoint} instance.
	 * @param sessionRepository the session repository
	 */
	public SessionsEndpoint(
			FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
		this.sessionRepository = sessionRepository;
	}

	@ReadOperation
	public SessionsReport sessionsForUsername(String username) {
		Map<String, ? extends Session> sessions = this.sessionRepository
				.findByPrincipalName(username);
		return new SessionsReport(sessions);
	}

	@ReadOperation
	public SessionDescriptor getSession(@Selector String sessionId) {
		Session session = this.sessionRepository.findById(sessionId);
		if (session == null) {
			return null;
		}
		return new SessionDescriptor(session);
	}

	@DeleteOperation
	public void deleteSession(@Selector String sessionId) {
		this.sessionRepository.deleteById(sessionId);
	}

	/**
	 * A report of user's {@link Session sessions}. Primarily intended for serialization
	 * to JSON.
	 */
	public static final class SessionsReport {

		private final List<SessionDescriptor> sessions;

		public SessionsReport(Map<String, ? extends Session> sessions) {
			this.sessions = sessions.values().stream().map(SessionDescriptor::new)
					.collect(Collectors.toList());
		}

		public List<SessionDescriptor> getSessions() {
			return this.sessions;
		}

	}

	/**
	 * A description of user's {@link Session session}. Primarily intended for
	 * serialization to JSON.
	 */
	public static final class SessionDescriptor {

		private final String id;

		private final Set<String> attributeNames;

		private final Instant creationTime;

		private final Instant lastAccessedTime;

		private final long maxInactiveInterval;

		private final boolean expired;

		public SessionDescriptor(Session session) {
			this.id = session.getId();
			this.attributeNames = session.getAttributeNames();
			this.creationTime = session.getCreationTime();
			this.lastAccessedTime = session.getLastAccessedTime();
			this.maxInactiveInterval = session.getMaxInactiveInterval().getSeconds();
			this.expired = session.isExpired();
		}

		public String getId() {
			return this.id;
		}

		public Set<String> getAttributeNames() {
			return this.attributeNames;
		}

		public Instant getCreationTime() {
			return this.creationTime;
		}

		public Instant getLastAccessedTime() {
			return this.lastAccessedTime;
		}

		public long getMaxInactiveInterval() {
			return this.maxInactiveInterval;
		}

		public boolean isExpired() {
			return this.expired;
		}

	}

}
