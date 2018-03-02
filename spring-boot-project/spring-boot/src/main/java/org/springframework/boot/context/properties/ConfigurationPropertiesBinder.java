/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.PropertyEditorRegistry;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * Internal class by the {@link ConfigurationPropertiesBindingPostProcessor} to handle the
 * actual {@link ConfigurationProperties} binding.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConfigurationPropertiesBinder {

	private final ApplicationContext applicationContext;

	private final PropertySources propertySources;

	private final Validator configurationPropertiesValidator;

	private final Validator jsr303Validator;

	private volatile Binder binder;

	ConfigurationPropertiesBinder(ApplicationContext applicationContext,
			String validatorBeanName) {
		this.applicationContext = applicationContext;
		this.propertySources = new PropertySourcesDeducer(applicationContext)
				.getPropertySources();
		this.configurationPropertiesValidator = getConfigurationPropertiesValidator(
				applicationContext, validatorBeanName);
		this.jsr303Validator = ConfigurationPropertiesJsr303Validator
				.getIfJsr303Present(applicationContext);
	}

	public void bind(Bindable<?> target) {
		ConfigurationProperties annotation = target
				.getAnnotation(ConfigurationProperties.class);
		Assert.state(annotation != null, "Missing @ConfigurationProperties on " + target);
		List<Validator> validators = getValidators(target);
		BindHandler bindHandler = getBindHandler(annotation, validators);
		getBinder().bind(annotation.prefix(), target, bindHandler);
	}

	private Validator getConfigurationPropertiesValidator(
			ApplicationContext applicationContext, String validatorBeanName) {
		if (applicationContext.containsBean(validatorBeanName)) {
			return applicationContext.getBean(validatorBeanName, Validator.class);
		}
		return null;
	}

	private List<Validator> getValidators(Bindable<?> target) {
		List<Validator> validators = new ArrayList<>(3);
		if (this.configurationPropertiesValidator != null) {
			validators.add(this.configurationPropertiesValidator);
		}
		if (this.jsr303Validator != null
				&& target.getAnnotation(Validated.class) != null) {
			validators.add(this.jsr303Validator);
		}
		if (target.getValue() != null && target.getValue().get() instanceof Validator) {
			validators.add((Validator) target.getValue().get());
		}
		return validators;
	}

	private BindHandler getBindHandler(ConfigurationProperties annotation,
			List<Validator> validators) {
		BindHandler handler = BindHandler.DEFAULT;
		if (annotation.ignoreInvalidFields()) {
			handler = new IgnoreErrorsBindHandler(handler);
		}
		if (!annotation.ignoreUnknownFields()) {
			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
			handler = new NoUnboundElementsBindHandler(handler, filter);
		}
		if (!validators.isEmpty()) {
			handler = new ValidationBindHandler(handler,
					validators.toArray(new Validator[0]));
		}
		return handler;
	}

	private Binder getBinder() {
		if (this.binder == null) {
			this.binder = new Binder(getConfigurationPropertySources(),
					getPropertySourcesPlaceholdersResolver(), getConversionService(),
					getPropertyEditorInitializer());
		}
		return this.binder;
	}

	private Iterable<ConfigurationPropertySource> getConfigurationPropertySources() {
		return ConfigurationPropertySources.from(this.propertySources);
	}

	private PropertySourcesPlaceholdersResolver getPropertySourcesPlaceholdersResolver() {
		return new PropertySourcesPlaceholdersResolver(this.propertySources);
	}

	private ConversionService getConversionService() {
		return new ConversionServiceDeducer(this.applicationContext)
				.getConversionService();
	}

	private Consumer<PropertyEditorRegistry> getPropertyEditorInitializer() {
		if (this.applicationContext instanceof ConfigurableApplicationContext) {
			return ((ConfigurableApplicationContext) this.applicationContext)
					.getBeanFactory()::copyRegisteredEditorsTo;
		}
		return null;
	}

}
