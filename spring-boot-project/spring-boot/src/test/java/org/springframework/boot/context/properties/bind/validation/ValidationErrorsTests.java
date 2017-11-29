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

package org.springframework.boot.context.properties.bind.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.origin.MockOrigin;
import org.springframework.boot.origin.Origin;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ValidationErrors}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
public class ValidationErrorsTests {

	private static final ConfigurationPropertyName NAME = ConfigurationPropertyName
			.of("foo");

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenNameIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		new ValidationErrors(null, Collections.emptySet(), Collections.emptyList());
	}

	@Test
	public void createWhenBoundPropertiesIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("BoundProperties must not be null");
		new ValidationErrors(NAME, null, Collections.emptyList());
	}

	@Test
	public void createWhenErrorsIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Errors must not be null");
		new ValidationErrors(NAME, Collections.emptySet(), null);
	}

	@Test
	public void getNameShouldReturnName() throws Exception {
		ConfigurationPropertyName name = NAME;
		ValidationErrors errors = new ValidationErrors(name, Collections.emptySet(),
				Collections.emptyList());
		assertThat((Object) errors.getName()).isEqualTo(name);
	}

	@Test
	public void getBoundPropertiesShouldReturnBoundProperties() throws Exception {
		Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();
		boundProperties.add(new ConfigurationProperty(NAME, "foo", null));
		ValidationErrors errors = new ValidationErrors(NAME, boundProperties,
				Collections.emptyList());
		assertThat(errors.getBoundProperties()).isEqualTo(boundProperties);
	}

	@Test
	public void getErrorsShouldReturnErrors() throws Exception {
		List<ObjectError> allErrors = new ArrayList<>();
		allErrors.add(new ObjectError("foo", "bar"));
		ValidationErrors errors = new ValidationErrors(NAME, Collections.emptySet(),
				allErrors);
		assertThat(errors.getAllErrors()).isEqualTo(allErrors);
	}

	@Test
	public void iteratorShouldIterateErrors() throws Exception {
		List<ObjectError> allErrors = new ArrayList<>();
		allErrors.add(new ObjectError("foo", "bar"));
		ValidationErrors errors = new ValidationErrors(NAME, Collections.emptySet(),
				allErrors);
		assertThat(errors.iterator()).containsExactlyElementsOf(allErrors);
	}

	@Test
	public void getErrorsShouldAdaptFieldErrorsToBeOriginProviders() throws Exception {
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
