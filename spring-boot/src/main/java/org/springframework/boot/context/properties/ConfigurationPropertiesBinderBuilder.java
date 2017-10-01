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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;

/**
 * Builder for creating {@link ConfigurationPropertiesBinder} based on the state of the
 * {@link ApplicationContext}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ConfigurationPropertiesBinderBuilder {

	/**
	 * The bean name of the configuration properties validator.
	 */
	public static final String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	/**
	 * The bean name of the configuration properties conversion service.
	 */
	public static final String CONVERSION_SERVICE_BEAN_NAME = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;

	private static final String[] VALIDATOR_CLASSES = { "javax.validation.Validator",
			"javax.validation.ValidatorFactory" };

	private final ApplicationContext applicationContext;

	private ConversionService conversionService;

	private Validator validator;

	private Iterable<PropertySource<?>> propertySources;

	/**
	 * Creates an instance with the {@link ApplicationContext} to use.
	 * @param applicationContext the application context
	 */
	public ConfigurationPropertiesBinderBuilder(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		this.applicationContext = applicationContext;
	}

	/**
	 * Specify the {@link ConversionService} to use or {@code null} to use the default.
	 * <p>
	 * By default, use a {@link ConversionService} bean named
	 * {@value #CONVERSION_SERVICE_BEAN_NAME} if any. Otherwise create a
	 * {@link DefaultConversionService} with any {@link ConfigurationPropertiesBinding}
	 * qualified {@link Converter} and {@link GenericConverter} beans found in the
	 * context.
	 * @param conversionService the conversion service to use or {@code null}
	 * @return this instance
	 */
	public ConfigurationPropertiesBinderBuilder withConversionService(
			ConversionService conversionService) {
		this.conversionService = conversionService;
		return this;
	}

	/**
	 * Specify the {@link Validator} to use or {@code null} to use the default.
	 * <p>
	 * By default, use a {@link Validator} bean named {@value #VALIDATOR_BEAN_NAME} if
	 * any. If not, create a JSR 303 Validator if the necessary libraries are available.
	 * No validation occurs otherwise.
	 * @param validator the validator to use or {@code null}
	 * @return this instance
	 */
	public ConfigurationPropertiesBinderBuilder withValidator(Validator validator) {
		this.validator = validator;
		return this;
	}

	/**
	 * Specify the {@link PropertySource property sources} to use.
	 * @param propertySources the configuration the binder should use
	 * @return this instance
	 * @see #withEnvironment(ConfigurableEnvironment)
	 */
	public ConfigurationPropertiesBinderBuilder withPropertySources(
			Iterable<PropertySource<?>> propertySources) {
		this.propertySources = propertySources;
		return this;
	}

	/**
	 * Specify the {@link ConfigurableEnvironment Environment} to use, use all available
	 * {@link PropertySource}.
	 * @param environment the environment to use
	 * @return this instance
	 * @see #withPropertySources(Iterable)
	 */
	public ConfigurationPropertiesBinderBuilder withEnvironment(
			ConfigurableEnvironment environment) {
		return withPropertySources(environment.getPropertySources());
	}

	/**
	 * Build a {@link ConfigurationPropertiesBinder} based on the state of the builder,
	 * discovering the {@link ConversionService} and {@link Validator} if necessary.
	 * @return a {@link ConfigurationPropertiesBinder}
	 */
	public ConfigurationPropertiesBinder build() {
		return new ConfigurationPropertiesBinder(this.propertySources,
				determineConversionService(), determineValidator());
	}

	private Validator determineValidator() {
		if (this.validator != null) {
			return this.validator;
		}
		Validator defaultValidator = getOptionalBean(VALIDATOR_BEAN_NAME,
				Validator.class);
		if (defaultValidator != null) {
			return defaultValidator;
		}
		if (isJsr303Present()) {
			return new ValidatedLocalValidatorFactoryBean(this.applicationContext);
		}
		return null;
	}

	private ConversionService determineConversionService() {
		if (this.conversionService != null) {
			return this.conversionService;
		}
		ConversionService conversionServiceByName = getOptionalBean(
				CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
		if (conversionServiceByName != null) {
			return conversionServiceByName;
		}
		return createDefaultConversionService();
	}

	private ConversionService createDefaultConversionService() {
		ConversionServiceFactory conversionServiceFactory = this.applicationContext
				.getAutowireCapableBeanFactory()
				.createBean(ConversionServiceFactory.class);
		return conversionServiceFactory.createConversionService();
	}

	private boolean isJsr303Present() {
		for (String validatorClass : VALIDATOR_CLASSES) {
			if (!ClassUtils.isPresent(validatorClass,
					this.applicationContext.getClassLoader())) {
				return false;
			}
		}
		return true;
	}

	private <T> T getOptionalBean(String name, Class<T> type) {
		try {
			return this.applicationContext.getBean(name, type);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	private static class ConversionServiceFactory {

		private List<Converter<?, ?>> converters = Collections.emptyList();

		private List<GenericConverter> genericConverters = Collections.emptyList();

		/**
		 * A list of custom converters (in addition to the defaults) to use when
		 * converting properties for binding.
		 * @param converters the converters to set
		 */
		@Autowired(required = false)
		@ConfigurationPropertiesBinding
		public void setConverters(List<Converter<?, ?>> converters) {
			this.converters = converters;
		}

		/**
		 * A list of custom converters (in addition to the defaults) to use when
		 * converting properties for binding.
		 * @param converters the converters to set
		 */
		@Autowired(required = false)
		@ConfigurationPropertiesBinding
		public void setGenericConverters(List<GenericConverter> converters) {
			this.genericConverters = converters;
		}

		public ConversionService createConversionService() {
			DefaultConversionService conversionService = new DefaultConversionService();
			for (Converter<?, ?> converter : this.converters) {
				conversionService.addConverter(converter);
			}
			for (GenericConverter genericConverter : this.genericConverters) {
				conversionService.addConverter(genericConverter);
			}
			return conversionService;
		}

	}

}
