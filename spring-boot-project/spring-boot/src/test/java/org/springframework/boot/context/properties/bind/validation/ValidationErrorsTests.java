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

package org.springframework.boot.context.properties.bind.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.MockOrigin;
import org.springframework.boot.origin.Origin;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ValidationErrors}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class ValidationErrorsTests {

	private static final ConfigurationPropertyName NAME = ConfigurationPropertyName
			.of("foo");

	@Test
	public void createWhenNameIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ValidationErrors(null, Collections.emptySet(),
						Collections.emptyList()))
				.withMessageContaining("Name must not be null");
	}

	@Test
	public void createWhenBoundPropertiesIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new ValidationErrors(NAME, null, Collections.emptyList()))
				.withMessageContaining("BoundProperties must not be null");
	}

	@Test
	public void createWhenErrorsIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new ValidationErrors(NAME, Collections.emptySet(), null))
				.withMessageContaining("Errors must not be null");
	}

	@Test
	public void getNameShouldReturnName() {
		ConfigurationPropertyName name = NAME;
		ValidationErrors errors = new ValidationErrors(name, Collections.emptySet(),
				Collections.emptyList());
		assertThat((Object) errors.getName()).isEqualTo(name);
	}

	@Test
	public void getBoundPropertiesShouldReturnBoundProperties() {
		Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();
		boundProperties.add(new ConfigurationProperty(NAME, "foo", null));
		ValidationErrors errors = new ValidationErrors(NAME, boundProperties,
				Collections.emptyList());
		assertThat(errors.getBoundProperties()).isEqualTo(boundProperties);
	}

	@Test
	public void getErrorsShouldReturnErrors() {
		List<ObjectError> allErrors = new ArrayList<>();
		allErrors.add(new ObjectError("foo", "bar"));
		ValidationErrors errors = new ValidationErrors(NAME, Collections.emptySet(),
				allErrors);
		assertThat(errors.getAllErrors()).isEqualTo(allErrors);
	}

	@Test
	public void iteratorShouldIterateErrors() {
		List<ObjectError> allErrors = new ArrayList<>();
		allErrors.add(new ObjectError("foo", "bar"));
		ValidationErrors errors = new ValidationErrors(NAME, Collections.emptySet(),
				allErrors);
		assertThat(errors.iterator()).toIterable().containsExactlyElementsOf(allErrors);
	}

	@Test
	public void getErrorsShouldAdaptFieldErrorsToBeOriginProviders() {
		Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();
		ConfigurationPropertyName name1 = ConfigurationPropertyName.of("foo.bar");
		Origin origin1 = MockOrigin.of("line1");
		boundProperties.add(new ConfigurationProperty(name1, "boot", origin1));
		ConfigurationPropertyName name2 = ConfigurationPropertyName.of("foo.baz.bar");
		Origin origin2 = MockOrigin.of("line2");
		boundProperties.add(new ConfigurationProperty(name2, "boot", origin2));
		List<ObjectError> allErrors = new ArrayList<>();
		allErrors.add(new FieldError("objectname", "bar", "message"));
		ValidationErrors errors = new ValidationErrors(
				ConfigurationPropertyName.of("foo.baz"), boundProperties, allErrors);
		assertThat(Origin.from(errors.getAllErrors().get(0))).isEqualTo(origin2);
	}

}
