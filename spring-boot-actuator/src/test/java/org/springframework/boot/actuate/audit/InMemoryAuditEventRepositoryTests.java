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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link InMemoryAuditEventRepository}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class InMemoryAuditEventRepositoryTests {

	@Test
	public void lessThanCapacity() throws Exception {
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
		repository.add(new AuditEvent("dave", "a"));
		repository.add(new AuditEvent("dave", "b"));
		List<AuditEvent> events = repository.find("dave", null);
		assertThat(events.size(), equalTo(2));
		assertThat(events.get(0).getType(), equalTo("a"));
		assertThat(events.get(1).getType(), equalTo("b"));

	}

	@Test
	public void capacity() throws Exception {
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository(2);
		repository.add(new AuditEvent("dave", "a"));
		repository.add(new AuditEvent("dave", "b"));
		repository.add(new AuditEvent("dave", "c"));
		List<AuditEvent> events = repository.find("dave", null);
		assertThat(events.size(), equalTo(2));
		assertThat(events.get(0).getType(), equalTo("b"));
		assertThat(events.get(1).getType(), equalTo("c"));
	}

	@Test
	public void findByPrincipal() throws Exception {
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
		repository.add(new AuditEvent("dave", "a"));
		repository.add(new AuditEvent("phil", "b"));
		repository.add(new AuditEvent("dave", "c"));
		repository.add(new AuditEvent("phil", "d"));
		List<AuditEvent> events = repository.find("dave", null);
		assertThat(events.size(), equalTo(2));
		assertThat(events.get(0).getType(), equalTo("a"));
		assertThat(events.get(1).getType(), equalTo("c"));
	}

	@Test
	public void findByDate() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2000, 1, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Map<String, Object> data = new HashMap<String, Object>();
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
		repository.add(new AuditEvent(calendar.getTime(), "dave", "a", data));
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		repository.add(new AuditEvent(calendar.getTime(), "phil", "b", data));
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		Date after = calendar.getTime();
		repository.add(new AuditEvent(calendar.getTime(), "dave", "c", data));
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		repository.add(new AuditEvent(calendar.getTime(), "phil", "d", data));
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		List<AuditEvent> events = repository.find(null, after);
		assertThat(events.size(), equalTo(2));
		assertThat(events.get(0).getType(), equalTo("c"));
		assertThat(events.get(1).getType(), equalTo("d"));
		events = repository.find("dave", after);
		assertThat(events.size(), equalTo(1));
		assertThat(events.get(0).getType(), equalTo("c"));
	}

}
