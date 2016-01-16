/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link DefaultHealthIndicatorRegistry}.
 *
 * @author Vedran Pavic
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultHealthIndicatorRegistryTest {

	@Mock
	private HealthIndicator one;

	@Mock
	private HealthIndicator two;

	private DefaultHealthIndicatorRegistry registry;

	private DefaultHealthIndicatorRegistryProperties properties = new DefaultHealthIndicatorRegistryProperties();

	@Before
	public void setup() {
		given(this.one.health()).willReturn(new Health.Builder().up().build());
		given(this.two.health()).willReturn(new Health.Builder().unknown().build());

		this.registry = new DefaultHealthIndicatorRegistry(this.properties);
	}

	@Test
	public void createAndRunAll() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		Map<String, Health> healths = this.registry.runHealthIndicators();
		assertThat(healths.size(), equalTo(2));
		assertThat(healths.get("one").getStatus(), equalTo(Status.UP));
		assertThat(healths.get("two").getStatus(), equalTo(Status.UNKNOWN));
	}

	@Test
	public void createAndRunAllInParallel() {
		this.properties.setRunInParallel(true);
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		Map<String, Health> healths = this.registry.runHealthIndicators();
		assertThat(healths.size(), equalTo(2));
		assertThat(healths.get("one").getStatus(), equalTo(Status.UP));
		assertThat(healths.get("two").getStatus(), equalTo(Status.UNKNOWN));
	}

	@Test
	public void createAndRunSingle() {
		this.registry.register("one", this.one);
		Health health = this.registry.runHealthIndicator("one");
		assertThat(health.getStatus(), equalTo(Status.UP));
	}

	@Test
	public void testUnregister() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		Map<String, Health> healths = this.registry.runHealthIndicators();
		assertThat(healths.size(), equalTo(2));
		this.registry.unregister("two");
		healths = this.registry.runHealthIndicators();
		assertThat(healths.size(), equalTo(1));
	}

}
