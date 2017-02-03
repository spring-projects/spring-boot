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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CanonicalPropertySourcesPropertySource}.
 *
 * @author Phillip Webb
 */
public class CanonicalPropertySourcesPropertySourceTests {

	private List<CanonicalPropertySource> canonicals = new ArrayList<>();

	private CanonicalPropertySourcesPropertySource propertySource = new CanonicalPropertySourcesPropertySource(
			"test", this.canonicals);

	@Test
	public void getPropertyShouldReturnValue() throws Exception {
		this.canonicals.add(new MockCanonicalPropertySource("foo.bar", "baz"));
		assertThat(this.propertySource.getProperty("foo.bar")).isEqualTo("baz");
	}

	@Test
	public void getPropertyWhenNameIsNotValidShouldReturnNull() throws Exception {
		this.canonicals.add(new MockCanonicalPropertySource("foo.bar", "baz"));
		assertThat(this.propertySource.getProperty(".foo.bar")).isNull();
	}

	@Test
	public void getPropertyWhenMultipleShouldReturnFirst() throws Exception {
		this.canonicals.add(new MockCanonicalPropertySource("foo.bar", "baz"));
		this.canonicals.add(new MockCanonicalPropertySource("foo.bar", "bill"));
		assertThat(this.propertySource.getProperty("foo.bar")).isEqualTo("baz");
	}

	@Test
	public void getPropertyWhenNoneShouldReturnFirst() throws Exception {
		this.canonicals.add(new MockCanonicalPropertySource("foo.bar", "baz"));
		assertThat(this.propertySource.getProperty("foo.foo")).isNull();
	}

	@Test
	public void getPropertyNamesShouldReturnPropertyNames() throws Exception {
		MockCanonicalPropertySource canonical1 = new MockCanonicalPropertySource();
		canonical1.add("a", "a");
		canonical1.add("b", "b");
		canonical1.add("c", "c");
		this.canonicals.add(canonical1);
		MockCanonicalPropertySource canonical2 = new MockCanonicalPropertySource();
		canonical2.add("c", "c");
		canonical2.add("d", "d");
		canonical2.add("e", "e");
		this.canonicals.add(canonical2);
		String[] propertyNames = this.propertySource.getPropertyNames();
		assertThat(propertyNames).containsExactly("a", "b", "c", "d", "e");
	}

	@Test
	public void getPropertyOriginShouldReturnOrigin() throws Exception {
		this.canonicals.add(new MockCanonicalPropertySource("foo.bar", "baz", "line1"));
		assertThat(this.propertySource.getPropertyOrigin("foo.bar").toString())
				.isEqualTo("line1");
	}

	@Test
	public void getPropertyOriginWhenMissingShouldReturnNull() throws Exception {
		this.canonicals.add(new MockCanonicalPropertySource("foo.bar", "baz", "line1"));
		assertThat(this.propertySource.getPropertyOrigin("foo.foo")).isNull();
	}

	@Test
	public void getNameShouldReturnName() throws Exception {
		assertThat(this.propertySource.getName()).isEqualTo("test");
	}

	@Test
	public void getSourceShouldReturnSource() throws Exception {
		assertThat(this.propertySource.getSource()).isSameAs(this.canonicals);
	}

}
