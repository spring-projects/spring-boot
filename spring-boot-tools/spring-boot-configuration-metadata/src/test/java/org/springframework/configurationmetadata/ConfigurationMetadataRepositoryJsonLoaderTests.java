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

package org.springframework.configurationmetadata;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ConfigurationMetadataRepository}.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationMetadataRepositoryJsonLoaderTests extends AbstractConfigurationMetadataTests {

	private final ConfigurationMetadataRepositoryJsonLoader loader = new ConfigurationMetadataRepositoryJsonLoader();

	@Test
	public void nullResource() throws IOException {
		thrown.expect(IllegalArgumentException.class);
		loader.loadAll(null);
	}

	@Test
	public void simpleRepository() throws IOException {
		ConfigurationMetadataRepository repo = loader.loadAll(Collections.singleton(getInputStreamFor("foo")));
		validateFoo(repo);
		assertEquals(1, repo.getAllGroups().size());

		contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter");
		assertEquals(3, repo.getAllProperties().size());
	}

	@Test
	public void severalRepositoriesNoConflict() throws IOException {
		ConfigurationMetadataRepository repo = loader.loadAll(
				Arrays.asList(getInputStreamFor("foo"), getInputStreamFor("bar")));
		validateFoo(repo);
		validateBar(repo);
		assertEquals(2, repo.getAllGroups().size());

		contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter",
				"spring.bar.name", "spring.bar.description", "spring.bar.counter");
		assertEquals(6, repo.getAllProperties().size());
	}

	@Test
	public void repositoryWithRoot() throws IOException {
		ConfigurationMetadataRepository repo = loader.loadAll(
				Arrays.asList(getInputStreamFor("foo"), getInputStreamFor("root")));
		validateFoo(repo);
		assertEquals(2, repo.getAllGroups().size());

		contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter",
				"spring.root.name", "spring.root2.name");
		assertEquals(5, repo.getAllProperties().size());
	}

	@Test
	public void severalRepositoriesIdenticalGroups() throws IOException {
		ConfigurationMetadataRepository repo = loader.loadAll(
				Arrays.asList(getInputStreamFor("foo"), getInputStreamFor("foo2")));
		assertEquals(1, repo.getAllGroups().size());
		ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
		contains(group.getSources(), "org.acme.Foo", "org.acme.Foo2", "org.springframework.boot.FooProperties");
		assertEquals(3, group.getSources().size());
		contains(group.getProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter",
				"spring.foo.enabled", "spring.foo.type");
		assertEquals(5, group.getProperties().size());
		contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter",
				"spring.foo.enabled", "spring.foo.type");
		assertEquals(5, repo.getAllProperties().size());
	}

	private void validateFoo(ConfigurationMetadataRepository repo) {
		ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
		contains(group.getSources(), "org.acme.Foo", "org.springframework.boot.FooProperties");
		ConfigurationMetadataSource source = group.getSources().get("org.acme.Foo");
		contains(source.getProperties(), "spring.foo.name", "spring.foo.description");
		assertEquals(2, source.getProperties().size());
		ConfigurationMetadataSource source2 = group.getSources().get("org.springframework.boot.FooProperties");
		contains(source2.getProperties(), "spring.foo.name", "spring.foo.counter");
		assertEquals(2, source2.getProperties().size());
	}

	private void validateBar(ConfigurationMetadataRepository repo) {
		ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.bar");
		contains(group.getSources(), "org.acme.Bar", "org.springframework.boot.BarProperties");
		ConfigurationMetadataSource source = group.getSources().get("org.acme.Bar");
		contains(source.getProperties(), "spring.bar.name", "spring.bar.description");
		assertEquals(2, source.getProperties().size());
		ConfigurationMetadataSource source2 = group.getSources().get("org.springframework.boot.BarProperties");
		contains(source2.getProperties(), "spring.bar.name", "spring.bar.counter");
		assertEquals(2, source2.getProperties().size());
	}

	private void contains(Map<String, ?> source, String... keys) {
		for (String key : keys) {
			assertTrue("Item '" + key + "' not found. Got " + source.keySet(), source.containsKey(key));
		}
	}
}
