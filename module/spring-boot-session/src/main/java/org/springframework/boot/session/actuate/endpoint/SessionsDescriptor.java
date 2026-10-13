/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.actuate.endpoint;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.session.Session;

/**
 * Description of user's {@link Session sessions}.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public final class SessionsDescriptor implements OperationResponseBody {

	private final List<SessionDescriptor> sessions;

	public SessionsDescriptor(Map<String, ? extends Session> sessions) {
		this.sessions = sessions.values().stream().map(SessionDescriptor::new).toList();
	}

	public List<SessionDescriptor> getSessions() {
		return this.sessions;
	}

	/**
	 * A description of user's {@link Session session} exposed by {@code sessions}
	 * endpoint. Primarily intended for serialization to JSON.
	 */
	public static final class SessionDescriptor {

		private final String id;

		private final Set<String> attributeNames;

		private final Instant creationTime;

		private final Instant lastAccessedTime;

		private final long maxInactiveInterval;

		private final boolean expired;

		SessionDescriptor(Session session) {
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
