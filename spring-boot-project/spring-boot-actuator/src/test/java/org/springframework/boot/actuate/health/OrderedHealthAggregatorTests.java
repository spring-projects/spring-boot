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

package org.springframework.boot.actuate.health;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OrderedHealthAggregator}.
 *
 * @author Christian Dupuis
 */
public class OrderedHealthAggregatorTests {

	private OrderedHealthAggregator healthAggregator;

	@Before
	public void setup() {
		this.healthAggregator = new OrderedHealthAggregator();
	}

	@Test
	public void defaultOrder() {
		Map<String, Health> healths = new HashMap<>();
		healths.put("h1", new Health.Builder().status(Status.DOWN).build());
		healths.put("h2", new Health.Builder().status(Status.UP).build());
		healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
		healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
		assertThat(this.healthAggregator.aggregate(healths).getStatus())
				.isEqualTo(Status.DOWN);
	}

	@Test
	public void customOrder() {
		this.healthAggregator.setStatusOrder(Status.UNKNOWN, Status.UP,
				Status.OUT_OF_SERVICE, Status.DOWN);
		Map<String, Health> healths = new HashMap<>();
		healths.put("h1", new Health.Builder().status(Status.DOWN).build());
		healths.put("h2", new Health.Builder().status(Status.UP).build());
		healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
		healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
		assertThat(this.healthAggregator.aggregate(healths).getStatus())
				.isEqualTo(Status.UNKNOWN);
	}

	@Test
	public void defaultOrderWithCustomStatus() {
		Map<String, Health> healths = new HashMap<>();
		healths.put("h1", new Health.Builder().status(Status.DOWN).build());
		healths.put("h2", new Health.Builder().status(Status.UP).build());
		healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
		healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
		healths.put("h5", new Health.Builder().status(new Status("CUSTOM")).build());
		assertThat(this.healthAggregator.aggregate(healths).getStatus())
				.isEqualTo(Status.DOWN);
	}

	@Test
	public void customOrderWithCustomStatus() {
		this.healthAggregator.setStatusOrder(
				Arrays.asList("DOWN", "OUT_OF_SERVICE", "UP", "UNKNOWN", "CUSTOM"));
		Map<String, Health> healths = new HashMap<>();
		healths.put("h1", new Health.Builder().status(Status.DOWN).build());
		healths.put("h2", new Health.Builder().status(Status.UP).build());
		healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
		healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
		healths.put("h5", new Health.Builder().status(new Status("CUSTOM")).build());
		assertThat(this.healthAggregator.aggregate(healths).getStatus())
				.isEqualTo(Status.DOWN);
	}

}
