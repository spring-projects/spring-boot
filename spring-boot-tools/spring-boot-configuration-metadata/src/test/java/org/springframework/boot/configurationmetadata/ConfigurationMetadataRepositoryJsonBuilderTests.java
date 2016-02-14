/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationmetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ConfigurationMetadataRepository}.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationMetadataRepositoryJsonBuilderTests
		extends AbstractConfigurationMetadataTests {

	@Test
	public void nullResource() throws IOException {
		this.thrown.expect(IllegalArgumentException.class);
		ConfigurationMetadataRepositoryJsonBuilder.create().withJsonResource(null);
	}

	@Test
	public void simpleRepository() throws IOException {
		InputStream foo = getInputStreamFor("foo");
		try {
			ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder
					.create(foo).build();
			validateFoo(repo);
			assertEquals(1, repo.getAllGroups().size());
			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter");
			assertEquals(3, repo.getAllProperties().size());
		}
		finally {
			foo.close();
		}
	}

	@Test
	public void severalRepositoriesNoConflict() throws IOException {
		InputStream foo = getInputStreamFor("foo");
		InputStream bar = getInputStreamFor("bar");
		try {
			ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder
					.create(foo, bar).build();
			validateFoo(repo);
			validateBar(repo);
			assertEquals(2, repo.getAllGroups().size());
			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.bar.name", "spring.bar.description",
					"spring.bar.counter");
			assertEquals(6, repo.getAllProperties().size());
		}
		finally {
			foo.close();
			bar.close();
		}
	}

	@Test
	public void repositoryWithRoot() throws IOException {
		InputStream foo = getInputStreamFor("foo");
		InputStream root = getInputStreamFor("root");
		try {
			ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder
					.create(foo, root).build();
			validateFoo(repo);
			assertEquals(2, repo.getAllGroups().size());

			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.root.name", "spring.root2.name");
			assertEquals(5, repo.getAllProperties().size());
		}
		finally {
			foo.close();
			root.close();
		}
	}

	@Test
	public void severalRepositoriesIdenticalGroups() throws IOException {
		InputStream foo = getInputStreamFor("foo");
		InputStream foo2 = getInputStreamFor("foo2");
		try {
			ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder
					.create(foo, foo2).build();
			assertEquals(1, repo.getAllGroups().size());
			ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
			contains(group.getSources(), "org.acme.Foo", "org.acme.Foo2",
					"org.springframework.boot.FooProperties");
			assertEquals(3, group.getSources().size());
			contains(group.getProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.foo.enabled", "spring.foo.type");
			assertEquals(5, group.getProperties().size());
			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.foo.enabled", "spring.foo.type");
			assertEquals(5, repo.getAllProperties().size());
		}
		finally {
			foo.close();
			foo2.close();
		}
	}

	@Test
	public void builderInstancesAreIsolated() throws IOException {
		InputStream foo = getInputStreamFor("foo");
		InputStream bar = getInputStreamFor("bar");
		try {
			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder
					.create();
			ConfigurationMetadataRepository firstRepo = builder.withJsonResource(foo)
					.build();
			validateFoo(firstRepo);
			ConfigurationMetadataRepository secondRepo = builder.withJsonResource(bar)
					.build();
			validateFoo(secondRepo);
			validateBar(secondRepo);
			// first repo not impacted by second build
			assertNotEquals(firstRepo, secondRepo);
			assertEquals(1, firstRepo.getAllGroups().size());
			assertEquals(3, firstRepo.getAllProperties().size());
			assertEquals(2, secondRepo.getAllGroups().size());
			assertEquals(6, secondRepo.getAllProperties().size());
		}
		finally {
			foo.close();
			bar.close();
		}
	}

	private void validateFoo(ConfigurationMetadataRepository repo) {
		ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
		contains(group.getSources(), "org.acme.Foo",
				"org.springframework.boot.FooProperties");
		ConfigurationMetadataSource source = group.getSources().get("org.acme.Foo");
		contains(source.getProperties(), "spring.foo.name", "spring.foo.description");
		assertEquals(2, source.getProperties().size());
		ConfigurationMetadataSource source2 = group.getSources()
				.get("org.springframework.boot.FooProperties");
		contains(source2.getProperties(), "spring.foo.name", "spring.foo.counter");
		assertEquals(2, source2.getProperties().size());
		validatePropertyHints(repo.getAllProperties().get("spring.foo.name"), 0, 0);
		validatePropertyHints(repo.getAllProperties().get("spring.foo.description"), 0,
				0);
		validatePropertyHints(repo.getAllProperties().get("spring.foo.counter"), 1, 1);
	}

	private void validateBar(ConfigurationMetadataRepository repo) {
		ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.bar");
		contains(group.getSources(), "org.acme.Bar",
				"org.springframework.boot.BarProperties");
		ConfigurationMetadataSource source = group.getSources().get("org.acme.Bar");
		contains(source.getProperties(), "spring.bar.name", "spring.bar.description");
		assertEquals(2, source.getProperties().size());
		ConfigurationMetadataSource source2 = group.getSources()
				.get("org.springframework.boot.BarProperties");
		contains(source2.getProperties(), "spring.bar.name", "spring.bar.counter");
		assertEquals(2, source2.getProperties().size());
		validatePropertyHints(repo.getAllProperties().get("spring.bar.name"), 0, 0);
		validatePropertyHints(repo.getAllProperties().get("spring.bar.description"), 2,
				2);
		validatePropertyHints(repo.getAllProperties().get("spring.bar.counter"), 0, 0);
	}

	private void validatePropertyHints(ConfigurationMetadataProperty property,
			int valueHints, int valueProviders) {
		assertEquals(valueHints, property.getValueHints().size());
		assertEquals(valueProviders, property.getValueHints().size());
	}

	private void contains(Map<String, ?> source, String... keys) {
		for (String key : keys) {
			assertTrue("Item '" + key + "' not found. Got " + source.keySet(),
					source.containsKey(key));
		}
	}

}
