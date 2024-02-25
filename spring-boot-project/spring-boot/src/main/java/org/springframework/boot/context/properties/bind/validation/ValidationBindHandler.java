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

package org.springframework.boot.context.properties.bind.validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.DataObjectPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.core.ResolvableType;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.AbstractBindingResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;

/**
 * {@link BindHandler} to apply {@link Validator Validators} to bound results.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ValidationBindHandler extends AbstractBindHandler {

	private final Validator[] validators;

	private final Map<ConfigurationPropertyName, ResolvableType> boundTypes = new LinkedHashMap<>();

	private final Map<ConfigurationPropertyName, Object> boundResults = new LinkedHashMap<>();

	private final Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();

	private BindValidationException exception;

	/**
	 * Constructs a new ValidationBindHandler with the specified validators.
	 * @param validators the validators to be used for validation
	 */
	public ValidationBindHandler(Validator... validators) {
		this.validators = validators;
	}

	/**
	 * Constructs a new ValidationBindHandler with the specified parent BindHandler and
	 * validators.
	 * @param parent the parent BindHandler
	 * @param validators the validators to be used for validation
	 */
	public ValidationBindHandler(BindHandler parent, Validator... validators) {
		super(parent);
		this.validators = validators;
	}

	/**
	 * Overrides the onStart method in the ValidationBindHandler class.
	 *
	 * This method is called when binding starts for a specific configuration property
	 * name and target. It stores the type of the target in the boundTypes map using the
	 * configuration property name as the key.
	 * @param name the configuration property name
	 * @param target the target to be bound
	 * @param context the bind context
	 * @return the bindable target
	 */
	@Override
	public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
		this.boundTypes.put(name, target.getType());
		return super.onStart(name, target, context);
	}

	/**
	 * This method is called when the binding process is successful. It stores the bound
	 * result in the boundResults map using the configuration property name as the key. If
	 * the bind context has a configuration property, it adds it to the boundProperties
	 * list.
	 * @param name the name of the configuration property being bound
	 * @param target the bindable target for the binding process
	 * @param context the bind context for the binding process
	 * @param result the result of the binding process
	 * @return the result of the super class's onSuccess method
	 */
	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		this.boundResults.put(name, result);
		if (context.getConfigurationProperty() != null) {
			this.boundProperties.add(context.getConfigurationProperty());
		}
		return super.onSuccess(name, target, context, result);
	}

	/**
	 * Handles the failure of binding a configuration property.
	 * @param name the name of the configuration property
	 * @param target the bindable target
	 * @param context the bind context
	 * @param error the exception that occurred during binding
	 * @return the result of the binding process
	 * @throws Exception if an error occurs during the handling of the failure
	 */
	@Override
	public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
			throws Exception {
		Object result = super.onFailure(name, target, context, error);
		if (result != null) {
			clear();
			this.boundResults.put(name, result);
		}
		validate(name, target, context, result);
		return result;
	}

	/**
	 * Clears the bound types, bound results, bound properties, and exception of the
	 * ValidationBindHandler.
	 */
	private void clear() {
		this.boundTypes.clear();
		this.boundResults.clear();
		this.boundProperties.clear();
		this.exception = null;
	}

	/**
	 * This method is called when the binding process finishes. It performs validation on
	 * the bound properties and then calls the superclass's onFinish method.
	 * @param name the name of the configuration property being bound
	 * @param target the bindable target object
	 * @param context the bind context
	 * @param result the result of the binding process
	 * @throws Exception if an error occurs during validation or when calling the
	 * superclass's onFinish method
	 */
	@Override
	public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
			throws Exception {
		validate(name, target, context, result);
		super.onFinish(name, target, context, result);
	}

	/**
	 * Validates the given configuration property name, target, context, and result.
	 * @param name the configuration property name to validate
	 * @param target the bindable target to validate
	 * @param context the bind context
	 * @param result the result object to validate
	 */
	private void validate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
		if (this.exception == null) {
			Object validationTarget = getValidationTarget(target, context, result);
			Class<?> validationType = target.getBoxedType().resolve();
			if (validationTarget != null) {
				validateAndPush(name, validationTarget, validationType);
			}
		}
		if (context.getDepth() == 0 && this.exception != null) {
			throw this.exception;
		}
	}

	/**
	 * Returns the validation target based on the provided parameters.
	 * @param target the bindable target
	 * @param context the bind context
	 * @param result the validation result
	 * @return the validation target
	 */
	private Object getValidationTarget(Bindable<?> target, BindContext context, Object result) {
		if (result != null) {
			return result;
		}
		if (context.getDepth() == 0 && target.getValue() != null) {
			return target.getValue().get();
		}
		return null;
	}

	/**
	 * Validates the given target object using the registered validators and pushes the
	 * validation result.
	 * @param name the name of the configuration property being validated
	 * @param target the target object to be validated
	 * @param type the class type of the target object
	 */
	private void validateAndPush(ConfigurationPropertyName name, Object target, Class<?> type) {
		ValidationResult result = null;
		for (Validator validator : this.validators) {
			if (validator.supports(type)) {
				result = (result != null) ? result : new ValidationResult(name, target);
				validator.validate(target, result);
			}
		}
		if (result != null && result.hasErrors()) {
			this.exception = new BindValidationException(result.getValidationErrors());
		}
	}

	/**
	 * {@link AbstractBindingResult} implementation backed by the bound properties.
	 */
	private class ValidationResult extends BeanPropertyBindingResult {

		private final ConfigurationPropertyName name;

		/**
		 * Constructs a new ValidationResult object with the specified configuration
		 * property name and target object.
		 * @param name the configuration property name
		 * @param target the target object
		 */
		protected ValidationResult(ConfigurationPropertyName name, Object target) {
			super(target, null);
			this.name = name;
		}

		/**
		 * Returns the name of the object.
		 * @return the name of the object
		 */
		@Override
		public String getObjectName() {
			return this.name.toString();
		}

		/**
		 * Returns the type of the specified field.
		 * @param field the name of the field
		 * @return the type of the field, or null if the type cannot be resolved
		 */
		@Override
		public Class<?> getFieldType(String field) {
			ResolvableType type = getBoundField(ValidationBindHandler.this.boundTypes, field);
			Class<?> resolved = (type != null) ? type.resolve() : null;
			if (resolved != null) {
				return resolved;
			}
			return super.getFieldType(field);
		}

		/**
		 * Retrieves the actual value of a field from the validation result.
		 * @param field the name of the field
		 * @return the actual value of the field
		 */
		@Override
		protected Object getActualFieldValue(String field) {
			Object boundField = getBoundField(ValidationBindHandler.this.boundResults, field);
			if (boundField != null) {
				return boundField;
			}
			try {
				return super.getActualFieldValue(field);
			}
			catch (Exception ex) {
				if (isPropertyNotReadable(ex)) {
					return null;
				}
				throw ex;
			}
		}

		/**
		 * Checks if the given Throwable is an instance of NotReadablePropertyException or
		 * any of its causes.
		 * @param ex the Throwable to check
		 * @return true if the Throwable or any of its causes is an instance of
		 * NotReadablePropertyException, false otherwise
		 */
		private boolean isPropertyNotReadable(Throwable ex) {
			while (ex != null) {
				if (ex instanceof NotReadablePropertyException) {
					return true;
				}
				ex = ex.getCause();
			}
			return false;
		}

		/**
		 * Retrieves the bound field from the given map of bound fields based on the
		 * provided field name.
		 * @param boundFields the map of bound fields
		 * @param field the field name
		 * @return the bound field if found, otherwise null
		 */
		private <T> T getBoundField(Map<ConfigurationPropertyName, T> boundFields, String field) {
			try {
				ConfigurationPropertyName name = getName(field);
				T bound = boundFields.get(name);
				if (bound != null) {
					return bound;
				}
				if (name.hasIndexedElement()) {
					for (Map.Entry<ConfigurationPropertyName, T> entry : boundFields.entrySet()) {
						if (isFieldNameMatch(entry.getKey(), name)) {
							return entry.getValue();
						}
					}
				}
			}
			catch (Exception ex) {
				// Ignore
			}
			return null;
		}

		/**
		 * Checks if the given ConfigurationPropertyName matches the given fieldName.
		 * @param name the ConfigurationPropertyName to be checked
		 * @param fieldName the ConfigurationPropertyName to be compared with
		 * @return true if the names match, false otherwise
		 */
		private boolean isFieldNameMatch(ConfigurationPropertyName name, ConfigurationPropertyName fieldName) {
			if (name.getNumberOfElements() != fieldName.getNumberOfElements()) {
				return false;
			}
			for (int i = 0; i < name.getNumberOfElements(); i++) {
				String element = name.getElement(i, Form.ORIGINAL);
				String fieldElement = fieldName.getElement(i, Form.ORIGINAL);
				if (!ObjectUtils.nullSafeEquals(element, fieldElement)) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Returns the ConfigurationPropertyName for the given field.
		 * @param field the field for which to get the ConfigurationPropertyName
		 * @return the ConfigurationPropertyName for the given field
		 */
		private ConfigurationPropertyName getName(String field) {
			return this.name.append(DataObjectPropertyName.toDashedForm(field));
		}

		/**
		 * Returns the validation errors for the current configuration property.
		 * @return the validation errors for the current configuration property
		 */
		ValidationErrors getValidationErrors() {
			Set<ConfigurationProperty> boundProperties = ValidationBindHandler.this.boundProperties.stream()
				.filter((property) -> this.name.isAncestorOf(property.getName()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
			return new ValidationErrors(this.name, boundProperties, getAllErrors());
		}

	}

}
