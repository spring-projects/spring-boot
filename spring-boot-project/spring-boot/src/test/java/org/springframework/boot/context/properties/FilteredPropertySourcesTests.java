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

package org.springframework.boot.context.properties;

import java.util.Collections;

import org.junit.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FilteredPropertySources}.
 *
 * @author Andy Wilkinson
 */
public class FilteredPropertySourcesTests {

	@Test
	public void getReturnsNullForFilteredSource() {
		MutablePropertySources delegate = new MutablePropertySources();
		delegate.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		assertThat(new FilteredPropertySources(delegate, "foo").get("foo")).isNull();
	}

	@Test
	public void getReturnsSourceThatIsNotFiltered() {
		MutablePropertySources delegate = new MutablePropertySources();
		delegate.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		MapPropertySource barSource = new MapPropertySource("bar",
				Collections.emptyMap());
		delegate.addFirst(barSource);
		assertThat(new FilteredPropertySources(delegate, "foo").get("bar"))
				.isEqualTo(barSource);
	}

	@Test
	public void containsReturnsFalseForFilteredSource() {
		MutablePropertySources delegate = new MutablePropertySources();
		delegate.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		assertThat(new FilteredPropertySources(delegate, "foo").contains("foo"))
				.isFalse();
	}

	@Test
	public void containsReturnsTrueForSourceThatIsNotFiltered() {
		MutablePropertySources delegate = new MutablePropertySources();
		delegate.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		MapPropertySource barSource = new MapPropertySource("bar",
				Collections.emptyMap());
		delegate.addFirst(barSource);
		assertThat(new FilteredPropertySources(delegate, "foo").contains("bar")).isTrue();
	}

	@Test
	public void iteratorOmitsSourceThatIsFiltered() {
		MutablePropertySources delegate = new MutablePropertySources();
		MapPropertySource barSource = new MapPropertySource("bar",
				Collections.emptyMap());
		delegate.addFirst(barSource);
		delegate.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		MapPropertySource bazSource = new MapPropertySource("baz",
				Collections.emptyMap());
		delegate.addFirst(bazSource);
		assertThat(new FilteredPropertySources(delegate, "foo").iterator())
				.containsExactly(bazSource, barSource);
	}

}
