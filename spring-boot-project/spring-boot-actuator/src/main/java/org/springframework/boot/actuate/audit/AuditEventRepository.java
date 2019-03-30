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

package org.springframework.boot.actuate.audit;

import java.time.Instant;
import java.util.List;

/**
 * Repository for {@link AuditEvent}s.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 */
public interface AuditEventRepository {

	/**
	 * Log an event.
	 * @param event the audit event to log
	 */
	void add(AuditEvent event);

	/**
	 * Find audit events of specified type relating to the specified principal that
	 * occurred {@link Instant#isAfter(Instant) after} the time provided.
	 * @param principal the principal name to search for (or {@code null} if unrestricted)
	 * @param after time after which an event must have occurred (or {@code null} if
	 * unrestricted)
	 * @param type the event type to search for (or {@code null} if unrestricted)
	 * @return audit events of specified type relating to the principal
	 * @since 1.4.0
	 */
	List<AuditEvent> find(String principal, Instant after, String type);

}
