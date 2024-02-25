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

package org.springframework.boot.context.properties.bind.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.util.Assert;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * A collection of {@link ObjectError ObjectErrors} caused by bind validation failures.
 * Where possible, included {@link FieldError FieldErrors} will be OriginProvider.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ValidationErrors implements Iterable<ObjectError> {

	private final ConfigurationPropertyName name;

	private final Set<ConfigurationProperty> boundProperties;

	private final List<ObjectError> errors;

	/**
	 * Constructs a new instance of ValidationErrors with the specified parameters.
	 * @param name the name of the configuration property causing the validation errors
	 * (must not be null)
	 * @param boundProperties the set of bound configuration properties (must not be null)
	 * @param errors the list of object errors representing the validation errors (must
	 * not be null)
	 * @throws IllegalArgumentException if any of the parameters are null
	 */
	ValidationErrors(ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties,
			List<ObjectError> errors) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(boundProperties, "BoundProperties must not be null");
		Assert.notNull(errors, "Errors must not be null");
		this.name = name;
		this.boundProperties = Collections.unmodifiableSet(boundProperties);
		this.errors = convertErrors(name, boundProperties, errors);
	}

	/**
	 * Converts a list of ObjectErrors into a list of converted ObjectErrors.
	 * @param name the ConfigurationPropertyName object representing the name of the
	 * configuration property
	 * @param boundProperties the Set of ConfigurationProperty objects representing the
	 * bound properties
	 * @param errors the List of ObjectError objects representing the errors to be
	 * converted
	 * @return an unmodifiable List of converted ObjectError objects
	 */
	private List<ObjectError> convertErrors(ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties,
			List<ObjectError> errors) {
		List<ObjectError> converted = new ArrayList<>(errors.size());
		for (ObjectError error : errors) {
			converted.add(convertError(name, boundProperties, error));
		}
		return Collections.unmodifiableList(converted);
	}

	/**
	 * Converts the given error to an ObjectError if it is a FieldError.
	 * @param name the ConfigurationPropertyName associated with the error
	 * @param boundProperties the set of bound ConfigurationProperties
	 * @param error the error to be converted
	 * @return the converted error, or the original error if it is not a FieldError
	 */
	private ObjectError convertError(ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties,
			ObjectError error) {
		if (error instanceof FieldError fieldError) {
			return convertFieldError(name, boundProperties, fieldError);
		}
		return error;
	}

	/**
	 * Converts a {@link FieldError} to an {@link OriginTrackedFieldError} if it is not
	 * already an instance of {@link OriginProvider}.
	 * @param name the {@link ConfigurationPropertyName} associated with the error
	 * @param boundProperties the set of {@link ConfigurationProperty} objects that are
	 * bound to the error
	 * @param error the {@link FieldError} to be converted
	 * @return the converted {@link FieldError} as an {@link OriginTrackedFieldError} if
	 * necessary
	 */
	private FieldError convertFieldError(ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties,
			FieldError error) {
		if (error instanceof OriginProvider) {
			return error;
		}
		return OriginTrackedFieldError.of(error, findFieldErrorOrigin(name, boundProperties, error));
	}

	/**
	 * Finds the origin of a field error based on the given configuration property name,
	 * set of bound properties, and field error.
	 * @param name the configuration property name
	 * @param boundProperties the set of bound properties
	 * @param error the field error
	 * @return the origin of the field error, or null if not found
	 */
	private Origin findFieldErrorOrigin(ConfigurationPropertyName name, Set<ConfigurationProperty> boundProperties,
			FieldError error) {
		for (ConfigurationProperty boundProperty : boundProperties) {
			if (isForError(name, boundProperty.getName(), error)) {
				return Origin.from(boundProperty);
			}
		}
		return null;
	}

	/**
	 * Checks if the given ConfigurationPropertyName is for the specified error.
	 * @param name the ConfigurationPropertyName to check
	 * @param boundPropertyName the ConfigurationPropertyName of the bound property
	 * @param error the FieldError to compare with
	 * @return true if the given ConfigurationPropertyName is for the specified error,
	 * false otherwise
	 */
	private boolean isForError(ConfigurationPropertyName name, ConfigurationPropertyName boundPropertyName,
			FieldError error) {
		return name.isParentOf(boundPropertyName)
				&& boundPropertyName.getLastElement(Form.UNIFORM).equalsIgnoreCase(error.getField());
	}

	/**
	 * Return the name of the item that was being validated.
	 * @return the name of the item
	 */
	public ConfigurationPropertyName getName() {
		return this.name;
	}

	/**
	 * Return the properties that were bound before validation failed.
	 * @return the boundProperties
	 */
	public Set<ConfigurationProperty> getBoundProperties() {
		return this.boundProperties;
	}

	/**
	 * Checks if there are any errors in the validation errors list.
	 * @return true if there are errors, false otherwise
	 */
	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	/**
	 * Return the list of all validation errors.
	 * @return the errors
	 */
	public List<ObjectError> getAllErrors() {
		return this.errors;
	}

	/**
	 * Returns an iterator over the elements in this ValidationErrors object.
	 * @return an iterator over the elements in this ValidationErrors object
	 */
	@Override
	public Iterator<ObjectError> iterator() {
		return this.errors.iterator();
	}

}
