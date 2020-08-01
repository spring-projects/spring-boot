/*
 * Copyright 2012-2019 the original author or authors.
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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.internal.Objects;

import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata.ItemType;

/**
 * AssertJ assert for {@link ItemMetadata}.
 *
 * @author Stephane Nicoll
 */
public class ItemMetadataAssert extends AbstractAssert<ItemMetadataAssert, ItemMetadata>
		implements AssertProvider<ItemMetadataAssert> {

	private static final Objects objects = Objects.instance();

	public ItemMetadataAssert(ItemMetadata itemMetadata) {
		super(itemMetadata, ItemMetadataAssert.class);
		objects.assertNotNull(this.info, itemMetadata);
	}

	public ItemMetadataAssert isProperty() {
		objects.assertEqual(this.info, this.actual.isOfItemType(ItemType.PROPERTY), true);
		return this;
	}

	public ItemMetadataAssert isGroup() {
		objects.assertEqual(this.info, this.actual.isOfItemType(ItemType.GROUP), true);
		return this;
	}

	public ItemMetadataAssert hasName(String name) {
		objects.assertEqual(this.info, this.actual.getName(), name);
		return this;
	}

	public ItemMetadataAssert hasType(String type) {
		objects.assertEqual(this.info, this.actual.getType(), type);
		return this;
	}

	public ItemMetadataAssert hasType(Class<?> type) {
		return hasType(type.getName());
	}

	public ItemMetadataAssert hasDescription(String description) {
		objects.assertEqual(this.info, this.actual.getDescription(), description);
		return this;
	}

	public ItemMetadataAssert hasNoDescription() {
		return hasDescription(null);
	}

	public ItemMetadataAssert hasSourceType(String type) {
		objects.assertEqual(this.info, this.actual.getSourceType(), type);
		return this;
	}

	public ItemMetadataAssert hasSourceType(Class<?> type) {
		return hasSourceType(type.getName());
	}

	public ItemMetadataAssert hasSourceMethod(String type) {
		objects.assertEqual(this.info, this.actual.getSourceMethod(), type);
		return this;
	}

	public ItemMetadataAssert hasDefaultValue(Object defaultValue) {
		objects.assertEqual(this.info, this.actual.getDefaultValue(), defaultValue);
		return this;
	}

	public ItemMetadataAssert isDeprecatedWithNoInformation() {
		assertItemDeprecation();
		return this;
	}

	public ItemMetadataAssert isDeprecatedWithReason(String reason) {
		ItemDeprecation deprecation = assertItemDeprecation();
		objects.assertEqual(this.info, deprecation.getReason(), reason);
		return this;
	}

	public ItemMetadataAssert isDeprecatedWithReplacement(String replacement) {
		ItemDeprecation deprecation = assertItemDeprecation();
		objects.assertEqual(this.info, deprecation.getReplacement(), replacement);
		return this;
	}

	public ItemMetadataAssert isNotDeprecated() {
		objects.assertNull(this.info, this.actual.getDeprecation());
		return this;
	}

	private ItemDeprecation assertItemDeprecation() {
		ItemDeprecation deprecation = this.actual.getDeprecation();
		objects.assertNotNull(this.info, deprecation);
		objects.assertNull(this.info, deprecation.getLevel());
		return deprecation;
	}

	@Override
	public ItemMetadataAssert assertThat() {
		return this;
	}

}
