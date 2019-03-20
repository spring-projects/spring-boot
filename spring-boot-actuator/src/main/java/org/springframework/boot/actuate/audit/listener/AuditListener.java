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

package org.springframework.boot.actuate.audit.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;

/**
 * The default {@link AbstractAuditListener} implementation. Listens for
 * {@link AuditApplicationEvent}s and stores them in a {@link AuditEventRepository}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
public class AuditListener extends AbstractAuditListener {

	private static final Log logger = LogFactory.getLog(AuditListener.class);

	private final AuditEventRepository auditEventRepository;

	public AuditListener(AuditEventRepository auditEventRepository) {
		this.auditEventRepository = auditEventRepository;
	}

	@Override
	protected void onAuditEvent(AuditEvent event) {
		if (logger.isDebugEnabled()) {
			logger.debug(event);
		}
		this.auditEventRepository.add(event);
	}

}
