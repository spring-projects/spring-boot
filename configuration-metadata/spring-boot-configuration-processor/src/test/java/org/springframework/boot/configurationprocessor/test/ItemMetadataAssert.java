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

package org.springframework.boot.configurationprocessor.test;

import java.util.function.Function;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * AssertJ assert for {@link ItemMetadata}.
 *
 * @author Stephane Nicoll
 * @author Stefano Cordio
 */
public class ItemMetadataAssert extends AbstractAssert<ItemMetadataAssert, ItemMetadata>
		implements AssertProvider<ItemMetadataAssert> {

	public ItemMetadataAssert(ItemMetadata itemMetadata) {
		super(itemMetadata, ItemMetadataAssert.class);
		isNotNull();
	}

	public ItemMetadataAssert isProperty() {
		extracting((actual) -> actual.isOfItemType(ItemType.PROPERTY)).isEqualTo(true);
		return this;
	}

	public ItemMetadataAssert isGroup() {
		extracting((actual) -> actual.isOfItemType(ItemType.GROUP)).isEqualTo(true);
		return this;
	}

	public ItemMetadataAssert hasName(String name) {
		extracting(ItemMetadata::getName).isEqualTo(name);
		return this;
	}

	public ItemMetadataAssert hasType(String type) {
		extracting(ItemMetadata::getType).isEqualTo(type);
		return this;
	}

	public ItemMetadataAssert hasType(Class<?> type) {
		return hasType(type.getName());
	}

	public ItemMetadataAssert hasDescription(String description) {
		extracting(ItemMetadata::getDescription).isEqualTo(description);
		return this;
	}

	public ItemMetadataAssert hasNoDescription() {
		return hasDescription(null);
	}

	public ItemMetadataAssert hasSourceType(String type) {
		extracting(ItemMetadata::getSourceType).isEqualTo(type);
		return this;
	}

	public ItemMetadataAssert hasSourceType(Class<?> type) {
		return hasSourceType(type.getName());
	}

	public ItemMetadataAssert hasSourceMethod(String type) {
		extracting(ItemMetadata::getSourceMethod).isEqualTo(type);
		return this;
	}

	public ItemMetadataAssert hasDefaultValue(Object defaultValue) {
		extracting(ItemMetadata::getDefaultValue).isEqualTo(defaultValue);
		return this;
	}

	public ItemMetadataAssert isDeprecatedWithNoInformation() {
		assertItemDeprecation();
		return this;
	}

	public ItemMetadataAssert isDeprecatedWithReason(String reason) {
		assertItemDeprecation().extracting(ItemDeprecation::getReason).isEqualTo(reason);
		return this;
	}

	public ItemMetadataAssert isDeprecatedWithReplacement(String replacement) {
		assertItemDeprecation().extracting(ItemDeprecation::getReplacement).isEqualTo(replacement);
		return this;
	}

	public ItemMetadataAssert isNotDeprecated() {
		extracting(ItemMetadata::getDeprecation).isNull();
		return this;
	}

	private ObjectAssert<ItemDeprecation> assertItemDeprecation() {
		ObjectAssert<ItemDeprecation> itemDeprecationAssert = extracting(ItemMetadata::getDeprecation);
		itemDeprecationAssert.extracting(ItemDeprecation::getLevel).isNull();
		return itemDeprecationAssert;
	}

	private <T> ObjectAssert<T> extracting(Function<ItemMetadata, T> extractor) {
		return super.extracting(extractor, Assertions::assertThat);
	}

	@Override
	public ItemMetadataAssert assertThat() {
		return this;
	}

}
