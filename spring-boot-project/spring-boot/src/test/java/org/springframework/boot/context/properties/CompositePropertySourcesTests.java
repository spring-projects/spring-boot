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
 * Tests for {@link CompositePropertySources}.
 *
 * @author Andy Wilkinson
 */
public class CompositePropertySourcesTests {

	@Test
	public void containsReturnsFalseWithNoBackingSources() {
		assertThat(new CompositePropertySources().contains("foo")).isFalse();
	}

	@Test
	public void getReturnsNullWithNoBackingSources() {
		assertThat(new CompositePropertySources().get("foo")).isNull();
	}

	@Test
	public void iteratorIsEmptyWithNoBackingSources() {
		assertThat(new CompositePropertySources().iterator()).hasSize(0);
	}

	@Test
	public void containsReturnsTrueForPropertySourceFoundInBackingSources() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		assertThat(new CompositePropertySources(sources).contains("foo")).isTrue();
	}

	@Test
	public void containsReturnsFalseForPropertySourceNotFoundInBackingSources() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("bar", Collections.emptyMap()));
		assertThat(new CompositePropertySources(sources).contains("foo")).isFalse();
	}

	@Test
	public void getReturnsPropertySourceFoundInBackingSources() {
		MutablePropertySources sources = new MutablePropertySources();
		MapPropertySource fooSource = new MapPropertySource("foo",
				Collections.emptyMap());
		sources.addFirst(fooSource);
		assertThat(new CompositePropertySources(sources).get("foo")).isEqualTo(fooSource);
	}

	@Test
	public void getReturnsNullWhenPropertySourceNotFoundInBackingSources() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		assertThat(new CompositePropertySources(sources).get("bar")).isNull();
	}

	@Test
	public void iteratorContainsSingleEntryWithSingleBackingSource() {
		MutablePropertySources sources = new MutablePropertySources();
		MapPropertySource fooSource = new MapPropertySource("foo",
				Collections.emptyMap());
		sources.addFirst(fooSource);
		assertThat(new CompositePropertySources(sources).iterator())
				.containsExactly(fooSource);
	}

	@Test
	public void iteratorReflectsOrderingOfSourcesAcrossMultipleBackingSources() {
		MutablePropertySources sourcesOne = new MutablePropertySources();
		MapPropertySource fooSource = new MapPropertySource("foo",
				Collections.emptyMap());
		sourcesOne.addFirst(fooSource);
		MapPropertySource barSource = new MapPropertySource("bar",
				Collections.emptyMap());
		sourcesOne.addFirst(barSource);
		MutablePropertySources sourcesTwo = new MutablePropertySources();
		MapPropertySource bazSource = new MapPropertySource("baz",
				Collections.emptyMap());
		sourcesTwo.addFirst(bazSource);
		assertThat(new CompositePropertySources(sourcesOne, sourcesTwo).iterator())
				.containsExactly(barSource, fooSource, bazSource);
	}

	@Test
	public void containsReflectsChangesInTheBackingSources() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		assertThat(new CompositePropertySources(sources).contains("bar")).isFalse();
		MapPropertySource barSource = new MapPropertySource("bar",
				Collections.emptyMap());
		sources.addFirst(barSource);
		assertThat(new CompositePropertySources(sources).contains("bar")).isTrue();
	}

	@Test
	public void getReflectsChangesInTheBackingSources() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("foo", Collections.emptyMap()));
		assertThat(new CompositePropertySources(sources).get("bar")).isNull();
		MapPropertySource barSource = new MapPropertySource("bar",
				Collections.emptyMap());
		sources.addFirst(barSource);
		assertThat(new CompositePropertySources(sources).get("bar")).isEqualTo(barSource);
	}

	@Test
	public void iteratorReflectsChangesInTheBackingSources() {
		MutablePropertySources sources = new MutablePropertySources();
		MapPropertySource fooSource = new MapPropertySource("foo",
				Collections.emptyMap());
		sources.addFirst(fooSource);
		assertThat(new CompositePropertySources(sources).iterator())
				.containsExactly(fooSource);
		MapPropertySource barSource = new MapPropertySource("bar",
				Collections.emptyMap());
		sources.addFirst(barSource);
		assertThat(new CompositePropertySources(sources).iterator())
				.containsExactly(barSource, fooSource);
	}

}
