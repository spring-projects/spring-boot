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

package org.springframework.actuate.audit.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.actuate.audit.AuditEvent;
import org.springframework.actuate.audit.AuditEventRepository;
import org.springframework.context.ApplicationListener;

/**
 * {@link ApplicationListener} that listens for {@link AuditEvent}s and stores them in a
 * {@link AuditEventRepository}.
 * 
 * @author Dave Syer
 */
public class AuditListener implements ApplicationListener<AuditApplicationEvent> {

	private static Log logger = LogFactory.getLog(AuditListener.class);

	private final AuditEventRepository auditEventRepository;

	public AuditListener(AuditEventRepository auditEventRepository) {
		this.auditEventRepository = auditEventRepository;
	}

	@Override
	public void onApplicationEvent(AuditApplicationEvent event) {
		logger.info(event.getAuditEvent());
		this.auditEventRepository.add(event.getAuditEvent());
	}

}
