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

package org.springframework.boot.actuate.endpoint.jmx;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link JmxEndpoint} for {@link AuditEventRepository}.
 *
 * @author Vedran Pavic
 * @since 1.5.0
 */
public class AuditEventsJmxEndpoint extends AbstractJmxEndpoint {

	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	private final AuditEventRepository auditEventRepository;

	public AuditEventsJmxEndpoint(ObjectMapper objectMapper,
			AuditEventRepository auditEventRepository) {
		super(objectMapper);
		Assert.notNull(auditEventRepository, "AuditEventRepository must not be null");
		this.auditEventRepository = auditEventRepository;
	}

	@ManagedOperation(description = "Retrieves a list of audit events meeting the given criteria")
	public Object getData(String dateAfter) {
		List<AuditEvent> auditEvents = this.auditEventRepository
				.find(parseDate(dateAfter));
		return convert(auditEvents);
	}

	@ManagedOperation(description = "Retrieves a list of audit events meeting the given criteria")
	public Object getData(String dateAfter, String principal) {
		List<AuditEvent> auditEvents = this.auditEventRepository.find(principal,
				parseDate(dateAfter));
		return convert(auditEvents);
	}

	@ManagedOperation(description = "Retrieves a list of audit events meeting the given criteria")
	public Object getData(String principal, String dateAfter, String type) {
		List<AuditEvent> auditEvents = this.auditEventRepository.find(principal,
				parseDate(dateAfter), type);
		return convert(auditEvents);
	}

	private Date parseDate(String date) {
		try {
			if (StringUtils.hasLength(date)) {
				return new SimpleDateFormat(DATE_FORMAT).parse(date);
			}
			return null;
		}
		catch (ParseException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

}
