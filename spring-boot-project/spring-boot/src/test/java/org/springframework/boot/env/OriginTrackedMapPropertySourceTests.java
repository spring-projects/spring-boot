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

package org.springframework.boot.env;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OriginTrackedMapPropertySource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class OriginTrackedMapPropertySourceTests {

	private Map<String, Object> map = new LinkedHashMap<>();

	private OriginTrackedMapPropertySource source = new OriginTrackedMapPropertySource(
			"test", this.map);

	private Origin origin = mock(Origin.class);

	@Test
	public void getPropertyWhenMissingShouldReturnNull() {
		assertThat(this.source.getProperty("test")).isNull();
	}

	@Test
	public void getPropertyWhenNonTrackedShouldReturnValue() {
		this.map.put("test", "foo");
		assertThat(this.source.getProperty("test")).isEqualTo("foo");
	}

	@Test
	public void getPropertyWhenTrackedShouldReturnValue() {
		this.map.put("test", OriginTrackedValue.of("foo", this.origin));
		assertThat(this.source.getProperty("test")).isEqualTo("foo");
	}

	@Test
	public void getPropertyOriginWhenMissingShouldReturnNull() {
		assertThat(this.source.getOrigin("test")).isNull();
	}

	@Test
	public void getPropertyOriginWhenNonTrackedShouldReturnNull() {
		this.map.put("test", "foo");
		assertThat(this.source.getOrigin("test")).isNull();
	}

	@Test
	public void getPropertyOriginWhenTrackedShouldReturnOrigin() {
		this.map.put("test", OriginTrackedValue.of("foo", this.origin));
		assertThat(this.source.getOrigin("test")).isEqualTo(this.origin);
	}

}
