/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import static org.assertj.core.api.Assertions.assertThat;

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
			assertThat(repo.getAllGroups()).hasSize(1);
			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter");
			assertThat(repo.getAllProperties()).hasSize(3);
		}
		finally {
			foo.close();
		}
	}

	@Test
	public void hintsOnMaps() throws IOException {
		InputStream map = getInputStreamFor("map");
		try {
			ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder
					.create(map).build();
			validateMap(repo);
			assertThat(repo.getAllGroups()).hasSize(1);
			contains(repo.getAllProperties(), "spring.map.first", "spring.map.second",
					"spring.map.keys", "spring.map.values");
			assertThat(repo.getAllProperties()).hasSize(4);
		}
		finally {
			map.close();
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
			assertThat(repo.getAllGroups()).hasSize(2);
			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.bar.name", "spring.bar.description",
					"spring.bar.counter");
			assertThat(repo.getAllProperties()).hasSize(6);
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
			assertThat(repo.getAllGroups()).hasSize(2);

			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.root.name", "spring.root2.name");
			assertThat(repo.getAllProperties()).hasSize(5);
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
			assertThat(repo.getAllGroups()).hasSize(1);
			ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
			contains(group.getSources(), "org.acme.Foo", "org.acme.Foo2",
					"org.springframework.boot.FooProperties");
			assertThat(group.getSources()).hasSize(3);
			contains(group.getProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.foo.enabled", "spring.foo.type");
			assertThat(group.getProperties()).hasSize(5);
			contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description",
					"spring.foo.counter", "spring.foo.enabled", "spring.foo.type");
			assertThat(repo.getAllProperties()).hasSize(5);
		}
		finally {
			foo.close();
			foo2.close();
		}
	}

	@Test
	public void emptyGroups() throws IOException {
		InputStream in = getInputStreamFor("empty-groups");
		try {
			ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder
					.create(in).build();
			validateEmptyGroup(repo);
			assertThat(repo.getAllGroups()).hasSize(1);
			contains(repo.getAllProperties(), "name", "title");
			assertThat(repo.getAllProperties()).hasSize(2);
		}
		finally {
			in.close();
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
			assertThat(secondRepo).isNotEqualTo(firstRepo);
			assertThat(firstRepo.getAllGroups()).hasSize(1);
			assertThat(firstRepo.getAllProperties()).hasSize(3);
			assertThat(secondRepo.getAllGroups()).hasSize(2);
			assertThat(secondRepo.getAllProperties()).hasSize(6);
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
		assertThat(source.getProperties()).hasSize(2);
		ConfigurationMetadataSource source2 = group.getSources()
				.get("org.springframework.boot.FooProperties");
		contains(source2.getProperties(), "spring.foo.name", "spring.foo.counter");
		assertThat(source2.getProperties()).hasSize(2);
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
		assertThat(source.getProperties()).hasSize(2);
		ConfigurationMetadataSource source2 = group.getSources()
				.get("org.springframework.boot.BarProperties");
		contains(source2.getProperties(), "spring.bar.name", "spring.bar.counter");
		assertThat(source2.getProperties()).hasSize(2);
		validatePropertyHints(repo.getAllProperties().get("spring.bar.name"), 0, 0);
		validatePropertyHints(repo.getAllProperties().get("spring.bar.description"), 2,
				2);
		validatePropertyHints(repo.getAllProperties().get("spring.bar.counter"), 0, 0);
	}

	private void validateMap(ConfigurationMetadataRepository repo) {
		ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.map");
		ConfigurationMetadataSource source = group.getSources().get("org.acme.Map");
		contains(source.getProperties(), "spring.map.first", "spring.map.second",
				"spring.map.keys", "spring.map.values");
		assertThat(source.getProperties()).hasSize(4);
		ConfigurationMetadataProperty first = repo.getAllProperties()
				.get("spring.map.first");
		assertThat(first.getHints().getKeyHints()).hasSize(2);
		assertThat(first.getHints().getValueProviders()).hasSize(0);
		assertThat(first.getHints().getKeyHints().get(0).getValue()).isEqualTo("one");
		assertThat(first.getHints().getKeyHints().get(0).getDescription())
				.isEqualTo("First.");
		assertThat(first.getHints().getKeyHints().get(1).getValue()).isEqualTo("two");
		assertThat(first.getHints().getKeyHints().get(1).getDescription())
				.isEqualTo("Second.");
		ConfigurationMetadataProperty second = repo.getAllProperties()
				.get("spring.map.second");
		assertThat(second.getHints().getValueHints()).hasSize(2);
		assertThat(second.getHints().getValueProviders()).hasSize(0);
		assertThat(second.getHints().getValueHints().get(0).getValue()).isEqualTo("42");
		assertThat(second.getHints().getValueHints().get(0).getDescription())
				.isEqualTo("Choose me.");
		assertThat(second.getHints().getValueHints().get(1).getValue()).isEqualTo("24");
		assertThat(second.getHints().getValueHints().get(1).getDescription()).isNull();
		ConfigurationMetadataProperty keys = repo.getAllProperties()
				.get("spring.map.keys");
		assertThat(keys.getHints().getValueHints()).hasSize(0);
		assertThat(keys.getHints().getValueProviders()).hasSize(1);
		assertThat(keys.getHints().getValueProviders().get(0).getName()).isEqualTo("any");
		ConfigurationMetadataProperty values = repo.getAllProperties()
				.get("spring.map.values");
		assertThat(values.getHints().getValueHints()).hasSize(0);
		assertThat(values.getHints().getValueProviders()).hasSize(1);
		assertThat(values.getHints().getValueProviders().get(0).getName())
				.isEqualTo("handle-as");
		assertThat(values.getHints().getValueProviders().get(0).getParameters())
				.hasSize(1);
		assertThat(values.getHints().getValueProviders().get(0).getParameters()
				.get("target")).isEqualTo("java.lang.Integer");
	}

	private void validateEmptyGroup(ConfigurationMetadataRepository repo) {
		ConfigurationMetadataGroup group = repo.getAllGroups().get("");
		contains(group.getSources(), "org.acme.Foo", "org.acme.Bar");
		ConfigurationMetadataSource source = group.getSources().get("org.acme.Foo");
		contains(source.getProperties(), "name");
		assertThat(source.getProperties()).hasSize(1);
		ConfigurationMetadataSource source2 = group.getSources().get("org.acme.Bar");
		contains(source2.getProperties(), "title");
		assertThat(source2.getProperties()).hasSize(1);
		validatePropertyHints(repo.getAllProperties().get("name"), 0, 0);
		validatePropertyHints(repo.getAllProperties().get("title"), 0, 0);
	}

	private void validatePropertyHints(ConfigurationMetadataProperty property,
			int valueHints, int valueProviders) {
		assertThat(property.getHints().getValueHints().size()).isEqualTo(valueHints);
		assertThat(property.getHints().getValueProviders().size())
				.isEqualTo(valueProviders);
	}

	private void contains(Map<String, ?> source, String... keys) {
		for (String key : keys) {
			assertThat(source).containsKey(key);
		}
	}

}
