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

package org.springframework.boot.context.properties;

import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Bind {@link ConfigurationProperties} annotated object from a configurable list of
 * {@link PropertySource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see ConfigurationPropertiesBinderBuilder
 */
public class ConfigurationPropertiesBinder {

	private final Iterable<PropertySource<?>> propertySources;

	private final ConversionService conversionService;

	private final Validator validator;

	private Iterable<ConfigurationPropertySource> configurationSources;

	ConfigurationPropertiesBinder(Iterable<PropertySource<?>> propertySources,
			ConversionService conversionService, Validator validator) {
		Assert.notNull(propertySources, "PropertySources must not be null");
		this.propertySources = propertySources;
		this.conversionService = conversionService;
		this.validator = validator;
		this.configurationSources = ConfigurationPropertySources.from(propertySources);
	}

	/**
	 * Bind the specified {@code target} object if it is annotated with
	 * {@link ConfigurationProperties}, otherwise ignore it.
	 * @param target the target to bind the configuration property sources to
	 * @throws ConfigurationPropertiesBindingException if the binding failed
	 */
	public void bind(Object target) {
		ConfigurationProperties annotation = AnnotationUtils
				.findAnnotation(target.getClass(), ConfigurationProperties.class);
		if (annotation != null) {
			bind(target, annotation);
		}
	}

	/**
	 * Bind the specified {@code target} object using the configuration defined by the
	 * specified {@code annotation}.
	 * @param target the target to bind the configuration property sources to
	 * @param annotation the binding configuration
	 * @throws ConfigurationPropertiesBindingException if the binding failed
	 */
	void bind(Object target, ConfigurationProperties annotation) {
		Binder binder = new Binder(this.configurationSources,
				new PropertySourcesPlaceholdersResolver(this.propertySources),
				this.conversionService);
		Validator validator = determineValidator(target);
		BindHandler handler = getBindHandler(annotation, validator);
		Bindable<?> bindable = Bindable.ofInstance(target);
		try {
			binder.bind(annotation.prefix(), bindable, handler);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindingException(target.getClass(),
					getAnnotationDetails(annotation), ex);
		}
	}

	private Validator determineValidator(Object bean) {
		boolean supportsBean = (this.validator != null
				&& this.validator.supports(bean.getClass()));
		if (ClassUtils.isAssignable(Validator.class, bean.getClass())) {
			if (supportsBean) {
				return new ChainingValidator(this.validator, (Validator) bean);
			}
			return (Validator) bean;
		}
		return (supportsBean ? this.validator : null);
	}

	private BindHandler getBindHandler(ConfigurationProperties annotation,
			Validator validator) {
		BindHandler handler = BindHandler.DEFAULT;
		if (annotation.ignoreInvalidFields()) {
			handler = new IgnoreErrorsBindHandler(handler);
		}
		if (!annotation.ignoreUnknownFields()) {
			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
			handler = new NoUnboundElementsBindHandler(handler, filter);
		}
		if (validator != null) {
			handler = new ValidationBindHandler(handler, validator);
		}
		return handler;
	}

	private String getAnnotationDetails(ConfigurationProperties annotation) {
		if (annotation == null) {
			return "";
		}
		StringBuilder details = new StringBuilder();
		details.append("prefix=").append(annotation.prefix());
		details.append(", ignoreInvalidFields=").append(annotation.ignoreInvalidFields());
		details.append(", ignoreUnknownFields=").append(annotation.ignoreUnknownFields());
		return details.toString();
	}

	/**
	 * {@link Validator} implementation that wraps {@link Validator} instances and chains
	 * their execution.
	 */
	private static class ChainingValidator implements Validator {

		private final Validator[] validators;

		ChainingValidator(Validator... validators) {
			Assert.notNull(validators, "Validators must not be null");
			this.validators = validators;
		}

		@Override
		public boolean supports(Class<?> clazz) {
			for (Validator validator : this.validators) {
				if (validator.supports(clazz)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void validate(Object target, Errors errors) {
			for (Validator validator : this.validators) {
				if (validator.supports(target.getClass())) {
					validator.validate(target, errors);
				}
			}
		}

	}

}
