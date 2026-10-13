/*
 * Copyright 2012-present the original author or authors.
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
 * @since 1.0.0
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
