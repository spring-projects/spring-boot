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

package org.springframework.zero.actuate.audit;

import java.util.Collections;

import org.junit.Test;
import org.springframework.zero.actuate.audit.AuditEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link AuditEvent}.
 * 
 * @author Dave Syer
 */
public class AuditEventTests {

	@Test
	public void testNowEvent() throws Exception {
		AuditEvent event = new AuditEvent("phil", "UNKNOWN", Collections.singletonMap(
				"a", (Object) "b"));
		assertEquals("b", event.getData().get("a"));
		assertEquals("UNKNOWN", event.getType());
		assertEquals("phil", event.getPrincipal());
		assertNotNull(event.getTimestamp());
	}

	@Test
	public void testConvertStringsToData() throws Exception {
		AuditEvent event = new AuditEvent("phil", "UNKNOWN", "a=b", "c=d");
		assertEquals("b", event.getData().get("a"));
		assertEquals("d", event.getData().get("c"));
	}

}
