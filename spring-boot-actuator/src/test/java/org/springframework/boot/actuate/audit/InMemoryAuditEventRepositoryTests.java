/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InMemoryAuditEventRepository}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 */
public class InMemoryAuditEventRepositoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void lessThanCapacity() throws Exception {
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
		repository.add(new AuditEvent("dave", "a"));
		repository.add(new AuditEvent("dave", "b"));
		List<AuditEvent> events = repository.find("dave", null);
		assertThat(events.size()).isEqualTo(2);
		assertThat(events.get(0).getType()).isEqualTo("a");
		assertThat(events.get(1).getType()).isEqualTo("b");
	}

	@Test
	public void capacity() throws Exception {
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository(2);
		repository.add(new AuditEvent("dave", "a"));
		repository.add(new AuditEvent("dave", "b"));
		repository.add(new AuditEvent("dave", "c"));
		List<AuditEvent> events = repository.find("dave", null);
		assertThat(events.size()).isEqualTo(2);
		assertThat(events.get(0).getType()).isEqualTo("b");
		assertThat(events.get(1).getType()).isEqualTo("c");
	}

	@Test
	public void addNullAuditEvent() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("AuditEvent must not be null");
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
		repository.add(null);
	}

	@Test
	public void findByPrincipal() throws Exception {
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
		repository.add(new AuditEvent("dave", "a"));
		repository.add(new AuditEvent("phil", "b"));
		repository.add(new AuditEvent("dave", "c"));
		repository.add(new AuditEvent("phil", "d"));
		List<AuditEvent> events = repository.find("dave", null);
		assertThat(events.size()).isEqualTo(2);
		assertThat(events.get(0).getType()).isEqualTo("a");
		assertThat(events.get(1).getType()).isEqualTo("c");
	}

	@Test
	public void findByPrincipalAndType() throws Exception {
		InMemoryAuditEventRepository repository = new InMemoryAuditEventRepository();
		repository.add(new AuditEvent("dave", "a"));
		repository.add(new AuditEvent("phil", "b"));
		repository.add(new AuditEvent("dave", "c"));
		repository.add(new AuditEvent("phil", "d"));
		List<AuditEvent> events = repository.find("dave", null, "a");
		assertThat(events.size()).isEqualTo(1);
		assertThat(events.get(0).getPrincipal()).isEqualTo("dave");
		assertThat(events.get(0).getType()).isEqualTo("a");
	}

	@Test
	public void findByDate() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2000, 1, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Map<String, Object> data = new HashMap<>();
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
		List<AuditEvent> events = repository.find(after);
		assertThat(events.size()).isEqualTo(2);
		assertThat(events.get(0).getType()).isEqualTo("c");
		assertThat(events.get(1).getType()).isEqualTo("d");
		events = repository.find("dave", after);
		assertThat(events.size()).isEqualTo(1);
		assertThat(events.get(0).getType()).isEqualTo("c");
	}

}
