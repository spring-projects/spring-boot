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

package org.springframework.boot.context.embedded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MimeMappings}.
 *
 * @author Phillip Webb
 */
public class MimeMappingsTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test(expected = UnsupportedOperationException.class)
	public void defaultsCannotBeModified() throws Exception {
		MimeMappings.DEFAULT.add("foo", "foo/bar");
	}

	@Test
	public void createFromExisting() throws Exception {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		MimeMappings clone = new MimeMappings(mappings);
		mappings.add("baz", "bar");
		assertThat(clone.get("foo")).isEqualTo("bar");
		assertThat(clone.get("baz")).isNull();
	}

	@Test
	public void createFromMap() throws Exception {
		Map<String, String> mappings = new HashMap<String, String>();
		mappings.put("foo", "bar");
		MimeMappings clone = new MimeMappings(mappings);
		mappings.put("baz", "bar");
		assertThat(clone.get("foo")).isEqualTo("bar");
		assertThat(clone.get("baz")).isNull();
	}

	@Test
	public void iterate() throws Exception {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		mappings.add("baz", "boo");
		List<MimeMappings.Mapping> mappingList = new ArrayList<MimeMappings.Mapping>();
		for (MimeMappings.Mapping mapping : mappings) {
			mappingList.add(mapping);
		}
		assertThat(mappingList.get(0).getExtension()).isEqualTo("foo");
		assertThat(mappingList.get(0).getMimeType()).isEqualTo("bar");
		assertThat(mappingList.get(1).getExtension()).isEqualTo("baz");
		assertThat(mappingList.get(1).getMimeType()).isEqualTo("boo");
	}

	@Test
	public void getAll() throws Exception {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		mappings.add("baz", "boo");
		List<MimeMappings.Mapping> mappingList = new ArrayList<MimeMappings.Mapping>();
		mappingList.addAll(mappings.getAll());
		assertThat(mappingList.get(0).getExtension()).isEqualTo("foo");
		assertThat(mappingList.get(0).getMimeType()).isEqualTo("bar");
		assertThat(mappingList.get(1).getExtension()).isEqualTo("baz");
		assertThat(mappingList.get(1).getMimeType()).isEqualTo("boo");
	}

	@Test
	public void addNew() throws Exception {
		MimeMappings mappings = new MimeMappings();
		assertThat(mappings.add("foo", "bar")).isNull();
	}

	@Test
	public void addReplacesExisting() throws Exception {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		assertThat(mappings.add("foo", "baz")).isEqualTo("bar");
	}

	@Test
	public void remove() throws Exception {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		assertThat(mappings.remove("foo")).isEqualTo("bar");
		assertThat(mappings.remove("foo")).isNull();
	}

	@Test
	public void get() throws Exception {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		assertThat(mappings.get("foo")).isEqualTo("bar");
	}

	@Test
	public void getMissing() throws Exception {
		MimeMappings mappings = new MimeMappings();
		assertThat(mappings.get("foo")).isNull();
	}

	@Test
	public void makeUnmodifiable() throws Exception {
		MimeMappings mappings = new MimeMappings();
		mappings.add("foo", "bar");
		MimeMappings unmodifiable = MimeMappings.unmodifiableMappings(mappings);
		try {
			unmodifiable.remove("foo");
		}
		catch (UnsupportedOperationException ex) {
			// Expected
		}
		mappings.remove("foo");
		assertThat(unmodifiable.get("foo")).isNull();
	}

}
