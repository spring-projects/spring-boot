/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Date;
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
	 * Find audit events since the time provided.
	 * @param after timestamp of earliest result required (or {@code null} if
	 * unrestricted)
	 * @return audit events
	 * @since 1.4.0
	 */
	List<AuditEvent> find(Date after);

	/**
	 * Find audit events relating to the specified principal since the time provided.
	 * @param principal the principal name to search for (or {@code null} if unrestricted)
	 * @param after timestamp of earliest result required (or {@code null} if
	 * unrestricted)
	 * @return audit events relating to the principal
	 */
	List<AuditEvent> find(String principal, Date after);

	/**
	 * Find audit events of specified type relating to the specified principal since the
	 * time provided.
	 * @param principal the principal name to search for (or {@code null} if unrestricted)
	 * @param after timestamp of earliest result required (or {@code null} if
	 * unrestricted)
	 * @param type the event type to search for (or {@code null} if unrestricted)
	 * @return audit events of specified type relating to the principal
	 * @since 1.4.0
	 */
	List<AuditEvent> find(String principal, Date after, String type);

}
