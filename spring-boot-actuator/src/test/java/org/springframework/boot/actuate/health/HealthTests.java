/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link Health}.
 * 
 * @author Phillip Webb
 */
public class HealthTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void statusMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Status must not be null");
		new Health(null, null);
	}

	@Test
	public void createWithStatus() throws Exception {
		Health health = new Health(Status.UP, null);
		assertThat(health.getStatus(), equalTo(Status.UP));
		assertThat(health.getDetails().size(), equalTo(0));
	}

	@Test
	public void createWithDetails() throws Exception {
		Health health = new Health(Status.UP, Collections.singletonMap("a", "b"));
		assertThat(health.getStatus(), equalTo(Status.UP));
		assertThat(health.getDetails().get("a"), equalTo((Object) "b"));
	}

	@Test
	public void equalsAndHashCode() throws Exception {
		Health h1 = new Health(Status.UP, Collections.singletonMap("a", "b"));
		Health h2 = new Health(Status.UP, Collections.singletonMap("a", "b"));
		Health h3 = new Health(Status.UP, null);
		assertThat(h1, equalTo(h1));
		assertThat(h1, equalTo(h2));
		assertThat(h1, not(equalTo(h3)));
		assertThat(h1.hashCode(), equalTo(h1.hashCode()));
		assertThat(h1.hashCode(), equalTo(h2.hashCode()));
		assertThat(h1.hashCode(), not(equalTo(h3.hashCode())));
	}

	@Test
	public void withException() throws Exception {
		RuntimeException ex = new RuntimeException("bang");
		Health health = new Health(Status.UP, Collections.singletonMap("a", "b"))
				.withException(ex);
		assertThat(health.getDetails().get("a"), equalTo((Object) "b"));
		assertThat(health.getDetails().get("error"),
				equalTo((Object) "java.lang.RuntimeException: bang"));
	}

	@Test
	public void withDetails() throws Exception {
		Health health = new Health(Status.UP, Collections.singletonMap("a", "b"))
				.withDetail("c", "d");
		assertThat(health.getDetails().get("a"), equalTo((Object) "b"));
		assertThat(health.getDetails().get("c"), equalTo((Object) "d"));
	}

	@Test
	public void unknownWithDetails() throws Exception {
		Health health = Health.unknown().withDetail("a", "b");
		assertThat(health.getStatus(), equalTo(Status.UNKNOWN));
		assertThat(health.getDetails().get("a"), equalTo((Object) "b"));
	}

	@Test
	public void unknown() throws Exception {
		Health health = Health.unknown();
		assertThat(health.getStatus(), equalTo(Status.UNKNOWN));
		assertThat(health.getDetails().size(), equalTo(0));
	}

	@Test
	public void upWithDetails() throws Exception {
		Health health = Health.up().withDetail("a", "b");
		assertThat(health.getStatus(), equalTo(Status.UP));
		assertThat(health.getDetails().get("a"), equalTo((Object) "b"));
	}

	@Test
	public void up() throws Exception {
		Health health = Health.up();
		assertThat(health.getStatus(), equalTo(Status.UP));
		assertThat(health.getDetails().size(), equalTo(0));
	}

	@Test
	public void downWithException() throws Exception {
		RuntimeException ex = new RuntimeException("bang");
		Health health = Health.down(ex);
		assertThat(health.getStatus(), equalTo(Status.DOWN));
		assertThat(health.getDetails().get("error"),
				equalTo((Object) "java.lang.RuntimeException: bang"));
	}

	@Test
	public void down() throws Exception {
		Health health = Health.down();
		assertThat(health.getStatus(), equalTo(Status.DOWN));
		assertThat(health.getDetails().size(), equalTo(0));
	}

	@Test
	public void outOfService() throws Exception {
		Health health = Health.outOfService();
		assertThat(health.getStatus(), equalTo(Status.OUT_OF_SERVICE));
		assertThat(health.getDetails().size(), equalTo(0));
	}

	@Test
	public void statusCode() throws Exception {
		Health health = Health.status("UP");
		assertThat(health.getStatus(), equalTo(Status.UP));
		assertThat(health.getDetails().size(), equalTo(0));
	}

	@Test
	public void status() throws Exception {
		Health health = Health.status(Status.UP);
		assertThat(health.getStatus(), equalTo(Status.UP));
		assertThat(health.getDetails().size(), equalTo(0));
	}

}
