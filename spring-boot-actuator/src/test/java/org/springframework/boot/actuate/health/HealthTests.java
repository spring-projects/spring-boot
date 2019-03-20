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

package org.springframework.boot.actuate.health;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

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
		new Health.Builder(null, null);
	}

	@Test
	public void createWithStatus() throws Exception {
		Health health = Health.status(Status.UP).build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().size()).isEqualTo(0);
	}

	@Test
	public void createWithDetails() throws Exception {
		Health health = new Health.Builder(Status.UP, Collections.singletonMap("a", "b"))
				.build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("a")).isEqualTo("b");
	}

	@Test
	public void equalsAndHashCode() throws Exception {
		Health h1 = new Health.Builder(Status.UP, Collections.singletonMap("a", "b"))
				.build();
		Health h2 = new Health.Builder(Status.UP, Collections.singletonMap("a", "b"))
				.build();
		Health h3 = new Health.Builder(Status.UP).build();
		assertThat(h1).isEqualTo(h1);
		assertThat(h1).isEqualTo(h2);
		assertThat(h1).isNotEqualTo(h3);
		assertThat(h1.hashCode()).isEqualTo(h1.hashCode());
		assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
		assertThat(h1.hashCode()).isNotEqualTo(h3.hashCode());
	}

	@Test
	public void withException() throws Exception {
		RuntimeException ex = new RuntimeException("bang");
		Health health = new Health.Builder(Status.UP, Collections.singletonMap("a", "b"))
				.withException(ex).build();
		assertThat(health.getDetails().get("a")).isEqualTo("b");
		assertThat(health.getDetails().get("error"))
				.isEqualTo("java.lang.RuntimeException: bang");
	}

	@Test
	public void withDetails() throws Exception {
		Health health = new Health.Builder(Status.UP, Collections.singletonMap("a", "b"))
				.withDetail("c", "d").build();
		assertThat(health.getDetails().get("a")).isEqualTo("b");
		assertThat(health.getDetails().get("c")).isEqualTo("d");
	}

	@Test
	public void unknownWithDetails() throws Exception {
		Health health = new Health.Builder().unknown().withDetail("a", "b").build();
		assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
		assertThat(health.getDetails().get("a")).isEqualTo("b");
	}

	@Test
	public void unknown() throws Exception {
		Health health = new Health.Builder().unknown().build();
		assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
		assertThat(health.getDetails().size()).isEqualTo(0);
	}

	@Test
	public void upWithDetails() throws Exception {
		Health health = new Health.Builder().up().withDetail("a", "b").build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("a")).isEqualTo("b");
	}

	@Test
	public void up() throws Exception {
		Health health = new Health.Builder().up().build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().size()).isEqualTo(0);
	}

	@Test
	public void downWithException() throws Exception {
		RuntimeException ex = new RuntimeException("bang");
		Health health = Health.down(ex).build();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("error"))
				.isEqualTo("java.lang.RuntimeException: bang");
	}

	@Test
	public void down() throws Exception {
		Health health = Health.down().build();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().size()).isEqualTo(0);
	}

	@Test
	public void outOfService() throws Exception {
		Health health = Health.outOfService().build();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
		assertThat(health.getDetails().size()).isEqualTo(0);
	}

	@Test
	public void statusCode() throws Exception {
		Health health = Health.status("UP").build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().size()).isEqualTo(0);
	}

	@Test
	public void status() throws Exception {
		Health health = Health.status(Status.UP).build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().size()).isEqualTo(0);
	}

}
