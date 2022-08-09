/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.Set;

import org.springframework.session.Session;

/**
 * A description of user's {@link Session session} exposed by {@code sessions} endpoint.
 * Primarily intended for serialization to JSON.
 *
 * @author Vedran Pavic
 * @since 3.0.0
 */
public final class SessionDescriptor {

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
