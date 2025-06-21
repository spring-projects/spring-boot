/*
 * Copyright 2012-present the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemHint;
import org.springframework.boot.configurationprocessor.metadata.ItemHint.ValueHint;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;
import org.springframework.boot.configurationprocessor.metadata.Metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetadataCollector}.
 *
 * @author Stephane Nicoll
 */
class MetadataCollectorTests {

	private static final Predicate<ItemMetadata> NO_MERGE = (metadata) -> false;

	private static final ConfigurationMetadata SINGLE_ITEM_METADATA = readMetadata("""
			{
				"properties": [
					{ "name": "name", "type": "java.lang.String" }
				]
			}
			""");

	@Test
	void addSingleItemMetadata() {
		MetadataCollector collector = createSimpleCollector();
		collector.add(SINGLE_ITEM_METADATA.getItems().get(0));
		assertThat(collector.getMetadata()).has(Metadata.withProperty("name", String.class));
	}

	@Test
	void addIfAbsentAddsPropertyIfItDoesNotExist() {
		MetadataCollector collector = createSimpleCollector();
		collector.addIfAbsent(SINGLE_ITEM_METADATA.getItems().get(0));
		ConfigurationMetadata metadata = collector.getMetadata();
		assertThat(metadata).has(Metadata.withProperty("name", String.class));
		assertThat(metadata.getItems()).hasSize(1);
	}

	@Test
	void addIfAbsentIgnoresExistingProperty() {
		MetadataCollector collector = createSimpleCollector();
		collector.addIfAbsent(SINGLE_ITEM_METADATA.getItems().get(0));
		collector.addIfAbsent(SINGLE_ITEM_METADATA.getItems().get(0));
		collector.addIfAbsent(SINGLE_ITEM_METADATA.getItems().get(0));
		ConfigurationMetadata metadata = collector.getMetadata();
		assertThat(metadata).has(Metadata.withProperty("name", String.class));
		assertThat(metadata.getItems()).hasSize(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	void addNewMetadataDoesNotInvokeConflictResolution() {
		MetadataCollector collector = createSimpleCollector();
		Consumer<ItemMetadata> conflictResolution = mock(Consumer.class);
		collector.add(SINGLE_ITEM_METADATA.getItems().get(0), conflictResolution);
		then(conflictResolution).shouldHaveNoInteractions();
	}

	@SuppressWarnings("unchecked")
	@Test
	void addMetadataWithExistingInstanceInvokesConflictResolution() {
		MetadataCollector collector = createSimpleCollector();
		ItemMetadata metadata = SINGLE_ITEM_METADATA.getItems().get(0);
		collector.add(metadata);
		Consumer<ItemMetadata> conflictResolution = mock(Consumer.class);
		collector.add(metadata, conflictResolution);
		then(conflictResolution).should().accept(metadata);
	}

	@Test
	void addSingleItemHint() {
		MetadataCollector collector = createSimpleCollector();
		collector.add(SINGLE_ITEM_METADATA.getItems().get(0));
		ValueHint firstValueHint = new ValueHint("one", "First.");
		ValueHint secondValueHint = new ValueHint("two", "Second.");
		ItemHint itemHint = new ItemHint("name", List.of(firstValueHint, secondValueHint), Collections.emptyList());
		collector.add(itemHint);
		assertThat(collector.getMetadata())
			.has(Metadata.withHint("name").withValue(0, "one", "First.").withValue(1, "two", "Second."));
	}

	@Test
	@SuppressWarnings("unchecked")
	void getMetadataDoesNotInvokeMergeFunctionIfPreviousMetadataIsNull() {
		Predicate<ItemMetadata> mergedRequired = mock(Predicate.class);
		MetadataCollector collector = new MetadataCollector(mergedRequired, null);
		collector.add(SINGLE_ITEM_METADATA.getItems().get(0));
		collector.getMetadata();
		then(mergedRequired).shouldHaveNoInteractions();
	}

	@Test
	@SuppressWarnings("unchecked")
	void getMetadataAddPreviousItemIfMergeFunctionReturnsTrue() {
		Predicate<ItemMetadata> mergedRequired = mock(Predicate.class);
		ItemMetadata itemMetadata = SINGLE_ITEM_METADATA.getItems().get(0);
		given(mergedRequired.test(itemMetadata)).willReturn(true);
		MetadataCollector collector = new MetadataCollector(mergedRequired, SINGLE_ITEM_METADATA);
		assertThat(collector.getMetadata()).has(Metadata.withProperty("name", String.class));
		then(mergedRequired).should().test(itemMetadata);
	}

	@Test
	@SuppressWarnings("unchecked")
	void getMetadataDoesNotAddPreviousItemIfMergeFunctionReturnsFalse() {
		Predicate<ItemMetadata> mergedRequired = mock(Predicate.class);
		ItemMetadata itemMetadata = SINGLE_ITEM_METADATA.getItems().get(0);
		given(mergedRequired.test(itemMetadata)).willReturn(false);
		MetadataCollector collector = new MetadataCollector(mergedRequired, SINGLE_ITEM_METADATA);
		assertThat(collector.getMetadata().getItems()).isEmpty();
		then(mergedRequired).should().test(itemMetadata);
	}

	private MetadataCollector createSimpleCollector() {
		return new MetadataCollector(NO_MERGE, null);
	}

	private static ConfigurationMetadata readMetadata(String json) {
		try {
			JsonMarshaller marshaller = new JsonMarshaller();
			return marshaller.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Invalid JSON: " + json, ex);
		}
	}

}
