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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BindHandler} to apply {@link Validator Validators} to bound results.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ValidationBindHandler extends AbstractBindHandler {

	private final Validator[] validators;

	private boolean validate;

	private final Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();

	public ValidationBindHandler(Validator... validators) {
		super();
		this.validators = validators;
	}

	public ValidationBindHandler(BindHandler parent, Validator... validators) {
		super(parent);
		this.validators = validators;
	}

	@Override
	public boolean onStart(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context) {
		if (context.getDepth() == 0) {
			this.validate = shouldValidate(target);
		}
		return super.onStart(name, target, context);
	}

	private boolean shouldValidate(Bindable<?> target) {
		Validated annotation = AnnotationUtils
				.findAnnotation(target.getBoxedType().resolve(), Validated.class);
		return (annotation != null);
	}

	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context, Object result) {
		if (context.getConfigurationProperty() != null) {
			this.boundProperties.add(context.getConfigurationProperty());
		}
		return super.onSuccess(name, target, context, result);
	}

	@Override
	public void onFinish(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context, Object result) throws Exception {
		if (this.validate) {
			validate(name, target, result);
		}
		super.onFinish(name, target, context, result);
	}

	private void validate(ConfigurationPropertyName name, Bindable<?> target,
			Object result) {
		Object validationTarget = getValidationTarget(target, result);
		Class<?> validationType = target.getBoxedType().resolve();
		validate(name, validationTarget, validationType);
	}

	private Object getValidationTarget(Bindable<?> target, Object result) {
		if (result != null) {
			return result;
		}
		if (target.getValue() != null) {
			return target.getValue().get();
		}
		return null;
	}

	private void validate(ConfigurationPropertyName name, Object target, Class<?> type) {
		if (target != null) {
			BindingResult errors = new BeanPropertyBindingResult(target, name.toString());
			Arrays.stream(this.validators).filter((v) -> v.supports(type))
					.forEach((v) -> v.validate(target, errors));
			if (errors.hasErrors()) {
				throwBindValidationException(name, errors);
			}
		}
	}

	private void throwBindValidationException(ConfigurationPropertyName name,
			BindingResult errors) {
		Set<ConfigurationProperty> boundProperties = this.boundProperties.stream()
				.filter((property) -> name.isAncestorOf(property.getName()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		ValidationErrors validationErrors = new ValidationErrors(name, boundProperties,
				errors.getAllErrors());
		throw new BindValidationException(validationErrors);
	}

}
