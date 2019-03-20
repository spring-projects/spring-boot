/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.Collections;

import org.junit.Test;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AuditListener}.
 *
 * @author Phillip Webb
 */
public class AuditListenerTests {

	@Test
	public void testStoredEvents() {
		AuditEventRepository repository = mock(AuditEventRepository.class);
		AuditEvent event = new AuditEvent("principal", "type",
				Collections.<String, Object>emptyMap());
		AuditListener listener = new AuditListener(repository);
		listener.onApplicationEvent(new AuditApplicationEvent(event));
		verify(repository).add(event);
	}

}
