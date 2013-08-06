/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.audit;

import java.util.Date;
import java.util.List;

/**
 * Repository for {@link AuditEvent}s.
 * 
 * @author Dave Syer
 */
public interface AuditEventRepository {

	/**
	 * Find audit events relating to the specified principal since the time provided.
	 * 
	 * @param principal the principal name to search for
	 * @param after timestamp of earliest result required
	 * @return audit events relating to the principal
	 */
	List<AuditEvent> find(String principal, Date after);

	/**
	 * Log an event.
	 * 
	 * @param event the audit event to log
	 */
	void add(AuditEvent event);

}
