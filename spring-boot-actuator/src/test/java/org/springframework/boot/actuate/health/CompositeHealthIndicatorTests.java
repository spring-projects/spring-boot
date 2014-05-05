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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CompositeHealthIndicator}
 * 
 * @author Tyler J. Frederick
 * @author Phillip Webb
 */
public class CompositeHealthIndicatorTests {

	@Mock
	private HealthIndicator<String> one;

	@Mock
	private HealthIndicator<String> two;

	@Mock
	private HealthIndicator<String> three;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.one.health()).willReturn("1");
		given(this.two.health()).willReturn("2");
		given(this.three.health()).willReturn("3");
	}

	@Test
	public void createWithIndicators() throws Exception {
		Map<String, HealthIndicator<?>> indicators = new HashMap<String, HealthIndicator<?>>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		CompositeHealthIndicator composite = new CompositeHealthIndicator(indicators);
		Map<String, Object> result = composite.health();
		assertThat(result.size(), equalTo(2));
		assertThat(result, hasEntry("one", (Object) "1"));
		assertThat(result, hasEntry("two", (Object) "2"));
	}

	@Test
	public void createWithIndicatorsAndAdd() throws Exception {
		Map<String, HealthIndicator<?>> indicators = new HashMap<String, HealthIndicator<?>>();
		indicators.put("one", this.one);
		indicators.put("two", this.two);
		CompositeHealthIndicator composite = new CompositeHealthIndicator(indicators);
		composite.addHealthIndicator("three", this.three);
		Map<String, Object> result = composite.health();
		assertThat(result.size(), equalTo(3));
		assertThat(result, hasEntry("one", (Object) "1"));
		assertThat(result, hasEntry("two", (Object) "2"));
		assertThat(result, hasEntry("three", (Object) "3"));
	}

	@Test
	public void createWithoutAndAdd() throws Exception {
		CompositeHealthIndicator composite = new CompositeHealthIndicator();
		composite.addHealthIndicator("one", this.one);
		composite.addHealthIndicator("two", this.two);
		Map<String, Object> result = composite.health();
		assertThat(result.size(), equalTo(2));
		assertThat(result, hasEntry("one", (Object) "1"));
		assertThat(result, hasEntry("two", (Object) "2"));
	}

}
