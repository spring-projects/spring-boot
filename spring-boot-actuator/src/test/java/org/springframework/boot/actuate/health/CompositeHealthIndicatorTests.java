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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CompositeHealthIndicator}
 * 
 * @author Tyler J. Frederick
 * @author Phillip Webb
 * @author Christian Dupuis
 */
public class CompositeHealthIndicatorTests {

	private HealthAggregator healthAggregator;

	@Mock
	private HealthIndicator one;

	@Mock
	private HealthIndicator two;

	@Mock
	private HealthIndicator three;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.one.health()).willReturn(
				new Health.Builder().unknown().withDetail("1", "1").build());
		given(this.two.health()).willReturn(
				new Health.Builder().unknown().withDetail("2", "2").build());
		given(this.three.health()).willReturn(
				new Health.Builder().unknown().withDetail("3", "3").build());

		this.healthAggregator = new OrderedHealthAggregator();
	}

	@Test
	public void createWithIndicators() throws Exception {
		Map<String, HealthIndicator> indicators = new HashMap<String, HealthIndicator>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		CompositeHealthIndicator composite = new CompositeHealthIndicator(
				this.healthAggregator, indicators);
		Health result = composite.health();
		assertThat(result.getDetails().size(), equalTo(2));
		assertThat(
				result.getDetails(),
				hasEntry("one",
						(Object) new Health.Builder().unknown().withDetail("1", "1")
								.build()));
		assertThat(
				result.getDetails(),
				hasEntry("two",
						(Object) new Health.Builder().unknown().withDetail("2", "2")
								.build()));
	}

	@Test
	public void createWithIndicatorsAndAdd() throws Exception {
		Map<String, HealthIndicator> indicators = new HashMap<String, HealthIndicator>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		CompositeHealthIndicator composite = new CompositeHealthIndicator(
				this.healthAggregator, indicators);
		composite.addHealthIndicator("three", this.three);
		Health result = composite.health();
		assertThat(result.getDetails().size(), equalTo(3));
		assertThat(
				result.getDetails(),
				hasEntry("one",
						(Object) new Health.Builder().unknown().withDetail("1", "1")
								.build()));
		assertThat(
				result.getDetails(),
				hasEntry("two",
						(Object) new Health.Builder().unknown().withDetail("2", "2")
								.build()));
		assertThat(
				result.getDetails(),
				hasEntry("three",
						(Object) new Health.Builder().unknown().withDetail("3", "3")
								.build()));
	}

	@Test
	public void createWithoutAndAdd() throws Exception {
		CompositeHealthIndicator composite = new CompositeHealthIndicator(
				this.healthAggregator);
		composite.addHealthIndicator("one", this.one);
		composite.addHealthIndicator("two", this.two);
		Health result = composite.health();
		assertThat(result.getDetails().size(), equalTo(2));
		assertThat(
				result.getDetails(),
				hasEntry("one",
						(Object) new Health.Builder().unknown().withDetail("1", "1")
								.build()));
		assertThat(
				result.getDetails(),
				hasEntry("two",
						(Object) new Health.Builder().unknown().withDetail("2", "2")
								.build()));
	}

	@Test
	public void testSerialization() throws Exception {
		Map<String, HealthIndicator> indicators = new HashMap<String, HealthIndicator>();
		indicators.put("db1", this.one);
		indicators.put("db2", this.two);
		CompositeHealthIndicator innerComposite = new CompositeHealthIndicator(
				this.healthAggregator, indicators);
		CompositeHealthIndicator composite = new CompositeHealthIndicator(
				this.healthAggregator);
		composite.addHealthIndicator("db", innerComposite);

		Health result = composite.health();

		ObjectMapper mapper = new ObjectMapper();
		assertEquals("{\"status\":\"UNKNOWN\",\"db\":{\"status\":\"UNKNOWN\""
				+ ",\"db1\":{\"status\":\"UNKNOWN\",\"1\":\"1\"},"
				+ "\"db2\":{\"status\":\"UNKNOWN\",\"2\":\"2\"}}}",
				mapper.writeValueAsString(result));
	}

}
