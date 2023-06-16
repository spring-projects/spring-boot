/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.configurationmetadata.changelog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.changelog.ConfigurationMetadataDiff.Difference;
import org.springframework.boot.configurationmetadata.changelog.ConfigurationMetadataDiff.Difference.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationMetadataDiff}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ConfigurationMetadataDiffTests {

	@Test
	void diffContainsDifferencesBetweenLeftAndRightInputs() {
		NamedConfigurationMetadataRepository left = new NamedConfigurationMetadataRepository("1.0",
				load("sample-1.0.json"));
		NamedConfigurationMetadataRepository right = new NamedConfigurationMetadataRepository("2.0",
				load("sample-2.0.json"));
		ConfigurationMetadataDiff diff = ConfigurationMetadataDiff.of(left, right);
		assertThat(diff).isNotNull();
		assertThat(diff.leftName()).isEqualTo("1.0");
		assertThat(diff.rightName()).isEqualTo("2.0");
		assertThat(diff.differences()).hasSize(4);
		List<Difference> added = diff.differences()
			.stream()
			.filter((difference) -> difference.type() == Type.ADDED)
			.collect(Collectors.toList());
		assertThat(added).hasSize(1);
		assertProperty(added.get(0).right(), "test.add", String.class, "new");
		List<Difference> deleted = diff.differences()
			.stream()
			.filter((difference) -> difference.type() == Type.DELETED)
			.collect(Collectors.toList());
		assertThat(deleted).hasSize(2)
			.anySatisfy((entry) -> assertProperty(entry.left(), "test.delete", String.class, "delete"))
			.anySatisfy((entry) -> assertProperty(entry.right(), "test.delete.deprecated", String.class, "delete"));
		List<Difference> deprecated = diff.differences()
			.stream()
			.filter((difference) -> difference.type() == Type.DEPRECATED)
			.collect(Collectors.toList());
		assertThat(deprecated).hasSize(1);
		assertProperty(deprecated.get(0).left(), "test.deprecate", String.class, "wrong");
		assertProperty(deprecated.get(0).right(), "test.deprecate", String.class, "wrong");
	}

	private void assertProperty(ConfigurationMetadataProperty property, String id, Class<?> type, Object defaultValue) {
		assertThat(property).isNotNull();
		assertThat(property.getId()).isEqualTo(id);
		assertThat(property.getType()).isEqualTo(type.getName());
		assertThat(property.getDefaultValue()).isEqualTo(defaultValue);
	}

	private ConfigurationMetadataRepository load(String filename) {
		try (InputStream inputStream = new FileInputStream("src/test/resources/" + filename)) {
			return ConfigurationMetadataRepositoryJsonBuilder.create(inputStream).build();
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
