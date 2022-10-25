/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.Metadata;
import org.springframework.boot.configurationprocessor.metadata.TestJsonConverter;
import org.springframework.boot.configurationsample.simple.DeprecatedSingleProperty;
import org.springframework.boot.configurationsample.simple.SimpleProperties;
import org.springframework.boot.configurationsample.specific.SimpleConflictingProperties;
import org.springframework.core.test.tools.CompilationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Metadata generation tests for merging additional metadata.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class MergeMetadataGenerationTests extends AbstractMetadataGenerationTests {

	@Test
	void mergingOfAdditionalProperty() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty(null, "foo", "java.lang.String",
				AdditionalMetadata.class.getName(), null, null, null, null);
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
		assertThat(metadata).has(Metadata.withProperty("foo", String.class).fromSource(AdditionalMetadata.class));
	}

	@Test
	void mergingOfAdditionalPropertyMatchingGroup() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty(null, "simple", "java.lang.String", null, null, null, null,
				null);
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withGroup("simple").fromSource(SimpleProperties.class));
		assertThat(metadata).has(Metadata.withProperty("simple", String.class));
	}

	@Test
	void mergeExistingPropertyDefaultValue() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("simple", "flag", null, null, null, null, true, null);
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.flag", Boolean.class).fromSource(SimpleProperties.class)
				.withDescription("A simple flag.").withDeprecation(null, null).withDefaultValue(true));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	void mergeExistingPropertyWithSeveralCandidates() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("simple", "flag", Boolean.class.getName(), null, null, null,
				true, null);
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class,
				SimpleConflictingProperties.class);
		assertThat(metadata.getItems()).hasSize(6);
		List<ItemMetadata> items = metadata.getItems().stream().filter((item) -> item.getName().equals("simple.flag"))
				.toList();
		assertThat(items).hasSize(2);
		ItemMetadata matchingProperty = items.stream().filter((item) -> item.getType().equals(Boolean.class.getName()))
				.findFirst().orElse(null);
		assertThat(matchingProperty).isNotNull();
		assertThat(matchingProperty.getDefaultValue()).isEqualTo(true);
		assertThat(matchingProperty.getSourceType()).isEqualTo(SimpleProperties.class.getName());
		assertThat(matchingProperty.getDescription()).isEqualTo("A simple flag.");
		ItemMetadata nonMatchingProperty = items.stream()
				.filter((item) -> item.getType().equals(String.class.getName())).findFirst().orElse(null);
		assertThat(nonMatchingProperty).isNotNull();
		assertThat(nonMatchingProperty.getDefaultValue()).isEqualTo("hello");
		assertThat(nonMatchingProperty.getSourceType()).isEqualTo(SimpleConflictingProperties.class.getName());
		assertThat(nonMatchingProperty.getDescription()).isNull();
	}

	@Test
	void mergeExistingPropertyDescription() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("simple", "comparator", null, null, null, "A nice comparator.",
				null, null);
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.comparator", "java.util.Comparator<?>")
				.fromSource(SimpleProperties.class).withDescription("A nice comparator."));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	void mergeExistingPropertyDeprecation() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("simple", "comparator", null, null, null, null, null,
				new ItemDeprecation("Don't use this.", "simple.complex-comparator", "error"));
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
		assertThat(metadata).has(
				Metadata.withProperty("simple.comparator", "java.util.Comparator<?>").fromSource(SimpleProperties.class)
						.withDeprecation("Don't use this.", "simple.complex-comparator", "error"));
		assertThat(metadata.getItems()).hasSize(4);
	}

	@Test
	void mergeExistingPropertyDeprecationOverride() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("singledeprecated", "name", null, null, null, null, null,
				new ItemDeprecation("Don't use this.", "single.name"));
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, DeprecatedSingleProperty.class);
		assertThat(metadata).has(Metadata.withProperty("singledeprecated.name", String.class.getName())
				.fromSource(DeprecatedSingleProperty.class).withDeprecation("Don't use this.", "single.name"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void mergeExistingPropertyDeprecationOverrideLevel() throws Exception {
		ItemMetadata property = ItemMetadata.newProperty("singledeprecated", "name", null, null, null, null, null,
				new ItemDeprecation(null, null, "error"));
		String additionalMetadata = buildAdditionalMetadata(property);
		ConfigurationMetadata metadata = compile(additionalMetadata, DeprecatedSingleProperty.class);
		assertThat(metadata).has(Metadata.withProperty("singledeprecated.name", String.class.getName())
				.fromSource(DeprecatedSingleProperty.class)
				.withDeprecation("renamed", "singledeprecated.new-name", "error"));
		assertThat(metadata.getItems()).hasSize(3);
	}

	@Test
	void mergeOfInvalidAdditionalMetadata() {
		String metadata = "Hello World";
		assertThatExceptionOfType(CompilationException.class)
				.isThrownBy(() -> compile(metadata, SimpleProperties.class))
				.withMessageContaining("Invalid additional meta-data");
	}

	@Test
	void mergingOfSimpleHint() throws Exception {
		String hints = buildAdditionalHints(ItemHint.newHint("simple.the-name",
				new ItemHint.ValueHint("boot", "Bla bla"), new ItemHint.ValueHint("spring", null)));
		ConfigurationMetadata metadata = compile(hints, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class).withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata)
				.has(Metadata.withHint("simple.the-name").withValue(0, "boot", "Bla bla").withValue(1, "spring", null));
	}

	@Test
	void mergingOfHintWithNonCanonicalName() throws Exception {
		String hints = buildAdditionalHints(
				ItemHint.newHint("simple.theName", new ItemHint.ValueHint("boot", "Bla bla")));
		ConfigurationMetadata metadata = compile(hints, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class).withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata).has(Metadata.withHint("simple.the-name").withValue(0, "boot", "Bla bla"));
	}

	@Test
	void mergingOfHintWithProvider() throws Exception {
		String hints = buildAdditionalHints(new ItemHint("simple.theName", Collections.emptyList(),
				Arrays.asList(new ItemHint.ValueProvider("first", Collections.singletonMap("target", "org.foo")),
						new ItemHint.ValueProvider("second", null))));
		ConfigurationMetadata metadata = compile(hints, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.the-name", String.class)
				.fromSource(SimpleProperties.class).withDescription("The name of this simple properties.")
				.withDefaultValue("boot").withDeprecation(null, null));
		assertThat(metadata).has(
				Metadata.withHint("simple.the-name").withProvider("first", "target", "org.foo").withProvider("second"));
	}

	@Test
	void mergingOfAdditionalDeprecation() throws Exception {
		String deprecations = buildPropertyDeprecations(ItemMetadata.newProperty("simple", "wrongName",
				"java.lang.String", null, null, null, null, new ItemDeprecation("Lame name.", "simple.the-name")));
		ConfigurationMetadata metadata = compile(deprecations, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.wrong-name", String.class).withDeprecation("Lame name.",
				"simple.the-name"));
	}

	@Test
	void mergingOfAdditionalMetadata() throws Exception {
		JSONObject property = new JSONObject();
		property.put("name", "foo");
		property.put("type", "java.lang.String");
		property.put("sourceType", AdditionalMetadata.class.getName());
		JSONArray properties = new JSONArray();
		properties.put(property);
		JSONObject json = new JSONObject();
		json.put("properties", properties);
		String additionalMetadata = json.toString();
		ConfigurationMetadata metadata = compile(additionalMetadata, SimpleProperties.class);
		assertThat(metadata).has(Metadata.withProperty("simple.comparator"));
		assertThat(metadata).has(Metadata.withProperty("foo", String.class).fromSource(AdditionalMetadata.class));
	}

	private String buildAdditionalMetadata(ItemMetadata... metadata) throws Exception {
		TestJsonConverter converter = new TestJsonConverter();
		JSONObject additionalMetadata = new JSONObject();
		JSONArray properties = new JSONArray();
		for (ItemMetadata itemMetadata : metadata) {
			properties.put(converter.toJsonObject(itemMetadata));
		}
		additionalMetadata.put("properties", properties);
		return additionalMetadata.toString();
	}

	private String buildAdditionalHints(ItemHint... hints) throws Exception {
		TestJsonConverter converter = new TestJsonConverter();
		JSONObject additionalMetadata = new JSONObject();
		additionalMetadata.put("hints", converter.toJsonArray(Arrays.asList(hints)));
		return additionalMetadata.toString();
	}

	private String buildPropertyDeprecations(ItemMetadata... items) throws Exception {
		JSONArray propertiesArray = new JSONArray();
		for (ItemMetadata item : items) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", item.getName());
			if (item.getType() != null) {
				jsonObject.put("type", item.getType());
			}
			ItemDeprecation deprecation = item.getDeprecation();
			if (deprecation != null) {
				JSONObject deprecationJson = new JSONObject();
				if (deprecation.getReason() != null) {
					deprecationJson.put("reason", deprecation.getReason());
				}
				if (deprecation.getReplacement() != null) {
					deprecationJson.put("replacement", deprecation.getReplacement());
				}
				jsonObject.put("deprecation", deprecationJson);
			}
			propertiesArray.put(jsonObject);

		}
		JSONObject additionalMetadata = new JSONObject();
		additionalMetadata.put("properties", propertiesArray);
		return additionalMetadata.toString();
	}

	static class AdditionalMetadata {

	}

}
