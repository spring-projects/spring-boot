/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * {@link MvcEndpoint} to expose {@link AuditEvent}s.
 *
 * @author Vedran Pavic
 * @author Phillip Webb
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "endpoints.auditevents")
public class AuditEventsMvcEndpoint extends AbstractNamedMvcEndpoint {

	private final AuditEventRepository auditEventRepository;

	public AuditEventsMvcEndpoint(AuditEventRepository auditEventRepository) {
		super("auditevents", "/auditevents", true);
		Assert.notNull(auditEventRepository, "AuditEventRepository must not be null");
		this.auditEventRepository = auditEventRepository;
	}

	@ActuatorGetMapping
	@ResponseBody
	public ResponseEntity<?> findByPrincipalAndAfterAndType(
			@RequestParam(required = false) String principal,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ") Date after,
			@RequestParam(required = false) String type) {
		if (!isEnabled()) {
			return DISABLED_RESPONSE;
		}
		Map<Object, Object> result = new LinkedHashMap<Object, Object>();
		result.put("events", this.auditEventRepository.find(principal, after, type));
		return ResponseEntity.ok(result);
	}

}
