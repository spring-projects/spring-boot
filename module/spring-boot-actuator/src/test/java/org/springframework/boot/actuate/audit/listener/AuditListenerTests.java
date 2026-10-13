/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.audit.listener;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AuditListener}.
 *
 * @author Phillip Webb
 */
class AuditListenerTests {

	@Test
	void testStoredEvents() {
		AuditEventRepository repository = mock(AuditEventRepository.class);
		AuditEvent event = new AuditEvent("principal", "type", Collections.emptyMap());
		AuditListener listener = new AuditListener(repository);
		listener.onApplicationEvent(new AuditApplicationEvent(event));
		then(repository).should().add(event);
	}

}
