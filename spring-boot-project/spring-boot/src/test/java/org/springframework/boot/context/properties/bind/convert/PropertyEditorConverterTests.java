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

package org.springframework.boot.context.properties.bind.convert;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.junit.Test;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyEditorConverter}.
 *
 * @author Phillip Webb
 */
public class PropertyEditorConverterTests {

	private final PropertyEditorConverter converter = new PropertyEditorConverter();

	@Test
	public void matchesShouldLimitToPropertyEditor() {
		String converted = new SimpleTypeConverter().convertIfNecessary(123,
				String.class);
		assertThat(converted).isEqualTo("123");
		// Even though the SimpleTypeConverter can convert, we should limit to just
		// PropertyEditors not implicit support
		assertThat(this.converter.matches(TypeDescriptor.valueOf(Integer.class),
				TypeDescriptor.valueOf(String.class))).isFalse();
	}

	@Test
	public void convertShouldSupportConventionBasedEditors() throws Exception {
		String source = "org/springframework/boot/context/properties/bind/convert/resource.txt";
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = TypeDescriptor.valueOf(Resource.class);
		assertThat(this.converter.matches(sourceType, targetType)).isTrue();
		Object converted = this.converter.convert(source, sourceType, targetType);
		assertThat(converted).isNotNull().isInstanceOf(Resource.class);
		assertThat(converted.toString()).endsWith("resource.txt]");
	}

	@Test
	public void convertShouldSupportDefaultEditors() throws Exception {
		String source = "en_UK";
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = TypeDescriptor.valueOf(Locale.class);
		assertThat(this.converter.matches(sourceType, targetType)).isTrue();
		Object converted = this.converter.convert(source, sourceType, targetType);
		assertThat(converted).isNotNull().isInstanceOf(Locale.class);
		assertThat(converted.toString()).endsWith("en_UK");
	}

	@Test
	public void matchShouldNotMatchCollection() throws Exception {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		assertThat(this.converter.matches(sourceType,
				TypeDescriptor.valueOf(Collection.class))).isFalse();
		assertThat(this.converter.matches(sourceType, TypeDescriptor.valueOf(List.class)))
				.isFalse();
		assertThat(this.converter.matches(sourceType, TypeDescriptor.valueOf(Set.class)))
				.isFalse();
	}

	@Test
	public void matchShouldNotMatchMap() throws Exception {
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		assertThat(this.converter.matches(sourceType, TypeDescriptor.valueOf(Map.class)))
				.isFalse();
		assertThat(this.converter.matches(sourceType,
				TypeDescriptor.valueOf(SortedMap.class))).isFalse();
	}

}
