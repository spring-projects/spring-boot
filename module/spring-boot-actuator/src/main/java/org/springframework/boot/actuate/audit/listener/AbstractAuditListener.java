/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.audit.listener;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.context.ApplicationListener;

/**
 * Abstract {@link ApplicationListener} to handle {@link AuditApplicationEvent}s.
 *
 * @author Vedran Pavic
 * @since 1.4.0
 */
public abstract class AbstractAuditListener implements ApplicationListener<AuditApplicationEvent> {

	@Override
	public void onApplicationEvent(AuditApplicationEvent event) {
		onAuditEvent(event.getAuditEvent());
	}

	protected abstract void onAuditEvent(AuditEvent event);

}
