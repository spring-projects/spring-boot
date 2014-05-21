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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
	public void testDefaultOrdering() {
		Map<String, Health> healths = new HashMap<String, Health>();
		healths.put("h1", new Health(Status.DOWN));
		healths.put("h2", new Health(Status.UP));
		healths.put("h3", new Health(Status.UNKOWN));
		healths.put("h4", new Health(Status.OUT_OF_SERVICE));
		assertEquals(Status.DOWN, this.healthAggregator.aggregate(healths).getStatus());
	}

	@Test
	public void testDefaultOrderingWithCustomStatus() {
		Map<String, Health> healths = new HashMap<String, Health>();
		healths.put("h1", new Health(Status.DOWN));
		healths.put("h2", new Health(Status.UP));
		healths.put("h3", new Health(Status.UNKOWN));
		healths.put("h4", new Health(Status.OUT_OF_SERVICE));
		healths.put("h5", new Health(new Status("CUSTOM")));
		assertEquals(new Status("CUSTOM"),
				this.healthAggregator.aggregate(healths).getStatus());
	}

	@Test
	public void testDefaultOrderingWithCustomStatusAndOrder() {
		this.healthAggregator.setStatusOrder(Arrays.asList("DOWN", "OUT_OF_SERVICE",
				"UP", "UNKOWN", "CUSTOM"));
		Map<String, Health> healths = new HashMap<String, Health>();
		healths.put("h1", new Health(Status.DOWN));
		healths.put("h2", new Health(Status.UP));
		healths.put("h3", new Health(Status.UNKOWN));
		healths.put("h4", new Health(Status.OUT_OF_SERVICE));
		healths.put("h5", new Health(new Status("CUSTOM")));
		assertEquals(Status.DOWN, this.healthAggregator.aggregate(healths).getStatus());
	}

}
